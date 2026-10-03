package net.mcskill.mobexpansion.entity.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.redstone.DroneHoverGoal;
import net.mcskill.mobexpansion.entity.ai.redstone.DroneShootGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class RedstoneDroneEntity extends RedstoneConstructEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack1");
    private static final RawAnimation DAMAGE_ANIMATION = RawAnimation.begin().thenPlay("damage");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private static final int ATTACK_TICKS = 12;
    private static final int SHOOT_DELAY_TICKS = 5;
    private static final float PROJECTILE_SPEED = 1.45F;
    private static final float PROJECTILE_INACCURACY = 1.8F;
    private static final double MUZZLE_DISTANCE = 0.9D;

    public RedstoneDroneEntity(EntityType<? extends RedstoneDroneEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 8;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.ARMOR, 1.0D);
    }

    @Override
    protected int getAttackCooldownTicks() {
        return 4;
    }

    @Override
    protected int getDeathAnimationTicks() {
        return 30;
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
        registerSharedGoals();
        goalSelector.addGoal(2, new DroneShootGoal(this));
        goalSelector.addGoal(3, new DroneHoverGoal(this));
    }

    public boolean tryStartShoot(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return false;
        beginAttack("attack1", ATTACK_TICKS, SHOOT_DELAY_TICKS);
        return true;
    }

    @Override
    protected void performAttackHit() {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive())
            return;
        shootAt(target);
    }

    private void shootAt(LivingEntity target) {
        final Vec3 lookVector = getViewVector(1.0F);
        final Vec3 muzzlePosition = new Vec3(getX(), getY() + getBbHeight() * 0.45D, getZ())
                .add(lookVector.scale(-MUZZLE_DISTANCE));

        final RedstoneProjectileEntity projectile = new RedstoneProjectileEntity(level(), this);
        projectile.setDamage(MobExParameters.getFloat(this, "projectileDamage", 4.0F));
        projectile.moveTo(muzzlePosition.x, muzzlePosition.y, muzzlePosition.z, getYRot() + 180.0F, getXRot());

        final double deltaX = target.getX() - muzzlePosition.x;
        final double deltaY = target.getY(0.35D) - muzzlePosition.y;
        final double deltaZ = target.getZ() - muzzlePosition.z;
        final double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        projectile.shoot(deltaX, deltaY + horizontalDistance * 0.06D, deltaZ, PROJECTILE_SPEED, PROJECTILE_INACCURACY);
        level().addFreshEntity(projectile);
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (isSpawning()) {
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
                .triggerableAnim("spawn", SPAWN_ANIMATION)
                .triggerableAnim("attack1", ATTACK_ANIMATION)
                .triggerableAnim("damage", DAMAGE_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<RedstoneDroneEntity> animationState) {
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
        return "redstone_drone";
    }

    @Override
    public String textureName() {
        return "redstone_drone";
    }

    @Override
    public String animationName() {
        return "redstone_drone";
    }
}
