package com.narutotoallbijuu.registry;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.entity.BijuuEntity;
import com.narutotoallbijuu.entity.BijuuGuardianEntity;
import com.narutotoallbijuu.entity.ChakraBlastEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AllBijuuMod.MODID);

    public static final RegistryObject<EntityType<BijuuEntity>> BIJUU = ENTITIES.register("bijuu",
            () -> EntityType.Builder.of(BijuuEntity::new, MobCategory.MISC)
                    .sized(3.4f, 5.8f).clientTrackingRange(12).fireImmune().build("bijuu"));

    public static final RegistryObject<EntityType<BijuuGuardianEntity>> GUARDIAN = ENTITIES.register("bijuu_guardian",
            () -> EntityType.Builder.of(BijuuGuardianEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build("bijuu_guardian"));

    public static final RegistryObject<EntityType<ChakraBlastEntity>> CHAKRA_BLAST = ENTITIES.register("chakra_blast",
            () -> EntityType.Builder.<ChakraBlastEntity>of(ChakraBlastEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f).clientTrackingRange(10).updateInterval(1).fireImmune().build("chakra_blast"));

    private ModEntities() {
    }
}
