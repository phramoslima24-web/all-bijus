package com.narutotoallbijuu.client.bb;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.entity.BijuuEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.Map;

/**
 * Modelo de entidade dirigido por um .bbmodel: reproduz as animacoes do arquivo (idle, walk, run, attack, hurt, death,
 * dama, skill1..4, weak e fly) com mistura entre camadas.
 */
public class BbEntityModel extends EntityModel<BijuuEntity> {
    private static final String[] ACTION_NAMES = {null, "attack", "hurt", "dama", "skill1", "skill2", "skill3", "skill4"};

    private final ModelPart root;
    private final BbModelData data;
    private final ModelPart[] parts;
    private final int n;
    private final float[] outR;
    private final float[] outP;
    private final float[] actR;
    private final float[] actP;
    private final boolean[] actRT;
    private final boolean[] actPT;
    private final float[] tmp = new float[3];
    private final int headIdx;
    /** Preenchido pelo renderizador antes de desenhar (suaviza a animacao de morte). */
    public float partial;

    public BbEntityModel(ModelPart root, BbModelData data) {
        this.root = root;
        this.data = data;
        this.n = data.bones.size();
        this.parts = new ModelPart[n];
        for (BbModelData.Bone b : data.roots) {
            bind(root, b);
        }
        this.outR = new float[n * 3];
        this.outP = new float[n * 3];
        this.actR = new float[n * 3];
        this.actP = new float[n * 3];
        this.actRT = new boolean[n];
        this.actPT = new boolean[n];
        BbModelData.Bone h = data.byName.get("head");
        this.headIdx = h == null ? -1 : h.index;
    }

    private void bind(ModelPart parent, BbModelData.Bone b) {
        ModelPart p = parent.getChild(b.name);
        parts[b.index] = p;
        for (BbModelData.Bone c : b.children) {
            bind(p, c);
        }
    }

