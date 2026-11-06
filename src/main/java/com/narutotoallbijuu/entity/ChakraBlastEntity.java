package com.narutotoallbijuu.entity;

import com.narutotoallbijuu.AllBijuuConfig;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.logic.AbilityUtil;
import com.narutotoallbijuu.logic.BijuuEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Esfera de chakra: usada pelos projeteis das habilidades e pela Bijuu Dama. */
public class ChakraBlastEntity extends Projectile {
    private static final EntityDataAccessor<Integer> DATA_BIJUU = SynchedEntityData.defineId(ChakraBlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(ChakraBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_DAMA = SynchedEntityData.defineId(ChakraBlastEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 10;
    private float radius = 3;
    private int life = 60;
    private int age;

    public ChakraBlastEntity(EntityType<? extends ChakraBlastEntity> type, Level level) {
        super(type, level);
    }

    public void setup(Bijuu b, float damage, float radius, float size, int life, boolean dama) {
        this.entityData.set(DATA_BIJUU, b.ordinal());
        this.entityData.set(DATA_SIZE, size);
        this.entityData.set(DATA_DAMA, dama);
        this.damage = damage;
        this.radius = radius;
        this.life = life;
    }

    public void setDama(boolean dama) {
        this.entityData.set(DATA_DAMA, dama);
    }

    public Bijuu getBijuu() {
        Bijuu b = Bijuu.byIndex(this.entityData.get(DATA_BIJUU));
        return b == null ? Bijuu.ICHIBI : b;
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public boolean isDama() {
        return this.entityData.get(DATA_DAMA);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BIJUU, 0);
        this.entityData.define(DATA_SIZE, 1.0f);
        this.entityData.define(DATA_DAMA, false);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 mv = this.getDeltaMovement();
        if (!this.level().isClientSide) {
            ServerLevel sl = (ServerLevel) this.level();
            this.age++;
            Bijuu b = getBijuu();
            float size = getSize();
            sl.sendParticles(BijuuEffects.trail(b), getX(), getY() + 0.3, getZ(), isDama() ? 6 : 2, size * 0.3, size * 0.3, size * 0.3, 0.02);
            sl.sendParticles(AbilityUtil.dust(b.color, Math.max(1.0f, size)), getX(), getY() + 0.3, getZ(), isDama() ? 10 : 3, size * 0.25, size * 0.25, size * 0.25, 0.01);
            if (this.age > this.life) {
                explode(this.position());
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                explode(hit.getLocation());
                return;
            }
        }
        this.setPos(this.getX() + mv.x, this.getY() + mv.y, this.getZ() + mv.z);
    }

    private void explode(Vec3 p) {
        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }
        ServerLevel sl = (ServerLevel) this.level();
        Bijuu b = getBijuu();
        Entity owner = getOwner();
        LivingEntity src = owner instanceof LivingEntity l ? l : null;
        float r = this.radius;
        boolean dama = isDama();
        for (LivingEntity e : AbilityUtil.inRadius(sl, p, r, src)) {
            double d = Math.sqrt(e.distanceToSqr(p));
            float f = (float) (1.0 - 0.5 * Math.min(1.0, d / r));
            AbilityUtil.hurt(src, e, this.damage * f, b, true);
            AbilityUtil.push(e, e.position().subtract(p), dama ? 2.0 : 1.2, dama ? 0.8 : 0.5);
        }
        AbilityUtil.explosionFx(sl, p, r, b, dama);
        if (dama && AllBijuuConfig.DAMA_BLOCK_DAMAGE.get()) {
            crater(sl, p, r);
        }
        this.discard();
    }

    private void crater(ServerLevel sl, Vec3 p, float r) {
        int limit = AllBijuuConfig.DAMA_CRATER_BLOCK_LIMIT.get();
        int rr = (int) Math.ceil(r);
        BlockPos c = BlockPos.containing(p);
        int count = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -rr; dy <= rr && count < limit; dy++) {
            for (int dx = -rr; dx <= rr && count < limit; dx++) {
                for (int dz = -rr; dz <= rr && count < limit; dz++) {
                    double dist = dx * dx + (dy * 1.3) * (dy * 1.3) + dz * dz;
                    if (dist > r * r) {
                        continue;
                    }
                    pos.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (pos.getY() < sl.getMinBuildHeight() || pos.getY() >= sl.getMaxBuildHeight()) {
                        continue;
                    }
                    BlockState st = sl.getBlockState(pos);
                    if (st.isAir() || st.getDestroySpeed(sl, pos) < 0 || st.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    sl.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    count++;
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
