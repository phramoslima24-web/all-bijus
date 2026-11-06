package com.narutotoallbijuu;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AllBijuuConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BOSS_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CHAKRA_COST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CHAKRA_DRAIN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MASTERY_GAIN_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue USE_NARUTOTO_CHAKRA;
    public static final ForgeConfigSpec.BooleanValue ALLOW_WITH_KURAMA;
    public static final ForgeConfigSpec.BooleanValue DAMA_BLOCK_DAMAGE;
    public static final ForgeConfigSpec.IntValue DAMA_MAX_RADIUS;
    public static final ForgeConfigSpec.IntValue DAMA_CRATER_BLOCK_LIMIT;
    public static final ForgeConfigSpec.IntValue DAMA_MIN_STAGE;
    public static final ForgeConfigSpec.IntValue EXTRACTION_TICKS;
    public static final ForgeConfigSpec.BooleanValue EXTRACTION_KILLS_HOST;
    public static final ForgeConfigSpec.IntValue SEAL_TICKS;
    public static final ForgeConfigSpec.IntValue VESSEL_EXPIRY_TICKS;
    public static final ForgeConfigSpec.IntValue BIJUU_RESPAWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue BIJUU_SEALABLE_WHEN_WEAK;
    public static final ForgeConfigSpec.IntValue LAIR_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue LAIR_MAX_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue GUARDIAN_COUNT_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue QUEST_REWARD_NINJA_EXP;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("balance");
        DAMAGE_MULTIPLIER = b.comment("Multiplicador global do dano das habilidades dos jogadores.")
                .defineInRange("damageMultiplier", 1.0, 0.1, 10.0);
        BOSS_DAMAGE_MULTIPLIER = b.comment("Multiplicador do dano das habilidades dos Bijuu (entidades).")
                .defineInRange("bossDamageMultiplier", 1.0, 0.1, 10.0);
        BOSS_HEALTH_MULTIPLIER = b.comment("Multiplicador da vida dos Bijuu (aplicado ao spawn).")
                .defineInRange("bossHealthMultiplier", 1.0, 0.1, 20.0);
        CHAKRA_COST_MULTIPLIER = b.comment("Multiplicador do custo de chakra das habilidades.")
                .defineInRange("chakraCostMultiplier", 1.0, 0.0, 10.0);
        CHAKRA_DRAIN_MULTIPLIER = b.comment("Multiplicador do consumo de chakra das transformacoes.")
                .defineInRange("chakraDrainMultiplier", 1.0, 0.0, 10.0);
        MASTERY_GAIN_MULTIPLIER = b.comment("Multiplicador de ganho de dominio.")
                .defineInRange("masteryGainMultiplier", 1.0, 0.0, 50.0);
        b.pop();

        b.push("narutoto");
        USE_NARUTOTO_CHAKRA = b.comment("Se verdadeiro, usa tambem o chakra do Narutoto (se disponivel) como reserva e dreno parcial.")
                .define("useNarutotoChakra", true);
        ALLOW_WITH_KURAMA = b.comment("Permite hospedar um Bijuu deste addon sendo Jinchuuriki do Kurama (Narutoto).")
                .define("allowWithKurama", false);
        QUEST_REWARD_NINJA_EXP = b.comment("Experiencia ninja (Narutoto) por tier de Bijuu ao selar.")
                .defineInRange("questRewardNinjaExpPerTier", 100, 0, 100000);
        b.pop();

        b.push("bijuu_dama");
        DAMA_BLOCK_DAMAGE = b.comment("Se a Bijuu Dama destroi blocos (cratera).")
                .define("blockDamage", true);
        DAMA_MAX_RADIUS = b.comment("Raio maximo da explosao da Bijuu Dama.")
                .defineInRange("maxRadius", 18, 4, 48);
        DAMA_CRATER_BLOCK_LIMIT = b.comment("Limite de blocos destruidos por explosao (desempenho).")
                .defineInRange("craterBlockLimit", 2500, 0, 20000);
        DAMA_MIN_STAGE = b.comment("Estagio minimo de transformacao para carregar a Bijuu Dama.")
                .defineInRange("minStage", 5, 1, 6);
        b.pop();

        b.push("sealing");
        EXTRACTION_TICKS = b.comment("Duracao do ritual de extracao em ticks.")
                .defineInRange("extractionTicks", 300, 40, 12000);
        SEAL_TICKS = b.comment("Duracao do ritual de selamento em ticks.")
                .defineInRange("sealTicks", 200, 20, 12000);
        EXTRACTION_KILLS_HOST = b.comment("Se verdadeiro o hospedeiro morre ao ter o Bijuu extraido.")
                .define("extractionKillsHost", false);
        VESSEL_EXPIRY_TICKS = b.comment("Ticks ate um Receptaculo nao usado expirar e o Bijuu voltar a ficar livre.")
                .defineInRange("vesselExpiryTicks", 144000, 1200, 100000000);
        BIJUU_RESPAWN_TICKS = b.comment("Ticks ate um Bijuu derrotado/liberado poder ser invocado de novo.")
                .defineInRange("bijuuRespawnTicks", 24000, 0, 100000000);
        BIJUU_SEALABLE_WHEN_WEAK = b.comment("Se verdadeiro, o Bijuu fica enfraquecido (e selavel) em 25% da vida em vez de morrer.")
                .define("sealableWhenWeak", true);
        b.pop();

        b.push("world");
        LAIR_MIN_DISTANCE = b.comment("Distancia minima do covil ao spawn do mundo.")
                .defineInRange("lairMinDistance", 1500, 200, 30000);
        LAIR_MAX_DISTANCE = b.comment("Distancia maxima do covil ao spawn do mundo.")
                .defineInRange("lairMaxDistance", 3500, 300, 30000);
        GUARDIAN_COUNT_MULTIPLIER = b.comment("Multiplicador da quantidade de guardioes por covil.")
                .defineInRange("guardianCountMultiplier", 1.0, 0.1, 10.0);
        b.pop();

        SPEC = b.build();
    }

    private AllBijuuConfig() {
    }
}
