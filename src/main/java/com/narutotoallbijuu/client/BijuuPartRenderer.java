package com.narutotoallbijuu.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.client.bb.BbEntityModel;
import com.narutotoallbijuu.client.bb.BbModelData;
import com.narutotoallbijuu.entity.BijuuEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renderizador de um Bijuu especifico: modelo proprio (.bbmodel), textura propria e camada emissiva (olhos, chamas, magma). */
public class BijuuPartRenderer extends MobRenderer<BijuuEntity, BbEntityModel> {
    private final Bijuu bijuu;
    private final BbModelData data;

    public BijuuPartRenderer(EntityRendererProvider.Context ctx, Bijuu b) {
        super(ctx, new BbEntityModel(ctx.bakeLayer(BijuuModels.layer(b)), BijuuModels.get(b)), 1.0f);
        this.bijuu = b;
        this.data = BijuuModels.get(b);
        this.shadowRadius = b.mob.width() * 0.45f;
        this.addLayer(new Glow(this, BijuuModels.glow(b)));
    }

    @Override
    public void render(BijuuEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        this.getModel().partial = partial;
        super.render(e, yaw, partial, ps, buf, light);
    }

    @Override
    protected void scale(BijuuEntity e, PoseStack ps, float partial) {
        float s = bijuu.mob.height() * 16f / data.heightPx;
        ps.scale(s, s, s);
    }

    /** O Bijuu nao tomba 90 graus ao morrer: a animacao "death" do proprio modelo cuida da queda. */
    @Override
    protected float getFlipDegrees(BijuuEntity e) {
        return 0f;
    }

    @Override
    public ResourceLocation getTextureLocation(BijuuEntity e) {
        return BijuuModels.texture(bijuu);
    }

    private static final class Glow extends RenderLayer<BijuuEntity, BbEntityModel> {
        private final ResourceLocation tex;

        Glow(RenderLayerParent<BijuuEntity, BbEntityModel> parent, ResourceLocation tex) {
            super(parent);
            this.tex = tex;
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, BijuuEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float headYaw, float headPitch) {
            if (e.isInvisible()) {
                return;
            }
            VertexConsumer vc = buf.getBuffer(RenderType.eyes(tex));
            this.getParentModel().renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        }
    }
}
