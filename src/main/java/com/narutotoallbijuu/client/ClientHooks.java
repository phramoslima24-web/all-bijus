package com.narutotoallbijuu.client;

import com.narutotoallbijuu.data.JinchuurikiData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Estado recebido do servidor para renderizar também as transformações de outros jogadores. */
public final class ClientHooks {
    private static final Map<UUID, JinchuurikiData> TRACKED = new ConcurrentHashMap<>();

    private ClientHooks() {}

    public static void sync(UUID playerId, CompoundTag tag) {
        if (playerId == null || tag == null) return;
        JinchuurikiData data = new JinchuurikiData();
        data.load(tag);
        TRACKED.put(playerId, data);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(playerId)) {
            JinchuurikiData.of(mc.player).load(tag);
        }
    }

    public static JinchuurikiData dataFor(AbstractClientPlayer player) {
        JinchuurikiData tracked = TRACKED.get(player.getUUID());
        return tracked != null ? tracked : JinchuurikiData.of(player);
    }

    public static void remove(UUID playerId) {
        TRACKED.remove(playerId);
    }

    public static void clear() {
        TRACKED.clear();
    }
}
