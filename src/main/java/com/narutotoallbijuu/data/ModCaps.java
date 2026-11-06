package com.narutotoallbijuu.data;

import com.narutotoallbijuu.AllBijuuMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class ModCaps {
    public static final Capability<JinchuurikiData> JINCHUURIKI = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation KEY = new ResourceLocation(AllBijuuMod.MODID, "jinchuuriki");

    private ModCaps() {
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final JinchuurikiData data = new JinchuurikiData();
        private final LazyOptional<JinchuurikiData> opt = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == JINCHUURIKI ? opt.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }
    }
}
