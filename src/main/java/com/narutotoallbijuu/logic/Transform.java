package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.AllBijuuConfig;
import com.narutotoallbijuu.bijuu.AbilityDef;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.Mastery;
import com.narutotoallbijuu.data.JinchuurikiData;
import com.narutotoallbijuu.entity.ChakraBlastEntity;
import com.narutotoallbijuu.net.ModNet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Transformacao, acoes do jogador, tick do hospedeiro, controle, descontrole e Bijuu Dama. */
public final class Transform {
    private Transform() {
    }

    public static final int A_UP = 0;
    public static final int A_OFF = 1;
    public static final int A_AB0 = 2;
    public static final int A_DAMA_START = 6;
    public static final int A_DAMA_RELEASE = 7;
    public static final int A_MEDITATE = 8;
    public static final int A_SYNC = 9;

    private static final int DAMA_FULL = 100;
    private static final int DAMA_MIN = 20;

    private static final UUID ID_HP = UUID.fromString("5b1f2a10-0a11-4c1e-9a10-000000000001");
    private static final UUID ID_ATK = UUID.fromString("5b1f2a10-0a11-4c1e-9a10-000000000002");
    private static final UUID ID_SPD = UUID.fromString("5b1f2a10-0a11-4c1e-9a10-000000000003");
    private static final UUID ID_ARM = UUID.fromString("5b1f2a10-0a11-4c1e-9a10-000000000004");
    private static final UUID ID_KB = UUID.fromString("5b1f2a10-0a11-4c1e-9a10-000000000005");

    private static final Map<UUID, Long> CHARGING = new HashMap<>();

    // ------------------------------------------------------------------ acoes

    public static void handleAction(ServerPlayer p, int action) {
        if (action == A_SYNC) {
            ModNet.sync(p);
            return;
        }
        JinchuurikiData d = JinchuurikiData.of(p);
        Bijuu b = d.current();
        if (b == null) {
            Msg.bar(p, "no_host");
            return;
        }
        long now = p.level().getGameTime();
        switch (action) {
            case A_UP -> up(p, d, b);
            case A_OFF -> {
                if (d.stage > 0) {
                    d.stage = 0;
                    d.rampageTicks = 0;
                    CHARGING.remove(p.getUUID());
                    d.dirty = true;
                    Msg.bar(p, "off");
                }
            }
            case A_DAMA_START -> startDama(p, d, b, now);
            case A_DAMA_RELEASE -> releaseDama(p, d, b, now);
            case A_MEDITATE -> meditate(p, d);
            default -> {
                if (action >= A_AB0 && action < A_AB0 + 4) {
                    ability(p, d, b, action - A_AB0, now);
                }
            }
        }
        if (d.dirty) {
            ModNet.sync(p);
            d.dirty = false;
        }
    }

