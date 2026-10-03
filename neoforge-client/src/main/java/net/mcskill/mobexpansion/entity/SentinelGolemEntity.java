package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.SentinelGolemCombatGoal;
import net.mcskill.mobexpansion.init.MobExSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.List;

public class SentinelGolemEntity extends MobExGeckoEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK1_ANIMATION = RawAnimation.begin().thenPlay("attack1");
    private static final RawAnimation ATTACK2_ANIMATION = RawAnimation.begin().thenPlay("attack2");
    private static final RawAnimation ESTOCADA_ANIMATION = RawAnimation.begin().thenPlay("estocada");
    private static final RawAnimation GOLPT_ANIMATION = RawAnimation.begin().thenPlay("golpt");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private static final int AWAKEN_LOCK_TICKS = 96;
    private static final int ATTACK1_TICKS = 48;
    private static final int ATTACK1_HIT_TICK = 22;
    private static final int ATTACK2_TICKS = 48;
    private static final int ATTACK2_HIT_TICK = 22;
    private static final int ESTOCADA_TICKS = 16;
    private static final int ESTOCADA_HIT_START = 2;
    private static final int ESTOCADA_HIT_END = 8;
    private static final int GOLPT_TICKS = 60;
    private static final int GOLPT_LEAP_TICK = 27;
    private static final int GOLPT_SLAM_START = 28;
    private static final int GOLPT_SLAM_END = 36;
    private static final int DEATH_ANIMATION_TICKS = 104;

    private int awakenTicksRemaining;
    private boolean attacking;
    private int attackTicksRemaining;
    private int attackDurationTicks;
    private int damageWindowStart = -1;
    private int damageWindowEnd = -1;
    private int attackCooldownTicks;
    @Nullable
    private String activeAttackName;
    private int nextMeleeIndex;
    private boolean spawnSoundQueued;

    public SentinelGolemEntity(EntityType<? extends SentinelGolemEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 40;
        setPersistenceRequired();
    }

    @SuppressWarnings("deprecation")
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        final SpawnGroupData spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (spawnType != MobSpawnType.TRIGGERED)
            beginAwakening();
        return spawnData;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void beginAwakening() {
        awakenTicksRemaining = AWAKEN_LOCK_TICKS;
        spawnSoundQueued = true;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide()) {
            triggerAnim("main", "spawn");
            playSound(MobExSounds.SENTINEL_GM2.get(), 0.5F, 0.1F);
        }
    }

    public boolean isAwakening() {
        return awakenTicksRemaining > 0;
    }

    public boolean isBusy() {
        return isAwakening() || attacking || attackCooldownTicks > 0 || isDeadOrDying();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new SentinelGolemCombatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, MobExTargeting::isSurvivalPlayer));
    }

    private boolean isValidTarget(LivingEntity livingEntity) {
        if (!livingEntity.isAlive() || livingEntity instanceof SentinelGolemEntity || livingEntity instanceof SentinelStatueEntity)
            return false;
        return MobExTargeting.isValidHostileTarget(livingEntity);
    }

    public boolean tryMeleeAttack(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return false;
        if (nextMeleeIndex % 2 == 0) {
            beginAttack("attack1", ATTACK1_TICKS, ATTACK1_HIT_TICK, ATTACK1_HIT_TICK);
            playSound(MobExSounds.SENTINEL_GM1.get(), 1.0F, 1.0F);
        } else {
            beginAttack("attack2", ATTACK2_TICKS, ATTACK2_HIT_TICK, ATTACK2_HIT_TICK);
            playSound(MobExSounds.SENTINEL_GM1.get(), 1.0F, 1.1F);
        }
        nextMeleeIndex++;
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 99, false, false));
        return true;
    }

    public boolean tryEstocada(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return false;
        beginAttack("estocada", ESTOCADA_TICKS, ESTOCADA_HIT_START, ESTOCADA_HIT_END);
        playSound(MobExSounds.SENTINEL_GM1.get(), 1.0F, 0.7F);
        playSound(MobExSounds.SENTINEL_GM5.get(), 1.0F, 0.7F);
        leapToward(target, 1.35D, 0.15D);
        return true;
    }

    public boolean tryJumpSlam(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return false;
        beginAttack("golpt", GOLPT_TICKS, GOLPT_SLAM_START, GOLPT_SLAM_END);
        playSound(MobExSounds.SENTINEL_GM1.get(), 1.0F, 0.5F);
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 99, false, false));
        return true;
    }

    private void beginAttack(String attackName, int durationTicks, int damageStartTick, int damageEndTick) {
        attacking = true;
        activeAttackName = attackName;
        attackDurationTicks = durationTicks;
        attackTicksRemaining = durationTicks;
        damageWindowStart = damageStartTick;
        damageWindowEnd = damageEndTick;
        getNavigation().stop();
        triggerAnim("main", attackName);
    }

    private int getAttackElapsedTicks() {
        return attackDurationTicks - attackTicksRemaining;
    }

    @Override
    public void tick() {
        if (attackCooldownTicks > 0)
            attackCooldownTicks--;

        if (isAwakening()) {
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
            awakenTicksRemaining--;
            if (spawnSoundQueued && awakenTicksRemaining == AWAKEN_LOCK_TICKS - 56) {
                playSound(MobExSounds.SENTINEL_GM7.get(), 1.0F, 0.6F);
                spawnSoundQueued = false;
            }
            if (awakenTicksRemaining <= 0)
                removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            else
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
        }

        if (!level().isClientSide() && attacking)
            tickAttack();

        super.tick();
    }

    private void tickAttack() {
        getNavigation().stop();
        final int elapsedTicks = getAttackElapsedTicks();
        final LivingEntity target = getTarget();

        if ("golpt".equals(activeAttackName)) {
            if (elapsedTicks == 13)
                addEffect(new MobEffectInstance(MobEffects.LEVITATION, 14, 6, false, false));
            if (elapsedTicks == GOLPT_LEAP_TICK && target != null && target.isAlive()) {
                removeEffect(MobEffects.LEVITATION);
                leapToward(target, 1.6D, -0.2D);
            }
        }

        if (elapsedTicks >= damageWindowStart && elapsedTicks <= damageWindowEnd)
            performAttackHit(elapsedTicks);

        attackTicksRemaining--;
        if (attackTicksRemaining <= 0)
            endAttack();
    }

    private void performAttackHit(int elapsedTicks) {
        final String attackName = activeAttackName;
        if (attackName == null)
            return;

        switch (attackName) {
            case "attack1" -> {
                if (elapsedTicks == ATTACK1_HIT_TICK) {
                    playSound(MobExSounds.SENTINEL_GM4.get(), 1.0F, 1.0F);
                    dealMeleeBurst(MobExParameters.getFloat(this, "meleeDamage", 5.0F), 2.5D, 0.55D);
                }
            }
            case "attack2" -> {
                if (elapsedTicks == ATTACK2_HIT_TICK) {
                    playSound(MobExSounds.SENTINEL_GM3.get(), 1.0F, 1.0F);
                    dealMeleeBurst(MobExParameters.getFloat(this, "meleeDamage", 5.0F), 2.5D, 0.55D);
                }
            }
            case "estocada" -> dealMeleeBurst(MobExParameters.getFloat(this, "estocadaDamage", 5.0F), 3.5D, 0.35D);
            case "golpt" -> {
                if (elapsedTicks == GOLPT_SLAM_START)
                    playSound(MobExSounds.SENTINEL_GM6.get(), 0.7F, 1.0F);
                dealSlamBurst(MobExParameters.getFloat(this, "slamDamage", 7.0F));
            }
            default -> {
            }
        }
    }

    private void dealMeleeBurst(float damageAmount, double radius, double knockbackStrength) {
        final AABB area = getBoundingBox().inflate(radius, 1.0D, radius);
        final List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, area, this::isValidTarget);
        for (LivingEntity victim : victims) {
            if (distanceToSqr(victim) > radius * radius)
                continue;
            victim.hurt(damageSources().mobAttack(this), damageAmount);
            victim.knockback(knockbackStrength, victim.getX() - getX(), victim.getZ() - getZ());
        }
    }

    private void dealSlamBurst(float damageAmount) {
        final double slamRadius = MobExParameters.get(this, "slamRadius", 4.0D);
        final AABB area = getBoundingBox().inflate(slamRadius, 1.0D, slamRadius);
        final List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, area, this::isValidTarget);
        for (LivingEntity victim : victims) {
            if (distanceToSqr(victim) > slamRadius * slamRadius)
                continue;
            victim.hurt(damageSources().mobAttack(this), damageAmount);
            victim.knockback(0.8D, victim.getX() - getX(), victim.getZ() - getZ());
            victim.setDeltaMovement(victim.getDeltaMovement().add(0.0D, 0.35D, 0.0D));
        }
    }

    private void leapToward(LivingEntity target, double horizontalStrength, double verticalStrength) {
        final Vec3 direction = target.position().subtract(position());
        final double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontal < 0.001D)
            return;
        setDeltaMovement(
                direction.x / horizontal * horizontalStrength,
                verticalStrength,
                direction.z / horizontal * horizontalStrength
        );
        hasImpulse = true;
    }

    private void endAttack() {
        attacking = false;
        activeAttackName = null;
        attackTicksRemaining = 0;
        attackDurationTicks = 0;
        damageWindowStart = -1;
        damageWindowEnd = -1;
        attackCooldownTicks = 12;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.is(DamageTypes.GENERIC_KILL) || damageSource.is(DamageTypes.FELL_OUT_OF_WORLD))
            return super.isInvulnerableTo(damageSource);
        if (damageSource.is(DamageTypes.MOB_ATTACK) || damageSource.is(DamageTypes.PLAYER_ATTACK) || damageSource.is(DamageTypes.MOB_ATTACK_NO_AGGRO))
            return false;
        if (damageSource.is(DamageTypes.ON_FIRE) || damageSource.is(DamageTypes.IN_FIRE) || damageSource.is(DamageTypes.LAVA))
            return false;
        return !damageSource.is(DamageTypes.EXPLOSION) && !damageSource.is(DamageTypes.PLAYER_EXPLOSION);
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (isAwakening())
            return false;
        if (isInvulnerableTo(damageSource))
            return false;
        if (damageSource.is(DamageTypes.EXPLOSION) || damageSource.is(DamageTypes.PLAYER_EXPLOSION))
            amount *= MobExParameters.getFloat(this, "explosionDamageMultiplier", 0.5F);
        return super.hurt(damageSource, amount);
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!isDeadOrDying()) {
            triggerAnim("main", "death");
            playSound(MobExSounds.SENTINEL_GM2.get(), 1.0F, 1.0F);
        }
        super.die(damageSource);
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= DEATH_ANIMATION_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return MobExSounds.SENTINEL_GM2.get();
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return MobExSounds.SENTINEL_GM2.get();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt("AwakenTicks", awakenTicksRemaining);
        compoundTag.putBoolean("Attacking", attacking);
        compoundTag.putInt("AttackTicks", attackTicksRemaining);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        awakenTicksRemaining = compoundTag.getInt("AwakenTicks");
        attacking = compoundTag.getBoolean("Attacking");
        attackTicksRemaining = compoundTag.getInt("AttackTicks");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("spawn", SPAWN_ANIMATION)
                .triggerableAnim("attack1", ATTACK1_ANIMATION)
                .triggerableAnim("attack2", ATTACK2_ANIMATION)
                .triggerableAnim("estocada", ESTOCADA_ANIMATION)
                .triggerableAnim("golpt", GOLPT_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<SentinelGolemEntity> animationState) {
        if (isDeadOrDying())
            return animationState.setAndContinue(DEATH_ANIMATION);
        if (isAwakening() || attacking)
            return PlayState.CONTINUE;
        if (animationState.isMoving())
            return animationState.setAndContinue(WALK_ANIMATION);
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
        return "sentinell_sword_active";
    }
}
