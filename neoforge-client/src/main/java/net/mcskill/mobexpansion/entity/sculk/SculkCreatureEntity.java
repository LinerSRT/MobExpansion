package net.mcskill.mobexpansion.entity.sculk;

import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Существа скалка: общая фракция, «скалковые» звуки и общая тревога — раненый скалк поднимает соседей.
 */
public abstract class SculkCreatureEntity extends AbilityMobEntity {
    public static final String FACTION = "sculk";

    protected SculkCreatureEntity(EntityType<? extends SculkCreatureEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public String faction() {
        return FACTION;
    }

    /** Поднять тревогу: соседние существа скалка без цели атакуют того же врага. */
    public static void alertSculk(AbilityMobEntity source, LivingEntity enemy, double radius) {
        for (AbilityMobEntity ally : source.level().getEntitiesOfClass(AbilityMobEntity.class, source.getBoundingBox().inflate(radius),
                mob -> mob != source && FACTION.equals(mob.faction()) && mob.getTarget() == null))
            ally.setTarget(enemy);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        final boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && isEnemy(attacker))
            alertSculk(this, attacker, 12.0D);
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_CATALYST_BLOOM;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SCULK_BLOCK_BREAK;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SCULK_SHRIEKER_BREAK;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.SCULK_BLOCK_STEP, 0.5F, 1.0F);
    }
}
