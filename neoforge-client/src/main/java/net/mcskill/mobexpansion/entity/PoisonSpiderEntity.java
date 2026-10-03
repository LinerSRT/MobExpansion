package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.PoisonSpiderRangedGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderAvoidBrightLightGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderEnterNestGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderFleeToNestGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderNearestAttackableTargetGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderPatrolGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderProtectNestmateGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderRefillSilkGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderReturnToNestGoal;
import net.mcskill.mobexpansion.entity.ai.SpiderWeaveSilkGoal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.List;

public class PoisonSpiderEntity extends RegularSpiderEntity implements RangedAttackMob {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("atk");
    private static final RawAnimation TRAP_ANIMATION = RawAnimation.begin().thenPlay("trap");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private static final float DEFAULT_SPIT_SPEED = 1.15F;
    private static final float SPIT_INACCURACY = 3.0F;
    private static final int SPIT_DELAY_TICKS = 25;

    @Nullable
    private LivingEntity pendingSpitTarget;
    private float pendingSpitVelocityScale;
    private int spitDelayTicks;

    public PoisonSpiderEntity(EntityType<? extends PoisonSpiderEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SpiderFleeToNestGoal(this, 1.35D));
        goalSelector.addGoal(2, new PoisonSpiderRangedGoal(this, 1.0D));
        goalSelector.addGoal(3, new SpiderRefillSilkGoal(this));
        goalSelector.addGoal(4, new SpiderEnterNestGoal(this));
        goalSelector.addGoal(5, new SpiderWeaveSilkGoal(this));
        goalSelector.addGoal(6, new SpiderAvoidBrightLightGoal(this));
        goalSelector.addGoal(7, new SpiderPatrolGoal(this, 1.0D));
        goalSelector.addGoal(8, new SpiderReturnToNestGoal(this, 1.0D));
        goalSelector.addGoal(9, new MoveTowardsRestrictionGoal(this, 1.0D));
        goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new SpiderProtectNestmateGoal(this));
        targetSelector.addGoal(3, new SpiderNearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isValidAttackTarget));
    }

    @Override
    protected double getMaxTargetHeightAbove() {
        return 12.0D;
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity target, float velocityScale) {
        if (level().isClientSide() || !target.isAlive() || isPreparingSpit())
            return;

        triggerAnim("main", "trap");
        pendingSpitTarget = target;
        pendingSpitVelocityScale = velocityScale;
        spitDelayTicks = SPIT_DELAY_TICKS;
        getNavigation().stop();
    }

    public boolean isPreparingSpit() {
        return spitDelayTicks > 0;
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && spitDelayTicks > 0) {
            if (pendingSpitTarget != null && pendingSpitTarget.isAlive())
                getLookControl().setLookAt(pendingSpitTarget, 30.0F, 30.0F);
            getNavigation().stop();
            spitDelayTicks--;
            if (spitDelayTicks <= 0)
                releasePendingSpit();
        }
        super.tick();
    }

    private void releasePendingSpit() {
        final LivingEntity target = pendingSpitTarget;
        final float velocityScale = pendingSpitVelocityScale;
        pendingSpitTarget = null;
        pendingSpitVelocityScale = 0.0F;
        if (target == null || !target.isAlive())
            return;

        final PoisonProjectileEntity projectile = new PoisonProjectileEntity(level(), this);
        final double eyeY = getEyeY() - 0.15D;
        projectile.moveTo(getX(), eyeY, getZ(), getYRot(), getXRot());

        final double deltaX = target.getX() - getX();
        final double deltaY = target.getY(0.4D) - eyeY;
        final double deltaZ = target.getZ() - getZ();
        final double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        final float spitSpeed = MobExParameters.getFloat(this, "spitSpeed", DEFAULT_SPIT_SPEED);
        projectile.shoot(deltaX, deltaY + horizontalDistance * 0.15D, deltaZ, spitSpeed * Mth.clamp(velocityScale, 0.1F, 1.0F), SPIT_INACCURACY);
        playSound(SoundEvents.LLAMA_SPIT, 1.0F, 0.8F + random.nextFloat() * 0.3F);
        level().addFreshEntity(projectile);
    }

    @Override
    public float getFleeHealthRatio() {
        return MobExParameters.getFloat(this, "fleeHealthRatio", 0.15F);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        if (effectInstance.is(MobEffects.POISON))
            return false;
        return super.canBeAffected(effectInstance);
    }

    @Override
    public boolean canWeaveSilk() {
        return super.canWeaveSilk() && !isPreparingSpit();
    }

    @Override
    protected void applyMeleeAttackEffects(LivingEntity livingTarget) {
        triggerAnim("main", "atk");
        livingTarget.addEffect(new MobEffectInstance(
                MobEffects.POISON,
                MobExParameters.getInt(this, "bitePoisonDuration", 160),
                MobExParameters.getInt(this, "bitePoisonAmplifier", 1)
        ));
        livingTarget.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                MobExParameters.getInt(this, "biteSlowDuration", 100),
                1
        ));
        signalNestTrap(livingTarget);
    }

    private void signalNestTrap(LivingEntity trappedTarget) {
        if (level().isClientSide() || getSpawnerUUID().isEmpty())
            return;

        final List<RegularSpiderEntity> nestmates = level().getEntitiesOfClass(
                RegularSpiderEntity.class,
                getBoundingBox().inflate(14.0D),
                nestmate -> nestmate != this && nestmate.isAlive() && isSameNest(nestmate)
        );
        for (RegularSpiderEntity nestmate : nestmates) {
            nestmate.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
            if ((nestmate.getTarget() == null || nestmate.getTarget() == trappedTarget)
                    && nestmate.isWithinVerticalEngageRange(trappedTarget))
                nestmate.setTarget(trappedTarget);
            if (!(nestmate instanceof PoisonSpiderEntity))
                nestmate.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handlePoisonAnimations)
                .triggerableAnim("spawn", SPAWN_ANIMATION)
                .triggerableAnim("atk", ATTACK_ANIMATION)
                .triggerableAnim("trap", TRAP_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handlePoisonAnimations(AnimationState<PoisonSpiderEntity> animationState) {
        if (isDeadOrDying())
            return animationState.setAndContinue(DEATH_ANIMATION);
        if (animationState.isMoving())
            return animationState.setAndContinue(WALK_ANIMATION);
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public String modelName() {
        return "poison_spider";
    }

    @Override
    public String textureName() {
        return "poison_spider";
    }

    @Override
    public String animationName() {
        return "poison_spider";
    }
}
