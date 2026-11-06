package com.narutotoallbijuu.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.entity.BijuuEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Ponto unico de registro da entidade; delega para um renderizador (modelo + textura + animacoes) por Bijuu. */
public class BijuuRenderer extends EntityRenderer<BijuuEntity> {
    private final BijuuPartRenderer[] parts = new BijuuPartRenderer[Bijuu.values().length];

    public BijuuRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        for (Bijuu b : Bijuu.values()) {
            parts[b.ordinal()] = new BijuuPartRenderer(ctx, b);
        }
    }

    @Override
    public void render(BijuuEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        parts[e.getBijuu().ordinal()].render(e, yaw, partial, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BijuuEntity e) {
        return BijuuModels.texture(e.getBijuu());
    }
}
