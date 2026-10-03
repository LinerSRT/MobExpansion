package net.mcskill.mobexpansion.entity.redstone;

import net.mcskill.mobexpansion.entity.MobExGeckoEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public abstract class RedstoneConstructEntity extends MobExGeckoEntity {
    public static final int SPAWN_ANIMATION_TICKS = 23;

    private static final String OWNER_UUID_TAG = "OwnerUUID";
    private static final String SPAWN_TICKS_TAG = "SpawnTicks";
    private static final String SPAWN_PLAYED_TAG = "SpawnPlayed";

    @Nullable
    private UUID ownerUUID;
    private int spawnTicksRemaining;
    private boolean spawnAnimationPlayed;
    private boolean attacking;
    private int attackTicksRemaining;
    private int attackDurationTicks;
    private int damageWindowStartTick = -1;
    private int damageWindowEndTick = -1;
    private int attackCooldownTicks;
    @Nullable
    private String activeAttackName;

    protected RedstoneConstructEntity(EntityType<? extends RedstoneConstructEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
    }

    public void setOwnerUUID(@Nullable UUID ownerId) {
        ownerUUID = ownerId;
    }

    public Optional<UUID> getOwnerUUID() {
        return Optional.ofNullable(ownerUUID);
    }

    @Nullable
    public RedstoneEngineerEntity getOwnerEngineer() {
        if (ownerUUID == null || !(level() instanceof ServerLevel serverLevel))
            return null;
        final Entity ownerEntity = serverLevel.getEntity(ownerUUID);
        if (ownerEntity instanceof RedstoneEngineerEntity engineer && engineer.isAlive())
            return engineer;
        return null;
    }

    public void beginSpawnFromBox() {
        spawnTicksRemaining = getSpawnAnimationTicks();
        spawnAnimationPlayed = false;
        if (!level().isClientSide())
            triggerAnim("main", "spawn");
        spawnAnimationPlayed = true;
    }

    protected int getSpawnAnimationTicks() {
        return SPAWN_ANIMATION_TICKS;
    }

    public boolean isSpawning() {
        return spawnTicksRemaining > 0;
    }

    public boolean isAttacking() {
        return attacking;
    }

    protected void beginAttack(String attackName, int durationTicks, int damageAtTick) {
        beginAttack(attackName, durationTicks, damageAtTick, damageAtTick);
    }

    protected void beginAttack(String attackName, int durationTicks, int damageStartTick, int damageEndTick) {
        if (level().isClientSide() || isBusy())
            return;
        attacking = true;
        activeAttackName = attackName;
        attackDurationTicks = durationTicks;
        attackTicksRemaining = durationTicks;
        damageWindowStartTick = Math.max(0, damageStartTick);
        damageWindowEndTick = Math.max(damageWindowStartTick, damageEndTick);
        getNavigation().stop();
        triggerAnim("main", attackName);
    }

    public boolean isBusy() {
        return isSpawning() || attacking || attackCooldownTicks > 0;
    }

    protected int getAttackCooldownTicks() {
        return 12;
    }

    protected int getAttackElapsedTicks() {
        return attackDurationTicks - attackTicksRemaining;
    }

    @Override
    public void tick() {
        if (attackCooldownTicks > 0)
            attackCooldownTicks--;

        if (isSpawning()) {
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
            spawnTicksRemaining--;
        }

        if (!level().isClientSide() && attacking) {
            getNavigation().stop();
            final int elapsedTicks = getAttackElapsedTicks();
            if (elapsedTicks >= damageWindowStartTick && elapsedTicks <= damageWindowEndTick)
                performAttackHit();
            attackTicksRemaining--;
            if (attackTicksRemaining <= 0)
                endAttack();
        }

        syncTargetWithOwner();
        super.tick();
    }

    private void syncTargetWithOwner() {
        if (level().isClientSide())
            return;
        final RedstoneEngineerEntity owner = getOwnerEngineer();
        if (owner == null)
            return;

        final LivingEntity ownerTarget = owner.getTarget();
        if (ownerTarget != null && ownerTarget.isAlive() && RedstoneFaction.isValidTarget(ownerTarget)) {
            final LivingEntity currentTarget = getTarget();
            if (currentTarget == null || !currentTarget.isAlive() || currentTarget != ownerTarget)
                setTarget(ownerTarget);
        }
    }

    public void onPackAlert(LivingEntity threat) {
        if (threat == null || !threat.isAlive() || !RedstoneFaction.isValidTarget(threat) || isSpawning())
            return;
        setTarget(threat);
    }

    protected abstract void performAttackHit();

    protected void endAttack() {
        attacking = false;
        activeAttackName = null;
        attackTicksRemaining = 0;
        attackDurationTicks = 0;
        damageWindowStartTick = -1;
        damageWindowEndTick = -1;
        attackCooldownTicks = getAttackCooldownTicks();
    }

    @Nullable
    protected String getActiveAttackName() {
        return activeAttackName;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (RedstoneFaction.isAlly(target))
            return false;
        return super.doHurtTarget(target);
    }

    protected void registerSharedGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, RedstoneFaction::isValidTarget));
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (isSpawning())
            return false;
        if (RedstoneFaction.isAlly(damageSource.getEntity()))
            return false;
        final boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt && damageSource.getEntity() instanceof LivingEntity attacker && RedstoneFaction.isValidTarget(attacker)) {
            setTarget(attacker);
            final RedstoneEngineerEntity owner = getOwnerEngineer();
            if (owner != null)
                owner.onConstructAttacked(this, attacker);
        }
        return wasHurt;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (isSpawning() || attacking)
            return;
        super.knockback(strength, x, z);
    }

    @Override
    public boolean isPushable() {
        return !isSpawning() && !attacking && super.isPushable();
    }

    @Override
    public void push(@NotNull Entity entity) {
        if (isSpawning())
            return;
        super.push(entity);
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!isDeadOrDying())
            triggerAnim("main", "death");
        notifyOwnerOfDeath();
        super.die(damageSource);
    }

    private void notifyOwnerOfDeath() {
        final RedstoneEngineerEntity owner = getOwnerEngineer();
        if (owner != null)
            owner.onConstructRemoved(this);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide() && reason != RemovalReason.KILLED)
            notifyOwnerOfDeath();
        super.remove(reason);
    }

    private static final int DEFAULT_DEATH_ANIMATION_TICKS = 40;

    protected int getDeathAnimationTicks() {
        return DEFAULT_DEATH_ANIMATION_TICKS;
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= getDeathAnimationTicks() && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (ownerUUID != null)
            compoundTag.putUUID(OWNER_UUID_TAG, ownerUUID);
        compoundTag.putInt(SPAWN_TICKS_TAG, spawnTicksRemaining);
        compoundTag.putBoolean(SPAWN_PLAYED_TAG, spawnAnimationPlayed);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.hasUUID(OWNER_UUID_TAG))
            ownerUUID = compoundTag.getUUID(OWNER_UUID_TAG);
        spawnTicksRemaining = compoundTag.getInt(SPAWN_TICKS_TAG);
        spawnAnimationPlayed = compoundTag.getBoolean(SPAWN_PLAYED_TAG);
    }
}
