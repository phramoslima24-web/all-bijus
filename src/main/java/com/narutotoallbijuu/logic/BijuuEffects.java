package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.bijuu.Bijuu;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class BijuuEffects {
    private BijuuEffects() {
    }

    /** Efeito de status aplicado ao acertar uma habilidade. */
    public static void apply(Bijuu b, LivingEntity t, LivingEntity src, float dmg) {
        switch (b) {
            case ICHIBI -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            case NIBI -> t.setSecondsOnFire(4);
            case SANBI -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            case YONBI -> t.setSecondsOnFire(6);
            case GOBI -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            case ROKUBI -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            case NANABI -> {
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
            }
            case HACHIBI -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
    }

    public static ParticleOptions trail(Bijuu b) {
        return switch (b) {
            case ICHIBI -> ParticleTypes.CLOUD;
            case NIBI -> ParticleTypes.SOUL_FIRE_FLAME;
            case SANBI -> ParticleTypes.DRIPPING_WATER;
            case YONBI -> ParticleTypes.LAVA;
            case GOBI -> ParticleTypes.CLOUD;
            case ROKUBI -> ParticleTypes.HAPPY_VILLAGER;
            case NANABI -> ParticleTypes.WITCH;
            case HACHIBI -> ParticleTypes.LARGE_SMOKE;
        };
    }
}
