package net.mcskill.mobexpansion.entity.robot;

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
 * Мокрица-танк: тяжёлая броня, бортовые пушки (левая/правая по очереди). Сворачивается в шар и катится
 * на цель, сминая всё на пути (в шаре почти неуязвима). Выходя из переката, иногда выпускает ЭМИ-импульс:
 * урон по площади, замедление и слабость.
 */
public class WoodlouseTankEntity extends RobotBugEntity {
    private static final int NORMAL = 0;
    private static final int ROLLING = 1;

    private Ability rollExit;
    private Ability rollExitEmp;

    public WoodlouseTankEntity(EntityType<? extends WoodlouseTankEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(cannon("left_fire", 1.4D));
        abilities.add(cannon("right_fire", -1.4D));

        rollExit = Ability.builder("roll_exit").seconds(0.55).cooldown(0)
                .when((mob, target) -> false)
                .build();
        rollExitEmp = Ability.builder("roll_exit_emp").seconds(4.15).cooldown(0)
                .when((mob, target) -> false)
                .at(6, (mob, target) -> mob.particleRing(ParticleTypes.ELECTRIC_SPARK, mob.position().add(0, 0.5, 0), 2.0D, 24))
                .at(23, (mob, target) -> {
                    final Vec3 center = mob.position().add(0.0D, 0.8D, 0.0D);
                    for (LivingEntity hit : mob.hitArea(center, 6.0D, mob.damage(1.2), 1.0D, 0.4D)) {
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 100, 2);
                        mob.effect(hit, MobEffects.WEAKNESS, 100, 0);
                    }
                    for (double r = 1.5D; r <= 6.0D; r += 1.5D)
                        mob.particleRing(ParticleTypes.ELECTRIC_SPARK, center, r, (int) (r * 10));
                    mob.particles(ParticleTypes.FLASH, center, 1, 0.0, 0.0);
                    mob.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2F, 1.4F);
                })
                .build();
        abilities.add(rollExit);
        abilities.add(rollExitEmp);

        // перекат: катится на цель, подправляя курс, и сминает всех на пути
        abilities.add(Ability.builder("roll").seconds(4.36).range(4.0, 18.0).cooldown(180).weight(2)
                .moving().unstoppable()
                .onStart((mob, target) -> {
                    setStance(ROLLING);
                    mob.sound(SoundEvents.ARMADILLO_ROLL, 1.2F, 0.6F);
                })
                .every(12, 82, 1, (mob, target, tick) -> {
                    final Vec3 dir = target != null ? target.position().subtract(mob.position()) : mob.forward();
                    mob.lunge(dir, 0.55D, 0.0D);
                    mob.hitAreaOnce(mob.position().add(0.0D, 0.8D, 0.0D), 2.0D, mob.damage(1.4), 1.5D, 0.5D)
                            .forEach(hit -> mob.sound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1.0F, 0.8F));
                    if (tick % 4 == 0)
                        mob.particles(ParticleTypes.CLOUD, mob.position(), 3, 0.5, 0.02);
                })
                .onEnd((mob, target) -> mob.forceAbility(mob.healthBelow(0.6) || mob.getRandom().nextFloat() < 0.35F ? rollExitEmp : rollExit, target))
                .build());
    }

    private Ability cannon(String name, double side) {
        return Ability.builder(name).seconds(0.6).range(3.0, 22.0).cooldown(18).weight(3)
                .at(4, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 barrel = mob.localPos(side, 1.0D, 1.0D);
                    mob.shootAt(mob.projectile("bullet", barrel).scale(1.8F).damage(mob.damage(1.0))
                                    .explosion(1.5D, mob.damage(0.6)).trail(ParticleTypes.SMOKE, 1).impact(ParticleTypes.LARGE_SMOKE, null),
                            target, 1.6F, 2.0F);
                    mob.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.2F, 0.6F);
                    mob.particles(ParticleTypes.LARGE_SMOKE, barrel, 3, 0.1, 0.02);
                })
                .build();
    }

    @Override
    protected void onAbilityFinished(@Nullable Ability ability, boolean interrupted) {
        if (ability != null && "roll".equals(ability.name()))
            setStance(NORMAL);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return super.hurt(source, getStance() == ROLLING ? amount * 0.4F : amount);
    }

    @Override
    protected CombatStyle combatStyle() {
        return CombatStyle.KITE;
    }

    @Override
    public double preferredMinRange() {
        return 5.0D;
    }

    @Override
    public double preferredMaxRange() {
        return 16.0D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(5.2);
    }

    @Override
    public String modelName() {
        return "woodlouse_tank";
    }

    @Override
    public String textureName() {
        return "woodlouse_tank";
    }

    @Override
    public String animationName() {
        return "woodlouse_tank";
    }
}
