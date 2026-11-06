package com.narutotoallbijuu.data;

import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.Mastery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Dados do jogador: Bijuu hospedado, dominio, controle, chakra, cooldowns e progresso de missoes. */
public class JinchuurikiData {
    public static final int N = 8;
    public static final int CD_DAMA = 4;

    public int bijuu = -1;
    public double[] mastery = new double[N];
    public double control;
    public double chakra;
    public int compat;
    public int stage;
    public int rampageTicks;
    public int exhaustedTicks;
    /** Indices 0-3 habilidades, 4 Bijuu Dama. */
    public long[] cooldowns = new long[N];
    public boolean meditating;
    public double training;
    public int[] questStage = new int[N];
    public int[] questProgress = new int[N];
    public int tracked;
    public long shellUntil;
    public long autoDefenseReady;
    public boolean grantedFlight;

    public transient boolean dirty = true;
    public transient int appliedStage = -99;
    public transient double lastX, lastY, lastZ;

    public static JinchuurikiData of(Player p) {
        return p.getCapability(ModCaps.JINCHUURIKI).resolve().orElseGet(JinchuurikiData::new);
    }

    public Bijuu current() {
        return Bijuu.byIndex(bijuu);
    }

    public double currentMastery() {
        return bijuu >= 0 ? mastery[bijuu] : 0;
    }

    public double maxChakra() {
        Bijuu b = current();
        return b == null ? 0 : Mastery.maxChakra(b, currentMastery());
    }

    public void addMastery(double amount) {
        if (bijuu >= 0 && amount > 0) {
            mastery[bijuu] += amount;
            dirty = true;
        }
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("bijuu", bijuu);
        t.putDouble("control", control);
        t.putDouble("chakra", chakra);
        t.putInt("compat", compat);
        t.putInt("stage", stage);
        t.putInt("rampage", rampageTicks);
        t.putInt("exhausted", exhaustedTicks);
        t.putBoolean("meditating", meditating);
        t.putDouble("training", training);
        t.putInt("tracked", tracked);
        t.putLong("shellUntil", shellUntil);
        t.putLong("autoDefenseReady", autoDefenseReady);
        t.putBoolean("grantedFlight", grantedFlight);
        for (int i = 0; i < N; i++) {
            t.putDouble("m" + i, mastery[i]);
            t.putLong("cd" + i, cooldowns[i]);
            t.putInt("qs" + i, questStage[i]);
            t.putInt("qp" + i, questProgress[i]);
        }
        return t;
    }

    public void load(CompoundTag t) {
        bijuu = t.contains("bijuu") ? t.getInt("bijuu") : -1;
        control = t.getDouble("control");
        chakra = t.getDouble("chakra");
        compat = t.getInt("compat");
        stage = t.getInt("stage");
        rampageTicks = t.getInt("rampage");
        exhaustedTicks = t.getInt("exhausted");
        meditating = t.getBoolean("meditating");
        training = t.getDouble("training");
        tracked = t.getInt("tracked");
        shellUntil = t.getLong("shellUntil");
        autoDefenseReady = t.getLong("autoDefenseReady");
        grantedFlight = t.getBoolean("grantedFlight");
        for (int i = 0; i < N; i++) {
            mastery[i] = t.getDouble("m" + i);
            cooldowns[i] = t.getLong("cd" + i);
            questStage[i] = t.getInt("qs" + i);
            questProgress[i] = t.getInt("qp" + i);
        }
        dirty = true;
    }

    public void copyFrom(JinchuurikiData o) {
        load(o.save());
    }

    /** Remove o Bijuu mantendo dominio e progresso de missoes. */
    public void clearHost() {
        bijuu = -1;
        stage = 0;
        control = 0;
        chakra = 0;
        compat = 0;
        rampageTicks = 0;
        exhaustedTicks = 0;
        meditating = false;
        shellUntil = 0;
        dirty = true;
    }
}