    private static void up(ServerPlayer p, JinchuurikiData d, Bijuu b) {
        if (d.exhaustedTicks > 0) {
            Msg.bar(p, "exhausted", d.exhaustedTicks / 20);
            return;
        }
        int max = Mastery.maxStage(b, d.currentMastery());
        if (d.stage >= max) {
            Msg.bar(p, "max_stage");
            return;
        }
        if (d.chakra < d.maxChakra() * 0.05) {
            Msg.bar(p, "low_chakra");
            return;
        }
        d.stage++;
        d.meditating = false;
        d.rampageTicks = 0;
        d.dirty = true;
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.6f + 0.1f * d.stage);
        Msg.bar(p, "up", d.stage);
    }

    private static void meditate(ServerPlayer p, JinchuurikiData d) {
        if (d.stage > 0) {
            return;
        }
        d.meditating = !d.meditating;
        d.lastX = p.getX();
        d.lastY = p.getY();
        d.lastZ = p.getZ();
        d.dirty = true;
        Msg.bar(p, d.meditating ? "meditate_on" : "meditate_off");
    }

    /** Paga custo: primeiro o chakra do Bijuu, depois (se configurado) o chakra do Narutoto. */
    private static boolean pay(ServerPlayer p, JinchuurikiData d, double base) {
        double cost = base * AllBijuuConfig.CHAKRA_COST_MULTIPLIER.get();
        if (d.chakra >= cost) {
            d.chakra -= cost;
            d.dirty = true;
            return true;
        }
        if (AllBijuuConfig.USE_NARUTOTO_CHAKRA.get() && NarutotoBridge.isAvailable()) {
            double rest = cost - d.chakra;
            if (NarutotoBridge.chakra(p) >= rest) {
                d.chakra = 0;
                NarutotoBridge.addChakra(p, -rest);
                d.dirty = true;
                return true;
            }
        }
        return false;
    }

    private static double stagePower(JinchuurikiData d, Bijuu b) {
        return b.profile.atk() * (0.6 + 0.4 * d.stage / 6.0) * (0.75 + 0.25 * Math.min(1.0, d.control / 100.0));
    }

    private static void ability(ServerPlayer p, JinchuurikiData d, Bijuu b, int slot, long now) {
        AbilityDef def = b.ability(slot);
        if (d.stage < def.minStage()) {
            Msg.bar(p, "stage_required", def.minStage());
            return;
        }
        if (d.cooldowns[slot] > now) {
            Msg.bar(p, "cooldown", (d.cooldowns[slot] - now) / 20);
            return;
        }
        if (!pay(p, d, def.cost())) {
            Msg.bar(p, "no_chakra");
            return;
        }
        d.cooldowns[slot] = now + def.cooldown();
        CastCtx ctx = new CastCtx(p, (ServerLevel) p.level(), b, def.power() * stagePower(d, b), null);
        Abilities.cast(ctx, slot);
        d.addMastery(1.0 * AllBijuuConfig.MASTERY_GAIN_MULTIPLIER.get());
        d.dirty = true;
    }

    // ------------------------------------------------------------- Bijuu Dama

    private static void startDama(ServerPlayer p, JinchuurikiData d, Bijuu b, long now) {
        if (d.stage < AllBijuuConfig.DAMA_MIN_STAGE.get()) {
            Msg.bar(p, "dama_min", AllBijuuConfig.DAMA_MIN_STAGE.get());
            return;
        }
        if (d.cooldowns[JinchuurikiData.CD_DAMA] > now) {
            Msg.bar(p, "cooldown", (d.cooldowns[JinchuurikiData.CD_DAMA] - now) / 20);
            return;
        }
        if (d.chakra < b.dama.cost() * AllBijuuConfig.CHAKRA_COST_MULTIPLIER.get()) {
            Msg.bar(p, "no_chakra");
            return;
        }
        CHARGING.put(p.getUUID(), now);
    }

    private static void releaseDama(ServerPlayer p, JinchuurikiData d, Bijuu b, long now) {
        Long start = CHARGING.remove(p.getUUID());
        if (start == null) {
            return;
        }
        fireDama(p, d, b, (int) Math.min(DAMA_FULL, now - start));
    }

    private static void fireDama(ServerPlayer p, JinchuurikiData d, Bijuu b, int ticks) {
        CHARGING.remove(p.getUUID());
        if (ticks < DAMA_MIN) {
            Msg.bar(p, "dama_cancel");
            return;
        }
        long now = p.level().getGameTime();
        if (d.cooldowns[JinchuurikiData.CD_DAMA] > now || !pay(p, d, b.dama.cost())) {
            Msg.bar(p, "no_chakra");
            return;
        }
        double factor = 0.4 + 0.6 * Math.min(1.0, ticks / (double) DAMA_FULL);
        float radius = (float) Math.min(AllBijuuConfig.DAMA_MAX_RADIUS.get(), (5 + d.stage * 1.5) * factor * b.dama.power());
        CastCtx ctx = new CastCtx(p, (ServerLevel) p.level(), b, stagePower(d, b) * factor * b.dama.power(), null);
        ChakraBlastEntity e = AbilityUtil.shoot(ctx, p.getLookAngle(), 1.4, 45, radius, 1.0f + radius * 0.25f, 200);
        e.setDama(true);
        d.cooldowns[JinchuurikiData.CD_DAMA] = now + b.dama.cooldown();
        d.addMastery(5.0 * AllBijuuConfig.MASTERY_GAIN_MULTIPLIER.get());
        d.dirty = true;
        ModNet.sync(p);
    }

    private static void chargeTick(ServerPlayer p, JinchuurikiData d, Bijuu b) {
        Long start = CHARGING.get(p.getUUID());
        if (start == null) {
            return;
        }
        long t = p.level().getGameTime() - start;
        if (d.stage < AllBijuuConfig.DAMA_MIN_STAGE.get()) {
            CHARGING.remove(p.getUUID());
            return;
        }
        ServerLevel l = (ServerLevel) p.level();
        double f = Math.min(1.0, t / (double) DAMA_FULL);
        Vec3 c = p.getEyePosition().add(p.getLookAngle().scale(3.0));
        double r = 0.3 + 1.4 * f;
        l.sendParticles(AbilityUtil.dust(b.color, (float) (1.0 + 2.0 * f)), c.x, c.y, c.z, 10, r, r, r, 0.01);
        l.sendParticles(AbilityUtil.dust(b.accent, 1.2f), c.x, c.y, c.z, 6, r * 1.3, r * 1.3, r * 1.3, 0.01);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, false));
        if (t >= DAMA_FULL) {
            fireDama(p, d, b, DAMA_FULL);
        }
    }

    // ------------------------------------------------------------------- tick

    public static void tick(ServerPlayer p) {
        JinchuurikiData d = JinchuurikiData.of(p);
        Bijuu b = d.current();
        if (b == null) {
            if (d.appliedStage != -1) {
                removeModifiers(p);
                revokeFlight(p, d);
                CHARGING.remove(p.getUUID());
                d.appliedStage = -1;
            }
            return;
        }
        if (d.appliedStage != d.stage) {
            applyModifiers(p, d, b);
            d.appliedStage = d.stage;
            d.dirty = true;
        }
        if (p.tickCount % 20 == 0) {
            secondTick(p, d, b);
        }
        if (d.stage > 0 && p.tickCount % 6 == 0) {
            aura(p, d, b);
        }
        chargeTick(p, d, b);
        if (d.dirty && p.tickCount % 10 == 0) {
            ModNet.sync(p);
            d.dirty = false;
        }
    }

    public static int allowedStage(JinchuurikiData d) {
        return Math.min(Mastery.MAX_STAGE, 1 + (int) (d.control / 18.0));
    }

    private static void secondTick(ServerPlayer p, JinchuurikiData d, Bijuu b) {
        double max = d.maxChakra();
        if (d.exhaustedTicks > 0) {
            d.exhaustedTicks = Math.max(0, d.exhaustedTicks - 20);
        }
        double mult = AllBijuuConfig.MASTERY_GAIN_MULTIPLIER.get();
        if (d.stage > 0) {
            double drain = Mastery.drainPerSecond(b, d.stage, d.currentMastery(), AllBijuuConfig.CHAKRA_DRAIN_MULTIPLIER.get());
            d.chakra -= drain;
            if (AllBijuuConfig.USE_NARUTOTO_CHAKRA.get()) {
                NarutotoBridge.addChakra(p, -drain * 0.25);
            }
            if (d.chakra <= 0) {
                d.chakra = 0;
                d.stage = 0;
                d.exhaustedTicks = 300;
                CHARGING.remove(p.getUUID());
                Msg.bar(p, "no_chakra");
            } else {
                d.addMastery(0.3 * d.stage * mult);
                double cap = Mastery.controlCap(b, d.currentMastery(), d.compat);
                if (d.stage > allowedStage(d)) {
                    d.rampageTicks += 20;
                    rampage(p, d, b);
                    if (d.rampageTicks >= 400) {
                        d.stage = 0;
                        d.rampageTicks = 0;
                        d.exhaustedTicks = 400;
                        Msg.bar(p, "rampage_end");
                    }
                } else {
                    d.rampageTicks = 0;
                    if (d.control < cap) {
                        d.control = Math.min(cap, d.control + 0.3);
                    }
                }
            }
        } else {
            double reg = (1.5 + max * 0.004) * b.profile.regen() * (d.meditating ? 3 : 1);
            d.chakra = Math.min(max, d.chakra + reg);
            if (d.meditating) {
                if (p.distanceToSqr(d.lastX, d.lastY, d.lastZ) > 0.5) {
                    d.meditating = false;
                    Msg.bar(p, "meditate_break");
                } else {
                    double cap = Mastery.controlCap(b, d.currentMastery(), d.compat);
                    d.control = Math.min(cap, d.control + 1.2);
                    d.addMastery(0.6 * mult);
                    d.training += 1;
                    ((ServerLevel) p.level()).sendParticles(AbilityUtil.dust(b.color, 1.0f), p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.5, 0.8, 0.5, 0.01);
                }
            }
        }
        if (d.chakra > max) {
            d.chakra = max;
        }
        boolean wantFly = b == Bijuu.NANABI && d.stage >= 4;
        if (wantFly && !d.grantedFlight) {
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
            d.grantedFlight = true;
        } else if (!wantFly && d.grantedFlight) {
            revokeFlight(p, d);
        }
        d.dirty = true;
    }

    private static void rampage(ServerPlayer p, JinchuurikiData d, Bijuu b) {
        ServerLevel l = (ServerLevel) p.level();
        l.sendParticles(AbilityUtil.dust(0xFF0000, 2.0f), p.getX(), p.getY() + 1, p.getZ(), 30, 1.5, 1.0, 1.5, 0.02);
        for (LivingEntity e : AbilityUtil.inRadius(l, p.position(), 6, p)) {
            AbilityUtil.hurt(p, e, (float) (4 + d.stage * 2), b, true);
        }
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        p.hurt(p.damageSources().magic(), 2.0f);
        Msg.bar(p, "rampage");
    }

    private static void aura(ServerPlayer p, JinchuurikiData d, Bijuu b) {
        ServerLevel l = (ServerLevel) p.level();
        int n = 2 + d.stage * 2;
        l.sendParticles(AbilityUtil.dust(b.color, 1.0f + 0.2f * d.stage), p.getX(), p.getY() + p.getBbHeight() / 2, p.getZ(), n, 0.5, 0.9, 0.5, 0.01);
        if (d.stage >= 3) {
            l.sendParticles(BijuuEffects.trail(b), p.getX(), p.getY() + p.getBbHeight() / 2, p.getZ(), 2, 0.4, 0.8, 0.4, 0.01);
        }
        if (d.stage >= 5) {
            l.sendParticles(AbilityUtil.dust(b.accent, 1.6f), p.getX(), p.getY() + 0.2, p.getZ(), 4, 0.8, 0.2, 0.8, 0.0);
        }
    }

    // ------------------------------------------------------------- atributos

    public static void applyModifiers(ServerPlayer p, JinchuurikiData d, Bijuu b) {
        removeModifiers(p);
        double f = d.stage == 0 ? 0.1 : 0.25 + 0.15 * d.stage;
        int s = Math.max(1, d.stage);
        add(p, Attributes.MAX_HEALTH, ID_HP, "allbijuu_hp", 4.0 * f * b.profile.hp() * s, AttributeModifier.Operation.ADDITION);
        add(p, Attributes.ATTACK_DAMAGE, ID_ATK, "allbijuu_atk", 0.25 * f * b.profile.atk() * s, AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(p, Attributes.MOVEMENT_SPEED, ID_SPD, "allbijuu_spd", Math.min(0.5, 0.06 * f * b.profile.spd() * s), AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(p, Attributes.ARMOR, ID_ARM, "allbijuu_arm", 1.0 * f * b.profile.def() * s, AttributeModifier.Operation.ADDITION);
        add(p, Attributes.KNOCKBACK_RESISTANCE, ID_KB, "allbijuu_kb", Math.min(0.6, 0.08 * d.stage), AttributeModifier.Operation.ADDITION);
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }

    private static void add(ServerPlayer p, Attribute a, UUID id, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst != null) {
            inst.removeModifier(id);
            inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
        }
    }

    public static void removeModifiers(ServerPlayer p) {
        Attribute[] attrs = {Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE};
        UUID[] ids = {ID_HP, ID_ATK, ID_SPD, ID_ARM, ID_KB};
        for (int i = 0; i < attrs.length; i++) {
            AttributeInstance inst = p.getAttribute(attrs[i]);
            if (inst != null) {
                inst.removeModifier(ids[i]);
            }
        }
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }

    private static void revokeFlight(ServerPlayer p, JinchuurikiData d) {
        if (d.grantedFlight) {
            if (!p.isCreative() && !p.isSpectator()) {
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
            d.grantedFlight = false;
        }
    }
}
