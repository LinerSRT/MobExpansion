package net.mcskill.mobexpansion.entity.redstone;

import net.mcskill.mobexpansion.entity.IModel;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
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

public class RedstoneProjectileEntity extends ThrowableProjectile implements GeoEntity, IModel {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private static final float BASE_DAMAGE = 6.0F;
    private static final int MAX_LIFETIME_TICKS = 80;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean spawnAnimationPlayed;
    private float damage = BASE_DAMAGE;

    public RedstoneProjectileEntity(EntityType<? extends RedstoneProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public RedstoneProjectileEntity(Level level, LivingEntity shooter) {
        super(MobExEntities.REDSTONE_PROJECTILE.get(), shooter, level);
    }

    public void setDamage(float damageAmount) {
        damage = Math.max(1.0F, damageAmount);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.02D;
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
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        if (entity == getOwner() || RedstoneFaction.isAlly(entity))
            return false;
        if (entity instanceof RedstoneProjectileEntity || entity instanceof WrenchProjectileEntity)
            return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        super.onHit(hitResult);
        if (level().isClientSide())
            return;
        playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 0.45F, 1.4F + random.nextFloat() * 0.2F);
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

    private PlayState handleAnimations(AnimationState<RedstoneProjectileEntity> animationState) {
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public String modelName() {
        return "redstone_projectile";
    }

    @Override
    public String textureName() {
        return "redstone_projectile";
    }

    @Override
    public String animationName() {
        return "redstone_projectile";
    }
}
