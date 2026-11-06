package com.narutotoallbijuu.client.bb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.narutotoallbijuu.AllBijuuMod;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le um arquivo .bbmodel (Blockbench, formato "free" com Box UV) diretamente do jar do mod.
 * O proprio .bbmodel e a fonte da geometria, dos ossos e das animacoes: editar o arquivo no Blockbench e recompilar
 * muda o que o jogo renderiza.
 *
 * Convencao de coordenadas: Blockbench usa Y para cima e a frente em -Z. O renderizador de entidades vanilla usa Y para baixo
 * (e espelha X), entao convertemos: x' = -x, y' = 24 - y, z' = z (em pixels). Rotacoes mantem o mesmo valor em graus
 * (Blockbench ja usa a mesma convencao de sinal do Java para X/Y/Z).
 */
public final class BbModelData {
    public static final class Cube {
        public float fx, fy, fz, tx, ty, tz;
        public int u, v;
    }

    public static final class Bone {
        public String name;
        public Bone parent;
        public int index;
        public float ox, oy, oz;
        public float initX, initY, initZ;
        public final List<Cube> cubes = new ArrayList<>();
        public final List<Bone> children = new ArrayList<>();
    }

    /** Linhas: {tempo, x, y, z}. */
    public static final class Track {
        public float[][] rot = new float[0][];
        public float[][] pos = new float[0][];
    }

    public static final class Anim {
        public String name;
        public float length;
        public boolean loop;
        public final Map<String, Track> tracks = new HashMap<>();
    }

    public final String id;
    public final int texW;
    public final int texH;
    public final List<Bone> bones = new ArrayList<>();
    public final List<Bone> roots = new ArrayList<>();
    public final Map<String, Bone> byName = new HashMap<>();
    public final Map<String, Anim> anims = new HashMap<>();
    /** Altura do modelo em pixels do Blockbench (maior Y de todos os cubos). */
    public float heightPx = 16f;

