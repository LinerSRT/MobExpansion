package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.MosquitoAttackGoal;
import net.mcskill.mobexpansion.entity.ai.MosquitoHoverGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class MosquitoEntity extends SewerMobEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");
    private static final int ATTACK_DURATION_TICKS = 30;
    private static final int ATTACK_DAMAGE_TICK = 15;

    public MosquitoEntity(EntityType<? extends MosquitoEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        final FlyingPathNavigation flyingNavigation = new FlyingPathNavigation(this, level);
        flyingNavigation.setCanOpenDoors(false);
        flyingNavigation.setCanFloat(true);
        flyingNavigation.setCanPassDoors(true);
        return flyingNavigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MosquitoAttackGoal(this));
        goalSelector.addGoal(3, new MosquitoHoverGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, SewerFaction::isValidTarget));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        final var flyingSpeed = getAttribute(Attributes.FLYING_SPEED);
        if (flyingSpeed != null)
            flyingSpeed.setBaseValue(MobExParameters.get(this, "flyingSpeed", 0.35D));
        if (!level().isClientSide() && getDeltaMovement().y > 0.35D)
            setDeltaMovement(getDeltaMovement().x, 0.35D, getDeltaMovement().z);
    }

    public boolean tryStartAttack(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return false;
        beginAttack("attack", ATTACK_DURATION_TICKS, ATTACK_DAMAGE_TICK);
        return true;
    }

    @Override
    protected void performAttackHit() {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive())
            return;

        playSound(SoundEvents.BEE_STING, 1.0F, 2.0F);
        if (target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE)))
            heal(MobExParameters.getFloat(this, "drainHeal", 5.0F));

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNEEZE, target.getX(), target.getY() + 1.5D, target.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.5D, target.getZ(), 6, 0.35D, 0.35D, 0.35D, 0.0D);
        }
    }

    @Override
    protected int getHurtSoundCooldownTicks() {
        return 10;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 2;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_POLLINATE;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public float getVoicePitch() {
        return 2.0F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (isAttacking()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (isEffectiveAi() || isControlledByLocalInstance()) {
            if (isInWater()) {
                moveRelative(0.02F, travelVector);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(0.8D));
            } else if (isInLava()) {
                moveRelative(0.02F, travelVector);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(0.5D));
            } else {
                moveRelative(getSpeed(), travelVector);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(0.91D));
            }
        }
        calculateEntityAnimation(false);
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("attack", ATTACK_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<MosquitoEntity> animationState) {
        if (isDeadOrDying())
            return animationState.setAndContinue(DEATH_ANIMATION);
        if (isAttacking())
            return PlayState.CONTINUE;
        if (animationState.isMoving())
            return animationState.setAndContinue(WALK_ANIMATION);
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public String modelName() {
        return "mosquito";
    }

    @Override
    public String textureName() {
        return "mosquito";
    }

    @Override
    public String animationName() {
        return "mosquito";
    }
}
