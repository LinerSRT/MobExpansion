package net.mcskill.mobexpansion.entity.meadows;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.particles.ParticleOptions;
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
 * Мерцающий единорог: бодает рогом, лягается и, разогнавшись, сносит всё на пути радужным тараном.
 * В бою бежит галопом.
 */
public class GlimmeringUnicornEntity extends MeadowsCreatureEntity {
    private static final ParticleOptions[] RAINBOW = {
            dust(1.0F, 0.4F, 0.39F, 2.5F), dust(1.0F, 0.69F, 0.27F, 2.5F), dust(0.99F, 0.99F, 0.59F, 2.5F),
            dust(0.62F, 0.88F, 0.62F, 2.5F), dust(0.62F, 0.76F, 0.81F, 2.5F), dust(0.8F, 0.6F, 0.79F, 2.5F)
    };

    public GlimmeringUnicornEntity(EntityType<? extends GlimmeringUnicornEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("horn").seconds(1.5).range(0, 3.6).cooldown(30).weight(3)
                .at(12, (mob, target) -> {
                    for (LivingEntity hit : mob.hitCone(3.6, 110.0F, mob.damage(1.0), 1.2, 0.5))
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 30, 2);
                    mob.particles(ParticleTypes.END_ROD, mob.forwardPos(2.0D, 1.8D), 12, 0.4, 0.05);
                    mob.sound(SoundEvents.HORSE_ANGRY, 1.2F, 1.2F);
                })
                .build());

        abilities.add(Ability.builder("backfoot").seconds(2.0).range(0, 3.6).cooldown(50).weight(3)
                .at(21, (mob, target) -> {
                    for (LivingEntity hit : mob.hitCone(3.6, 180.0F, mob.damage(1.0), 1.0, 0.8))
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 30, 2);
                    mob.particles(ParticleTypes.CLOUD, mob.forwardPos(1.5D, 0.5D), 8, 0.4, 0.05);
                    mob.sound(SoundEvents.HORSE_LAND, 1.5F, 0.8F);
                })
                .build());

        // таран: замах, затем 5 секунд бега напролом
        abilities.add(Ability.builder("charge").seconds(6.5).range(4.0, 14.0).cooldown(300).weight(2).moving().unstoppable()
                .at(1, (mob, target) -> mob.sound(SoundEvents.HORSE_ANGRY, 1.5F, 0.8F))
                .every(20, 119, 1, (mob, target, tick) -> {
                    final Vec3 direction = target != null && mob.distanceTo(target) > 2.0F ? target.position().subtract(mob.position()) : mob.forward();
                    mob.lunge(direction, 0.55D, 0.0D);
                    final Vec3 tail = mob.localPos(0.0D, 0.0D, -1.5D);
                    for (int i = 0; i < RAINBOW.length; i++)
                        mob.particles(RAINBOW[i], tail.add(0.0D, 0.3D + i * 0.3D, 0.0D), 1, 0.1, 0.0);
                    if (tick % 4 == 0)
                        mob.sound(SoundEvents.HORSE_GALLOP, 1.0F, 1.0F);
                })
                .every(22, 119, 10, (mob, target, tick) -> {
                    for (LivingEntity hit : mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 2.4D, mob.damage(0.35), 0.9, 0.25)) {
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 20, 2);
                        mob.particles(ParticleTypes.WAX_OFF, hit.position().add(0.0D, 1.0D, 0.0D), 4, 0.3, 0.1);
                    }
                })
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        // «в бою» для клиентской анимации бега
        if (!level().isClientSide())
            setAggressive(getTarget() != null);
    }

    @Override
    protected String walkAnimation() {
        return isAggressive() ? "running" : "walking";
    }

    @Override
    public double chaseSpeed() {
        return 1.35D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(2.5);
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }

    @Override
    protected String id() {
        return "glimmering_unicorn";
    }
}
