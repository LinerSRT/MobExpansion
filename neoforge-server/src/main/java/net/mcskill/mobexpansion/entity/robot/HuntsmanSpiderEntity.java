package net.mcskill.mobexpansion.entity.robot;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Паук-охотник (снайпер): держится далеко, целится лазерным прицелом и делает мощный выстрел.
 * На большой дистанции — долгое прицеливание и заряженный разрывной выстрел. Если цель пропала из виду —
 * сбрасывает прицел; если подобрались вплотную — отпрыгивает назад.
 */
public class HuntsmanSpiderEntity extends RobotBugEntity {
    private static final DustParticleOptions LASER = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 0.6F);

    private boolean chargedShot;
    private Ability fire;

    public HuntsmanSpiderEntity(EntityType<? extends HuntsmanSpiderEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        fire = Ability.builder("fire").seconds(1.25).range(0, 64).cooldown(0).weight(1)
                .when((mob, target) -> false) // запускается только после прицеливания
                .at(4, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 muzzle = mob.localPos(0.0D, 1.3D, 1.8D);
                    final float damage = mob.damage(chargedShot ? 2.4 : 1.6);
                    final var bullet = mob.projectile("bullet", muzzle).damage(damage).knockback(0.5).scale(1.6F).pierce().life(60)
                            .trail(chargedShot ? ParticleTypes.FLAME : ParticleTypes.CRIT, 2).impact(ParticleTypes.SMOKE, null);
                    if (chargedShot)
                        bullet.explosion(2.0D, mob.damage(0.8));
                    mob.shootAt(bullet, target, 3.6F, 0.15F);
                    mob.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.5F, chargedShot ? 0.5F : 0.9F);
                    mob.particles(ParticleTypes.EXPLOSION, muzzle, 1, 0.0, 0.0);
                })
                .build();
        abilities.add(fire);

        abilities.add(aim("aim", 2.0, 4.0, 18.0, false));
        abilities.add(aim("aim_long", 3.45, 18.0, 48.0, true));

        // отскок назад, когда цель слишком близко
        abilities.add(Ability.builder("leap").seconds(0.75).range(0.0, 5.0).cooldown(80).weight(4).moving()
                .at(2, (mob, target) -> {
                    if (target == null)
                        return;
                    Vec3 away = DefaultRandomPos.getPosAway(mob, 9, 4, target.position());
                    if (away == null)
                        away = mob.position().add(mob.position().subtract(target.position()).normalize().scale(7.0D));
                    mob.leapTo(away, 14);
                    mob.sound(SoundEvents.SPIDER_AMBIENT, 1.0F, 1.6F);
                })
                .build());
    }

    private Ability aim(String name, double seconds, double minRange, double maxRange, boolean charged) {
        return Ability.builder(name).seconds(seconds).range(minRange, maxRange).cooldown(charged ? 90 : 50).weight(3)
                .onStart((mob, target) -> {
                    chargedShot = charged;
                    mob.sound(SoundEvents.CROSSBOW_LOADING_START.value(), 1.0F, charged ? 0.6F : 1.0F);
                })
                .every(0, 200, 2, (mob, target, tick) -> {
                    if (target == null || !mob.getSensing().hasLineOfSight(target)) {
                        // цель потеряна — опускаем прицел
                        mob.interruptAbility();
                        mob.playAnimation("noTarget");
                        return;
                    }
                    mob.particleLine(LASER, mob.localPos(0.0D, 1.3D, 1.8D), target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D), 0.6D);
                })
                .onEnd((mob, target) -> {
                    final LivingEntity current = mob.getTarget();
                    if (current != null && mob.getSensing().hasLineOfSight(current))
                        mob.forceAbility(fire, current);
                })
                .build();
    }

    @Override
    protected List<String> extraAnimations() {
        return List.of("noTarget");
    }

    @Override
    protected CombatStyle combatStyle() {
        return CombatStyle.KITE;
    }

    @Override
    public double preferredMinRange() {
        return 10.0D;
    }

    @Override
    public double preferredMaxRange() {
        return 24.0D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(5.0);
    }

    @Override
    public String modelName() {
        return "huntsman_spider";
    }

    @Override
    public String textureName() {
        return "huntsman_spider";
    }

    @Override
    public String animationName() {
        return "huntsman_spider";
    }
}
