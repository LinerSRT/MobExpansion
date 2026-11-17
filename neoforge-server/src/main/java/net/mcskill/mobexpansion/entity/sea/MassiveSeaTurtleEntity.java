package net.mcskill.mobexpansion.entity.sea;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Исполинская морская черепаха с рифом на панцире. Нейтральна: неспешно плавает, пока её не тронут.
 * Обидчика таранит панцирем и отталкивает волной от ласт. У модели одна анимация — плавание.
 */
public class MassiveSeaTurtleEntity extends SeaCreatureEntity {
    public MassiveSeaTurtleEntity(EntityType<? extends MassiveSeaTurtleEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.12D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // таран: полторы секунды напролом к цели
        abilities.add(Ability.builder("ram").animation(null).seconds(2.5).range(4.0, 24.0).cooldown(160).weight(2).moving().unstoppable()
                .at(1, (mob, target) -> sound(SoundEvents.TURTLE_AMBIENT_LAND, 3.0F, 0.4F))
                .every(10, 40, 1, (mob, target, tick) -> {
                    final Vec3 direction = target != null && mob.distanceTo(target) > 3.0F ? directionTo(target, 4.0D) : mob.forward();
                    swimLunge(direction, 0.5D);
                    if (tick % 2 == 0)
                        mob.particles(ParticleTypes.BUBBLE, mob.localPos(0.0D, 3.0D, -7.0D), 12, 2.0, 0.05);
                })
                .every(12, 40, 2, (mob, target, tick) -> {
                    for (LivingEntity hit : hitAreaOnce(mob.localPos(0.0D, 4.0D, 5.0D), 5.0D, damage(1.0D), 2.0D, 0.4D))
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                })
                .at(41, (mob, target) -> halt())
                .build());

        // волна от ласт: отталкивает всех, кто подплыл вплотную
        abilities.add(Ability.builder("flipper_wave").animation(null).seconds(1.5).range(0, 10.0).cooldown(120).weight(3).ignoreSight()
                .at(10, (mob, target) -> {
                    final Vec3 center = mob.position().add(0.0D, 4.0D, 0.0D);
                    hitArea(center, 10.0D, damage(0.6D), 1.8D, 0.3D);
                    for (int ring = 6; ring <= 12; ring += 3)
                        mob.particleRing(ParticleTypes.BUBBLE, center, ring, ring * 6);
                    sound(SoundEvents.GENERIC_SPLASH, 3.0F, 0.5F);
                })
                .build());
    }

    @Override
    protected void registerTargetGoals() {
        // нейтральная: бьёт только обидчика
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
    }

    @Override
    protected String idleAnimation() {
        return "waiting";
    }

    @Override
    protected String walkAnimation() {
        return "waiting";
    }

    @Override
    protected String deathAnimation() {
        return "waiting";
    }

    @Override
    protected int deathTicks() {
        return 30;
    }

    @Override
    protected int maxPitch() {
        return 20;
    }

    @Override
    protected int maxTurn() {
        return 3;
    }

    @Override
    protected boolean suffocatesOutOfWater() {
        return false;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4F;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }
}
