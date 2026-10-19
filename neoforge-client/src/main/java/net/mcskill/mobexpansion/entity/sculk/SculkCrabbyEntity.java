package net.mcskill.mobexpansion.entity.sculk;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.particles.ParticleTypes;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Скалк-краб: вылезает из земли при появлении. Щиплется (иногда замедляя), а если цель убежала далеко —
 * зарывается и выныривает прямо под ней, подбрасывая. Пока вылезает — неуязвим.
 */
public class SculkCrabbyEntity extends SculkCreatureEntity {
    private Ability emerge;

    public SculkCrabbyEntity(EntityType<? extends SculkCrabbyEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("pinch").seconds(1.25).range(0, 2.2).cooldown(14)
                .at(7, (mob, target) -> {
                    for (LivingEntity hit : mob.hitCone(2.0, 100.0F, mob.damage(1.0), 0.2, 0.05)) {
                        if (mob.getRandom().nextFloat() < 0.3F)
                            mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 60, 0);
                        mob.sound(SoundEvents.TURTLE_EGG_CRACK, 1.0F, 1.4F);
                    }
                })
                .build());

        // засада: нырнуть в землю и вылезти под целью
        abilities.add(Ability.builder("burrow").animation("spawn").seconds(4.0).range(8.0, 22.0).cooldown(220).weight(2)
                .unstoppable().ignoreSight()
                .onStart((mob, target) -> {
                    mob.particles(ParticleTypes.SCULK_SOUL, mob.position(), 12, 0.4, 0.02);
                    if (target != null) {
                        final double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
                        final Vec3 spot = mob.groundAt(target.position().add(Math.cos(angle) * 1.5D, 0.0D, Math.sin(angle) * 1.5D));
                        mob.teleportTo(spot.x, spot.y, spot.z);
                    }
                    mob.sound(SoundEvents.SCULK_BLOCK_SPREAD, 1.0F, 0.8F);
                })
                .at(50, (mob, target) -> {
                    mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), 1.6D, mob.damage(1.2), 0.3D, 0.6D);
                    mob.particles(ParticleTypes.SCULK_SOUL, mob.position(), 16, 0.6, 0.05);
                })
                .build());
        emerge = Ability.builder("spawn").seconds(4.0).cooldown(0).unstoppable()
                .when((mob, target) -> false)
                .every(0, 60, 10, (mob, target, tick) -> mob.particles(ParticleTypes.SCULK_SOUL, mob.position(), 4, 0.4, 0.02))
                .build();
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        abilities();
        return emerge;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isUsingAbility("spawn") || (isUsingAbility("burrow") && abilityTick() < 50))
            return false;
        return super.hurt(source, amount);
    }

    @Override
    public double chaseSpeed() {
        return 1.1D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(8.0);
    }

    @Override
    public String modelName() {
        return "sculk_crabby";
    }

    @Override
    public String textureName() {
        return "sculk_crabby";
    }

    @Override
    public String animationName() {
        return "sculk_crabby";
    }
}
