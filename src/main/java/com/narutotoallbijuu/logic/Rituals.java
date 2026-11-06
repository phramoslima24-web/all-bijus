package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.AllBijuuConfig;
import com.narutotoallbijuu.entity.BijuuEntity;
import com.narutotoallbijuu.registry.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Rituais com duracao (selar Bijuu enfraquecido, extrair de hospedeiro, usar receptaculo). */
public final class Rituals {
    private Rituals() {
    }

    public enum Type {SEAL_ENTITY, EXTRACT, SEAL_VESSEL}

    private static final class R {
        Type type;
        UUID target;
        long start;
        long end;
    }

    private static final Map<UUID, R> ACTIVE = new HashMap<>();

    public static void start(ServerPlayer p, Entity target, Type t, int ticks) {
        R r = new R();
        r.type = t;
        r.target = target.getUUID();
        r.start = p.level().getGameTime();
        r.end = r.start + ticks;
        ACTIVE.put(p.getUUID(), r);
        Msg.bar(p, "ritual_start", ticks / 20);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static Item itemFor(Type t) {
        return t == Type.SEAL_VESSEL ? ModItems.VESSEL.get() : ModItems.SEAL_SCROLL.get();
    }

    private static ItemStack held(ServerPlayer p, Item item) {
        if (p.getMainHandItem().is(item)) {
            return p.getMainHandItem();
        }
        if (p.getOffhandItem().is(item)) {
            return p.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    public static void tick(MinecraftServer s) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        List<UUID> ids = new ArrayList<>(ACTIVE.keySet());
        for (UUID id : ids) {
            R r = ACTIVE.get(id);
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (r == null || p == null || !p.isAlive()) {
                ACTIVE.remove(id);
                continue;
            }
            long now = p.level().getGameTime();
            Entity t = r.type == Type.SEAL_VESSEL ? p : ((ServerLevel) p.level()).getEntity(r.target);
            boolean ok = t != null && t.isAlive() && p.distanceTo(t) <= 10 && !held(p, itemFor(r.type)).isEmpty();
            if (ok && r.type == Type.SEAL_ENTITY) {
                ok = t instanceof BijuuEntity be && be.isWeak();
            }
            if (!ok) {
                ACTIVE.remove(id);
                Msg.bar(p, "ritual_cancel");
                continue;
            }
            if (now >= r.end) {
                ACTIVE.remove(id);
                complete(p, t, r);
                continue;
            }
            if ((now - r.start) % 10 == 0) {
                int pct = (int) (100 * (now - r.start) / Math.max(1, r.end - r.start));
                Msg.bar(p, "ritual_progress", pct);
                Vec3 c = t.position();
                AbilityUtil.ring((ServerLevel) p.level(), c.add(0, 0.2 + (now % 40) * 0.05, 0), 1.2 + t.getBbWidth() * 0.4, 24, 0xFFFFFF, 1.0f);
            }
        }
    }

    private static void complete(ServerPlayer p, Entity t, R r) {
        ItemStack stack = held(p, itemFor(r.type));
        if (stack.isEmpty()) {
            return;
        }
        boolean ok = false;
        switch (r.type) {
            case SEAL_ENTITY -> {
                if (t instanceof BijuuEntity be) {
                    ok = SealingManager.sealFromEntity(p, be);
                }
            }
            case EXTRACT -> {
                if (t instanceof ServerPlayer host) {
                    ok = SealingManager.extract(p, host);
                }
            }
            case SEAL_VESSEL -> ok = SealingManager.sealFromVessel(p, stack);
        }
        if (ok) {
            stack.shrink(1);
        }
    }

    public static int sealTicks() {
        return AllBijuuConfig.SEAL_TICKS.get();
    }

    public static int extractTicks() {
        return AllBijuuConfig.EXTRACTION_TICKS.get();
    }
}
