package com.narutotoallbijuu.bijuu;

public enum Element {
    WIND("wind", 0xE6CC8A),
    FIRE("fire", 0x59B8FF),
    WATER("water", 0x3A7BFF),
    LAVA("lava", 0xFF5A1F),
    STEAM("steam", 0xE8F4FF),
    ACID("acid", 0x9CFF3A),
    INSECT("insect", 0x7A4DD6),
    BEAST("beast", 0xC23A3A);

    public final String id;
    public final int color;

    Element(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String langKey() {
        return "element.narutoto_allbijuu." + id;
    }
}
