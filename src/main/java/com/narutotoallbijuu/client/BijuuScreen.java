package com.narutotoallbijuu.client;

import com.narutotoallbijuu.bijuu.AbilityDef;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.Mastery;
import com.narutotoallbijuu.data.JinchuurikiData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Tela de status do Jinchuuriki (tecla O). */
public class BijuuScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 218;
    private static final int TEXT = 0xFFF1F4FA;
    private static final int MUTED = 0xFFB7C0D0;
    private static final int PANEL = 0xF20B101B;

    public BijuuScreen() {
        super(Component.translatable("screen.narutoto_allbijuu.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            super.render(g, mx, my, pt);
            return;
        }

        int panelW = Math.min(PANEL_WIDTH, Math.max(220, this.width - 16));
        int panelH = Math.min(PANEL_HEIGHT, Math.max(190, this.height - 16));
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;
        int right = x + panelW;

        // Painel em camadas para separar melhor a interface do mundo.
        g.fill(x - 3, y - 3, right + 3, y + panelH + 3, 0x66000000);
        g.fill(x, y, right, y + panelH, PANEL);
        g.fill(x, y, right, y + 2, 0xFF7B61D1);
        g.fill(x, y + 2, right, y + 3, 0xFF263653);
        g.fill(x + 8, y + 23, right - 8, y + 24, 0x553F526E);

        g.drawString(this.font, this.title, x + 10, y + 9, TEXT);
        g.drawString(this.font, Component.literal("NARUTO • JINCHUURIKI"), right - 126, y + 9, MUTED);

        JinchuurikiData d = JinchuurikiData.of(mc.player);
        Bijuu b = d.current();
        int contentX = x + 10;
        int contentW = panelW - 20;
        int ty = y + 32;

        if (b == null) {
            g.drawString(this.font, Component.translatable("msg.narutoto_allbijuu.no_host"), contentX, ty, TEXT);
            Bijuu tracked = Bijuu.byIndex(d.tracked);
            if (tracked != null) {
                g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.tracking",
                        tracked.displayName(), d.questStage[tracked.ordinal()]), contentX, ty + 18, MUTED);
            }
            g.drawString(this.font, Component.literal("Use o Diario dos Bijuu para iniciar sua jornada."),
                    contentX, ty + 42, MUTED);
            super.render(g, mx, my, pt);
            return;
        }

        int accent = 0xFF000000 | b.color;
        g.drawString(this.font, b.displayName(), contentX, ty, accent);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.stage",
                d.stage, Mastery.maxStage(b, d.currentMastery())), right - 112, ty, TEXT);

        int barY = ty + 17;
        drawBar(g, contentX, barY, contentW, 5, d.chakra, d.maxChakra(), accent);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.chakra",
                (int) d.chakra, (int) d.maxChakra()), contentX, barY + 9, MUTED);

        int statsY = barY + 25;
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.mastery",
                (int) d.currentMastery(), (int) (Mastery.progressToNext(b, d.currentMastery()) * 100)),
                contentX, statsY, TEXT);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.control",
                (int) d.control, (int) Mastery.controlCap(b, d.currentMastery(), d.compat)),
                contentX + contentW / 2, statsY, TEXT);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.compat", d.compat),
                contentX, statsY + 13, MUTED);

        int listY = statsY + 31;
        g.fill(contentX, listY - 5, right - 10, listY - 4, 0x553F526E);
        g.drawString(this.font, Component.literal("HABILIDADES"), contentX, listY, MUTED);
        int rowY = listY + 14;
        for (int i = 0; i < 4; i++) {
            AbilityDef def = b.ability(i);
            long now = mc.level == null ? 0 : mc.level.getGameTime();
            long cd = Math.max(0, (d.cooldowns[i] - now + 19) / 20);
            boolean unlocked = d.stage >= def.minStage();
            int color = unlocked ? TEXT : 0xFF747D8D;
            Component line = Component.translatable("screen.narutoto_allbijuu.ability",
                    i + 1, b.abilityName(i), def.minStage(), (int) def.cost(), cd);
            g.drawString(this.font, line, contentX, rowY + i * 12, color);
        }

        long now = mc.level == null ? 0 : mc.level.getGameTime();
        long damaCd = Math.max(0, (d.cooldowns[JinchuurikiData.CD_DAMA] - now + 19) / 20);
        int damaY = rowY + 51;
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.dama",
                b.damaName(), b.dama.minStage(), (int) b.dama.cost(), damaCd), contentX, damaY, accent);

        super.render(g, mx, my, pt);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int width, int height,
                                double value, double maximum, int color) {
        double safeMax = Math.max(1.0, maximum);
        double ratio = Math.max(0.0, Math.min(1.0, value / safeMax));
        g.fill(x, y, x + width, y + height, 0xFF202938);
        int filled = (int) Math.round(width * ratio);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + height, color);
        }
        g.fill(x, y, x + width, y + 1, 0x558FFFFF);
    }
}
