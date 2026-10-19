package net.mcskill.mobexpansion.entity.sculk;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Эхо-окутыватель: скалковый маг. Держит дистанцию и стреляет самонаводящимися сгустками душ
 * (правой/левой рукой), поднимает из земли скалковые шипы под целью, а подпустив врага близко — взрывается
 * волной душ. Раненый — окутывается столпом тьмы: враги вокруг слепнут, а сам он восстанавливается.
 */
public class EchoEnshrouderEntity extends SculkCreatureEntity {
    public EchoEnshrouderEntity(EntityType<? extends EchoEnshrouderEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 35;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(shot("right_shot", -0.6D));
        abilities.add(shot("left_shot", 0.6D));

        abilities.add(Ability.builder("summon_spike").seconds(3.0).range(3.0, 20.0).cooldown(90).weight(3)
                .at(15, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 lead = target.position().add(target.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).scale(15.0D));
                    spike(mob, mob.groundAt(target.position()));
                    spike(mob, mob.groundAt(lead.add(1.2D, 0.0D, 0.0D)));
                    spike(mob, mob.groundAt(lead.add(-1.2D, 0.0D, 0.8D)));
                    mob.sound(SoundEvents.SCULK_BLOCK_SPREAD, 1.5F, 0.6F);
                })
                .build());

        abilities.add(Ability.builder("soul_explosion").seconds(4.0).range(0.0, 5.0).cooldown(150).weight(4).unstoppable()
                .every(0, 28, 4, (mob, target, tick) -> mob.particleRing(ParticleTypes.SOUL, mob.position().add(0.0D, 1.5D, 0.0D), 6.0D - tick / 6.0D, 24))
                .at(30, (mob, target) -> {
                    final Vec3 center = mob.position().add(0.0D, 1.5D, 0.0D);
                    for (LivingEntity hit : mob.hitArea(center, 6.0D, mob.damage(1.6), 1.4D, 0.5D))
                        mob.effect(hit, MobEffects.DARKNESS, 80, 0);
                    mob.particles(ParticleTypes.SOUL, center, 60, 2.0, 0.1);
                    mob.particles(ParticleTypes.SONIC_BOOM, center, 1, 0.0, 0.0);
                    mob.sound(SoundEvents.WARDEN_SONIC_BOOM, 1.5F, 1.2F);
                })
                .build());

        // столп тьмы: враги вокруг слепнут, маг лечится
        abilities.add(Ability.builder("enshroud").seconds(7.0).range(0.0, 20.0).cooldown(420).weight(5).unstoppable().ignoreSight()
                .when((mob, target) -> mob.healthBelow(0.7))
                .at(20, (mob, target) -> {
                    mob.spawnEffect("shroud_of_darkness", mob.position(), 120).animation("spawn").glowing(false).follow(mob, Vec3.ZERO);
                    mob.sound(SoundEvents.WARDEN_EMERGE, 1.5F, 1.4F);
                })
                .every(20, 130, 10, (mob, target, tick) -> {
                    for (LivingEntity enemy : mob.enemiesNear(mob.position(), 12.0D)) {
                        mob.effect(enemy, MobEffects.DARKNESS, 80, 0);
                        mob.effect(enemy, MobEffects.BLINDNESS, 30, 0);
                    }
                    mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 1));
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0));
                })
                .build());
    }

    private Ability shot(String name, double side) {
        return Ability.builder(name).seconds(3.0).range(4.0, 24.0).cooldown(24).weight(4)
                .at(16, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 hand = mob.localPos(side, 2.6D, 0.5D);
                    mob.shootAt(mob.projectile("", hand).damage(mob.damage(1.0)).homing(target, 0.06D).life(80)
                                    .effect(MobEffects.DARKNESS, 40, 0).trail(ParticleTypes.SOUL_FIRE_FLAME, 3).impact(ParticleTypes.SOUL, null),
                            target, 1.0F, 1.0F);
                    mob.sound(SoundEvents.SOUL_ESCAPE.value(), 2.0F, 0.8F);
                })
                .build();
    }

    /** Скалковый шип: вырастает (анимация spawn), через секунду бьёт и подбрасывает всех над собой. */
    private static void spike(AbilityMobEntity caster, Vec3 at) {
        caster.particles(ParticleTypes.SCULK_SOUL, at.add(0.0D, 0.1D, 0.0D), 6, 0.3, 0.01);
        caster.spawnEffect("sculk_spike", at, 60).animation("spawn").glowing(false).ticker((effect, age) -> {
            if (age == 20) {
                caster.hitArea(effect.position().add(0.0D, 0.6D, 0.0D), 1.3D, caster.damage(1.3), 0.0D, 0.9D);
                caster.particles(ParticleTypes.SCULK_SOUL, effect.position().add(0.0D, 1.0D, 0.0D), 10, 0.3, 0.05);
            }
        });
    }

    @Override
    protected CombatStyle combatStyle() {
        return CombatStyle.KITE;
    }

    @Override
    public double preferredMinRange() {
        return 7.0D;
    }

    @Override
    public double preferredMaxRange() {
        return 16.0D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(5.0);
    }

    @Override
    public String modelName() {
        return "echo_enshrouder";
    }

    @Override
    public String textureName() {
        return "echo_enshrouder";
    }

    @Override
    public String animationName() {
        return "echo_enshrouder";
    }
}