    public static BbModelData load(String id) {
        String path = "/assets/" + AllBijuuMod.MODID + "/models/entity/" + id + ".bbmodel";
        try (InputStream in = BbModelData.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Arquivo .bbmodel ausente no jar: " + path);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return new BbModelData(id, root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BbModelData(String id, JsonObject root) {
        this.id = id;
        JsonObject res = root.getAsJsonObject("resolution");
        this.texW = res != null && res.has("width") ? res.get("width").getAsInt() : 128;
        this.texH = res != null && res.has("height") ? res.get("height").getAsInt() : 128;

        Map<String, Cube> cubes = new HashMap<>();
        float maxY = 1f;
        for (JsonElement el : root.getAsJsonArray("elements")) {
            JsonObject o = el.getAsJsonObject();
            if (o.has("type") && !"cube".equals(o.get("type").getAsString())) {
                continue;
            }
            Cube c = new Cube();
            float[] f = vec(o.getAsJsonArray("from"));
            float[] t = vec(o.getAsJsonArray("to"));
            c.fx = f[0];
            c.fy = f[1];
            c.fz = f[2];
            c.tx = t[0];
            c.ty = t[1];
            c.tz = t[2];
            JsonArray uv = o.getAsJsonArray("uv_offset");
            c.u = uv == null ? 0 : (int) num(uv.get(0));
            c.v = uv == null ? 0 : (int) num(uv.get(1));
            cubes.put(o.get("uuid").getAsString(), c);
            maxY = Math.max(maxY, c.ty);
        }
        this.heightPx = maxY;

        for (JsonElement el : root.getAsJsonArray("outliner")) {
            if (el.isJsonObject()) {
                roots.add(readBone(el.getAsJsonObject(), null, cubes));
            }
        }
        for (Bone b : bones) {
            if (b.parent == null) {
                b.initX = -b.ox;
                b.initY = 24f - b.oy;
                b.initZ = b.oz;
            } else {
                b.initX = b.parent.ox - b.ox;
                b.initY = b.parent.oy - b.oy;
                b.initZ = b.oz - b.parent.oz;
            }
        }

        JsonArray anims = root.getAsJsonArray("animations");
        if (anims != null) {
            for (JsonElement el : anims) {
                readAnim(el.getAsJsonObject());
            }
        }
    }

    private Bone readBone(JsonObject g, Bone parent, Map<String, Cube> cubes) {
        Bone b = new Bone();
        b.name = g.get("name").getAsString();
        b.parent = parent;
        float[] o = vec(g.getAsJsonArray("origin"));
        b.ox = o[0];
        b.oy = o[1];
        b.oz = o[2];
        b.index = bones.size();
        bones.add(b);
        byName.put(b.name, b);
        JsonArray ch = g.getAsJsonArray("children");
        if (ch != null) {
            for (JsonElement c : ch) {
                if (c.isJsonPrimitive()) {
                    Cube cube = cubes.get(c.getAsString());
                    if (cube != null) {
                        b.cubes.add(cube);
                    }
                } else if (c.isJsonObject()) {
                    b.children.add(readBone(c.getAsJsonObject(), b, cubes));
                }
            }
        }
        return b;
    }

    private void readAnim(JsonObject a) {
        Anim an = new Anim();
        an.name = a.get("name").getAsString();
        an.length = a.has("length") ? a.get("length").getAsFloat() : 1f;
        an.loop = a.has("loop") && isLoop(a.get("loop"));
        JsonObject animators = a.getAsJsonObject("animators");
        if (animators != null) {
            for (Map.Entry<String, JsonElement> en : animators.entrySet()) {
                JsonObject an0 = en.getValue().getAsJsonObject();
                if (!an0.has("name") || !an0.has("keyframes")) {
                    continue;
                }
                String bone = an0.get("name").getAsString();
                List<float[]> rot = new ArrayList<>();
                List<float[]> pos = new ArrayList<>();
                for (JsonElement ke : an0.getAsJsonArray("keyframes")) {
                    JsonObject k = ke.getAsJsonObject();
                    String ch = k.get("channel").getAsString();
                    JsonArray dps = k.getAsJsonArray("data_points");
                    if (dps == null || dps.size() == 0) {
                        continue;
                    }
                    JsonObject dp = dps.get(0).getAsJsonObject();
                    float[] row = {num(k.get("time")), num(dp.get("x")), num(dp.get("y")), num(dp.get("z"))};
                    if ("rotation".equals(ch)) {
                        rot.add(row);
                    } else if ("position".equals(ch)) {
                        pos.add(row);
                    }
                }
                Track tr = new Track();
                rot.sort(Comparator.comparingDouble(r -> r[0]));
                pos.sort(Comparator.comparingDouble(r -> r[0]));
                tr.rot = rot.toArray(new float[0][]);
                tr.pos = pos.toArray(new float[0][]);
                an.tracks.put(bone, tr);
            }
        }
        anims.put(an.name, an);
    }

    private static boolean isLoop(JsonElement e) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
            return e.getAsBoolean();
        }
        String s = e.getAsString();
        return "loop".equals(s) || "true".equals(s);
    }

    private static float[] vec(JsonArray a) {
        return new float[]{num(a.get(0)), num(a.get(1)), num(a.get(2))};
    }

    /** Blockbench pode gravar numeros como texto ("12"); expressoes Molang nao sao suportadas e valem 0. */
    private static float num(JsonElement e) {
        if (e == null || e.isJsonNull()) {
            return 0f;
        }
        try {
            return e.getAsFloat();
        } catch (RuntimeException ex) {
            return 0f;
        }
    }

    /** Monta a definicao de camada vanilla (ossos, cubos, offsets de textura) a partir do .bbmodel. */
    public LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootDef = mesh.getRoot();
        for (Bone b : roots) {
            addBone(rootDef, b);
        }
        return LayerDefinition.create(mesh, texW, texH);
    }

    private void addBone(PartDefinition parent, Bone b) {
        CubeListBuilder cb = CubeListBuilder.create();
        for (Cube c : b.cubes) {
            cb.texOffs(c.u, c.v).addBox(b.ox - c.tx, b.oy - c.ty, c.fz - b.oz, c.tx - c.fx, c.ty - c.fy, c.tz - c.fz);
        }
        PartDefinition pd = parent.addOrReplaceChild(b.name, cb, PartPose.offset(b.initX, b.initY, b.initZ));
        for (Bone ch : b.children) {
            addBone(pd, ch);
        }
    }
}
