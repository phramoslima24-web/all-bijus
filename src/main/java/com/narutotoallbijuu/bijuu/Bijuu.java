package com.narutotoallbijuu.bijuu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

import java.util.EnumSet;
import java.util.Set;

public enum Bijuu {
    ICHIBI("shukaku", 1, Element.WIND, 0xD9B26A, 0x6B4A1E, BossEvent.BossBarColor.YELLOW,
            new Profile(0.9, 0.9, 1.15, 1.35, 1.0, 1.0),
            new Mob(700, 9, 0.26, 3.0f, 5.0f, 2.2f),
            set(Element.WATER), set(Element.WIND),
            new AbilityDef(2, 60, 120, 1.0), new AbilityDef(3, 110, 240, 1.3),
            new AbilityDef(4, 200, 420, 1.8), new AbilityDef(5, 330, 600, 2.4),
            new AbilityDef(5, 500, 6000, 1.0)),
    NIBI("matatabi", 2, Element.FIRE, 0x59B8FF, 0x1C3F9E, BossEvent.BossBarColor.BLUE,
            new Profile(1.1, 1.25, 0.9, 0.85, 1.0, 1.1),
            new Mob(750, 10, 0.34, 2.8f, 3.3f, 2.6f),
            set(Element.WATER), set(Element.FIRE),
            new AbilityDef(2, 60, 100, 1.0), new AbilityDef(3, 100, 220, 1.3),
            new AbilityDef(4, 190, 400, 1.8), new AbilityDef(5, 330, 640, 2.4),
            new AbilityDef(5, 520, 6000, 1.05)),
    SANBI("isobu", 3, Element.WATER, 0x4FA89B, 0x1F4F63, BossEvent.BossBarColor.GREEN,
            new Profile(0.9, 0.75, 1.35, 1.45, 1.0, 1.0),
            new Mob(950, 10, 0.24, 3.0f, 3.2f, 2.4f),
            set(Element.ACID), set(Element.WATER),
            new AbilityDef(2, 70, 140, 1.0), new AbilityDef(3, 100, 220, 1.2),
            new AbilityDef(4, 180, 420, 1.7), new AbilityDef(5, 340, 660, 2.4),
            new AbilityDef(5, 540, 6200, 1.1)),
    YONBI("songoku", 4, Element.LAVA, 0xE5472A, 0x5A1A12, BossEvent.BossBarColor.RED,
            new Profile(1.3, 0.95, 1.1, 1.0, 1.05, 0.95),
            new Mob(1100, 13, 0.27, 3.0f, 5.2f, 2.4f),
            set(Element.WATER), set(Element.LAVA, Element.FIRE),
            new AbilityDef(2, 70, 120, 1.1), new AbilityDef(3, 120, 240, 1.4),
            new AbilityDef(4, 210, 440, 1.9), new AbilityDef(5, 360, 700, 2.6),
            new AbilityDef(5, 560, 6400, 1.15)),
    GOBI("kokuo", 5, Element.STEAM, 0xF2F6FF, 0x6F86C9, BossEvent.BossBarColor.WHITE,
            new Profile(1.1, 1.3, 1.0, 1.0, 1.1, 1.05),
            new Mob(1150, 13, 0.36, 2.8f, 4.4f, 2.5f),
            set(Element.WIND), set(Element.STEAM, Element.WATER),
            new AbilityDef(2, 65, 110, 1.0), new AbilityDef(3, 110, 230, 1.35),
            new AbilityDef(4, 200, 420, 1.9), new AbilityDef(5, 360, 700, 2.5),
            new AbilityDef(5, 580, 6500, 1.2)),
    ROKUBI("saiken", 6, Element.ACID, 0x9CFF3A, 0x2F7A8C, BossEvent.BossBarColor.GREEN,
            new Profile(1.0, 0.9, 1.0, 0.95, 1.2, 1.15),
            new Mob(1050, 12, 0.22, 3.0f, 2.5f, 2.6f),
            set(Element.FIRE), set(Element.ACID),
            new AbilityDef(2, 70, 120, 1.0), new AbilityDef(3, 110, 260, 1.3),
            new AbilityDef(4, 190, 420, 1.8), new AbilityDef(5, 340, 700, 2.4),
            new AbilityDef(5, 580, 6600, 1.2)),
    NANABI("chomei", 7, Element.INSECT, 0x7A4DD6, 0x2B1E5E, BossEvent.BossBarColor.PURPLE,
            new Profile(1.0, 1.2, 0.85, 0.8, 1.15, 1.1),
            new Mob(1000, 12, 0.30, 3.0f, 4.8f, 2.4f),
            set(Element.FIRE), set(Element.INSECT),
            new AbilityDef(2, 60, 100, 1.0), new AbilityDef(3, 110, 220, 1.3),
            new AbilityDef(4, 190, 400, 1.7), new AbilityDef(5, 330, 720, 2.2),
            new AbilityDef(5, 600, 6800, 1.2)),
    HACHIBI("gyuki", 8, Element.BEAST, 0xC23A3A, 0x3B1B1B, BossEvent.BossBarColor.RED,
            new Profile(1.25, 0.9, 1.3, 1.25, 1.3, 0.9),
            new Mob(1500, 16, 0.28, 3.4f, 5.8f, 2.6f),
            set(Element.ACID), set(Element.BEAST),
            new AbilityDef(2, 80, 140, 1.1), new AbilityDef(3, 130, 260, 1.5),
            new AbilityDef(4, 220, 460, 2.0), new AbilityDef(5, 400, 760, 2.8),
            new AbilityDef(5, 700, 7600, 1.4));

    public record Profile(double atk, double spd, double hp, double def, double chakra, double regen) {
    }

    public record Mob(int health, double damage, double speed, float width, float height, float scale) {
    }

    public final String id;
    public final int tails;
    public final Element element;
    public final int color;
    public final int accent;
    public final BossEvent.BossBarColor bossColor;
    public final Profile profile;
    public final Mob mob;
    public final Set<Element> weakTo;
    public final Set<Element> resists;
    private final AbilityDef[] abilities;
    public final AbilityDef dama;

    Bijuu(String id, int tails, Element element, int color, int accent, BossEvent.BossBarColor bossColor,
          Profile profile, Mob mob, Set<Element> weakTo, Set<Element> resists,
          AbilityDef a0, AbilityDef a1, AbilityDef a2, AbilityDef a3, AbilityDef dama) {
        this.id = id;
        this.tails = tails;
        this.element = element;
        this.color = color;
        this.accent = accent;
        this.bossColor = bossColor;
        this.profile = profile;
        this.mob = mob;
        this.weakTo = weakTo;
        this.resists = resists;
        this.abilities = new AbilityDef[]{a0, a1, a2, a3};
        this.dama = dama;
    }

    private static Set<Element> set(Element first, Element... rest) {
        return EnumSet.of(first, rest);
    }

    public int tier() {
        return tails;
    }

    public AbilityDef ability(int slot) {
        return abilities[slot];
    }

    public Component displayName() {
        return Component.translatable("bijuu.narutoto_allbijuu." + id);
    }

    public Component abilityName(int slot) {
        return Component.translatable("ability.narutoto_allbijuu." + id + "." + slot);
    }

    public Component damaName() {
        return Component.translatable("ability.narutoto_allbijuu." + id + ".dama");
    }

    public static Bijuu byIndex(int i) {
        Bijuu[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }

    public static Bijuu byId(String id) {
        for (Bijuu b : values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }
}
