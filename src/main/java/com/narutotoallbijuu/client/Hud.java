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

        JinchuurikiData data = JinchuurikiData.of(mc.player);
        Bijuu bijuu = data.current();
        if (bijuu == null) {
            return;
        }

        int width = 182;
        int x = w / 2 - width / 2;
        int y = h - 58;
        double maximum = Math.max(1.0, data.maxChakra());
        double ratio = Math.max(0.0, Math.min(1.0, data.chakra / maximum));
        int fill = (int) Math.round(width * ratio);
        int accent = 0xFF000000 | bijuu.color;

        // Fundo e contorno evitam que a barra desapareca em cenarios claros.
        g.fill(x - 3, y - 13, x + width + 3, y + 8, 0x99070B12);
        g.fill(x - 1, y - 1, x + width + 1, y + 6, 0xFF05070B);
        g.fill(x, y, x + width, y + 4, 0xFF263044);
        if (fill > 0) {
            g.fill(x, y, x + fill, y + 4, accent);
        }
        g.fill(x, y, x + width, y + 1, 0x55FFFFFF);

        g.drawString(mc.font, Component.translatable("hud.narutoto_allbijuu.stage",
                bijuu.displayName(), data.stage), x, y - 10, 0xFFFFFFFF);
        if (data.rampageTicks > 0) {
            g.drawString(mc.font, Component.translatable("hud.narutoto_allbijuu.rampage"),
                    x + width - 58, y - 10, 0xFFFF5555);
        }
    };
}
