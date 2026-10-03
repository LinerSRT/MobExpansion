package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SewerBoltProjectileEntity extends ThrowableProjectile {
    private static final float HIT_DAMAGE = 5.0F;
    private static final float PASS_DAMAGE = 5.0F;
    private static final double PASS_RADIUS = 1.5D;
    private static final float KNOCKBACK_STRENGTH = 0.85F;
    private static final int MAX_LIFETIME_TICKS = 40;

    private float damage = HIT_DAMAGE;

    public SewerBoltProjectileEntity(EntityType<? extends SewerBoltProjectileEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public SewerBoltProjectileEntity(Level level, LivingEntity shooter) {
        super(MobExEntities.SEWER_BOLT_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
        if (shooter instanceof RatKingEntity)
            damage = MobExParameters.getFloat(shooter, "boltDamage", HIT_DAMAGE);
    }

    public void setDamage(float damageAmount) {
        damage = Math.max(1.0F, damageAmount);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 2, 0.05D, 0.05D, 0.05D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY(), getZ(), 4, 0.08D, 0.08D, 0.08D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.GLOW, getX(), getY(), getZ(), 3, 0.05D, 0.05D, 0.05D, 0.02D);
        }

        if (!level().isClientSide())
            damageNearbyPlayers();

        if (!level().isClientSide() && tickCount >= MAX_LIFETIME_TICKS)
            discard();
    }

    private void damageNearbyPlayers() {
        final float passDamage = resolvePassDamage();
        final List<LivingEntity> nearbyTargets = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(PASS_RADIUS),
                livingEntity -> livingEntity.isAlive()
                        && !SewerFaction.isAlly(livingEntity)
                        && livingEntity != getOwner()
                        && SewerFaction.isValidTarget(livingEntity)
        );
        for (LivingEntity nearbyTarget : nearbyTargets) {
            if (nearbyTarget.hurt(damageSources().thrown(this, getOwner()), passDamage))
                knockAway(nearbyTarget);
        }
    }

    private float resolvePassDamage() {
        final Entity owner = getOwner();
        if (owner instanceof RatKingEntity ratKing)
            return MobExParameters.getFloat(ratKing, "boltPassDamage", PASS_DAMAGE);
        return PASS_DAMAGE;
    }

    private void knockAway(LivingEntity target) {
        final Vec3 knockDirection = target.position().subtract(position()).normalize();
        target.push(knockDirection.x * KNOCKBACK_STRENGTH, 0.35D, knockDirection.z * KNOCKBACK_STRENGTH);
        target.hurtMarked = true;
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        if (entity == getOwner() || SewerFaction.isAlly(entity))
            return false;
        if (entity instanceof SewerBoltProjectileEntity || entity instanceof SewerPotionProjectileEntity)
            return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        super.onHit(hitResult);
        if (!level().isClientSide())
            discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        final Entity hitEntity = entityHitResult.getEntity();
        if (!(hitEntity instanceof LivingEntity livingTarget))
            return;
        if (livingTarget.hurt(damageSources().thrown(this, getOwner()), damage))
            knockAway(livingTarget);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putFloat("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains("Damage"))
            damage = compoundTag.getFloat("Damage");
    }
}
