package net.mcskill.mobexpansion.entity.boss;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Костяной прислужник — скелет, которого поднимает рёв {@link BoneColossusEntity}: вылезает из земли и бьёт в упор. */
public class BoneServantEntity extends AbilityMobEntity {
    private static final Ability SPAWN = Ability.builder("spawn").seconds(4.5).cooldown(0).when((mob, target) -> false).build();

    public BoneServantEntity(EntityType<? extends BoneServantEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    @Override
    public String faction() {
        return BoneColossusEntity.FACTION;
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("atk").seconds(1.5).range(0, 2.5).cooldown(20).weight(1)
                .at(12, (mob, target) -> {
                    mob.hitCone(2.5, 90.0F, mob.damage(1.0), 0.3, 0.1);
                    mob.sound(SoundEvents.SKELETON_HURT, 0.6F, 1.6F);
                })
                .build());
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        return SPAWN;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(3.5);
    }

    @Override
    public String modelName() {
        return "bone_servant";
    }

    @Override
    public String textureName() {
        return "bone_servant";
    }

    @Override
    public String animationName() {
        return "bone_servant";
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.SKELETON_STEP, 0.5F, 1.0F);
    }
}
