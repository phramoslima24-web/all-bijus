package com.narutotoallbijuu.registry;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.block.AltarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AllBijuuMod.MODID);

    public static final RegistryObject<Block> ALTAR = BLOCKS.register("bijuu_altar",
            () -> new AltarBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.STONE).lightLevel(s -> 10)));

    private ModBlocks() {
    }
}
