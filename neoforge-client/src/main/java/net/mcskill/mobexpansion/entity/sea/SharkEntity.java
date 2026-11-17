package net.mcskill.mobexpansion.entity.sea;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

/**
 * Акула (и акула-молот — тот же моб с другой моделью, анимации общие).
 * Издалека разгоняется и кусает на проходе (bite1), вплотную — короткий укус (bite2). Укус пускает кровь.
 */
public class SharkEntity extends SeaCreatureEntity {
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.91F, 0.43F, 0.43F), 1.2F);

    public SharkEntity(EntityType<? extends SharkEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // рывок с укусом: замах 25 тиков, затем двухсекундный рывок на цель; пасть смыкается на ~50-м тике
        abilities.add(Ability.builder("bite1").seconds(3.5).range(5.0, 14.0).cooldown(100).weight(3).moving()
                .every(25, 64, 1, (mob, target, tick) -> {
                    final Vec3 direction = target != null && mob.distanceTo(target) > 1.5F ? directionTo(target, 0.6D) : mob.forward();
                    swimLunge(direction, 0.6D);
                    if (tick % 3 == 0)
                        mob.particles(ParticleTypes.BUBBLE, mob.localPos(0.0D, 0.6D, -1.5D), 4, 0.3, 0.05);
                })
                .every(42, 56, 1, (mob, target, tick) -> bite(mob.forwardPos(1.6D, 0.6D), 1.8D, 1.0D))
                .at(65, (mob, target) -> halt())
                .build());

        abilities.add(Ability.builder("bite2").seconds(1.52).range(0, 4.0).cooldown(40).weight(4).moving()
                .every(0, 5, 1, (mob, target, tick) -> {
                    if (target != null)
                        swimLunge(directionTo(target, 0.6D), 0.3D);
                })
                .at(9, (mob, target) -> bite(mob.forwardPos(1.5D, 0.6D), 1.8D, 1.0D))
                .at(10, (mob, target) -> halt())
                .build());
    }

    /** Укус в точке: каждую цель — раз за применение, с кровью и пузырями. */
    private void bite(Vec3 center, double radius, double multiplier) {
        for (LivingEntity hit : hitAreaOnce(center, radius, damage(multiplier), 0.8D, 0.2D)) {
            final Vec3 at = hit.position().add(0.0D, hit.getBbHeight() * 0.5D, 0.0D);
            particles(BLOOD, at, 15, 0.25, 0.0);
            particles(hit.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.DAMAGE_INDICATOR, at, 20, 0.3, 0.05);
            sound(SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 0.8F + random.nextFloat() * 0.4F);
        }
    }

    @Override
    public double chaseSpeed() {
        return 1.2D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(3.0);
    }

    @Override
    public String animationName() {
        // у акулы-молота те же анимации
        return "shark";
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
