package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.AllBijuuConfig;
import com.narutotoallbijuu.bijuu.Bijuu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Contexto de uma habilidade: quem lanca (jogador ou Bijuu), poder e alvo opcional (IA). */
public final class CastCtx {
    public final LivingEntity caster;
    public final ServerLevel level;
    public final Bijuu bijuu;
    public final double power;
    public final LivingEntity aim;

    public CastCtx(LivingEntity caster, ServerLevel level, Bijuu bijuu, double power, LivingEntity aim) {
        this.caster = caster;
        this.level = level;
        this.bijuu = bijuu;
        this.power = power;
        this.aim = aim;
    }

    public Vec3 eye() {
        return caster.getEyePosition();
    }

    public Vec3 dir() {
        if (aim != null) {
            Vec3 d = aim.position().add(0, aim.getBbHeight() * 0.5, 0).subtract(eye());
            if (d.lengthSqr() > 1.0E-4) {
                return d.normalize();
            }
        }
        return caster.getLookAngle();
    }

    public Vec3 flatDir() {
        Vec3 d = dir();
        Vec3 f = new Vec3(d.x, 0, d.z);
        return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    public double dmg(double base) {
        double m = caster instanceof Player ? AllBijuuConfig.DAMAGE_MULTIPLIER.get() : AllBijuuConfig.BOSS_DAMAGE_MULTIPLIER.get();
        return base * power * m;
    }
}
