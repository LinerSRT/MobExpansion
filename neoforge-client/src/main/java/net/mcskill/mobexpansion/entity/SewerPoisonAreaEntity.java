package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SewerPoisonAreaEntity extends Entity {
    private static final int DEFAULT_LIFETIME_TICKS = 100;
    private static final int POISON_INTERVAL_TICKS = 10;
    private static final double DEFAULT_POISON_RADIUS = 4.0D;

    private int lifeTicks;
    private int lifetimeTicks = DEFAULT_LIFETIME_TICKS;
    private double poisonRadius = DEFAULT_POISON_RADIUS;

    public SewerPoisonAreaEntity(EntityType<? extends SewerPoisonAreaEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    public SewerPoisonAreaEntity(Level level) {
        this(MobExEntities.SEWER_POISON_AREA.get(), level);
    }

    public void configureFromOwner(LivingEntity owner) {
        poisonRadius = MobExParameters.get(owner, "poisonAreaRadius", DEFAULT_POISON_RADIUS);
        lifetimeTicks = MobExParameters.getInt(owner, "poisonAreaLifetime", DEFAULT_LIFETIME_TICKS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (level() instanceof ServerLevel serverLevel && lifeTicks % POISON_INTERVAL_TICKS == 0) {
            applyPoison(serverLevel);
            spawnRingParticles(serverLevel);
        }

        if (!level().isClientSide() && lifeTicks >= lifetimeTicks)
            discard();
    }

    private void applyPoison(ServerLevel serverLevel) {
        final AABB poisonArea = getBoundingBox().inflate(poisonRadius);
        final List<LivingEntity> victims = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                poisonArea,
                livingEntity -> livingEntity.isAlive() && !SewerFaction.isAlly(livingEntity)
        );
        for (LivingEntity victim : victims)
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
    }

    private void spawnRingParticles(ServerLevel serverLevel) {
        spawnRing(serverLevel, 3.0D, 20, 0.3D);
        spawnRing(serverLevel, 2.0D, 20, 0.4D);
        spawnRing(serverLevel, 1.0D, 20, 0.1D);
    }

    private void spawnRing(ServerLevel serverLevel, double radius, int points, double upwardSpeed) {
        for (int pointIndex = 0; pointIndex < points; pointIndex++) {
            final double angle = (Math.PI * 2.0D * pointIndex) / points;
            final double particleX = getX() + Math.cos(angle) * radius;
            final double particleZ = getZ() + Math.sin(angle) * radius;
            serverLevel.sendParticles(ParticleTypes.SNEEZE, particleX, getY() + 0.1D, particleZ, 1, 0.0D, upwardSpeed, 0.0D, 0.0D);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        lifeTicks = compoundTag.getInt("LifeTicks");
        if (compoundTag.contains("LifetimeTicks"))
            lifetimeTicks = compoundTag.getInt("LifetimeTicks");
        if (compoundTag.contains("PoisonRadius"))
            poisonRadius = compoundTag.getDouble("PoisonRadius");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("LifeTicks", lifeTicks);
        compoundTag.putInt("LifetimeTicks", lifetimeTicks);
        compoundTag.putDouble("PoisonRadius", poisonRadius);
    }
}
