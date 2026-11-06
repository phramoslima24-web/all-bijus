package com.narutotoallbijuu.client;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.client.bb.BbModelData;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/** Registro dos oito modelos individuais (um .bbmodel, uma textura e uma textura emissiva por Bijuu). */
public final class BijuuModels {
    private static final Map<Bijuu, BbModelData> CACHE = new EnumMap<>(Bijuu.class);

    private BijuuModels() {
    }

    public static synchronized BbModelData get(Bijuu b) {
        return CACHE.computeIfAbsent(b, k -> BbModelData.load(k.id));
    }

    public static ModelLayerLocation layer(Bijuu b) {
        return new ModelLayerLocation(new ResourceLocation(AllBijuuMod.MODID, "bijuu_" + b.id), "main");
    }

    public static ResourceLocation texture(Bijuu b) {
        return new ResourceLocation(AllBijuuMod.MODID, "textures/entity/bijuu_" + b.id + ".png");
    }

    public static ResourceLocation glow(Bijuu b) {
        return new ResourceLocation(AllBijuuMod.MODID, "textures/entity/bijuu_" + b.id + "_glow.png");
    }
}
