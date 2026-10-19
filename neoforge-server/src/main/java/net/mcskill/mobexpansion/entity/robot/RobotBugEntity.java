package net.mcskill.mobexpansion.entity.robot;

import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Механические насекомые (модели pb_nul-*): общая фракция и металлические звуки.
 */
public abstract class RobotBugEntity extends AbilityMobEntity {
    public static final String FACTION = "robot_bugs";

    protected RobotBugEntity(EntityType<? extends RobotBugEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public String faction() {
        return FACTION;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.4F, 1.6F);
    }

    @Override
    public float getVoicePitch() {
        return 1.3F + (random.nextFloat() - 0.5F) * 0.2F;
    }
}
