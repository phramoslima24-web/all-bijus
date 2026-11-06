package com.narutotoallbijuu.client;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.entity.BijuuGuardianEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GuardianRenderer extends HumanoidMobRenderer<BijuuGuardianEntity, HumanoidModel<BijuuGuardianEntity>> {
    private static final ResourceLocation TEX = new ResourceLocation(AllBijuuMod.MODID, "textures/entity/bijuu_guardian.png");

    public GuardianRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(BijuuGuardianEntity e) {
        return TEX;
    }
}
