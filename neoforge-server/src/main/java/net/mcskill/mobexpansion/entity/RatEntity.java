package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.SewerMeleeAttackGoal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class RatEntity extends SewerMobEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");

    private static final int ATTACK_DURATION_TICKS = 31;
    private static final int ATTACK_DAMAGE_TICK = 8;

    public RatEntity(EntityType<? extends RatEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void registerGoals() {
        registerSharedGoals(1.0D);
        goalSelector.addGoal(2, new SewerMeleeAttackGoal(this, 1.15D, MobExParameters.get(this, "attackReach", 2.0D), this::tryStartAttack));
    }

    public void tryStartAttack(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return;
        beginAttack("attack", ATTACK_DURATION_TICKS, ATTACK_DAMAGE_TICK);
        playSound(SoundEvents.AXOLOTL_IDLE_WATER, 1.0F, 0.1F);
    }

    @Override
    protected void performAttackHit() {
        final LivingEntity target = getTarget();
        final double attackReach = MobExParameters.get(this, "attackReach", 2.0D);
        if (target == null || !target.isAlive() || distanceToSqr(target) > attackReach * attackReach * 1.5D)
            return;
        playSound(SoundEvents.RABBIT_ATTACK, 1.0F, 1.7F);
        target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        playSound(SoundEvents.SILVERFISH_AMBIENT, 1.0F, 2.0F);
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.RABBIT_ATTACK;
    }

    @Override
    public float getVoicePitch() {
        return 2.0F;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("attack", ATTACK_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<RatEntity> animationState) {
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
        return "rat";
    }

    @Override
    public String textureName() {
        return "rat";
    }

    @Override
    public String animationName() {
        return "rat";
    }
}
