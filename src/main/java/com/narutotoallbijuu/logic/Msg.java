package com.narutotoallbijuu.logic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

public final class Msg {
    private Msg() {
    }

    public static Component c(String key, Object... args) {
        return Component.translatable("msg.narutoto_allbijuu." + key, args);
    }

    public static void bar(Player p, String key, Object... args) {
        p.displayClientMessage(c(key, args), true);
    }

    public static void chat(Player p, String key, Object... args) {
        p.sendSystemMessage(c(key, args));
    }

    public static void all(MinecraftServer s, String key, Object... args) {
        s.getPlayerList().broadcastSystemMessage(c(key, args), false);
    }
}
