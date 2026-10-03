package net.mcskill.mobexpansion.entity.redstone;

import net.mcskill.mobexpansion.entity.IModel;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
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

public class WrenchProjectileEntity extends ThrowableProjectile implements GeoEntity, IModel {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private static final float BASE_DAMAGE = 8.0F;
    private static final int MAX_LIFETIME_TICKS = 80;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean spawnAnimationPlayed;
    private float damage = BASE_DAMAGE;

    public WrenchProjectileEntity(EntityType<? extends WrenchProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public WrenchProjectileEntity(Level level, LivingEntity shooter) {
        super(MobExEntities.WRENCH_PROJECTILE.get(), shooter, level);
    }

    public void setDamage(float damageAmount) {
        damage = Math.max(1.0F, damageAmount);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04D;
    }

    @Override
    public void tick() {
        super.tick();
        if (!spawnAnimationPlayed && !level().isClientSide()) {
            spawnAnimationPlayed = true;
            triggerAnim("main", "spawn");
        }
        if (!level().isClientSide() && tickCount >= MAX_LIFETIME_TICKS)
            discard();
        if (level().isClientSide())
            level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        if (entity == getOwner() || RedstoneFaction.isAlly(entity))
            return false;
        if (entity instanceof WrenchProjectileEntity || entity instanceof RedstoneProjectileEntity)
            return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        super.onHit(hitResult);
        if (level().isClientSide())
            return;
        playSound(SoundEvents.ANVIL_HIT, 0.7F, 1.3F + random.nextFloat() * 0.2F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 10, 0.15D, 0.15D, 0.15D, 0.12D);
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 4, 0.1D, 0.1D, 0.1D, 0.01D);
        }
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        final Entity hitEntity = entityHitResult.getEntity();
        if (!(hitEntity instanceof LivingEntity livingTarget))
            return;
        livingTarget.hurt(damageSources().thrown(this, getOwner()), damage);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putBoolean("SpawnAnimPlayed", spawnAnimationPlayed);
        compoundTag.putFloat("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        spawnAnimationPlayed = compoundTag.getBoolean("SpawnAnimPlayed");
        if (compoundTag.contains("Damage"))
            damage = compoundTag.getFloat("Damage");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::handleAnimations)
                .triggerableAnim("spawn", SPAWN_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<WrenchProjectileEntity> animationState) {
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public String modelName() {
        return "wrench_projectile";
    }

    @Override
    public String textureName() {
        return "wrench_projectile";
    }

    @Override
    public String animationName() {
        return "wrench_projectile";
    }
}
