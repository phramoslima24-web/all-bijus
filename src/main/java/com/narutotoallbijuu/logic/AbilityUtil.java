package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.entity.BijuuEntity;
import com.narutotoallbijuu.entity.BijuuGuardianEntity;
import com.narutotoallbijuu.entity.ChakraBlastEntity;
import com.narutotoallbijuu.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Primitivas reutilizaveis das habilidades. */
public final class AbilityUtil {
    private AbilityUtil() {
    }

    public static boolean valid(LivingEntity src, LivingEntity t) {
        if (t == src || !t.isAlive() || t.isSpectator()) {
            return false;
        }
        if (t instanceof Player tp && (tp.getAbilities().invulnerable)) {
            return false;
        }
        if (src instanceof Player sp) {
            if (t instanceof Player tp && !sp.canHarmPlayer(tp)) {
                return false;
            }
            if (t instanceof TamableAnimal ta && ta.isOwnedBy(sp)) {
                return false;
            }
            return !src.isAlliedTo(t);
        }
        if (src instanceof BijuuEntity || src instanceof BijuuGuardianEntity) {
            return !(t instanceof BijuuEntity) && !(t instanceof BijuuGuardianEntity);
        }
        return true;
    }

    public static List<LivingEntity> inRadius(ServerLevel l, Vec3 c, double r, LivingEntity src) {
        AABB box = new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box)) {
            if (valid(src, e) && e.getBoundingBox().distanceToSqr(c) <= r * r) {
                out.add(e);
            }
        }
        return out;
    }

    public static List<LivingEntity> inCone(CastCtx c, double range, double halfAngleDeg) {
        Vec3 eye = c.eye();
        Vec3 dir = c.dir();
        double cos = Math.cos(Math.toRadians(halfAngleDeg));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : inRadius(c.level, eye, range + 2, c.caster)) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double len = to.length();
            if (len < 1.0E-3) {
                out.add(e);
                continue;
            }
            if (len > range + e.getBbWidth() * 0.5) {
                continue;
            }
            if (to.normalize().dot(dir) >= cos) {
                out.add(e);
            }
        }
        return out;
    }

    public static LivingEntity rayTarget(CastCtx c, double range) {
        if (c.aim != null) {
            return c.aim.isAlive() ? c.aim : null;
        }
        Vec3 eye = c.eye();
        Vec3 end = eye.add(c.dir().scale(range));
        AABB box = c.caster.getBoundingBox().expandTowards(c.dir().scale(range)).inflate(1.0);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(c.level, c.caster, eye, end, box,
                e -> e instanceof LivingEntity le && valid(c.caster, le));
        return r != null && r.getEntity() instanceof LivingEntity le ? le : null;
    }

    public static Vec3 rayPoint(CastCtx c, double range) {
        Vec3 eye = c.eye();
        Vec3 end = eye.add(c.dir().scale(range));
        BlockHitResult hit = c.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.caster));
        return hit.getLocation();
    }

    public static void hurt(LivingEntity src, LivingEntity t, float amt, Bijuu b, boolean effects) {
        DamageSource ds;
        if (src instanceof Player p) {
            ds = src.damageSources().playerAttack(p);
        } else if (src != null) {
            ds = src.damageSources().mobAttack(src);
        } else {
            ds = t.damageSources().magic();
        }
        t.invulnerableTime = 0;
        t.hurt(ds, amt);
        if (effects) {
            BijuuEffects.apply(b, t, src, amt);
        }
    }

    public static void push(LivingEntity t, Vec3 dir, double strength, double up) {
        if (t instanceof BijuuEntity) {
            return;
        }
        Vec3 d = new Vec3(dir.x, 0, dir.z);
        d = d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize();
        t.setDeltaMovement(t.getDeltaMovement().add(d.x * strength, up, d.z * strength));
        t.hurtMarked = true;
    }

    public static void setMotion(LivingEntity e, Vec3 v) {
        e.setDeltaMovement(v);
        e.hurtMarked = true;
        e.fallDistance = 0;
    }

    public static ParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    public static void particles(ServerLevel l, ParticleOptions o, Vec3 p, int count, double dx, double dy, double dz, double speed) {
        l.sendParticles(o, p.x, p.y, p.z, count, dx, dy, dz, speed);
    }

    public static void ring(ServerLevel l, Vec3 center, double radius, int points, int rgb, float size) {
        ParticleOptions o = dust(rgb, size);
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            l.sendParticles(o, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    public static void sound(ServerLevel l, Vec3 p, SoundEvent ev, LivingEntity src, float vol, float pitch) {
        l.playSound(null, p.x, p.y, p.z, ev, src instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, vol, pitch);
    }

    /** Zona de efeito repetido. */
    public static void zone(CastCtx c, Supplier<Vec3> center, double radius, int duration, int interval,
                            BiConsumer<LivingEntity, Integer> onEntity, Consumer<Vec3> fx) {
        int pulses = Math.max(1, duration / interval);
        ServerTasks.repeat(0, interval, pulses, i -> {
            Vec3 p = center.get();
            fx.accept(p);
            for (LivingEntity e : inRadius(c.level, p, radius, c.caster)) {
                onEntity.accept(e, i);
            }
        });
    }

    public static ChakraBlastEntity shoot(CastCtx c, Vec3 dir, double speed, double damage, float radius, float size, int life) {
        ChakraBlastEntity b = new ChakraBlastEntity(ModEntities.CHAKRA_BLAST.get(), c.level);
        b.setOwner(c.caster);
        b.setup(c.bijuu, (float) c.dmg(damage), radius, size, life, false);
        Vec3 d = dir.normalize();
        Vec3 start = c.eye().add(d.scale(1.2 + c.caster.getBbWidth() * 0.3)).subtract(0, 0.25, 0);
        b.setPos(start.x, start.y, start.z);
        b.setDeltaMovement(d.scale(speed));
        c.level.addFreshEntity(b);
        return b;
    }

    public static void explosionFx(ServerLevel l, Vec3 c, float r, Bijuu b, boolean big) {
        ParticleOptions d = dust(b.color, big ? 3.0f : 1.6f);
        int n = big ? 160 : 40;
        l.sendParticles(d, c.x, c.y, c.z, n, r * 0.35, r * 0.35, r * 0.35, 0.02);
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, big ? 3 : 1, r * 0.25, r * 0.1, r * 0.25, 0);
        l.sendParticles(BijuuEffects.trail(b), c.x, c.y, c.z, big ? 120 : 30, r * 0.4, r * 0.3, r * 0.4, 0.08);
        l.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, big ? 4.0f : 1.4f, big ? 0.6f : 1.0f);
    }
}
