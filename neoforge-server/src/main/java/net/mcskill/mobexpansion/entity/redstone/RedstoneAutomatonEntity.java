package net.mcskill.mobexpansion.entity.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.redstone.AutomatonMeleeGoal;
import net.mcskill.mobexpansion.entity.ai.redstone.ConstructLeashGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class RedstoneAutomatonEntity extends RedstoneConstructEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK1_ANIMATION = RawAnimation.begin().thenPlay("attack1");
    private static final RawAnimation ATTACK2_ANIMATION = RawAnimation.begin().thenPlay("attack2");
    private static final RawAnimation ATTACK3_ANIMATION = RawAnimation.begin().thenPlay("attack3");
    private static final RawAnimation ATTACK4_ANIMATION = RawAnimation.begin().thenPlay("attack4");
    private static final RawAnimation DAMAGE_ANIMATION = RawAnimation.begin().thenPlay("damage");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private static final int ATTACK1_TICKS = 36;
    private static final int ATTACK2_TICKS = 48;
    private static final int ATTACK3_TICKS = 36;
    private static final int ATTACK4_TICKS = 51;
    private static final double SPIN_RADIUS = 1.5D;
    private static final double MELEE_REACH = 3.0D;
    private static final int ATTACK_COOLDOWN_TICKS = 18;

    private int nextAttackIndex;

    public RedstoneAutomatonEntity(EntityType<? extends RedstoneAutomatonEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 12;
    }

    @Override
    protected int getDeathAnimationTicks() {
        return 40;
    }

    @Override
    protected int getAttackCooldownTicks() {
        return MobExParameters.getInt(this, "attackCooldownTicks", ATTACK_COOLDOWN_TICKS);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 52.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.FOLLOW_RANGE, 22.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D);
    }

    @Override
    protected void registerGoals() {
        registerSharedGoals();
        goalSelector.addGoal(1, new ConstructLeashGoal(this, 1.2D));
        goalSelector.addGoal(2, new AutomatonMeleeGoal(this, 1.2D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
    }

    public void tryStartAttack(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return;
        final int attackChoice = nextAttackIndex % 4;
        nextAttackIndex++;
        switch (attackChoice) {
            case 1 -> beginAttack("attack2", ATTACK2_TICKS, 24);
            case 2 -> beginAttack("attack3", ATTACK3_TICKS, 16);
            case 3 -> beginAttack("attack4", ATTACK4_TICKS, 24);
            default -> beginAttack("attack1", ATTACK1_TICKS, 16);
        }
    }

    @Override
    protected void performAttackHit() {
        final String attackName = getActiveAttackName();
        if (attackName == null)
            return;
        if ("attack4".equals(attackName)) {
            dealSpinDamage();
            return;
        }
        final LivingEntity target = getTarget();
        final double meleeReach = MobExParameters.get(this, "meleeReach", MELEE_REACH);
        if (target == null || !target.isAlive() || distanceToSqr(target) > meleeReach * meleeReach)
            return;
        doHurtTarget(target);
    }

    private void dealSpinDamage() {
        final double spinRadius = MobExParameters.get(this, "spinRadius", SPIN_RADIUS);
        final AABB spinArea = getBoundingBox().inflate(spinRadius, 0.5D, spinRadius);
        final List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, spinArea, RedstoneFaction::isValidTarget);
        final float spinDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE)
                * MobExParameters.getFloat(this, "spinDamageMultiplier", 0.85F);
        final double radiusSqr = spinRadius * spinRadius;
        for (LivingEntity victim : victims) {
            if (distanceToSqr(victim) > radiusSqr)
                continue;
            victim.hurt(damageSources().mobAttack(this), spinDamage);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("spawn", SPAWN_ANIMATION)
                .triggerableAnim("attack1", ATTACK1_ANIMATION)
                .triggerableAnim("attack2", ATTACK2_ANIMATION)
                .triggerableAnim("attack3", ATTACK3_ANIMATION)
                .triggerableAnim("attack4", ATTACK4_ANIMATION)
                .triggerableAnim("damage", DAMAGE_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<RedstoneAutomatonEntity> animationState) {
        if (isDeadOrDying())
            return animationState.setAndContinue(DEATH_ANIMATION);
        if (isAttacking() || isSpawning())
            return PlayState.CONTINUE;
        if (animationState.isMoving())
            return animationState.setAndContinue(WALK_ANIMATION);
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public String modelName() {
        return "redstone_automaton";
    }

    @Override
    public String textureName() {
        return "redstone_automaton";
    }

    @Override
    public String animationName() {
        return "redstone_automaton";
    }
}
