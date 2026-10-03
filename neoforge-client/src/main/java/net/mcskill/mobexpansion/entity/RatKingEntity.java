package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.RatKingMeleeGoal;
import net.mcskill.mobexpansion.entity.ai.RatKingRangedGoal;
import net.mcskill.mobexpansion.entity.ai.RatKingSummonGoal;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class RatKingEntity extends SewerMobEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation HAND_HIT_ANIMATION = RawAnimation.begin().thenPlay("hand_hit");
    private static final RawAnimation SWOOP_ANIMATION = RawAnimation.begin().thenPlay("swoop");
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().thenPlay("shoot");
    private static final RawAnimation POTION_ANIMATION = RawAnimation.begin().thenPlay("potion");
    private static final RawAnimation SUMMON_ANIMATION = RawAnimation.begin().thenPlay("summon");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");

    private static final int HAND_HIT_DURATION = 20;
    private static final int HAND_HIT_DAMAGE_TICK = 12;
    private static final int SWOOP_DURATION = 32;
    private static final int SWOOP_DAMAGE_START = 12;
    private static final int SWOOP_DAMAGE_END = 20;
    private static final int SHOOT_DURATION = 45;
    private static final int SHOOT_DAMAGE_TICK = 32;
    private static final int POTION_DURATION = 31;
    private static final int POTION_DAMAGE_TICK = 14;
    private static final int SUMMON_DURATION = 80;
    private static final int SUMMON_SPAWN_TICK = 20;

    private int shootCooldownTicks;
    private int potionCooldownTicks;
    private int summonCooldownTicks;
    private boolean summonQueued;
    private boolean summonedAt80;
    private boolean summonedAt50;
    private boolean summonedAt20;
    private boolean projectileFired;
    private boolean minionsSpawned;
    private int lastSwoopHitTick = -1;

    public RatKingEntity(EntityType<? extends RatKingEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 50;
        this.potionCooldownTicks = MobExParameters.getInt(this, "potionIntervalTicks", 160) / 2;
        this.summonCooldownTicks = MobExParameters.getInt(this, "summonIntervalTicks", 220) / 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        registerSharedGoals(1.0D);
        goalSelector.addGoal(1, new RatKingSummonGoal(this));
        goalSelector.addGoal(2, new RatKingMeleeGoal(this));
        goalSelector.addGoal(3, new RatKingRangedGoal(this));
    }

    public boolean prefersRangedCombat(LivingEntity target) {
        final double rangedPreferredDistance = MobExParameters.get(this, "rangedPreferredDistance", 5.5D);
        return target != null && distanceToSqr(target) > rangedPreferredDistance * rangedPreferredDistance;
    }

    public boolean prefersMeleeCombat(LivingEntity target) {
        return target != null && !prefersRangedCombat(target);
    }

    public boolean canStartRangedAttack(LivingEntity target) {
        if (target == null || !target.isAlive() || isBusy() || prefersMeleeCombat(target))
            return false;
        if (canThrowPotion(target))
            return true;
        return shootCooldownTicks <= 0;
    }

    public boolean canThrowPotion(LivingEntity target) {
        return potionCooldownTicks <= 0
                && target != null
                && prefersRangedCombat(target);
    }

    public boolean shouldSummonMinions() {
        return summonQueued;
    }

    @Nullable
    public String getActiveAttackNamePublic() {
        return getActiveAttackName();
    }

    public void tryStartMeleeAttack(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive())
            return;
        if (random.nextBoolean()) {
            startHandHit();
            return;
        }
        startSwoop();
    }

    public boolean tryStartRangedAttack(LivingEntity target) {
        if (isBusy() || target == null || !target.isAlive() || prefersMeleeCombat(target))
            return false;

        if (canThrowPotion(target))
            return startPotion(target);

        if (shootCooldownTicks <= 0)
            return startShoot(target);

        return false;
    }

    public void tryStartSummon() {
        if (isBusy() || !summonQueued)
            return;
        summonQueued = false;
        minionsSpawned = false;
        beginAttack("summon", SUMMON_DURATION, SUMMON_SPAWN_TICK);
    }

    private void startHandHit() {
        projectileFired = false;
        beginAttack("hand_hit", HAND_HIT_DURATION, HAND_HIT_DAMAGE_TICK);
    }

    private void startSwoop() {
        projectileFired = false;
        lastSwoopHitTick = -1;
        beginAttack("swoop", SWOOP_DURATION, SWOOP_DAMAGE_START, SWOOP_DAMAGE_END);
    }

    private boolean startShoot(LivingEntity target) {
        projectileFired = false;
        beginAttack("shoot", SHOOT_DURATION, SHOOT_DAMAGE_TICK);
        playSound(SoundEvents.CROSSBOW_LOADING_START.value(), 1.0F, 1.0F);
        return true;
    }

    private boolean startPotion(LivingEntity target) {
        projectileFired = false;
        beginAttack("potion", POTION_DURATION, POTION_DAMAGE_TICK);
        return true;
    }

    @Override
    protected void performAttackHit() {
        final String attackName = getActiveAttackName();
        if (attackName == null)
            return;

        switch (attackName) {
            case "hand_hit" -> performHandHit();
            case "swoop" -> performSwoopHit();
            case "shoot" -> performShoot();
            case "potion" -> performPotionThrow();
            case "summon" -> performSummon();
            default -> {
            }
        }
    }

    private void performHandHit() {
        playSound(SoundEvents.RABBIT_ATTACK, 1.0F, 1.0F);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 0.6F);
        damageInFrontCone(
                MobExParameters.get(this, "handHitRange", 3.0D),
                90.0F,
                MobExParameters.getFloat(this, "handHitDamage", 10.0F)
        );
    }

    private void performSwoopHit() {
        final int elapsedTicks = getAttackElapsedTicks();
        if (elapsedTicks == SWOOP_DAMAGE_START)
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.1F);

        if (elapsedTicks == lastSwoopHitTick)
            return;
        if ((elapsedTicks - SWOOP_DAMAGE_START) % 3 != 0)
            return;
        lastSwoopHitTick = elapsedTicks;

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.4D, getZ(), 8, 0.6D, 0.2D, 0.6D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.4D, getZ(), 4, 0.4D, 0.2D, 0.4D, 0.05D);
        }

        final double swoopRange = MobExParameters.get(this, "swoopRange", 3.5D);
        final float swoopDamage = MobExParameters.getFloat(this, "swoopDamage", 12.0F);
        final List<LivingEntity> nearbyTargets = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(swoopRange),
                SewerFaction::isValidTarget
        );
        for (LivingEntity nearbyTarget : nearbyTargets) {
            nearbyTarget.hurt(damageSources().mobAttack(this), swoopDamage);
            final Vec3 knockDirection = nearbyTarget.position().subtract(position()).normalize();
            nearbyTarget.push(knockDirection.x * 0.9D, 0.45D, knockDirection.z * 0.9D);
            nearbyTarget.hurtMarked = true;
        }
    }

    private void performShoot() {
        if (projectileFired)
            return;
        projectileFired = true;
        final LivingEntity shootTarget = getTarget();
        shootCooldownTicks = prefersRangedCombat(shootTarget)
                ? MobExParameters.getInt(this, "nearShootCooldown", 35)
                : MobExParameters.getInt(this, "farShootCooldown", 80);
        playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.7F);

        if (shootTarget == null || !(level() instanceof ServerLevel))
            return;

        final SewerBoltProjectileEntity bolt = new SewerBoltProjectileEntity(level(), this);
        bolt.setDamage(MobExParameters.getFloat(this, "boltDamage", 5.0F));
        final Vec3 muzzle = position().add(0.0D, getBbHeight() * 0.75D, 0.0D);
        bolt.moveTo(muzzle.x, muzzle.y, muzzle.z, getYRot(), getXRot());

        final double deltaX = shootTarget.getX() - muzzle.x;
        final double deltaY = shootTarget.getY(0.35D) - muzzle.y;
        final double deltaZ = shootTarget.getZ() - muzzle.z;
        bolt.shoot(deltaX, deltaY, deltaZ, 1.8F, 1.0F);
        level().addFreshEntity(bolt);
    }

    private void performPotionThrow() {
        if (projectileFired)
            return;
        projectileFired = true;
        potionCooldownTicks = MobExParameters.getInt(this, "potionIntervalTicks", 160);
        playSound(SoundEvents.RABBIT_ATTACK, 1.0F, 1.0F);
        playSound(SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 0.5F);
        damageInFrontCone(2.0D, 90.0F, MobExParameters.getFloat(this, "potionMeleeDamage", 10.0F));

        final LivingEntity target = getTarget();
        if (target == null)
            return;

        final SewerPotionProjectileEntity potion = new SewerPotionProjectileEntity(level(), this);
        final Vec3 muzzle = position().add(0.0D, getBbHeight() * 0.85D, 0.0D);
        potion.moveTo(muzzle.x, muzzle.y, muzzle.z, getYRot(), getXRot());

        final double deltaX = target.getX() - muzzle.x;
        final double deltaY = target.getY(0.2D) - muzzle.y + 0.4D;
        final double deltaZ = target.getZ() - muzzle.z;
        final double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        potion.shoot(deltaX, deltaY + horizontalDistance * 0.12D, deltaZ, 0.85F, 1.5F);
        level().addFreshEntity(potion);
    }

    private void performSummon() {
        if (minionsSpawned)
            return;
        minionsSpawned = true;
        if (!(level() instanceof ServerLevel serverLevel))
            return;

        final int summonRatCount = MobExParameters.getInt(this, "summonRatCount", 4);
        final double summonRadius = MobExParameters.get(this, "summonRadius", 4.5D);
        for (int pointIndex = 0; pointIndex < summonRatCount; pointIndex++) {
            final double angle = (Math.PI * 2.0D * pointIndex) / summonRatCount;
            final double spawnX = getX() + Math.cos(angle) * summonRadius;
            final double spawnZ = getZ() + Math.sin(angle) * summonRadius;
            spawnRatMinion(serverLevel, spawnX, getY(), spawnZ);
        }
    }

    private void spawnRatMinion(ServerLevel serverLevel, double spawnX, double spawnY, double spawnZ) {
        final RatEntity rat = MobExEntities.RAT.get().create(serverLevel);
        if (rat == null)
            return;

        rat.moveTo(spawnX, spawnY, spawnZ, random.nextFloat() * 360.0F, 0.0F);
        serverLevel.addFreshEntity(rat);
        serverLevel.sendParticles(ParticleTypes.SNEEZE, spawnX, spawnY + 0.2D, spawnZ, 20, 0.8D, 0.3D, 0.8D, 0.02D);

        final LivingEntity currentTarget = getTarget();
        if (currentTarget != null)
            rat.setTarget(currentTarget);
    }

    private void damageInFrontCone(double range, float coneDegrees, float damageAmount) {
        final float halfCone = coneDegrees * 0.5F;
        final List<LivingEntity> nearbyTargets = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(range),
                livingEntity -> livingEntity != this && SewerFaction.isValidTarget(livingEntity)
        );
        for (LivingEntity nearbyTarget : nearbyTargets) {
            final Vec3 toTarget = nearbyTarget.position().subtract(position()).normalize();
            final Vec3 lookVector = getLookAngle();
            final double dot = lookVector.dot(toTarget);
            final double angleDegrees = Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0D, 1.0D)));
            if (angleDegrees <= halfCone)
                nearbyTarget.hurt(damageSources().mobAttack(this), damageAmount);
        }
    }

    @Override
    public void tick() {
        if (shootCooldownTicks > 0)
            shootCooldownTicks--;
        if (potionCooldownTicks > 0)
            potionCooldownTicks--;
        if (summonCooldownTicks > 0)
            summonCooldownTicks--;

        if (!level().isClientSide() && isAttacking() && "shoot".equals(getActiveAttackName()) && getAttackElapsedTicks() == 15)
            playSound(SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), 1.0F, 1.0F);

        updateCombatAbilityTimers();
        super.tick();
    }

    private void updateCombatAbilityTimers() {
        if (level().isClientSide())
            return;

        final LivingEntity combatTarget = getTarget();
        if (combatTarget == null || !combatTarget.isAlive())
            return;

        if (summonCooldownTicks <= 0 && !summonQueued) {
            summonQueued = true;
            summonCooldownTicks = MobExParameters.getInt(this, "summonIntervalTicks", 220);
        }

        final float healthRatio = getHealth() / getMaxHealth();
        if (!summonedAt80 && healthRatio <= 0.8F) {
            summonedAt80 = true;
            summonQueued = true;
        }
        if (!summonedAt50 && healthRatio <= 0.5F) {
            summonedAt50 = true;
            summonQueued = true;
        }
        if (!summonedAt20 && healthRatio <= 0.2F) {
            summonedAt20 = true;
            summonQueued = true;
        }
    }

    @Override
    protected int getAttackCooldownTicks() {
        return 8;
    }

    @Override
    protected int getDeathAnimationTicks() {
        return 60;
    }

    @Override
    protected int getHurtSoundCooldownTicks() {
        return 60;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_ATTACK;
    }

    @Override
    public void playAmbientSound() {
        playSound(SoundEvents.RABBIT_ATTACK, 1.0F, 0.8F);
        playSound(SoundEvents.SILVERFISH_AMBIENT, 1.0F, 2.0F);
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.RABBIT_ATTACK;
    }

    @Override
    public float getVoicePitch() {
        return 1.2F;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!isDeadOrDying()) {
            playSound(SoundEvents.SILVERFISH_DEATH, 1.0F, 0.1F);
            playSound(SoundEvents.RABBIT_ATTACK, 1.0F, 0.1F);
        }
        super.die(damageSource);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("hand_hit", HAND_HIT_ANIMATION)
                .triggerableAnim("swoop", SWOOP_ANIMATION)
                .triggerableAnim("shoot", SHOOT_ANIMATION)
                .triggerableAnim("potion", POTION_ANIMATION)
                .triggerableAnim("summon", SUMMON_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<RatKingEntity> animationState) {
        if (isDeadOrDying())
            return animationState.setAndContinue(DEATH_ANIMATION);
        if (isAttacking())
            return PlayState.CONTINUE;
        if (animationState.isMoving())
            return animationState.setAndContinue(WALK_ANIMATION);
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putBoolean("SummonQueued", summonQueued);
        compoundTag.putBoolean("SummonedAt80", summonedAt80);
        compoundTag.putBoolean("SummonedAt50", summonedAt50);
        compoundTag.putBoolean("SummonedAt20", summonedAt20);
        compoundTag.putInt("ShootCooldown", shootCooldownTicks);
        compoundTag.putInt("PotionCooldown", potionCooldownTicks);
        compoundTag.putInt("SummonCooldown", summonCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        summonQueued = compoundTag.getBoolean("SummonQueued");
        summonedAt80 = compoundTag.getBoolean("SummonedAt80");
        summonedAt50 = compoundTag.getBoolean("SummonedAt50");
        summonedAt20 = compoundTag.getBoolean("SummonedAt20");
        shootCooldownTicks = compoundTag.getInt("ShootCooldown");
        potionCooldownTicks = compoundTag.getInt("PotionCooldown");
        summonCooldownTicks = compoundTag.getInt("SummonCooldown");
    }

    @Override
    public String modelName() {
        return "rat_king";
    }

    @Override
    public String textureName() {
        return "rat_king";
    }

    @Override
    public String animationName() {
        return "rat_king";
    }
}
