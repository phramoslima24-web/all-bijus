package com.narutotoallbijuu.registry;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.item.JournalItem;
import com.narutotoallbijuu.item.SealScrollItem;
import com.narutotoallbijuu.item.VesselItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AllBijuuMod.MODID);

    public static final RegistryObject<Item> JOURNAL = ITEMS.register("bijuu_journal",
            () -> new JournalItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SEAL_SCROLL = ITEMS.register("seal_scroll",
            () -> new SealScrollItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> VESSEL = ITEMS.register("bijuu_vessel",
            () -> new VesselItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHAKRA_FRAGMENT = ITEMS.register("chakra_fragment",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ALTAR = ITEMS.register("bijuu_altar",
            () -> new BlockItem(ModBlocks.ALTAR.get(), new Item.Properties()));

    private ModItems() {
    }
}
