package net.mcskill.mobexpansion.entity.possessed;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Пепельный доспех: всё, чего касается его клинок, загорается. Удар сверху, укол, широкий взмах, серия
 * быстрых рассечений, толчок в упор и рывок сквозь врагов с огненным следом. Раненый — копит жар и
 * взрывается огненной волной. Вокруг него всё время тлеет: стоящие вплотную загораются.
 */
public class PossessedCinderArmorEntity extends PossessedArmorEntity {
    public PossessedCinderArmorEntity(EntityType<? extends PossessedCinderArmorEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 260.0D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    private static void burn(List<LivingEntity> hits, int seconds) {
        for (LivingEntity hit : hits)
            hit.igniteForSeconds(seconds);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("overhead_attack").seconds(2.25).range(0, 3.5).cooldown(40).weight(3)
                .at(20, (mob, target) -> {
                    burn(mob.hitCone(3.6, 70.0F, mob.damage(1.4), 0.8, 0.3), 4);
                    mob.particles(ParticleTypes.FLAME, mob.forwardPos(2.5D, 0.2D), 20, 0.4, 0.05);
                    mob.sound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
                })
                .build());
        abilities.add(Ability.builder("stab").seconds(2.25).range(0, 4.2).cooldown(30).weight(3)
                .at(20, (mob, target) -> {
                    final Vec3 from = mob.position().add(0.0D, 1.2D, 0.0D);
                    burn(mob.hitLine(from, from.add(mob.forward().scale(4.4D)), 0.6D, mob.damage(1.2), 0.5, 0.1), 3);
                    mob.particleLine(ParticleTypes.FLAME, from, from.add(mob.forward().scale(4.4D)), 0.3D);
                })
                .build());
        abilities.add(Ability.builder("swing").seconds(2.5).range(0, 3.5).cooldown(25).weight(4)
                .at(15, (mob, target) -> {
                    burn(mob.hitCone(3.5, 160.0F, mob.damage(1.1), 0.9, 0.2), 3);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
                })
                .build());
        abilities.add(Ability.builder("slices").seconds(3.5).range(0, 3.2).cooldown(90).weight(2).unstoppable()
                .every(20, 33, 4, (mob, target, tick) -> {
                    burn(mob.hitCone(3.2, 120.0F, mob.damage(0.5), 0.15, 0.0), 2);
                    mob.particles(ParticleTypes.SMALL_FLAME, mob.forwardPos(1.6D, 1.2D), 10, 0.6, 0.02);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.2F);
                })
                .build());
        abilities.add(Ability.builder("pummel").seconds(1.0).range(0, 2.0).cooldown(30).weight(5)
                .at(5, (mob, target) -> burn(mob.hitCone(2.2, 90.0F, mob.damage(0.8), 1.6, 0.5), 2))
                .build());

        // рывок сквозь врагов, оставляя полосу огня
        abilities.add(Ability.builder("dash").seconds(4.0).range(5.0, 14.0).cooldown(140).weight(2)
                .moving().fixedFacing().unstoppable()
                .onStart((mob, target) -> {
                    if (target != null)
                        mob.faceTowards(target);
                })
                .at(15, (mob, target) -> {
                    mob.lunge(mob.forward(), 1.6D, 0.1D);
                    mob.sound(SoundEvents.FIRECHARGE_USE, 1.2F, 0.6F);
                })
                .every(15, 40, 1, (mob, target, tick) -> {
                    burn(mob.hitAreaOnce(mob.position().add(0.0D, 1.0D, 0.0D), 1.5D, mob.damage(1.2), 1.0D, 0.3D), 5);
                    mob.particles(ParticleTypes.FLAME, mob.position().add(0.0D, 0.3D, 0.0D), 6, 0.3, 0.02);
                    trailFire(mob);
                })
                .build());

        abilities.add(Ability.builder("explosion").seconds(5.5).range(0, 7.0).cooldown(320).weight(6).unstoppable().ignoreSight()
                .when((mob, target) -> mob.healthBelow(0.6))
                .every(0, 78, 3, (mob, target, tick) -> mob.particleRing(ParticleTypes.FLAME, mob.position().add(0.0D, 0.5D + tick / 40.0D, 0.0D), 7.0D * (1.0D - tick / 80.0D), 20))
                .at(80, (mob, target) -> {
                    final Vec3 center = mob.position().add(0.0D, 1.0D, 0.0D);
                    burn(mob.hitArea(center, 7.0D, mob.damage(2.2), 1.6D, 0.7D), 6);
                    mob.particles(ParticleTypes.EXPLOSION_EMITTER, center, 1, 0.0, 0.0);
                    mob.particles(ParticleTypes.FLAME, center, 80, 3.0, 0.15);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
                })
                .build());
    }

    /** Огненный след: короткоживущая зона без модели, поджигающая наступивших. */
    private static void trailFire(AbilityMobEntity mob) {
        if (mob.tickCount % 3 != 0)
            return;
        final Vec3 spot = mob.position();
        mob.spawnEffect("", spot, 60).ticker((effect, age) -> {
            if (age % 5 == 0)
                mob.particles(ParticleTypes.FLAME, spot.add(0.0D, 0.1D, 0.0D), 2, 0.3, 0.01);
            if (age % 10 == 0)
                for (LivingEntity victim : mob.enemiesNear(spot, 1.0D))
                    victim.igniteForSeconds(3);
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 40 == 0 && !isDeadOrDying()) {
            for (LivingEntity victim : enemiesNear(position().add(0.0D, 1.0D, 0.0D), 2.5D))
                victim.igniteForSeconds(2);
            particles(ParticleTypes.SMALL_FLAME, position().add(0.0D, 1.2D, 0.0D), 4, 0.4, 0.01);
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public String modelName() {
        return "possessed_cinder_armor";
    }

    @Override
    public String textureName() {
        return "possessed_cinder_armor";
    }

    @Override
    public String animationName() {
        return "possessed_cinder_armor";
    }
}
