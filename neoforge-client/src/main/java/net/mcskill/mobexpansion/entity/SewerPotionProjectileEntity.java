package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExEntities;
import net.mcskill.mobexpansion.init.MobExParameters;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class SewerPotionProjectileEntity extends ThrowableProjectile implements GeoEntity, IModel {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");

    private static final float IMPACT_DAMAGE = 6.0F;
    private static final double IMPACT_RADIUS = 4.0D;
    private static final int MAX_LIFETIME_TICKS = 80;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SewerPotionProjectileEntity(EntityType<? extends SewerPotionProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SewerPotionProjectileEntity(Level level, LivingEntity shooter) {
        super(MobExEntities.SEWER_POTION_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(ParticleTypes.SNEEZE, getX(), getY(), getZ(), 3, 0.05D, 0.05D, 0.05D, 0.01D);
        if (!level().isClientSide() && tickCount >= MAX_LIFETIME_TICKS)
            explodeAndDiscard();
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        if (entity == getOwner() || SewerFaction.isAlly(entity))
            return false;
        if (entity instanceof SewerPotionProjectileEntity || entity instanceof SewerBoltProjectileEntity)
            return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        super.onHit(hitResult);
        if (!level().isClientSide())
            explodeAndDiscard();
    }

    private void explodeAndDiscard() {
        playSound(SoundEvents.SPLASH_POTION_BREAK, 1.2F, 0.1F);
        playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.1F);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.GLOW_SQUID_INK, getX(), getY() + 0.7D, getZ(), 40, 1.5D, 0.7D, 1.5D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.SNEEZE, getX(), getY() + 0.1D, getZ(), 28, 2.5D, 0.2D, 2.5D, 0.02D);
        }

        final float impactDamage;
        final double impactRadius;
        final Entity owner = getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            impactDamage = MobExParameters.getFloat(livingOwner, "potionImpactDamage", IMPACT_DAMAGE);
            impactRadius = MobExParameters.get(livingOwner, "potionImpactRadius", IMPACT_RADIUS);
        } else {
            impactDamage = IMPACT_DAMAGE;
            impactRadius = IMPACT_RADIUS;
        }

        final AABB impactArea = getBoundingBox().inflate(impactRadius);
        final List<LivingEntity> victims = level().getEntitiesOfClass(
                LivingEntity.class,
                impactArea,
                livingEntity -> livingEntity.isAlive() && !SewerFaction.isAlly(livingEntity) && livingEntity != getOwner()
        );
        for (LivingEntity victim : victims) {
            victim.hurt(damageSources().thrown(this, getOwner()), impactDamage);
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }

        final SewerPoisonAreaEntity poisonArea = new SewerPoisonAreaEntity(level());
        if (owner instanceof LivingEntity livingOwner)
            poisonArea.configureFromOwner(livingOwner);
        poisonArea.moveTo(getX(), getY(), getZ());
        level().addFreshEntity(poisonArea);
        discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::handleAnimations));
    }

    private PlayState handleAnimations(AnimationState<SewerPotionProjectileEntity> animationState) {
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public String modelName() {
        return "potion_projectile";
    }

    @Override
    public String textureName() {
        return "potion_projectile";
    }

    @Override
    public String animationName() {
        return "potion_projectile";
    }
}
