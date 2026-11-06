package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.data.JinchuurikiData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** As 32 habilidades ativas (8 Bijuu x 4 slots), descritas por arquetipos parametrizados. */
public final class Abilities {
    private Abilities() {
    }

    public enum T {PROJ, CONE, AOE, DASH, ZONE, SHIELD, BARRAGE, FLIGHT}

    /**
     * dmg: dano base; range: PROJ/BARRAGE=velocidade, CONE/ZONE=alcance, DASH/FLIGHT=velocidade;
     * radius: PROJ/BARRAGE=raio da explosao, CONE=meio-angulo, AOE/ZONE/DASH/FLIGHT=raio;
     * dur: PROJ=vida, CONE=pulsos, DASH/ZONE/SHIELD/FLIGHT=ticks, BARRAGE=quantidade; kb: empurrao.
     */
    public record Spec(T t, double dmg, double range, double radius, int dur, double kb) {
    }

    private static Spec s(T t, double dmg, double range, double radius, int dur, double kb) {
        return new Spec(t, dmg, range, radius, dur, kb);
    }

    public static final Spec[][] SPECS = {
            // ICHIBI
            {s(T.SHIELD, 0, 0, 0, 160, 0), s(T.ZONE, 3, 24, 3, 60, 0), s(T.ZONE, 14, 30, 9, 40, 0), s(T.CONE, 10, 14, 35, 1, 2.0)},
            // NIBI
            {s(T.CONE, 6, 10, 25, 3, 0.6), s(T.DASH, 8, 1.6, 2.5, 10, 1.0), s(T.ZONE, 5, 24, 7, 80, 0), s(T.AOE, 16, 0, 8, 0, 1.6)},
            // SANBI
            {s(T.DASH, 6, 1.3, 3, 20, 1.0), s(T.PROJ, 12, 1.6, 3, 60, 0), s(T.ZONE, 3, 20, 8, 100, 0), s(T.CONE, 14, 18, 45, 1, 3.0)},
            // YONBI
            {s(T.PROJ, 14, 1.4, 3.5, 60, 0), s(T.CONE, 20, 7, 50, 1, 1.8), s(T.ZONE, 6, 20, 6, 100, 0), s(T.AOE, 22, 0, 10, 0, 2.0)},
            // GOBI
            {s(T.CONE, 7, 12, 30, 3, 0.5), s(T.DASH, 9, 1.8, 2.5, 12, 1.0), s(T.ZONE, 3, 22, 8, 100, 0), s(T.AOE, 20, 0, 9, 0, 1.8)},
            // ROKUBI
            {s(T.PROJ, 9, 1.2, 3, 70, 0), s(T.ZONE, 3, 20, 7, 120, 0), s(T.SHIELD, 0, 0, 0, 180, 0), s(T.BARRAGE, 6, 1.3, 2.2, 6, 0)},
            // NANABI
            {s(T.CONE, 5, 12, 40, 3, 0.3), s(T.FLIGHT, 6, 1.4, 4, 100, 1.0), s(T.BARRAGE, 5, 1.5, 2, 8, 0), s(T.ZONE, 4, 24, 9, 120, 0)},
            // HACHIBI
            {s(T.CONE, 5, 8, 40, 4, 0.4), s(T.DASH, 12, 1.7, 3, 12, 1.0), s(T.ZONE, 3, 22, 8, 100, 0), s(T.AOE, 26, 0, 10, 0, 2.2)},
    };

    public static Spec spec(Bijuu b, int slot) {
        return SPECS[b.ordinal()][slot];
    }

    /** Distancia maxima ao alvo para a IA usar a habilidade. */
    public static double aiRange(Bijuu b, int slot) {
        Spec sp = spec(b, slot);
        return switch (sp.t()) {
            case PROJ -> 28;
            case CONE -> sp.range();
            case AOE -> sp.radius();
            case DASH -> 14;
            case ZONE -> sp.range();
            case SHIELD -> 30;
            case BARRAGE -> 24;
            case FLIGHT -> 16;
        };
    }

    public static void cast(CastCtx c, int slot) {
        Spec sp = spec(c.bijuu, slot);
        switch (sp.t()) {
            case PROJ -> AbilityUtil.shoot(c, c.dir(), sp.range(), sp.dmg(), (float) sp.radius(), 1.2f, sp.dur());
            case CONE -> cone(c, sp);
            case AOE -> aoe(c, sp);
            case DASH -> dash(c, sp);
            case ZONE -> zone(c, sp);
            case SHIELD -> shield(c, sp);
            case BARRAGE -> barrage(c, sp);
            case FLIGHT -> flight(c, sp);
        }
    }

    private static void cone(CastCtx c, Spec sp) {
        Bijuu b = c.bijuu;
        int pulses = Math.max(1, sp.dur());
        ServerTasks.repeat(0, 4, pulses, i -> {
            if (!c.caster.isAlive()) {
                return;
            }
            Vec3 eye = c.eye();
            Vec3 dir = c.dir();
            double spread = Math.tan(Math.toRadians(sp.radius()));
            RandomSource r = c.level.random;
            for (int k = 0; k < 30; k++) {
                double dist = 1 + r.nextDouble() * sp.range();
                Vec3 d = dir.add((r.nextDouble() - 0.5) * spread * 1.6, (r.nextDouble() - 0.5) * spread * 1.2,
                        (r.nextDouble() - 0.5) * spread * 1.6).normalize();
                Vec3 p = eye.add(d.scale(dist));
                AbilityUtil.particles(c.level, k % 2 == 0 ? AbilityUtil.dust(b.color, 1.6f) : BijuuEffects.trail(b), p, 1, 0.1, 0.1, 0.1, 0.01);
            }
            AbilityUtil.sound(c.level, c.caster.position(), SoundEvents.BLAZE_SHOOT, c.caster, 1.0f, 0.7f);
            for (LivingEntity e : AbilityUtil.inCone(c, sp.range(), sp.radius())) {
                AbilityUtil.hurt(c.caster, e, (float) c.dmg(sp.dmg()), b, true);
                AbilityUtil.push(e, e.position().subtract(c.caster.position()), sp.kb(), 0.2);
            }
        });
    }

