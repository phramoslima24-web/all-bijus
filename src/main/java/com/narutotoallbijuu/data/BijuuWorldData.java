package com.narutotoallbijuu.data;

import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.BijuuState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Fonte unica da verdade sobre onde cada Bijuu esta. */
public class BijuuWorldData extends SavedData {
    public static final String NAME = "narutoto_allbijuu_bijuu";

    public static class Entry {
        public BijuuState state = BijuuState.FREE;
        public UUID host;
        public UUID entity;
        public UUID token;
        public long until;
        public long availableAt;
        public boolean altarBuilt;
        public long altarPos;
    }

    private final Entry[] entries = new Entry[Bijuu.values().length];

    public BijuuWorldData() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry();
        }
    }

    public static BijuuWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BijuuWorldData::load, BijuuWorldData::new, NAME);
    }

    public Entry get(Bijuu b) {
        return entries[b.ordinal()];
    }

    public static BijuuWorldData load(CompoundTag tag) {
        BijuuWorldData d = new BijuuWorldData();
        for (int i = 0; i < d.entries.length; i++) {
            if (!tag.contains("e" + i)) {
                continue;
            }
            CompoundTag t = tag.getCompound("e" + i);
            Entry e = d.entries[i];
            int s = t.getInt("state");
            e.state = BijuuState.values()[Math.max(0, Math.min(BijuuState.values().length - 1, s))];
            e.host = t.hasUUID("host") ? t.getUUID("host") : null;
            e.entity = t.hasUUID("entity") ? t.getUUID("entity") : null;
            e.token = t.hasUUID("token") ? t.getUUID("token") : null;
            e.until = t.getLong("until");
            e.availableAt = t.getLong("availableAt");
            e.altarBuilt = t.getBoolean("altar");
            e.altarPos = t.getLong("altarPos");
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (int i = 0; i < entries.length; i++) {
            Entry e = entries[i];
            CompoundTag t = new CompoundTag();
            t.putInt("state", e.state.ordinal());
            if (e.host != null) {
                t.putUUID("host", e.host);
            }
            if (e.entity != null) {
                t.putUUID("entity", e.entity);
            }
            if (e.token != null) {
                t.putUUID("token", e.token);
            }
            t.putLong("until", e.until);
            t.putLong("availableAt", e.availableAt);
            t.putBoolean("altar", e.altarBuilt);
            t.putLong("altarPos", e.altarPos);
            tag.put("e" + i, t);
        }
        return tag;
    }
}
