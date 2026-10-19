package net.mcskill.mobexpansion.entity.possessed;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Доспех руин: самый тяжёлый из одержимых. Бодает (оглушая), рубит, бьёт сверху с трещиной в земле,
 * подбрасывает нижним ударом, сносит натиском. Раненый — обрушивает «Руину»: расходящиеся кольца
 * ударной волны, которые подбрасывают и иссушают.
 */
public class PossessedRuinArmorEntity extends PossessedArmorEntity {
    public PossessedRuinArmorEntity(EntityType<? extends PossessedRuinArmorEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 320.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ARMOR, 18.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("headbutt").seconds(1.25).range(0, 2.5).cooldown(40).weight(3)
                .at(8, (mob, target) -> {
                    for (LivingEntity hit : mob.hitCone(2.5, 80.0F, mob.damage(0.9), 1.4, 0.4)) {
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 40, 3);
                        mob.effect(hit, MobEffects.CONFUSION, 80, 0);
                    }
                    mob.sound(SoundEvents.ANVIL_LAND, 0.8F, 0.7F);
                })
                .build());
        abilities.add(Ability.builder("swing").seconds(2.0).range(0, 3.8).cooldown(25).weight(4)
                .at(15, (mob, target) -> {
                    mob.hitCone(3.8, 160.0F, mob.damage(1.2), 1.0, 0.2);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);
                })
                .build());
        abilities.add(Ability.builder("overhead_attack").seconds(2.25).range(0, 3.8).cooldown(35).weight(3)
                .at(20, (mob, target) -> {
                    mob.hitCone(3.8, 60.0F, mob.damage(1.6), 0.6, 0.2);
                    final Vec3 crack = mob.forwardPos(3.0D, 0.1D);
                    mob.hitArea(crack, 2.0D, mob.damage(0.6), 0.4, 0.4);
                    mob.particles(ParticleTypes.EXPLOSION, crack, 2, 0.4, 0.0);
                    mob.particleLine(ParticleTypes.LARGE_SMOKE, mob.position().add(0, 0.1, 0), crack, 0.4D);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 0.8F);
                })
                .build());
        abilities.add(Ability.builder("stab").seconds(2.25).range(0, 4.5).cooldown(30).weight(3)
                .at(20, (mob, target) -> {
                    final Vec3 from = mob.position().add(0.0D, 1.2D, 0.0D);
                    mob.hitLine(from, from.add(mob.forward().scale(4.6D)), 0.6D, mob.damage(1.3), 0.6, 0.1);
                })
                .build());
        abilities.add(Ability.builder("underslash").seconds(3.0).range(0, 3.5).cooldown(70).weight(2)
                .at(15, (mob, target) -> {
                    mob.hitCone(3.5, 90.0F, mob.damage(1.1), 0.3, 1.25);
                    mob.particles(ParticleTypes.CLOUD, mob.forwardPos(2.0D, 0.3D), 10, 0.5, 0.05);
                    mob.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.6F);
                })
                .build());

        // натиск: бежит на цель, сминая всех на пути, и в конце бьёт оземь
        abilities.add(Ability.builder("rampage").seconds(2.25).range(4.0, 12.0).cooldown(150).weight(2).moving().unstoppable()
                .every(5, 36, 1, (mob, target, tick) -> {
                    final Vec3 dir = target != null ? target.position().subtract(mob.position()) : mob.forward();
                    mob.lunge(dir, 0.6D, 0.0D);
                    mob.hitAreaOnce(mob.position().add(0.0D, 1.0D, 0.0D), 1.6D, mob.damage(1.0), 1.2D, 0.3D);
                    if (tick % 4 == 0)
                        mob.particles(ParticleTypes.CLOUD, mob.position(), 2, 0.3, 0.02);
                })
                .at(37, (mob, target) -> {
                    mob.hitArea(mob.forwardPos(1.5D, 0.3D), 3.0D, mob.damage(1.3), 1.2, 0.4);
                    mob.particles(ParticleTypes.EXPLOSION, mob.forwardPos(1.5D, 0.3D), 2, 0.5, 0.0);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 0.7F);
                })
                .build());

        // Руина: пять колец ударной волны, расходящихся от доспеха
        abilities.add(Ability.builder("ruination").seconds(4.45).range(0, 10.0).cooldown(380).weight(6).unstoppable().ignoreSight()
                .when((mob, target) -> mob.healthBelow(0.75))
                .every(41, 57, 4, (mob, target, tick) -> {
                    final double radius = 2.0D + (tick - 41) / 2.0D;
                    final Vec3 center = mob.position().add(0.0D, 0.5D, 0.0D);
                    for (LivingEntity victim : mob.enemiesNear(center, radius + 1.5D)) {
                        if (victim.distanceTo(mob) < radius - 1.5D || !mob.firstHit(victim))
                            continue;
                        if (mob.hit(victim, mob.damage(1.5), 0.5D, 0.8D))
                            mob.effect(victim, MobEffects.WITHER, 80, 1);
                    }
                    mob.particleRing(ParticleTypes.LARGE_SMOKE, center, radius, (int) (radius * 12));
                    mob.particleRing(ParticleTypes.SOUL, center, radius, (int) (radius * 6));
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.5F);
                })
                .build());
    }

    @Override
    public double chaseSpeed() {
        return 0.95D;
    }

    @Override
    public String modelName() {
        return "possessed_ruin_armor";
    }

    @Override
    public String textureName() {
        return "possessed_ruin_armor";
    }

    @Override
    public String animationName() {
        return "possessed_ruin_armor";
    }
}