    private static void aoe(CastCtx c, Spec sp) {
        Bijuu b = c.bijuu;
        Vec3 p = c.caster.position();
        for (int k = 1; k <= 4; k++) {
            AbilityUtil.ring(c.level, p.add(0, 0.3 * k, 0), sp.radius() * k / 4.0, 24 + 8 * k, b.color, 1.8f);
        }
        AbilityUtil.explosionFx(c.level, p.add(0, 1, 0), (float) sp.radius(), b, false);
        for (LivingEntity e : AbilityUtil.inRadius(c.level, p, sp.radius(), c.caster)) {
            AbilityUtil.hurt(c.caster, e, (float) c.dmg(sp.dmg()), b, true);
            AbilityUtil.push(e, e.position().subtract(p), sp.kb(), 0.45);
        }
    }

    private static void dash(CastCtx c, Spec sp) {
        Bijuu b = c.bijuu;
        Vec3 d = c.flatDir();
        double speed = sp.range();
        AbilityUtil.setMotion(c.caster, new Vec3(d.x * speed, 0.2, d.z * speed));
        Set<UUID> hit = new HashSet<>();
        int steps = Math.max(1, sp.dur() / 2);
        ServerTasks.repeat(1, 2, steps, i -> {
            if (!c.caster.isAlive()) {
                return;
            }
            Vec3 m = c.caster.getDeltaMovement();
            if (i < steps - 1) {
                AbilityUtil.setMotion(c.caster, new Vec3(d.x * speed, Math.max(m.y, -0.1), d.z * speed));
            }
            Vec3 mid = c.caster.position().add(0, c.caster.getBbHeight() / 2, 0);
            AbilityUtil.particles(c.level, BijuuEffects.trail(b), mid, 8, 0.6, 0.6, 0.6, 0.02);
            AbilityUtil.particles(c.level, AbilityUtil.dust(b.color, 1.8f), mid, 8, 0.6, 0.6, 0.6, 0.02);
            for (LivingEntity e : AbilityUtil.inRadius(c.level, mid, sp.radius(), c.caster)) {
                if (hit.add(e.getUUID())) {
                    AbilityUtil.hurt(c.caster, e, (float) c.dmg(sp.dmg()), b, true);
                    AbilityUtil.push(e, d, sp.kb(), 0.35);
                }
            }
        });
    }

    private static void zone(CastCtx c, Spec sp) {
        Bijuu b = c.bijuu;
        Vec3 center = c.aim != null ? c.aim.position() : AbilityUtil.rayPoint(c, sp.range());
        AbilityUtil.zone(c, () -> center, sp.radius(), sp.dur(), 10,
                (e, i) -> AbilityUtil.hurt(c.caster, e, (float) c.dmg(sp.dmg()), b, true),
                p -> {
                    AbilityUtil.ring(c.level, p.add(0, 0.2, 0), sp.radius(), (int) (sp.radius() * 8), b.color, 1.4f);
                    AbilityUtil.particles(c.level, BijuuEffects.trail(b), p.add(0, 0.5, 0), 20, sp.radius() * 0.5, 0.6, sp.radius() * 0.5, 0.02);
                });
    }

    private static void shield(CastCtx c, Spec sp) {
        Bijuu b = c.bijuu;
        if (c.caster instanceof ServerPlayer p) {
            JinchuurikiData d = JinchuurikiData.of(p);
            d.shellUntil = c.level.getGameTime() + sp.dur();
            d.dirty = true;
        } else {
            c.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, sp.dur(), 2));
        }
        ServerTasks.repeat(0, 10, Math.max(1, sp.dur() / 10), i -> {
            if (c.caster.isAlive()) {
                AbilityUtil.ring(c.level, c.caster.position().add(0, 0.2 + 0.4 * (i % 4), 0), 1.6, 20, b.color, 1.2f);
            }
        });
    }

    private static void barrage(CastCtx c, Spec sp) {
        ServerTasks.repeat(0, 4, Math.max(1, sp.dur()), i -> {
            if (!c.caster.isAlive()) {
                return;
            }
            RandomSource r = c.level.random;
            Vec3 d = c.dir().add((r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.2 + 0.05, (r.nextDouble() - 0.5) * 0.3);
            AbilityUtil.shoot(c, d, sp.range(), sp.dmg(), (float) sp.radius(), 0.7f, 60);
        });
    }

    private static void flight(CastCtx c, Spec sp) {
        Bijuu b = c.bijuu;
        Vec3 d = c.flatDir();
        AbilityUtil.setMotion(c.caster, new Vec3(d.x * sp.range() * 0.8, 1.1, d.z * sp.range() * 0.8));
        c.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, sp.dur(), 0, false, false));
        Vec3 p = c.caster.position();
        AbilityUtil.ring(c.level, p.add(0, 0.3, 0), sp.radius(), 30, b.color, 1.6f);
        for (LivingEntity e : AbilityUtil.inRadius(c.level, p, sp.radius(), c.caster)) {
            AbilityUtil.hurt(c.caster, e, (float) c.dmg(sp.dmg()), b, true);
            AbilityUtil.push(e, e.position().subtract(p), sp.kb(), 0.5);
        }
    }
}
