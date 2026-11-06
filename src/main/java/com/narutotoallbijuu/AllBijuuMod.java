package com.narutotoallbijuu;

import com.mojang.logging.LogUtils;
import com.narutotoallbijuu.net.ModNet;
import com.narutotoallbijuu.registry.ModBlocks;
import com.narutotoallbijuu.registry.ModEntities;
import com.narutotoallbijuu.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AllBijuuMod.MODID)
public class AllBijuuMod {
    public static final String MODID = "narutoto_allbijuu";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AllBijuuMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AllBijuuConfig.SPEC);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(ModNet::register);
    }
}
