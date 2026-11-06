package com.narutotoallbijuu.entity;

import com.narutotoallbijuu.AllBijuuConfig;
import com.narutotoallbijuu.bijuu.AbilityDef;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.BijuuState;
import com.narutotoallbijuu.data.BijuuWorldData;
import com.narutotoallbijuu.logic.Abilities;
import com.narutotoallbijuu.logic.AbilityUtil;
import com.narutotoallbijuu.logic.CastCtx;
import com.narutotoallbijuu.logic.Msg;
import com.narutotoallbijuu.logic.ServerTasks;
import com.narutotoallbijuu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Entidade do Bijuu (chefe). Uma por Bijuu, controlada pelo BijuuWorldData. */
public class BijuuEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_BIJUU = SynchedEntityData.defineId(BijuuEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WEAK = SynchedEntityData.defineId(BijuuEntity.class, EntityDataSerializers.BOOLEAN);
    /** Acao de animacao em curso: (contador << 4) | codigo. O contador faz a mesma acao poder repetir. */
    private static final EntityDataAccessor<Integer> DATA_ACT = SynchedEntityData.defineId(BijuuEntity.class, EntityDataSerializers.INT);

    public static final int ACT_NONE = 0;
    public static final int ACT_ATTACK = 1;
    public static final int ACT_HURT = 2;
    public static final int ACT_DAMA = 3;
    /** ACT_SKILL0 + slot (0..3) -> animacoes skill1..skill4. */
    public static final int ACT_SKILL0 = 4;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private final int[] cds = new int[4];
    private int globalCd = 60;
    private int damaCd = 600;
    private int weakTicks;
    /** Marcado antes de remover quando o Bijuu foi selado/descartado de forma controlada. */
    public boolean sealing;
    /** Lado servidor: contador de acoes e tick ate o qual a acao atual tem prioridade. */
    private int actCounter;
    private long actUntil;
    /** Lado cliente: acao atual e tickCount em que comecou (usados pelo BbEntityModel). */
    public int actCode;
    public int actStart;

    public BijuuEntity(EntityType<? extends BijuuEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 700)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 10);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BIJUU, 0);
        this.entityData.define(DATA_WEAK, false);
        this.entityData.define(DATA_ACT, 0);
    }

    /** Dispara uma animacao de acao em todos os clientes. Acoes mais longas nao sao interrompidas por "hurt". */
    public void triggerAction(int code) {
        if (this.level().isClientSide) {
            return;
        }
        long now = this.level().getGameTime();
        if (code == ACT_HURT && now < this.actUntil) {
            return;
        }
        int dur = switch (code) {
            case ACT_ATTACK -> 16;
            case ACT_HURT -> 8;
            case ACT_DAMA -> 62;
            default -> 22;
        };
        this.actUntil = now + dur;
        this.actCounter = (this.actCounter + 1) & 0xFFF;
        this.entityData.set(DATA_ACT, (this.actCounter << 4) | code);
    }

    public Bijuu getBijuu() {
        Bijuu b = Bijuu.byIndex(this.entityData.get(DATA_BIJUU));
        return b == null ? Bijuu.ICHIBI : b;
    }

    public boolean isWeak() {
        return this.entityData.get(DATA_WEAK);
    }

    public void setBijuu(Bijuu b) {
        this.entityData.set(DATA_BIJUU, b.ordinal());
        applyStats(true);
    }

    private void applyStats(boolean heal) {
        Bijuu b = getBijuu();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(b.mob.health() * AllBijuuConfig.BOSS_HEALTH_MULTIPLIER.get());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(b.mob.damage());
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(b.mob.speed());
        if (heal) {
            this.setHealth(this.getMaxHealth());
        } else if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
        this.bossEvent.setName(b.displayName());
        this.bossEvent.setColor(b.bossColor);
        this.setCustomName(b.displayName());
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        Bijuu b = getBijuu();
        return EntityDimensions.scalable(b.mob.width(), b.mob.height());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_BIJUU.equals(key)) {
            this.refreshDimensions();
        }
        if (DATA_ACT.equals(key) && this.level().isClientSide) {
            int v = this.entityData.get(DATA_ACT);
            // ignora o valor inicial do pacote de spawn para nao repetir uma acao antiga
            this.actCode = this.tickCount < 2 ? 0 : (v & 15);
            this.actStart = this.tickCount;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void setTarget(LivingEntity t) {
        super.setTarget(isWeak() ? null : t);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean r = super.doHurtTarget(target);
        if (r) {
            triggerAction(ACT_ATTACK);
        }
        if (r && target instanceof LivingEntity le) {
            com.narutotoallbijuu.logic.BijuuEffects.apply(getBijuu(), le, this, 0);
        }
        return r;
    }

    @Override
    public boolean hurt(DamageSource src, float amt) {
        if (this.level().isClientSide) {
            return super.hurt(src, amt);
        }
        boolean bypass = src.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (isWeak()) {
            return bypass && super.hurt(src, amt);
        }
        if (AllBijuuConfig.BIJUU_SEALABLE_WHEN_WEAK.get() && !bypass) {
            float min = this.getMaxHealth() * 0.25f;
            if (this.getHealth() - amt <= min) {
                this.setHealth(min);
                enterWeak();
                this.playHurtSound(src);
                return true;
            }
        }
        boolean hit = super.hurt(src, amt);
        if (hit && this.isAlive()) {
            triggerAction(ACT_HURT);
        }
        return hit;
    }

    private void enterWeak() {
        this.entityData.set(DATA_WEAK, true);
        this.weakTicks = 0;
        this.setTarget(null);
        this.getNavigation().stop();
        MinecraftServer s = this.level().getServer();
        if (s != null) {
            Msg.all(s, "weak", getBijuu().displayName());
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Bijuu b = getBijuu();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.tickCount == 10) {
            validateRegistry();
            if (this.isRemoved()) {
                return;
            }
        }
        if (isWeak()) {
            this.weakTicks++;
            this.getNavigation().stop();
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false));
            if (this.tickCount % 20 == 0 && this.level() instanceof ServerLevel sl) {
                sl.sendParticles(AbilityUtil.dust(b.color, 1.6f), getX(), getY() + getBbHeight() * 0.5, getZ(), 12,
                        getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.01);
            }
            if (this.weakTicks > 4800) {
                this.entityData.set(DATA_WEAK, false);
                this.setHealth(this.getMaxHealth() * 0.6f);
            }
            return;
        }
        for (int i = 0; i < 4; i++) {
            if (cds[i] > 0) {
                cds[i]--;
            }
        }
        if (globalCd > 0) {
            globalCd--;
        }
        if (damaCd > 0) {
            damaCd--;
        }
        LivingEntity t = this.getTarget();
        if (t == null || !t.isAlive() || !(this.level() instanceof ServerLevel sl)) {
            return;
        }
        double dist = this.distanceTo(t);
        if (damaCd <= 0 && this.getHealth() < this.getMaxHealth() * 0.5f && dist < 40 && dist > 8) {
            damaCd = 1500;
            telegraphDama(sl, b);
            return;
        }
        if (globalCd > 0) {
            return;
        }
        int off = this.random.nextInt(4);
        for (int k = 0; k < 4; k++) {
            int i = (k + off) % 4;
            AbilityDef d = b.ability(i);
            if (cds[i] > 0 || dist > Abilities.aiRange(b, i)) {
                continue;
            }
            if (i >= 2 && this.getHealth() > this.getMaxHealth() * 0.75f) {
                continue;
            }
            CastCtx ctx = new CastCtx(this, sl, b, b.mob.damage() / 10.0, t);
            triggerAction(ACT_SKILL0 + i);
            Abilities.cast(ctx, i);
            cds[i] = d.cooldown();
            globalCd = 50;
            break;
        }
    }

    private void telegraphDama(ServerLevel sl, Bijuu b) {
        triggerAction(ACT_DAMA);
        ServerTasks.repeat(0, 5, 12, i -> {
            if (this.isAlive()) {
                Vec3 p = this.getEyePosition().add(this.getLookAngle().scale(2.5));
                sl.sendParticles(AbilityUtil.dust(b.color, 2.0f + i * 0.3f), p.x, p.y, p.z, 12, 0.5 + i * 0.1, 0.5 + i * 0.1, 0.5 + i * 0.1, 0.01);
            }
        });
        ServerTasks.after(60, () -> {
            LivingEntity t = this.getTarget();
            if (this.isAlive() && !this.isWeak() && t != null && t.isAlive()) {
                CastCtx c = new CastCtx(this, sl, b, b.mob.damage() / 10.0 * 1.2, t);
                ChakraBlastEntity e = AbilityUtil.shoot(c, c.dir(), 1.0, 45, (float) (7 + b.tails), 3.0f, 200);
                e.setDama(true);
            }
        });
    }

    /** Garante unicidade: so existe como entidade se o registro mundial aponta para esta entidade. */
    private void validateRegistry() {
        MinecraftServer s = this.level().getServer();
        if (s == null) {
            return;
        }
        BijuuWorldData.Entry e = BijuuWorldData.get(s).get(getBijuu());
        if (!(e.state == BijuuState.ENTITY && this.getUUID().equals(e.entity))) {
            this.sealing = true;
            this.discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            this.bossEvent.removeAllPlayers();
            if (reason.shouldDestroy() && !this.sealing) {
                markFree();
            }
        }
        super.remove(reason);
    }

    private void markFree() {
        MinecraftServer s = this.level().getServer();
        if (s == null) {
            return;
        }
        BijuuWorldData data = BijuuWorldData.get(s);
        BijuuWorldData.Entry e = data.get(getBijuu());
        UUID id = this.getUUID();
        if (e.state == BijuuState.ENTITY && id.equals(e.entity)) {
            e.state = BijuuState.FREE;
            e.entity = null;
            e.availableAt = s.overworld().getGameTime() + AllBijuuConfig.BIJUU_RESPAWN_TICKS.get();
            data.setDirty();
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(src, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.CHAKRA_FRAGMENT.get(), 3 + getBijuu().tails));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        this.bossEvent.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        this.bossEvent.removePlayer(p);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            hoverTick();
        }
        super.aiStep();
        if (this.level().isClientSide) {
            clientFx();
        }
    }

    /** Chomei voa: paira cerca de 1,5 bloco acima do solo enquanto nao esta enfraquecido (aterrissa quando fraco). */
    private void hoverTick() {
        if (getBijuu() != Bijuu.NANABI) {
            if (this.isNoGravity() && this.tickCount > 1) {
                this.setNoGravity(false);
            }
            return;
        }
        boolean hover = !isWeak() && this.isAlive();
        if (hover != this.isNoGravity()) {
            this.setNoGravity(hover);
        }
        if (!hover) {
            return;
        }
        net.minecraft.core.BlockPos.MutableBlockPos p = new net.minecraft.core.BlockPos.MutableBlockPos(this.getBlockX(), this.getBlockY(), this.getBlockZ());
        double ground = Double.NaN;
        for (int i = 0; i < 14; i++) {
            if (this.level().getBlockState(p).blocksMotion()) {
                ground = p.getY() + 1.0;
                break;
            }
            p.move(0, -1, 0);
        }
        double vy;
        if (Double.isNaN(ground)) {
            vy = -0.08;
        } else {
            vy = net.minecraft.util.Mth.clamp((ground + 1.5 - this.getY()) * 0.1, -0.12, 0.12);
        }
        Vec3 dm = this.getDeltaMovement();
        this.setDeltaMovement(dm.x, dm.y + (vy - dm.y) * 0.25, dm.z);
    }

    @Override
    protected float getFlyingSpeed() {
        return this.isNoGravity() && getBijuu() == Bijuu.NANABI ? this.getSpeed() * 0.16f : super.getFlyingSpeed();
    }

    /** Efeitos visuais do cliente: chamas azuis nas caudas da Matatabi. */
    private void clientFx() {
        if (!this.isAlive() || getBijuu() != Bijuu.NIBI || this.isInvisible()) {
            return;
        }
        int every = this.actCode != 0 ? 1 : 2;
        if (this.tickCount % every != 0) {
            return;
        }
        double yaw = Math.toRadians(this.yBodyRot);
        double bx = Math.sin(yaw);
        double bz = -Math.cos(yaw);
        double k = getBijuu().mob.height() / 56.0;
        for (int i = 0; i < 2; i++) {
            double side = (this.random.nextDouble() - 0.5) * 16.0 * k;
            double back = (32.0 + this.random.nextDouble() * 20.0) * k;
            double up = (42.0 + this.random.nextDouble() * 14.0) * k;
            double x = getX() + bx * back + (-bz) * side;
            double z = getZ() + bz * back + bx * side;
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, x, getY() + up, z, 0.0, 0.03, 0.0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float f, float m, DamageSource s) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("bijuu", this.entityData.get(DATA_BIJUU));
        t.putBoolean("weak", isWeak());
        t.putInt("weakTicks", weakTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("bijuu")) {
            this.entityData.set(DATA_BIJUU, t.getInt("bijuu"));
            applyStats(false);
        }
        this.entityData.set(DATA_WEAK, t.getBoolean("weak"));
        this.weakTicks = t.getInt("weakTicks");
    }
}