    public BbModelData data() {
        return data;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static void sample(float[][] kf, float t, float[] out) {
        if (kf.length == 0) {
            out[0] = out[1] = out[2] = 0f;
            return;
        }
        if (t <= kf[0][0]) {
            out[0] = kf[0][1];
            out[1] = kf[0][2];
            out[2] = kf[0][3];
            return;
        }
        for (int i = 0; i < kf.length - 1; i++) {
            float[] a = kf[i];
            float[] b = kf[i + 1];
            if (t <= b[0]) {
                float f = (t - a[0]) / Math.max(1e-6f, b[0] - a[0]);
                out[0] = a[1] + (b[1] - a[1]) * f;
                out[1] = a[2] + (b[2] - a[2]) * f;
                out[2] = a[3] + (b[3] - a[3]) * f;
                return;
            }
        }
        float[] l = kf[kf.length - 1];
        out[0] = l[1];
        out[1] = l[2];
        out[2] = l[3];
    }

    private float wrap(BbModelData.Anim a, float t) {
        if (a.loop && a.length > 0f) {
            t = t % a.length;
            if (t < 0f) {
                t += a.length;
            }
            return t;
        }
        return Math.min(Math.max(t, 0f), a.length);
    }

    /** Soma a animacao ponderada em outR/outP. */
    private void addAnim(BbModelData.Anim a, float t, float w) {
        if (a == null || w <= 0f) {
            return;
        }
        t = wrap(a, t);
        for (Map.Entry<String, BbModelData.Track> e : a.tracks.entrySet()) {
            BbModelData.Bone b = data.byName.get(e.getKey());
            if (b == null) {
                continue;
            }
            BbModelData.Track tr = e.getValue();
            int i = b.index * 3;
            if (tr.rot.length > 0) {
                sample(tr.rot, t, tmp);
                outR[i] += tmp[0] * w;
                outR[i + 1] += tmp[1] * w;
                outR[i + 2] += tmp[2] * w;
            }
            if (tr.pos.length > 0) {
                sample(tr.pos, t, tmp);
                outP[i] += tmp[0] * w;
                outP[i + 1] += tmp[1] * w;
                outP[i + 2] += tmp[2] * w;
            }
        }
    }

    /** Mistura a animacao de acao por cima da base, apenas nos canais que ela anima. */
    private void blendAction(BbModelData.Anim a, float t, float w) {
        if (a == null || w <= 0f) {
            return;
        }
        t = wrap(a, t);
        Arrays.fill(actRT, false);
        Arrays.fill(actPT, false);
        for (Map.Entry<String, BbModelData.Track> e : a.tracks.entrySet()) {
            BbModelData.Bone b = data.byName.get(e.getKey());
            if (b == null) {
                continue;
            }
            BbModelData.Track tr = e.getValue();
            int i = b.index * 3;
            if (tr.rot.length > 0) {
                sample(tr.rot, t, tmp);
                actR[i] = tmp[0];
                actR[i + 1] = tmp[1];
                actR[i + 2] = tmp[2];
                actRT[b.index] = true;
            }
            if (tr.pos.length > 0) {
                sample(tr.pos, t, tmp);
                actP[i] = tmp[0];
                actP[i + 1] = tmp[1];
                actP[i + 2] = tmp[2];
                actPT[b.index] = true;
            }
        }
        for (int b = 0; b < n; b++) {
            int i = b * 3;
            if (actRT[b]) {
                for (int k = 0; k < 3; k++) {
                    outR[i + k] += (actR[i + k] - outR[i + k]) * w;
                }
            }
            if (actPT[b]) {
                for (int k = 0; k < 3; k++) {
                    outP[i + k] += (actP[i + k] - outP[i + k]) * w;
                }
            }
        }
    }

    @Override
    public void setupAnim(BijuuEntity e, float limbSwing, float amt, float age, float netHeadYaw, float headPitch) {
        Arrays.fill(outR, 0f);
        Arrays.fill(outP, 0f);
        boolean dying = e.deathTime > 0;
        boolean weak = e.isWeak();
        if (dying && data.anims.containsKey("death")) {
            addAnim(data.anims.get("death"), (e.deathTime + partial) / 20f, 1f);
        } else if (weak && data.anims.containsKey("weak")) {
            addAnim(data.anims.get("weak"), age / 20f, 1f);
        } else {
            BbModelData.Anim fly = e.getBijuu() == Bijuu.NANABI ? data.anims.get("fly") : null;
            if (fly != null && !e.onGround()) {
                addAnim(fly, age / 20f, 1f);
            } else {
                float w = Mth.clamp(amt * 1.8f, 0f, 1f);
                addAnim(data.anims.get("idle"), age / 20f, 1f - w);
                if (w > 0f) {
                    float run = smooth((amt - 0.5f) / 0.3f);
                    BbModelData.Anim walk = data.anims.get("walk");
                    BbModelData.Anim runA = data.anims.get("run");
                    if (walk != null) {
                        float ph = limbSwing * 0.07f;
                        addAnim(walk, (ph - Mth.floor(ph)) * walk.length, w * (1f - run));
                    }
                    if (runA != null) {
                        float ph = limbSwing * 0.09f;
                        addAnim(runA, (ph - Mth.floor(ph)) * runA.length, w * run);
                    }
                }
            }
            int code = e.actCode;
            if (code > 0 && code < ACTION_NAMES.length) {
                BbModelData.Anim a = data.anims.get(ACTION_NAMES[code]);
                if (a != null) {
                    float t = (age - e.actStart) / 20f;
                    float tail = 0.15f;
                    if (t >= 0f && t < a.length + tail) {
                        float wa = Mth.clamp(t / 0.07f, 0f, 1f) * Mth.clamp((a.length + tail - t) / tail, 0f, 1f);
                        blendAction(a, Math.min(t, a.length), wa);
                    }
                }
            }
        }
        for (int b = 0; b < n; b++) {
            ModelPart p = parts[b];
            BbModelData.Bone bone = data.bones.get(b);
            int i = b * 3;
            p.xRot = outR[i] * Mth.DEG_TO_RAD;
            p.yRot = outR[i + 1] * Mth.DEG_TO_RAD;
            p.zRot = outR[i + 2] * Mth.DEG_TO_RAD;
            p.x = bone.initX - outP[i];
            p.y = bone.initY - outP[i + 1];
            p.z = bone.initZ + outP[i + 2];
        }
        if (headIdx >= 0 && !dying && !weak) {
            ModelPart h = parts[headIdx];
            h.yRot += Mth.clamp(netHeadYaw, -45f, 45f) * Mth.DEG_TO_RAD * 0.8f;
            h.xRot += Mth.clamp(headPitch, -30f, 30f) * Mth.DEG_TO_RAD * 0.6f;
        }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
