package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.AllBijuuConfig;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.BijuuState;
import com.narutotoallbijuu.data.BijuuWorldData;
import com.narutotoallbijuu.data.JinchuurikiData;
import com.narutotoallbijuu.entity.BijuuEntity;
import com.narutotoallbijuu.item.VesselItem;
import com.narutotoallbijuu.net.ModNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Selamento, extracao, receptaculos e reconciliacao de hospedeiros. */
public final class SealingManager {
    private SealingManager() {
    }

    public static int computeCompat(ServerPlayer p, Bijuu b) {
        boolean nar = NarutotoBridge.isAvailable();
        int elem = nar ? NarutotoBridge.elementBonus(p, b) : 0;
        int level = nar ? (int) Math.min(30, NarutotoBridge.ninjaLevel(p) * 0.5) : 0;
        int rnd = Math.floorMod(p.getUUID().hashCode() * 31 + b.ordinal() * 17, 16);
        return Math.min(100, 30 + elem + level + rnd);
    }

    public static void sealTo(ServerPlayer p, Bijuu b) {
        JinchuurikiData d = JinchuurikiData.of(p);
        d.clearHost();
        d.bijuu = b.ordinal();
        d.compat = computeCompat(p, b);
        d.control = 8 + d.compat * 0.2;
        d.stage = 0;
        d.chakra = d.maxChakra() * 0.5;
        d.questStage[b.ordinal()] = 4;
        d.appliedStage = -99;
        d.dirty = true;
        MinecraftServer s = p.server;
        BijuuWorldData data = BijuuWorldData.get(s);
        BijuuWorldData.Entry e = data.get(b);
        e.state = BijuuState.SEALED;
        e.host = p.getUUID();
        e.entity = null;
        e.token = null;
        e.until = 0;
        data.setDirty();
        NarutotoBridge.addNinjaExperience(p, AllBijuuConfig.QUEST_REWARD_NINJA_EXP.get() * b.tier());
        ModNet.sync(p);
        Msg.all(s, "seal_ok", p.getDisplayName(), b.displayName());
    }

    private static boolean canHost(ServerPlayer p) {
        JinchuurikiData d = JinchuurikiData.of(p);
        if (d.bijuu >= 0) {
            Msg.bar(p, "seal_fail_host");
            return false;
        }
        if (!AllBijuuConfig.ALLOW_WITH_KURAMA.get() && NarutotoBridge.isKuramaJinchuuriki(p)) {
            Msg.bar(p, "seal_fail_kurama");
            return false;
        }
        return true;
    }

    public static boolean sealFromEntity(ServerPlayer p, BijuuEntity be) {
        if (!be.isWeak() || !canHost(p)) {
            return false;
        }
        Bijuu b = be.getBijuu();
        be.sealing = true;
        be.discard();
        sealTo(p, b);
        return true;
    }

    public static boolean sealFromVessel(ServerPlayer p, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("bijuu") || !tag.hasUUID("token")) {
            Msg.bar(p, "vessel_invalid");
            return false;
        }
        Bijuu b = Bijuu.byIndex(tag.getInt("bijuu"));
        UUID token = tag.getUUID("token");
        BijuuWorldData data = BijuuWorldData.get(p.server);
        BijuuWorldData.Entry e = b == null ? null : data.get(b);
        long now = p.server.overworld().getGameTime();
        if (e == null || e.state != BijuuState.VESSEL || !token.equals(e.token) || now > e.until) {
            Msg.bar(p, "vessel_invalid");
            stack.shrink(stack.getCount());
            return false;
        }
        if (!canHost(p)) {
            return false;
        }
        sealTo(p, b);
        return true;
    }

    public static boolean extract(ServerPlayer by, ServerPlayer host) {
        JinchuurikiData hd = JinchuurikiData.of(host);
        Bijuu b = hd.current();
        if (b == null) {
            return false;
        }
        MinecraftServer s = by.server;
        BijuuWorldData data = BijuuWorldData.get(s);
        BijuuWorldData.Entry e = data.get(b);
        e.state = BijuuState.VESSEL;
        e.host = null;
        e.entity = null;
        e.token = UUID.randomUUID();
        e.until = s.overworld().getGameTime() + AllBijuuConfig.VESSEL_EXPIRY_TICKS.get();
        data.setDirty();
        hd.clearHost();
        ModNet.sync(host);
        Msg.chat(host, "extracted", b.displayName());
        ItemStack v = VesselItem.make(b, e.token);
        if (!by.getInventory().add(v)) {
            by.drop(v, false);
        }
        Msg.chat(by, "extract_ok", b.displayName());
        if (AllBijuuConfig.EXTRACTION_KILLS_HOST.get()) {
            host.kill();
        }
        return true;
    }

    /** Chamado uma vez por segundo: expira receptaculos nao usados. */
    public static void worldTick(MinecraftServer s) {
        BijuuWorldData data = BijuuWorldData.get(s);
        long now = s.overworld().getGameTime();
        boolean changed = false;
        for (Bijuu b : Bijuu.values()) {
            BijuuWorldData.Entry e = data.get(b);
            if (e.state == BijuuState.VESSEL && now > e.until) {
                e.state = BijuuState.FREE;
                e.token = null;
                e.availableAt = now;
                changed = true;
            }
        }
        if (changed) {
            data.setDirty();
        }
    }

    /** No login: alinha os dados do jogador com o registro mundial (evita duplicacao e conflito). */
    public static void reconcile(ServerPlayer p) {
        JinchuurikiData d = JinchuurikiData.of(p);
        BijuuWorldData data = BijuuWorldData.get(p.server);
        if (d.bijuu >= 0) {
            Bijuu b = d.current();
            BijuuWorldData.Entry e = b == null ? null : data.get(b);
            if (e == null) {
                d.clearHost();
            } else if (e.state == BijuuState.SEALED && p.getUUID().equals(e.host)) {
                // consistente
            } else if (e.state == BijuuState.FREE) {
                e.state = BijuuState.SEALED;
                e.host = p.getUUID();
                data.setDirty();
            } else {
                d.clearHost();
                Msg.chat(p, "host_conflict");
            }
        } else {
            for (Bijuu b : Bijuu.values()) {
                BijuuWorldData.Entry e = data.get(b);
                if (e.state == BijuuState.SEALED && p.getUUID().equals(e.host)) {
                    d.bijuu = b.ordinal();
                    d.compat = computeCompat(p, b);
                    d.control = Math.max(d.control, 8 + d.compat * 0.2);
                    d.chakra = d.maxChakra() * 0.5;
                    d.dirty = true;
                    break;
                }
            }
        }
        d.appliedStage = -99;
        ModNet.sync(p);
    }

    /** Hospedeiro morreu: o Bijuu e liberado (volta ao covil apos o tempo de respawn). */
    public static void onHostDeath(ServerPlayer p, JinchuurikiData d) {
        Bijuu b = d.current();
        if (b == null) {
            return;
        }
        BijuuWorldData data = BijuuWorldData.get(p.server);
        BijuuWorldData.Entry e = data.get(b);
        if (e.state == BijuuState.SEALED && p.getUUID().equals(e.host)) {
            e.state = BijuuState.FREE;
            e.host = null;
            e.availableAt = p.server.overworld().getGameTime() + AllBijuuConfig.BIJUU_RESPAWN_TICKS.get();
            data.setDirty();
        }
        d.clearHost();
        Msg.chat(p, "host_lost_death", b.displayName());
    }
}
