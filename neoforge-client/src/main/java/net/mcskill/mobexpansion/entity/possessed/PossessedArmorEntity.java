package net.mcskill.mobexpansion.entity.possessed;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Одержимые доспехи — мини-боссы ближнего боя: общая фракция, полоска босса, тяжёлые шаги и звон металла.
 * Удары по одержимой броне частично «вязнут» — урон от снарядов снижен на четверть.
 */
public abstract class PossessedArmorEntity extends AbilityMobEntity {
    public static final String FACTION = "possessed";

    protected PossessedArmorEntity(EntityType<? extends PossessedArmorEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 80;
    }

    @Override
    public String faction() {
        return FACTION;
    }

    @Override
    @Nullable
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(20.0);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.getDirectEntity() != null && source.getDirectEntity() != source.getEntity())
            amount *= 0.75F;
        return super.hurt(source, amount);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.ZOMBIE_ATTACK_IRON_DOOR;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 0.6F, 0.6F);
    }
}
