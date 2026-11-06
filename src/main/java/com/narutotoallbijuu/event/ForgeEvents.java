package com.narutotoallbijuu.event;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.data.JinchuurikiData;
import com.narutotoallbijuu.data.ModCaps;
import com.narutotoallbijuu.logic.AbilityUtil;
import com.narutotoallbijuu.logic.QuestManager;
import com.narutotoallbijuu.logic.Rituals;
import com.narutotoallbijuu.logic.SealingManager;
import com.narutotoallbijuu.logic.ServerTasks;
import com.narutotoallbijuu.logic.Transform;
import com.narutotoallbijuu.net.ModNet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AllBijuuMod.MODID)
public final class ForgeEvents {
    private ForgeEvents() {
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof Player) {
            e.addCapability(ModCaps.KEY, new ModCaps.Provider());
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        JinchuurikiData old = JinchuurikiData.of(e.getOriginal());
        JinchuurikiData now = JinchuurikiData.of(e.getEntity());
        now.copyFrom(old);
        now.appliedStage = -99;
        e.getOriginal().invalidateCaps();
        if (e.isWasDeath() && e.getEntity() instanceof ServerPlayer sp && now.bijuu >= 0) {
            SealingManager.onHostDeath(sp, now);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            SealingManager.reconcile(sp);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            ModNet.sync(sp);
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            ModNet.sync(sp);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer sp) {
            Transform.tick(sp);
            if (sp.tickCount % 40 == 0) {
                QuestManager.tick(sp, JinchuurikiData.of(sp));
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        ServerTasks.tick();
        Rituals.tick(e.getServer());
        if (e.getServer().getTickCount() % 20 == 0) {
            SealingManager.worldTick(e.getServer());
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        ServerTasks.clear();
        Rituals.clear();
    }

    /** Defesa automatica de areia do Shukaku. */
    @SubscribeEvent
    public static void attacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() < 1.0f) {
            return;
        }
        JinchuurikiData d = JinchuurikiData.of(p);
        long now = p.level().getGameTime();
        if (d.bijuu == Bijuu.ICHIBI.ordinal() && d.stage >= 1 && d.autoDefenseReady <= now
                && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            e.setCanceled(true);
            d.autoDefenseReady = now + Math.max(40, 200 - (long) (d.currentMastery() / 50));
            d.dirty = true;
            AbilityUtil.particles((ServerLevel) p.level(), AbilityUtil.dust(Bijuu.ICHIBI.color, 2.0f),
                    p.position().add(0, 1, 0), 40, 0.8, 1.0, 0.8, 0.02);
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            JinchuurikiData d = JinchuurikiData.of(p);
            if (d.shellUntil > p.level().getGameTime()) {
                e.setAmount(e.getAmount() * 0.4f);
            }
        }
    }
}
