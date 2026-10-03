package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.LeechAttackGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class LeechEntity extends SewerMobEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle_loop");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk_loop");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");

    private static final int ATTACK_DURATION_TICKS = 90;
    private static final int[] VAMP_HIT_TICKS = {10, 17, 24, 30};

    private int lastVampHitIndex = -1;

    public LeechEntity(EntityType<? extends LeechEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void registerGoals() {
        registerSharedGoals(0.9D);
        goalSelector.addGoal(2, new LeechAttackGoal(this));
    }

    public void tryStartAttack(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return;
        lastVampHitIndex = -1;
        beginAttack("attack", ATTACK_DURATION_TICKS, VAMP_HIT_TICKS[0], VAMP_HIT_TICKS[VAMP_HIT_TICKS.length - 1]);
    }

    @Override
    protected void performAttackHit() {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive())
            return;

        final int elapsedTicks = getAttackElapsedTicks();
        int hitIndex = -1;
        for (int index = 0; index < VAMP_HIT_TICKS.length; index++) {
            if (VAMP_HIT_TICKS[index] == elapsedTicks) {
                hitIndex = index;
                break;
            }
        }
        if (hitIndex < 0 || hitIndex == lastVampHitIndex)
            return;
        lastVampHitIndex = hitIndex;

        final double attackReach = MobExParameters.get(this, "attackReach", 4.5D);
        if (distanceToSqr(target) > attackReach * attackReach * 1.35D)
            return;

        playSound(SoundEvents.AXOLOTL_ATTACK, 0.5F, 0.1F);
        if (target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            heal(MobExParameters.getFloat(this, "vampHeal", 1.0F));
            spawnVampParticles(target);
        }
    }

    private void spawnVampParticles(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel))
            return;

        final Vec3 start = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        final Vec3 end = position().add(0.0D, getBbHeight() * 0.4D, 0.0D);
        final Vec3 delta = end.subtract(start);
        final int steps = 8;
        for (int step = 0; step <= steps; step++) {
            final double progress = step / (double) steps;
            final double particleX = start.x + delta.x * progress;
            final double particleY = start.y + delta.y * progress;
            final double particleZ = start.z + delta.z * progress;
            serverLevel.sendParticles(ParticleTypes.SNEEZE, particleX, particleY, particleZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, particleX, particleY, particleZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.2D, getZ(), 6, 0.2D, 0.1D, 0.2D, 0.0D);
    }

    @Override
    protected void endAttack() {
        lastVampHitIndex = -1;
        super.endAttack();
    }

    @Override
    protected int getAttackCooldownTicks() {
        return 40;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AXOLOTL_IDLE_AIR;
    }

    @Override
    public float getVoicePitch() {
        return 0.1F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("attack", ATTACK_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<LeechEntity> animationState) {
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
        return "leech";
    }

    @Override
    public String textureName() {
        return "leech";
    }

    @Override
    public String animationName() {
        return "leech";
    }
}
