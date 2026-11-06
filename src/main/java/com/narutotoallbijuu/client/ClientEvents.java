package com.narutotoallbijuu.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.logic.Transform;
import com.narutotoallbijuu.net.ModNet;
import com.narutotoallbijuu.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ClientEvents {
    private ClientEvents() {
    }

    private static final String CAT = "key.categories.narutoto_allbijuu";
    public static final KeyMapping TRANSFORM = new KeyMapping("key.narutoto_allbijuu.transform", InputConstants.KEY_G, CAT);
    public static final KeyMapping[] ABILITY = {
            new KeyMapping("key.narutoto_allbijuu.ability1", InputConstants.KEY_H, CAT),
            new KeyMapping("key.narutoto_allbijuu.ability2", InputConstants.KEY_J, CAT),
            new KeyMapping("key.narutoto_allbijuu.ability3", InputConstants.KEY_K, CAT),
            new KeyMapping("key.narutoto_allbijuu.ability4", InputConstants.KEY_L, CAT)};
    public static final KeyMapping DAMA = new KeyMapping("key.narutoto_allbijuu.dama", InputConstants.KEY_N, CAT);
    public static final KeyMapping MEDITATE = new KeyMapping("key.narutoto_allbijuu.meditate", InputConstants.KEY_Y, CAT);
    public static final KeyMapping STATUS = new KeyMapping("key.narutoto_allbijuu.status", InputConstants.KEY_O, CAT);

    @Mod.EventBusSubscriber(modid = AllBijuuMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(TRANSFORM);
            for (KeyMapping k : ABILITY) {
                e.register(k);
            }
            e.register(DAMA);
            e.register(MEDITATE);
            e.register(STATUS);
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModEntities.BIJUU.get(), BijuuRenderer::new);
            e.registerEntityRenderer(ModEntities.GUARDIAN.get(), GuardianRenderer::new);
            e.registerEntityRenderer(ModEntities.CHAKRA_BLAST.get(), BlastRenderer::new);
        }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
            for (com.narutotoallbijuu.bijuu.Bijuu b : com.narutotoallbijuu.bijuu.Bijuu.values()) {
                e.registerLayerDefinition(BijuuModels.layer(b), () -> BijuuModels.get(b).createLayer());
            }
        }

        @SubscribeEvent
        public static void overlay(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("allbijuu_hud", Hud.OVERLAY);
        }
    }

    @Mod.EventBusSubscriber(modid = AllBijuuMod.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private static boolean damaWas;

        private ForgeBus() {
        }

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                damaWas = false;
                return;
            }
            if (mc.screen != null) {
                if (damaWas) {
                    ModNet.sendAction(Transform.A_DAMA_RELEASE);
                    damaWas = false;
                }
                return;
            }
            while (TRANSFORM.consumeClick()) {
                ModNet.sendAction(Screen.hasShiftDown() ? Transform.A_OFF : Transform.A_UP);
            }
            for (int i = 0; i < ABILITY.length; i++) {
                while (ABILITY[i].consumeClick()) {
                    ModNet.sendAction(Transform.A_AB0 + i);
                }
            }
            boolean down = DAMA.isDown();
            if (down && !damaWas) {
                ModNet.sendAction(Transform.A_DAMA_START);
            }
            if (!down && damaWas) {
                ModNet.sendAction(Transform.A_DAMA_RELEASE);
            }
            damaWas = down;
            while (MEDITATE.consumeClick()) {
                ModNet.sendAction(Transform.A_MEDITATE);
            }
            while (STATUS.consumeClick()) {
                mc.setScreen(new BijuuScreen());
            }
        }
    }
}
