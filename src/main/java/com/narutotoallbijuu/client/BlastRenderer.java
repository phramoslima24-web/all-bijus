package com.narutotoallbijuu.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.entity.ChakraBlastEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Esfera de chakra desenhada como billboard emissivo tingido pela cor do Bijuu. */
public class BlastRenderer extends EntityRenderer<ChakraBlastEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(AllBijuuMod.MODID, "textures/entity/blast.png");
    private static final int FULL_BRIGHT = 15728880;

    public BlastRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(ChakraBlastEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        Bijuu b = e.getBijuu();
        float s = e.getSize() * 1.6f;
        int c = b.color;
        float r = ((c >> 16) & 255) / 255f;
        float g = ((c >> 8) & 255) / 255f;
        float bl = (c & 255) / 255f;
        ps.pushPose();
        ps.translate(0, e.getBbHeight() / 2, 0);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        ps.scale(s, s, s);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vertex(vc, m, n, -0.5f, -0.5f, 0, 0, 1, r, g, bl);
        vertex(vc, m, n, 0.5f, -0.5f, 0, 1, 1, r, g, bl);
        vertex(vc, m, n, 0.5f, 0.5f, 0, 1, 0, r, g, bl);
        vertex(vc, m, n, -0.5f, 0.5f, 0, 0, 0, r, g, bl);
        ps.popPose();
        super.render(e, yaw, partial, ps, buf, FULL_BRIGHT);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, float r, float g, float b) {
        vc.vertex(m, x, y, z).color(r, g, b, 1.0f).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ChakraBlastEntity e) {
        return TEX;
    }
}
