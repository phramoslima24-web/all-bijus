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
        int w = 300;
        int h = 200;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xDD101018);
        g.fill(x, y, x + w, y + 2, 0xFFFFFFFF);
        g.drawString(this.font, this.title, x + 8, y + 8, 0xFFFFFF);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        JinchuurikiData d = JinchuurikiData.of(mc.player);
        Bijuu b = d.current();
        int ty = y + 24;
        if (b == null) {
            g.drawString(this.font, Component.translatable("msg.narutoto_allbijuu.no_host"), x + 8, ty, 0xFFAAAA);
            Bijuu t = Bijuu.byIndex(d.tracked);
            if (t != null) {
                g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.tracking", t.displayName(), d.questStage[t.ordinal()]), x + 8, ty + 14, 0xCCCCCC);
            }
            super.render(g, mx, my, pt);
            return;
        }
        int col = 0xFF000000 | b.color;
        g.drawString(this.font, b.displayName(), x + 8, ty, col);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.stage", d.stage, Mastery.maxStage(b, d.currentMastery())), x + 8, ty + 12, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.mastery", (int) d.currentMastery(), (int) (Mastery.progressToNext(b, d.currentMastery()) * 100)), x + 8, ty + 24, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.control", (int) d.control, (int) Mastery.controlCap(b, d.currentMastery(), d.compat)), x + 8, ty + 36, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.chakra", (int) d.chakra, (int) d.maxChakra()), x + 8, ty + 48, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.compat", d.compat), x + 8, ty + 60, 0xFFFFFF);
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        int ay = ty + 78;
        for (int i = 0; i < 4; i++) {
            AbilityDef def = b.ability(i);
            long cd = Math.max(0, (d.cooldowns[i] - now) / 20);
            Component line = Component.translatable("screen.narutoto_allbijuu.ability", i + 1, b.abilityName(i), def.minStage(), (int) def.cost(), cd);
            g.drawString(this.font, line, x + 8, ay + i * 12, d.stage >= def.minStage() ? 0xFFFFFF : 0x888888);
        }
        long dcd = Math.max(0, (d.cooldowns[JinchuurikiData.CD_DAMA] - now) / 20);
        g.drawString(this.font, Component.translatable("screen.narutoto_allbijuu.dama", b.damaName(), b.dama.minStage(), (int) b.dama.cost(), dcd), x + 8, ay + 52, col);
        super.render(g, mx, my, pt);
    }
}
