package com.narutotoallbijuu.event;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.data.JinchuurikiData;
import com.narutotoallbijuu.entity.BijuuEntity;
import com.narutotoallbijuu.entity.BijuuGuardianEntity;
import com.narutotoallbijuu.registry.ModEntities;
import com.narutotoallbijuu.registry.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AllBijuuMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    private ModEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.BIJUU.get(), BijuuEntity.createAttributes().build());
        e.put(ModEntities.GUARDIAN.get(), BijuuGuardianEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void caps(RegisterCapabilitiesEvent e) {
        e.register(JinchuurikiData.class);
    }

    @SubscribeEvent
    public static void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(ModItems.JOURNAL.get());
            e.accept(ModItems.SEAL_SCROLL.get());
            e.accept(ModItems.CHAKRA_FRAGMENT.get());
            e.accept(ModItems.ALTAR.get());
        }
    }
}
