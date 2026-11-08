package com.narutotoallbijuu.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.data.JinchuurikiData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Camada visual do manto Jinchuuriki, renderizada por cima do skin/armadura do jogador. */
public final class BijuuTransformationLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WHITE = new ResourceLocation("narutoto_allbijuu", "textures/entity/transformation_white.png");
    private final BijuuTransformationModel model;

    public BijuuTransformationLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                   net.minecraft.client.model.geom.ModelPart baked) {
        super(parent);
        this.model = new BijuuTransformationModel(baked);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        JinchuurikiData data = ClientHooks.dataFor(player);
        Bijuu bijuu = data.current();
        if (bijuu == null || data.stage <= 0 || player.isInvisible()) return;

        model.configure(getParentModel(), bijuu, data.stage, ageInTicks + partialTicks);
        float alpha = Math.min(0.82F, 0.42F + data.stage * 0.065F);
        int color = bijuu.color;
        float r = ((color >> 16) & 255) / 255F;
        float g = ((color >> 8) & 255) / 255F;
        float b = (color & 255) / 255F;

        poseStack.pushPose();
        // Mantém o revestimento alinhado ao corpo do jogador e evita z-fighting com o skin.
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        model.renderToBuffer(poseStack, vc, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                r, g, b, alpha);
        poseStack.popPose();
    }
}
