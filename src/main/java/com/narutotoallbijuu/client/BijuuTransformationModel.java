package com.narutotoallbijuu.client;

import net.minecraft.client.model.geom.PartPose;
import com.narutotoallbijuu.bijuu.Bijuu;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * Camada 3D de chakra do hospedeiro. A geometria é montada como cubos articulados
 * (ossos independentes), permitindo caudas e detalhes com movimento real no jogo.
 */
public final class BijuuTransformationModel extends EntityModel<AbstractClientPlayer> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("narutoto_allbijuu", "jinchuuriki_mantle"), "main");
    private final ModelPart root;
    private final ModelPart body, head, leftArm, rightArm, leftLeg, rightLeg;
    private final ModelPart[] tails = new ModelPart[8];
    private final ModelPart[] tailTips = new ModelPart[8];
    private final ModelPart leftWing, rightWing, horns, shell, mantle;
    private final ModelPart ears, antennae, crest, spikes;

    public BijuuTransformationModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = root.getChild("head");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        for (int i = 0; i < 8; i++) {
            tails[i] = root.getChild("tail_" + i);
            tailTips[i] = tails[i].getChild("tip");
        }
        leftWing = root.getChild("left_wing");
        rightWing = root.getChild("right_wing");
        horns = root.getChild("horns");
        shell = root.getChild("shell");
        mantle = root.getChild("mantle");
        ears = root.getChild("ears");
        antennae = root.getChild("antennae");
        crest = root.getChild("crest");
        spikes = root.getChild("spikes");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Revestimento volumétrico sobre a silhueta do jogador.
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, 0F, -2.7F, 9F, 12F, 5.4F),
                PartPose.ZERO);
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4.2F, -8.4F, -4.2F, 8.4F, 8.4F, 8.4F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(28, 20).addBox(-1.2F, -2F, -2.4F, 4.4F, 12F, 4.8F),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(28, 20).addBox(-3.2F, -2F, -2.4F, 4.4F, 12F, 4.8F),
                PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(46, 20).addBox(-2.4F, 0F, -2.5F, 4.8F, 12F, 5F),
                PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(46, 20).addBox(-2.4F, 0F, -2.5F, 4.8F, 12F, 5F),
                PartPose.ZERO);

        // Oito caudas segmentadas; o número visível depende do estágio desbloqueado.
        for (int i = 0; i < 8; i++) {
            float x = (i - 3.5F) * 1.45F;
            PartDefinition tail = root.addOrReplaceChild("tail_" + i,
                    CubeListBuilder.create().texOffs(0, 36)
                            .addBox(-1.5F, -1.5F, 0F, 3F, 3F, 5.5F),
                    PartPose.offset(x, 8.5F, 2.6F));
            tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(16, 36)
                    .addBox(-1.05F, -1.05F, 0F, 2.1F, 2.1F, 5F),
                    PartPose.offset(0F, 0F, 5F));
        }

        // Peças específicas que mudam a silhueta conforme a criatura.
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-1F, -5F, 0F, 12F, 10F, 1.5F),
                PartPose.offset(3F, 2F, 2F));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-11F, -5F, 0F, 12F, 10F, 1.5F),
                PartPose.offset(-3F, 2F, 2F));
        root.addOrReplaceChild("horns", CubeListBuilder.create().texOffs(28, 44)
                .addBox(-5F, -11F, -1F, 2F, 5F, 2F)
                .addBox(3F, -11F, -1F, 2F, 5F, 2F),
                PartPose.ZERO);
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(36, 44)
                .addBox(-5.5F, 1F, 1.7F, 11F, 9F, 3F),
                PartPose.ZERO);
        root.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(0, 56)
                .addBox(-5F, -1F, -3F, 10F, 14F, 6F),
                PartPose.ZERO);
        root.addOrReplaceChild("ears", CubeListBuilder.create().texOffs(28, 52)
                .addBox(-4.5F, -11F, -1F, 2F, 4F, 2F)
                .addBox(2.5F, -11F, -1F, 2F, 4F, 2F),
                PartPose.ZERO);
        root.addOrReplaceChild("antennae", CubeListBuilder.create().texOffs(36, 56)
                .addBox(-3F, -12F, -1F, 1F, 5F, 1F)
                .addBox(2F, -12F, -1F, 1F, 5F, 1F),
                PartPose.ZERO);
        root.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(44, 56)
                .addBox(-3F, -10F, -3F, 6F, 3F, 3F),
                PartPose.ZERO);
        root.addOrReplaceChild("spikes", CubeListBuilder.create().texOffs(52, 56)
                .addBox(-5F, 1F, 1F, 2F, 4F, 2F)
                .addBox(3F, 1F, 1F, 2F, 4F, 2F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void configure(PlayerModel<AbstractClientPlayer> player, Bijuu bijuu, int stage, float age) {
        body.copyFrom(player.body);
        head.copyFrom(player.head);
        leftArm.copyFrom(player.leftArm);
        rightArm.copyFrom(player.rightArm);
        leftLeg.copyFrom(player.leftLeg);
        rightLeg.copyFrom(player.rightLeg);

        body.visible = head.visible = leftArm.visible = rightArm.visible = true;
        leftLeg.visible = rightLeg.visible = true;
        mantle.visible = stage >= 2;
        shell.visible = bijuu == Bijuu.SANBI;
        horns.visible = bijuu == Bijuu.GOBI || bijuu == Bijuu.HACHIBI || bijuu == Bijuu.ICHIBI;
        ears.visible = bijuu == Bijuu.NIBI || bijuu == Bijuu.ICHIBI;
        antennae.visible = bijuu == Bijuu.ROKUBI;
        crest.visible = bijuu == Bijuu.YONBI || bijuu == Bijuu.NIBI;
        spikes.visible = bijuu == Bijuu.SANBI || bijuu == Bijuu.ICHIBI;
        leftWing.visible = rightWing.visible = bijuu == Bijuu.NANABI;

        int visibleTails = Math.max(1, Math.min(8, (int)Math.ceil(bijuu.tails * stage / 6.0)));
        for (int i = 0; i < tails.length; i++) {
            boolean show = stage > 0 && i < visibleTails;
            tails[i].visible = show;
            float spread = (i - (visibleTails - 1) * 0.5F);
            tails[i].xRot = -0.58F + Mth.sin(age * 0.075F + i * 0.7F) * 0.13F;
            tails[i].yRot = spread * 0.13F + Mth.sin(age * 0.045F + i) * 0.08F;
            tails[i].zRot = spread * 0.045F;
            tailTips[i].xRot = -0.12F + Mth.sin(age * 0.09F + i) * 0.16F;
            tailTips[i].yRot = Mth.sin(age * 0.06F + i * 0.8F) * 0.12F;
        }
        if (bijuu == Bijuu.NANABI) {
            leftWing.zRot = Mth.sin(age * 0.28F) * 0.38F - 0.18F;
            rightWing.zRot = -leftWing.zRot;
        }
        horns.xRot = Mth.sin(age * 0.05F) * 0.025F;
        shell.xRot = 0.08F;
        mantle.xRot = body.xRot;
    }

    @Override
    public void setupAnim(AbstractClientPlayer entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // A pose é copiada explicitamente em configure(), pois depende do PlayerModel pai.
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
