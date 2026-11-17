package net.mcskill.mobexpansion.entity.sea;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Светящаяся медуза (розовая, синяя, зелёная — три типа на одной модели и анимации).
 * Как и в оригинале — мирная: медленно дрейфует и никого не трогает.
 */
public class JellyfishEntity extends SeaCreatureEntity {
    public JellyfishEntity(EntityType<? extends JellyfishEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.05D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0D, 30));
    }

    @Override
    public boolean pitchesModel() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return true;
    }

    @Override
    public String modelName() {
        return "jellyfish";
    }

    @Override
    public String animationName() {
        return "jellyfish";
    }

    @Override
    protected String walkAnimation() {
        return "idle";
    }

    @Override
    protected String deathAnimation() {
        // своей анимации смерти нет: застывает и рассыпается
        return "idle";
    }

    @Override
    protected int deathTicks() {
        return 10;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }
}
