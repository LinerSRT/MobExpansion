package net.mcskill.mobexpansion.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class SewerMobEntity extends MobExGeckoEntity {
    private boolean attacking;
    private int attackTicksRemaining;
    private int attackDurationTicks;
    private int damageWindowStartTick = -1;
    private int damageWindowEndTick = -1;
    private int attackCooldownTicks;
    private int hurtSoundCooldownTicks;
    @Nullable
    private String activeAttackName;

    protected SewerMobEntity(EntityType<? extends SewerMobEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public boolean isAttacking() {
        return attacking;
    }

    public boolean isBusy() {
        return attacking || attackCooldownTicks > 0;
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

    protected int getAttackElapsedTicks() {
        return attackDurationTicks - attackTicksRemaining;
    }

    protected int getAttackCooldownTicks() {
        return 15;
    }

    protected int getDeathAnimationTicks() {
        return 40;
    }

    protected int getHurtSoundCooldownTicks() {
        return 40;
    }

    @Nullable
    protected String getActiveAttackName() {
        return activeAttackName;
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

    protected void registerSharedGoals(double strollSpeed) {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, strollSpeed));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, SewerFaction::isValidTarget));
    }

    @Override
    public void tick() {
        if (attackCooldownTicks > 0)
            attackCooldownTicks--;
        if (hurtSoundCooldownTicks > 0)
            hurtSoundCooldownTicks--;

        if (!level().isClientSide() && attacking) {
            getNavigation().stop();
            final int elapsedTicks = getAttackElapsedTicks();
            if (elapsedTicks >= damageWindowStartTick && elapsedTicks <= damageWindowEndTick)
                performAttackHit();
            attackTicksRemaining--;
            if (attackTicksRemaining <= 0)
                endAttack();
        }

        super.tick();
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        if (SewerFaction.isAlly(damageSource.getEntity()))
            return false;
        final boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt && damageSource.getEntity() instanceof LivingEntity attacker && SewerFaction.isValidTarget(attacker))
            setTarget(attacker);
        return wasHurt;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (attacking)
            return;
        super.knockback(strength, x, z);
    }

    @Override
    public boolean isPushable() {
        return !attacking && super.isPushable();
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!isDeadOrDying())
            triggerAnim("main", "death");
        super.die(damageSource);
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
    protected void playHurtSound(@NotNull DamageSource damageSource) {
        if (hurtSoundCooldownTicks > 0)
            return;
        hurtSoundCooldownTicks = getHurtSoundCooldownTicks();
        final SoundEvent hurtSound = getHurtSound(damageSource);
        playSound(hurtSound, getSoundVolume(), getVoicePitch());
    }

    protected void freezeMovement() {
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt("AttackCooldown", attackCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        attackCooldownTicks = compoundTag.getInt("AttackCooldown");
    }
}
