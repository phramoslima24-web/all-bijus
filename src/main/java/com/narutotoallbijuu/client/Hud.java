package com.narutotoallbijuu.client;

import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.data.JinchuurikiData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class Hud {
    private Hud() {
    }

    public static final IGuiOverlay OVERLAY = (gui, g, pt, w, h) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        JinchuurikiData d = JinchuurikiData.of(mc.player);
        Bijuu b = d.current();
        if (b == null) {
            return;
        }
        int x = w / 2 - 91;
        int y = h - 58;
        double max = Math.max(1, d.maxChakra());
        int fill = (int) (182 * Math.min(1.0, d.chakra / max));
        g.fill(x - 1, y - 1, x + 183, y + 5, 0xAA000000);
        g.fill(x, y, x + fill, y + 4, 0xFF000000 | b.color);
        g.drawString(mc.font, Component.translatable("hud.narutoto_allbijuu.stage", b.displayName(), d.stage), x, y - 10, 0xFFFFFF);
        if (d.rampageTicks > 0) {
            g.drawString(mc.font, Component.translatable("hud.narutoto_allbijuu.rampage"), x + 120, y - 10, 0xFF4444);
        }
    };
}
