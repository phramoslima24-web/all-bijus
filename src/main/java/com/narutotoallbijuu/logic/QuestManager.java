package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.AllBijuuConfig;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.BijuuState;
import com.narutotoallbijuu.data.BijuuWorldData;
import com.narutotoallbijuu.data.JinchuurikiData;
import com.narutotoallbijuu.entity.BijuuEntity;
import com.narutotoallbijuu.entity.BijuuGuardianEntity;
import com.narutotoallbijuu.registry.ModBlocks;
import com.narutotoallbijuu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Missoes: localizar o covil, enfrentar guardioes, invocar o Bijuu no altar. */
public final class QuestManager {
    private QuestManager() {
    }

    private static final String[] ARROWS = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};

    /** Coordenadas X/Z (deterministicas pela seed) do covil de cada Bijuu. */
    public static BlockPos lairXZ(MinecraftServer s, Bijuu b) {
        long seed = s.overworld().getSeed();
        RandomSource r = RandomSource.create(seed * 31L + b.ordinal() * 7919L + 12345L);
        double ang = (b.ordinal() / 8.0) * Math.PI * 2 + (r.nextDouble() - 0.5) * 0.5;
        int min = AllBijuuConfig.LAIR_MIN_DISTANCE.get();
        int max = Math.max(AllBijuuConfig.LAIR_MAX_DISTANCE.get(), min + 100);
        double dist = min + (max - min) * r.nextDouble();
        BlockPos spawn = s.overworld().getSharedSpawnPos();
        return new BlockPos((int) (spawn.getX() + Math.cos(ang) * dist), 0, (int) (spawn.getZ() + Math.sin(ang) * dist));
    }

    public static void journalUse(ServerPlayer p, boolean shift) {
        JinchuurikiData d = JinchuurikiData.of(p);
        if (shift) {
            d.tracked = (d.tracked + 1) % Bijuu.values().length;
        }
        Bijuu b = Bijuu.byIndex(d.tracked);
        if (b == null) {
            return;
        }
        if (d.questStage[b.ordinal()] == 0) {
            d.questStage[b.ordinal()] = 1;
        }
        d.dirty = true;
        BijuuWorldData.Entry e = BijuuWorldData.get(p.server).get(b);
        switch (e.state) {
            case SEALED -> Msg.chat(p, "quest_state_sealed", b.displayName());
            case VESSEL -> Msg.chat(p, "quest_state_vessel", b.displayName());
            case ENTITY -> Msg.chat(p, "quest_state_entity", b.displayName());
            case FREE -> {
                BlockPos l = lairXZ(p.server, b);
                int dist = (int) Math.sqrt(p.distanceToSqr(l.getX() + 0.5, p.getY(), l.getZ() + 0.5));
                Msg.chat(p, "quest_start", b.displayName(), l.getX(), l.getZ(), dist);
            }
        }
    }

    /** Seta de rastreamento na barra de acao enquanto o diario esta na mao. */
    public static void trackingMsg(ServerPlayer p) {
        JinchuurikiData d = JinchuurikiData.of(p);
        Bijuu b = Bijuu.byIndex(d.tracked);
        if (b == null || d.questStage[b.ordinal()] == 0) {
            return;
        }
        BlockPos l = lairXZ(p.server, b);
        double dx = l.getX() + 0.5 - p.getX();
        double dz = l.getZ() + 0.5 - p.getZ();
        float yawTo = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI);
        float diff = Mth.wrapDegrees(yawTo - p.getYRot());
        int idx = Math.floorMod(Math.round(diff / 45f), 8);
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        Msg.bar(p, "tracker", b.displayName(), ARROWS[idx], dist);
    }

    /** Chamado a cada 2 s por jogador: constroi o covil quando um jogador em missao chega perto. */
    public static void tick(ServerPlayer p, JinchuurikiData d) {
        if (p.level().dimension() != Level.OVERWORLD) {
            return;
        }
        BijuuWorldData data = BijuuWorldData.get(p.server);
        for (Bijuu b : Bijuu.values()) {
            int qs = d.questStage[b.ordinal()];
            if (qs < 1 || qs > 3) {
                continue;
            }
            BijuuWorldData.Entry e = data.get(b);
            if (e.altarBuilt) {
                continue;
            }
            BlockPos l = lairXZ(p.server, b);
            double dx = l.getX() + 0.5 - p.getX();
            double dz = l.getZ() + 0.5 - p.getZ();
            if (dx * dx + dz * dz < 64 * 64) {
                ensureLair((ServerLevel) p.level(), b);
                if (e.altarBuilt) {
                    d.questStage[b.ordinal()] = Math.max(qs, 2);
                    d.dirty = true;
                    Msg.chat(p, "lair_found", b.displayName());
                }
            }
        }
    }

    public static void ensureLair(ServerLevel level, Bijuu b) {
        BijuuWorldData data = BijuuWorldData.get(level.getServer());
        BijuuWorldData.Entry e = data.get(b);
        if (e.altarBuilt) {
            return;
        }
        BlockPos xz = lairXZ(level.getServer(), b);
        int x = xz.getX();
        int z = xz.getZ();
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 2) {
            return;
        }
        int floorY = y - 1;
        RandomSource r = level.random;
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (dx * dx + dz * dz > 36) {
                    continue;
                }
                BlockState floor = r.nextFloat() < 0.3f ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                level.setBlock(new BlockPos(x + dx, floorY, z + dz), floor, 3);
                for (int dy = 1; dy <= 24; dy++) {
                    BlockPos below = new BlockPos(x + dx, floorY - dy, z + dz);
                    BlockState bs = level.getBlockState(below);
                    if (bs.isSolid() && bs.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(below, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                }
                for (int dy = 0; dy <= 8; dy++) {
                    BlockPos up = new BlockPos(x + dx, y + dy, z + dz);
                    if (!level.getBlockState(up).isAir()) {
                        level.setBlock(up, air, 3);
                    }
                }
            }
        }
        BlockPos altar = new BlockPos(x, y, z);
        level.setBlock(altar, ModBlocks.ALTAR.get().defaultBlockState(), 3);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int dy = 0; dy < 5; dy++) {
                    level.setBlock(new BlockPos(x + sx * 4, y + dy, z + sz * 4), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
                }
                level.setBlock(new BlockPos(x + sx * 4, y + 5, z + sz * 4), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
            }
        }
        int count = Math.max(2, (int) Math.round((2 + b.tier()) * AllBijuuConfig.GUARDIAN_COUNT_MULTIPLIER.get()));
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            int gx = x + (int) Math.round(Math.cos(a) * 8);
            int gz = z + (int) Math.round(Math.sin(a) * 8);
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz);
            BijuuGuardianEntity g = ModEntities.GUARDIAN.get().create(level);
            if (g != null) {
                g.moveTo(gx + 0.5, gy, gz + 0.5, r.nextFloat() * 360f, 0f);
                g.setLair(b);
                level.addFreshEntity(g);
            }
        }
        e.altarBuilt = true;
        e.altarPos = altar.asLong();
        data.setDirty();
    }

    public static void altarUse(ServerLevel level, BlockPos pos, ServerPlayer p) {
        BijuuWorldData data = BijuuWorldData.get(level.getServer());
        Bijuu found = null;
        for (Bijuu b : Bijuu.values()) {
            BijuuWorldData.Entry e = data.get(b);
            if (e.altarBuilt && e.altarPos == pos.asLong()) {
                found = b;
                break;
            }
        }
        if (found == null) {
            Msg.bar(p, "altar_dormant");
            return;
        }
        Bijuu b = found;
        BijuuWorldData.Entry e = data.get(b);
        AABB box = new AABB(pos).inflate(48);
        List<BijuuGuardianEntity> gs = level.getEntitiesOfClass(BijuuGuardianEntity.class, box,
                g -> g.isAlive() && g.getLairIndex() == b.ordinal());
        if (!gs.isEmpty()) {
            Msg.bar(p, "altar_guardians", gs.size());
            return;
        }
        if (e.state != BijuuState.FREE) {
            Msg.bar(p, "altar_busy", b.displayName());
            return;
        }
        long now = level.getServer().overworld().getGameTime();
        if (now < e.availableAt) {
            Msg.bar(p, "altar_wait", (e.availableAt - now) / 20);
            return;
        }
        BijuuEntity be = ModEntities.BIJUU.get().create(level);
        if (be == null) {
            return;
        }
        be.setBijuu(b);
        be.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 6.5, 180f, 0f);
        e.state = BijuuState.ENTITY;
        e.entity = be.getUUID();
        e.host = null;
        data.setDirty();
        level.addFreshEntity(be);
        JinchuurikiData d = JinchuurikiData.of(p);
        d.questStage[b.ordinal()] = Math.max(d.questStage[b.ordinal()], 3);
        d.dirty = true;
        Msg.all(level.getServer(), "altar_summon", b.displayName());
    }

    public static void onGuardianKilled(ServerPlayer p, int bijuuIdx) {
        Bijuu b = Bijuu.byIndex(bijuuIdx);
        double tier = b == null ? 1 : b.tier();
        NarutotoBridge.addNinjaExperience(p, 25.0 * (1 + tier));
        JinchuurikiData d = JinchuurikiData.of(p);
        if (bijuuIdx >= 0 && bijuuIdx < JinchuurikiData.N) {
            d.questProgress[bijuuIdx]++;
            d.dirty = true;
        }
    }
}
