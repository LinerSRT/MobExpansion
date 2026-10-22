package net.mcskill.mobexpansion.entity.meadows;

import net.mcskill.mobexpansion.entity.ability.Ability;
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
 * Цветочный змей: кусает вблизи, а издалека прыгает на добычу и сжимает её кольцами. Промахнувшись,
 * отскакивает назад и готовится к новому броску.
 */
public class FloralSerpentEntity extends MeadowsCreatureEntity {
    private static final Vec3 HOLD = new Vec3(0.0D, 0.3D, 1.2D);

    private Ability constrict;
    private Ability constrictFailed;

    public FloralSerpentEntity(EntityType<? extends FloralSerpentEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("bite").seconds(1.5).range(0, 4.0).cooldown(30).weight(4)
                .at(14, (mob, target) -> {
                    mob.hitCone(3.6, 100.0F, mob.damage(1.0), 0.3, 0.1);
                    mob.sound(SoundEvents.FOX_BITE, 1.2F, 0.6F);
                })
                .build());

        // прыжок на добычу: на лету проверяет, достал ли цель
        abilities.add(Ability.builder("constrict_jump").seconds(2.0).range(3.6, 20.0).cooldown(180).weight(3).moving().unstoppable()
                .at(1, (mob, target) -> mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F))
                .every(8, 30, 1, (mob, target, tick) -> {
                    if (target == null)
                        return;
                    mob.lunge(target.position().subtract(mob.position()), 0.85D, tick == 8 ? 0.35D : 0.0D);
                    if (mob.distanceTo(target) < 2.3D && mob.isEnemy(target))
                        mob.forceAbility(constrict, target);
                })
                .onEnd((mob, target) -> mob.forceAbility(constrictFailed, target))
                .build());

        constrict = Ability.builder("constrict").seconds(5.0).cooldown(0).when((mob, target) -> false).unstoppable()
                .onStart((mob, target) -> {
                    if (target != null)
                        mob.grab(target, HOLD);
                    mob.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.5F);
                })
                .every(5, 95, 5, (mob, target, tick) -> {
                    final LivingEntity victim = mob.held();
                    if (victim == null) {
                        mob.interruptAbility();
                        return;
                    }
                    victim.invulnerableTime = 0;
                    mob.hit(victim, mob.damage(0.15), 0.0D, 0.0D);
                    mob.effect(victim, MobEffects.MOVEMENT_SLOWDOWN, 15, 3);
                    mob.particles(ParticleTypes.CRIT, victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D), 5, 0.3, 0.1);
                    if (tick % 20 == 5)
                        mob.sound(SoundEvents.TURTLE_EGG_CRACK, 1.0F, 0.6F);
                })
                .onEnd((mob, target) -> mob.release(0.6D, 0.3D))
                .build();
        abilities.add(constrict);

        constrictFailed = Ability.builder("constrict_failed").seconds(2.0).cooldown(0).when((mob, target) -> false)
                .at(10, (mob, target) -> {
                    mob.lunge(mob.forward().scale(-1.0D), 0.9D, 0.3D);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.4F);
                })
                .build();
        abilities.add(constrictFailed);
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(2.5);
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected String id() {
        return "floral_serpent";
    }
}
