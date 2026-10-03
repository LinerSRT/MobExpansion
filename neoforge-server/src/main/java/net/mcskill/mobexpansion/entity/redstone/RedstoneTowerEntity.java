package net.mcskill.mobexpansion.entity.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.redstone.ConstructLeashGoal;
import net.mcskill.mobexpansion.entity.ai.redstone.TowerCombatGoal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RedstoneTowerEntity extends RedstoneConstructEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK1_ANIMATION = RawAnimation.begin().thenPlay("attack1");
    private static final RawAnimation ATTACK2_ANIMATION = RawAnimation.begin().thenPlay("attack2");
    private static final RawAnimation ATTACK3_ANIMATION = RawAnimation.begin().thenPlay("attack3");
    private static final RawAnimation DAMAGE_ANIMATION = RawAnimation.begin().thenPlay("damage");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private static final int ATTACK1_TICKS = 104;
    private static final int ATTACK1_LASER_START_TICK = 20;
    private static final int ATTACK1_LASER_END_TICK = 80;
    private static final int ATTACK2_TICKS = 68;
    private static final int ATTACK2_DAMAGE_START_TICK = 36;
    private static final int ATTACK2_DAMAGE_END_TICK = 44;
    private static final int ATTACK3_TICKS = 22;
    private static final int ATTACK3_SHOOT_TICK = 10;

    private static final double LASER_RADIUS = 6.0D;
    private static final double STOMP_RADIUS = 2.5D;
    private static final float PROJECTILE_SPEED = 1.2F;
    private static final float PROJECTILE_INACCURACY = 1.5F;

    private final Set<UUID> stunDamagedIds = new HashSet<>();
    private int nextAttackIndex;

    public RedstoneTowerEntity(EntityType<? extends RedstoneTowerEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 65.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.14D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D);
    }

    @Override
    protected int getSpawnAnimationTicks() {
        return 19;
    }

    @Override
    protected int getAttackCooldownTicks() {
        return 24;
    }

    @Override
    protected int getDeathAnimationTicks() {
        return 61;
    }

    @Override
    protected void registerGoals() {
        registerSharedGoals();
        goalSelector.addGoal(1, new ConstructLeashGoal(this, 0.9D));
        goalSelector.addGoal(2, new TowerCombatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.65D));
    }

    public boolean tryStartAttack(LivingEntity target, boolean inMeleeRange) {
        if (isBusy() || target == null || !target.isAlive())
            return false;

        if (inMeleeRange) {
            stunDamagedIds.clear();
            beginAttack("attack2", ATTACK2_TICKS, ATTACK2_DAMAGE_START_TICK, ATTACK2_DAMAGE_END_TICK);
            return true;
        }

        final int attackChoice = nextAttackIndex % 2;
        nextAttackIndex++;
        if (attackChoice == 0) {
            beginAttack("attack1", ATTACK1_TICKS, ATTACK1_LASER_START_TICK, ATTACK1_LASER_END_TICK);
        } else {
            beginAttack("attack3", ATTACK3_TICKS, ATTACK3_SHOOT_TICK);
        }
        return true;
    }

    @Override
    protected void performAttackHit() {
        final String attackName = getActiveAttackName();
        if (attackName == null)
            return;
        switch (attackName) {
            case "attack1" -> dealLaserNovaTick();
            case "attack2" -> dealStompStunTick();
            case "attack3" -> shootProjectileAtTarget();
            default -> {
            }
        }
    }

    @Override
    protected void endAttack() {
        stunDamagedIds.clear();
        super.endAttack();
    }

    private void dealLaserNovaTick() {
        final double laserRadius = MobExParameters.get(this, "laserRadius", LASER_RADIUS);
        final AABB area = getBoundingBox().inflate(laserRadius, 1.5D, laserRadius);
        final List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, area, RedstoneFaction::isValidTarget);
        final float laserDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE)
                * MobExParameters.getFloat(this, "laserDamageMultiplier", 0.22F);
        final double radiusSqr = laserRadius * laserRadius;

        for (LivingEntity victim : victims) {
            if (distanceToSqr(victim) > radiusSqr)
                continue;
            victim.hurt(damageSources().mobAttack(this), laserDamage);
        }

        if (getAttackElapsedTicks() == ATTACK1_LASER_START_TICK)
            playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.55F, 1.6F);
    }

    private void dealStompStunTick() {
        final double stompRadius = MobExParameters.get(this, "stompRadius", STOMP_RADIUS);
        final AABB area = getBoundingBox().inflate(stompRadius, 0.5D, stompRadius);
        final List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, area, RedstoneFaction::isValidTarget);
        final float tickDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE)
                * MobExParameters.getFloat(this, "stompDamageMultiplier", 0.35F);
        final boolean firstWindowTick = getAttackElapsedTicks() == ATTACK2_DAMAGE_START_TICK;
        final int slowDuration = MobExParameters.getInt(this, "stompSlowDuration", 80);
        final int slowAmplifier = MobExParameters.getInt(this, "stompSlowAmplifier", 2);
        final int weakDuration = MobExParameters.getInt(this, "stompWeakDuration", 60);
        final int weakAmplifier = MobExParameters.getInt(this, "stompWeakAmplifier", 0);
        final double radiusSqr = stompRadius * stompRadius;

        for (LivingEntity victim : victims) {
            if (distanceToSqr(victim) > radiusSqr)
                continue;
            victim.hurt(damageSources().mobAttack(this), tickDamage);
            if (!stunDamagedIds.contains(victim.getUUID())) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowDuration, slowAmplifier));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakDuration, weakAmplifier));
                victim.knockback(0.55D, victim.getX() - getX(), victim.getZ() - getZ());
                stunDamagedIds.add(victim.getUUID());
            }
        }

        if (firstWindowTick)
            playSound(SoundEvents.ANVIL_LAND, 0.85F, 0.7F);
    }

    private void shootProjectileAtTarget() {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive())
            return;
        shootProjectile(target);
    }

    private void shootProjectile(LivingEntity target) {
        final RedstoneProjectileEntity projectile = new RedstoneProjectileEntity(level(), this);
        projectile.setDamage(MobExParameters.getFloat(this, "projectileDamage", 7.0F));
        final double eyeY = getY() + getBbHeight() * 0.75D;
        projectile.moveTo(getX(), eyeY, getZ(), getYRot(), getXRot());

        final double deltaX = target.getX() - getX();
        final double deltaY = target.getY(0.4D) - eyeY;
        final double deltaZ = target.getZ() - getZ();
        final double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        projectile.shoot(deltaX, deltaY + horizontalDistance * 0.1D, deltaZ, PROJECTILE_SPEED, PROJECTILE_INACCURACY);
        level().addFreshEntity(projectile);
        playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.8F + random.nextFloat() * 0.2F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("spawn", SPAWN_ANIMATION)
                .triggerableAnim("attack1", ATTACK1_ANIMATION)
                .triggerableAnim("attack2", ATTACK2_ANIMATION)
                .triggerableAnim("attack3", ATTACK3_ANIMATION)
                .triggerableAnim("damage", DAMAGE_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<RedstoneTowerEntity> animationState) {
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
        return "redstone_tower";
    }

    @Override
    public String textureName() {
        return "redstone_tower";
    }

    @Override
    public String animationName() {
        return "redstone_tower";
    }
}
