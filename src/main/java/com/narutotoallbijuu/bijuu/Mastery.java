package com.narutotoallbijuu.bijuu;

/** Formulas de dominio, chakra e estagios de transformacao. */
public final class Mastery {
    private Mastery() {
    }

    public static final int MAX_STAGE = 6;
    private static final double[] BASE = {0, 0, 250, 1000, 3000, 7000, 15000};
    private static final double[] DRAIN = {0, 2, 6, 11, 18, 28, 45};

    public static double required(Bijuu b, int stage) {
        if (stage <= 1) {
            return 0;
        }
        return BASE[stage] * (0.7 + 0.15 * b.tier());
    }

    public static int maxStage(Bijuu b, double mastery) {
        int s = 1;
        for (int i = 2; i <= MAX_STAGE; i++) {
            if (mastery >= required(b, i)) {
                s = i;
            }
        }
        return s;
    }

    public static double progressToNext(Bijuu b, double mastery) {
        int s = maxStage(b, mastery);
        if (s >= MAX_STAGE) {
            return 1.0;
        }
        double lo = required(b, s);
        double hi = required(b, s + 1);
        return Math.max(0, Math.min(1, (mastery - lo) / (hi - lo)));
    }

    public static double maxChakra(Bijuu b, double mastery) {
        double lvl = maxStage(b, mastery);
        return (500 + 100 * b.tails) * (1 + 0.1 * (lvl - 1)) * b.profile.chakra();
    }

    public static double drainPerSecond(Bijuu b, int stage, double mastery, double multiplier) {
        if (stage <= 0) {
            return 0;
        }
        double req = Math.max(1, required(b, Math.max(2, stage)));
        double relief = 0.35 * Math.min(1.0, mastery / req);
        return DRAIN[stage] * multiplier * (1 + 0.04 * b.tier()) * (1 - relief);
    }

    public static double controlCap(Bijuu b, double mastery, int compat) {
        double frac = Math.min(1.0, mastery / required(b, MAX_STAGE));
        return Math.min(100, 25 + 60 * frac + compat * 0.15);
    }
}
