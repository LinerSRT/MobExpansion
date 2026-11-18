package net.mcskill.mobexpansion.entity.boss;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.FlyingAbilityMobEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Демонический глаз — летающий прислужник {@link DemonKnightEntity}: вылетает из меток молний,
 * кружит низко над целью и бьёт вокруг себя.
 */
public class DemonEyeEntity extends FlyingAbilityMobEntity {
    private static final Ability SPAWN = Ability.builder("spawn").seconds(2.3).cooldown(0).when((mob, target) -> false).build();

    public DemonEyeEntity(EntityType<? extends DemonEyeEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    @Override
    public String faction() {
        return DemonKnightEntity.FACTION;
    }

    @Override
    public double hoverHeight() {
        return 1.5D;
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("attack").seconds(3.0).range(0, 3.5).cooldown(30).weight(1)
                .at(20, (mob, target) -> {
                    mob.hitArea(mob.position().add(0.0D, 0.6D, 0.0D), 3.0D, mob.damage(1.0), 0.4, 0.2);
                    mob.particles(ParticleTypes.FLAME, mob.position().add(0.0D, 0.6D, 0.0D), 12, 0.6, 0.02);
                    mob.sound(SoundEvents.TRIDENT_THROW.value(), 1.1F, 1.2F);
                })
                .build());
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        return SPAWN;
    }

    @Override
    protected String idleAnimation() {
        return "flying";
    }

    @Override
    protected String walkAnimation() {
        return "flying";
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(1.25) + 5;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public String modelName() {
        return "demon_eye";
    }

    @Override
    public String textureName() {
        return "demon_eye";
    }

    @Override
    public String animationName() {
        return "demon_eye";
    }

    @Override
    public float getVoicePitch() {
        return 1.9F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SQUID_DEATH;
    }
}
