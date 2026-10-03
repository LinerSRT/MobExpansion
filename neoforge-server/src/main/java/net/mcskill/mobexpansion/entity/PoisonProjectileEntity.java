package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class PoisonProjectileEntity extends ThrowableProjectile implements GeoEntity, IModel {
    private static final EntityDataAccessor<Boolean> DATA_EXPLODING =
            SynchedEntityData.defineId(PoisonProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation EXPLODE_ANIMATION = RawAnimation.begin().thenPlayAndHold("expode");

    private static final double DEFAULT_POISON_RADIUS = 3.0D;
    private static final double POISON_VERTICAL_RANGE = 2.5D;
    private static final int EXPLODE_LIFETIME_TICKS = 40;
    private static final int DEFAULT_POISON_DURATION_TICKS = 160;
    private static final int DEFAULT_POISON_AMPLIFIER = 1;
    private static final int POISON_REAPPLY_INTERVAL = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean spawnAnimationPlayed;
    private int explodeTicks;
    private double poisonRadius = DEFAULT_POISON_RADIUS;
    private int poisonDurationTicks = DEFAULT_POISON_DURATION_TICKS;
    private int poisonAmplifier = DEFAULT_POISON_AMPLIFIER;

    public PoisonProjectileEntity(EntityType<? extends PoisonProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PoisonProjectileEntity(Level level, LivingEntity shooter) {
        super(MobExEntities.POISON_PROJECTILE.get(), shooter, level);
        configureFromOwner(shooter);
    }

    private void configureFromOwner(LivingEntity owner) {
        if (owner == null)
            return;
        poisonRadius = MobExParameters.get(owner, "poisonCloudRadius", DEFAULT_POISON_RADIUS);
        poisonDurationTicks = MobExParameters.getInt(owner, "poisonCloudDuration", DEFAULT_POISON_DURATION_TICKS);
        poisonAmplifier = MobExParameters.getInt(owner, "poisonCloudAmplifier", DEFAULT_POISON_AMPLIFIER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EXPLODING, false);
    }

    public boolean isExploding() {
        return entityData.get(DATA_EXPLODING);
    }

    private void setExploding(boolean exploding) {
        entityData.set(DATA_EXPLODING, exploding);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03D;
    }

    @Override
    public void tick() {
        if (isExploding()) {
            setDeltaMovement(getDeltaMovement().scale(0.0D));
            explodeTicks++;
            if (!level().isClientSide() && explodeTicks % POISON_REAPPLY_INTERVAL == 0)
                applyPoisonCloud();
            if (!level().isClientSide() && explodeTicks >= EXPLODE_LIFETIME_TICKS)
                discard();
            return;
        }

        super.tick();
        if (!spawnAnimationPlayed && !level().isClientSide()) {
            spawnAnimationPlayed = true;
            triggerAnim("main", "spawn");
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        if (entity == getOwner())
            return false;
        if (entity instanceof RegularSpiderEntity || entity instanceof SpiderSpawnerEntity)
            return false;
        if (entity instanceof PoisonProjectileEntity)
            return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        if (isExploding())
            return;
        if (hitResult.getType() == HitResult.Type.ENTITY)
            onHitEntity((EntityHitResult) hitResult);
        else if (hitResult.getType() == HitResult.Type.BLOCK)
            onHitBlock((BlockHitResult) hitResult);
        beginPoisonExplosion();
    }

    private void beginPoisonExplosion() {
        if (level().isClientSide() || isExploding())
            return;

        setExploding(true);
        explodeTicks = 0;
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        triggerAnim("main", "expode");
        playSound(SoundEvents.SLIME_SQUISH, 1.0F, 0.7F + random.nextFloat() * 0.2F);
        applyPoisonCloud();
        spawnPoisonParticles();
    }

    private void applyPoisonCloud() {
        if (getOwner() instanceof LivingEntity livingOwner)
            configureFromOwner(livingOwner);
        final AABB poisonArea = getBoundingBox().inflate(poisonRadius, POISON_VERTICAL_RANGE, poisonRadius);
        final List<LivingEntity> victims = level().getEntitiesOfClass(
                LivingEntity.class,
                poisonArea,
                livingEntity -> livingEntity.isAlive()
                        && livingEntity != getOwner()
                        && !(livingEntity instanceof RegularSpiderEntity)
                        && !(livingEntity instanceof SpiderSpawnerEntity)
                        && isInPoisonRange(livingEntity)
        );
        for (LivingEntity victim : victims)
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDurationTicks, poisonAmplifier));
    }

    private boolean isInPoisonRange(LivingEntity victim) {
        final AABB victimBox = victim.getBoundingBox();
        final double closestX = Mth.clamp(getX(), victimBox.minX, victimBox.maxX);
        final double closestY = Mth.clamp(getY(), victimBox.minY, victimBox.maxY);
        final double closestZ = Mth.clamp(getZ(), victimBox.minZ, victimBox.maxZ);
        final double deltaX = getX() - closestX;
        final double deltaY = getY() - closestY;
        final double deltaZ = getZ() - closestZ;
        if (deltaY * deltaY > POISON_VERTICAL_RANGE * POISON_VERTICAL_RANGE)
            return false;
        return deltaX * deltaX + deltaZ * deltaZ <= poisonRadius * poisonRadius;
    }

    private void spawnPoisonParticles() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        serverLevel.sendParticles(
                ParticleTypes.ITEM_SLIME,
                getX(),
                getY(),
                getZ(),
                28,
                1.2D,
                0.6D,
                1.2D,
                0.05D
        );
        serverLevel.sendParticles(
                ParticleTypes.SNEEZE,
                getX(),
                getY(),
                getZ(),
                20,
                1.0D,
                0.5D,
                1.0D,
                0.0D
        );
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putBoolean("Exploding", isExploding());
        compoundTag.putInt("ExplodeTicks", explodeTicks);
        compoundTag.putBoolean("SpawnAnimPlayed", spawnAnimationPlayed);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        setExploding(compoundTag.getBoolean("Exploding"));
        explodeTicks = compoundTag.getInt("ExplodeTicks");
        spawnAnimationPlayed = compoundTag.getBoolean("SpawnAnimPlayed");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::handleAnimations)
                .triggerableAnim("spawn", SPAWN_ANIMATION)
                .triggerableAnim("expode", EXPLODE_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<PoisonProjectileEntity> animationState) {
        if (isExploding())
            return animationState.setAndContinue(EXPLODE_ANIMATION);
        return animationState.setAndContinue(FLY_ANIMATION);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public String modelName() {
        return "poison_projectile";
    }

    @Override
    public String textureName() {
        return "poison_projectile";
    }

    @Override
    public String animationName() {
        return "poison_projectile";
    }
}
