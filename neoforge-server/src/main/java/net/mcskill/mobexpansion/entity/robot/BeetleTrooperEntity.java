package net.mcskill.mobexpansion.entity.robot;

import net.mcskill.mobexpansion.entity.ability.Ability;
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
 * Жук-штурмовик: держит дистанцию и поливает очередями из пушки; если цель подобралась вплотную —
 * таранит её, расшвыривая всех на пути.
 */
public class BeetleTrooperEntity extends RobotBugEntity {
    public BeetleTrooperEntity(EntityType<? extends BeetleTrooperEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // очередь из трёх пуль: моменты выстрелов взяты из анимации fire (0.1 / 0.3 / 0.5 c)
        final Ability.Action shot = (mob, target) -> {
            if (target == null)
                return;
            final Vec3 muzzle = mob.localPos(0.0D, 1.0D, 1.4D);
            mob.shootAt(mob.projectile("bullet", muzzle).damage(mob.damage(0.6)).knockback(0.1).scale(1.4F)
                    .trail(ParticleTypes.CRIT, 1).impact(ParticleTypes.SMOKE, null), target, 2.2F, 2.5F);
            mob.sound(SoundEvents.CROSSBOW_SHOOT, 0.8F, 1.7F);
            mob.particles(ParticleTypes.SMOKE, muzzle, 3, 0.05, 0.02);
        };
        abilities.add(Ability.builder("fire").seconds(1.08).range(3.0, 22.0).cooldown(26).weight(3)
                .at(2, shot).at(6, shot).at(10, shot)
                .build());

        // таран: рывок вперёд, каждого задетого — один раз за рывок
        abilities.add(Ability.builder("charge").seconds(2.44).range(0.0, 6.0).cooldown(120).weight(2)
                .moving().fixedFacing().unstoppable()
                .onStart((mob, target) -> {
                    if (target != null)
                        mob.faceTowards(target);
                    mob.sound(SoundEvents.RAVAGER_ROAR, 0.6F, 1.6F);
                })
                .every(10, 36, 1, (mob, target, tick) -> {
                    mob.lunge(mob.forward(), 0.55D, 0.0D);
                    mob.hitAreaOnce(mob.forwardPos(1.2D, 0.6D), 1.8D, mob.damage(1.6), 1.3D, 0.45D)
                            .forEach(hit -> mob.sound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.8F, 1.2F));
                    if (tick % 3 == 0)
                        mob.particles(ParticleTypes.CLOUD, mob.position(), 2, 0.3, 0.02);
                })
                .build());
    }

    @Override
    protected CombatStyle combatStyle() {
        return CombatStyle.KITE;
    }

    @Override
    public double preferredMinRange() {
        return 6.0D;
    }

    @Override
    public double preferredMaxRange() {
        return 14.0D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(4.96);
    }

    @Override
    public String modelName() {
        return "beetle_trooper";
    }

    @Override
    public String textureName() {
        return "beetle_trooper";
    }

    @Override
    public String animationName() {
        return "beetle_trooper";
    }
}
