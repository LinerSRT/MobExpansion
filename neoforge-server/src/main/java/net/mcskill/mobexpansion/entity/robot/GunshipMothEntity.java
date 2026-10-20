package net.mcskill.mobexpansion.entity.robot;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.FlyingAbilityMobEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Моль-канонерка: кружит над целью, обстреливает пулемётами с крыльев и раз в несколько секунд
 * выпускает залп самонаводящихся ракет.
 */
public class GunshipMothEntity extends FlyingAbilityMobEntity {
    private boolean leftGun;

    public GunshipMothEntity(EntityType<? extends GunshipMothEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.45D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // ракетный залп: 6 ракет поочерёдно с левого и правого крыла
        abilities.add(Ability.builder("rocket_barrage").seconds(3.6).range(6.0, 36.0).cooldown(110).weight(2).moving()
                .onStart((mob, target) -> mob.sound(SoundEvents.PISTON_EXTEND, 1.0F, 0.6F))
                .every(12, 52, 8, (mob, target, tick) -> {
                    if (target == null)
                        return;
                    leftGun = !leftGun;
                    final Vec3 launcher = mob.localPos(leftGun ? 4.8D : -4.8D, 4.2D, 1.2D);
                    mob.shootAt(mob.projectile("bullet", launcher).scale(4.0F).damage(mob.damage(0.5))
                                    .explosion(2.5D, mob.damage(1.0)).homing(target, 0.07D).life(80)
                                    .trail(ParticleTypes.LARGE_SMOKE, 1).impact(ParticleTypes.FLAME, null),
                            target, 0.9F, 6.0F);
                    mob.sound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 0.8F);
                })
                .build());

        // пулемёты: без отдельной анимации — моль продолжает лететь и стреляет короткими очередями
        abilities.add(Ability.builder("gun_burst").animation(null).duration(30).range(0.0, 28.0).cooldown(45).weight(3).moving()
                .every(0, 27, 3, (mob, target, tick) -> {
                    if (target == null)
                        return;
                    leftGun = !leftGun;
                    final Vec3 gun = mob.localPos(leftGun ? 3.0D : -3.0D, 3.6D, 2.4D);
                    mob.shootAt(mob.projectile("bullet", gun).damage(mob.damage(0.25)).knockback(0.05).life(40)
                            .trail(ParticleTypes.CRIT, 1), target, 2.4F, 4.0F);
                    mob.sound(SoundEvents.DISPENSER_LAUNCH, 0.5F, 1.8F);
                })
                .build());
    }

    @Override
    public double hoverHeight() {
        return 10.0D;
    }

    @Override
    public double preferredMinRange() {
        return 10.0D;
    }

    @Override
    public double preferredMaxRange() {
        return 16.0D;
    }

    @Override
    public String faction() {
        return RobotBugEntity.FACTION;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(6.1);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_LOOP_AGGRESSIVE;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public String modelName() {
        return "gunship_moth";
    }

    @Override
    public String textureName() {
        return "gunship_moth";
    }

    @Override
    public String animationName() {
        return "gunship_moth";
    }
}
