package com.narutotoallbijuu.client;

import com.narutotoallbijuu.data.JinchuurikiData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class ClientHooks {
    private ClientHooks() {
    }

    public static void sync(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && tag != null) {
            JinchuurikiData.of(mc.player).load(tag);
        }
    }
}
