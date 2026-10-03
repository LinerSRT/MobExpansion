package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class SentinelStatueEntity extends MobExGeckoEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");

    private boolean awakening;
    private int auraCooldown;
    private float lockedYRot;
    private float lockedXRot;
    private boolean rotationLocked;

    public SentinelStatueEntity(EntityType<? extends SentinelStatueEntity> entityType, Level level) {
        super(entityType, level);
        setNoAi(true);
        setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.ARMOR, 20.0D);
    }

    @Override
    protected void registerGoals() {
    }

    private void lockFacing() {
        if (!rotationLocked) {
            lockedYRot = getYRot();
            lockedXRot = getXRot();
            rotationLocked = true;
        }
        setYRot(lockedYRot);
        setXRot(lockedXRot);
        yRotO = lockedYRot;
        xRotO = lockedXRot;
        yBodyRot = lockedYRot;
        yBodyRotO = lockedYRot;
        yHeadRot = lockedYRot;
        yHeadRotO = lockedYRot;
    }

    @Override
    public void tick() {
        lockFacing();
        super.tick();
        lockFacing();
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide() || awakening || isDeadOrDying())
            return;

        if (--auraCooldown > 0)
            return;
        auraCooldown = MobExParameters.getInt(this, "auraIntervalTicks", 10);
        applySlowAura();
    }

    @Override
    public void lookAt(@NotNull Entity entity, float maxYRotIncrease, float maxXRotIncrease) {
    }

    @Override
    public void turn(double yRot, double xRot) {
    }

    private void applySlowAura() {
        final double auraRadius = MobExParameters.get(this, "auraRadius", 3.0D);
        final int auraSlowDuration = MobExParameters.getInt(this, "auraSlowDuration", 20);
        final int auraSlowAmplifier = MobExParameters.getInt(this, "auraSlowAmplifier", 0);
        final AABB auraBox = getBoundingBox().inflate(auraRadius);
        final List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, auraBox, this::isAuraTarget);
        for (LivingEntity victim : victims)
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, auraSlowDuration, auraSlowAmplifier, false, false));
    }

    private boolean isAuraTarget(LivingEntity livingEntity) {
        if (!livingEntity.isAlive() || livingEntity == this)
            return false;
        if (livingEntity instanceof SentinelStatueEntity || livingEntity instanceof SentinelGolemEntity)
            return false;
        if (livingEntity instanceof Player player)
            return !player.isSpectator() && !player.isCreative();
        return livingEntity instanceof Enemy;
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (level().isClientSide() || awakening || isDeadOrDying())
            return false;
        final LivingEntity attacker = damageSource.getEntity() instanceof LivingEntity livingAttacker ? livingAttacker : null;
        awaken(attacker);
        return false;
    }

    private void awaken(@Nullable LivingEntity attacker) {
        if (!(level() instanceof ServerLevel serverLevel) || awakening)
            return;
        awakening = true;

        final SentinelGolemEntity golem = MobExEntities.SENTINEL_GOLEM.get().create(serverLevel);
        if (golem == null) {
            this.awakening = false;
            return;
        }

        golem.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        golem.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.TRIGGERED, null);
        golem.beginAwakening();
        if (attacker != null && attacker.isAlive())
            golem.setTarget(attacker);
        serverLevel.addFreshEntityWithPassengers(golem);
        this.discard();
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(@NotNull Entity entity) {
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::handleAnimations));
    }

    private PlayState handleAnimations(AnimationState<SentinelStatueEntity> animationState) {
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public String modelName() {
        return "sentinell_sword_statue";
    }

    @Override
    public String textureName() {
        return "sentinell_sword_statue";
    }

    @Override
    public String animationName() {
        return "sentinell_sword_statue";
    }
}
