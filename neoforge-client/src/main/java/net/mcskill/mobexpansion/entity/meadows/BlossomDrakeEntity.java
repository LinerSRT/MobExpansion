package net.mcskill.mobexpansion.entity.meadows;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Цветочный дракон: когтями и хвостом бьёт вблизи, дышит пыльцой-огнём, а с разбега хватает добычу пастью,
 * взмывает с ней вверх и обрушивает на землю.
 */
public class BlossomDrakeEntity extends MeadowsCreatureEntity {
    private static final Vec3 HOLD = new Vec3(0.0D, 0.7D, 2.3D);

    private Ability bite;
    private Ability flyUp;
    private Ability smash;
    private Ability smashEnd;
    private boolean landed;

    public BlossomDrakeEntity(EntityType<? extends BlossomDrakeEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("claws").animation("clawattack").seconds(1.5).range(0, 4.0).cooldown(40).weight(4)
                .at(12, (mob, target) -> {
                    mob.hitCone(4.0, 180.0F, mob.damage(1.0), 0.9, 0.1);
                    mob.particles(ParticleTypes.SWEEP_ATTACK, mob.forwardPos(2.2D, 1.2D), 2, 0.6, 0.0);
                    mob.sound(SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 1.6F);
                })
                .build());

        abilities.add(Ability.builder("tail").animation("tailattack").seconds(2.0).range(0, 4.5).cooldown(60).weight(3)
                .at(16, (mob, target) -> {
                    mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), 4.0D, mob.damage(1.0), 1.4, 0.3);
                    mob.particleRing(ParticleTypes.CLOUD, mob.position().add(0.0D, 0.25D, 0.0D), 3.0D, 32);
                    mob.sound(SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.4F);
                })
                .build());

        // дыхание: струя пыльцы-огня впереди, поджигает
        abilities.add(Ability.builder("pollen_breath").animation("pollenbreath").seconds(3.5).range(2.0, 15.0).cooldown(200).weight(2)
                .at(1, (mob, target) -> mob.sound(SoundEvents.ENDER_DRAGON_GROWL, 1.5F, 2.0F))
                .every(16, 56, 2, (mob, target, tick) -> {
                    final Vec3 mouth = mob.localPos(0.0D, 1.8D, 1.8D);
                    final Vec3 direction = mob.forward();
                    for (int i = 1; i <= 7; i++)
                        mob.particles(pollen(2.0F), mouth.add(direction.scale(i * 2.0D)).add(0.0D, -i * 0.12D, 0.0D), 4, 0.15D + i * 0.06D, 0.0D);
                })
                .every(20, 56, 10, (mob, target, tick) -> {
                    mob.hitCone(15.0, 40.0F, mob.damage(0.35), 0.2, 0.05);
                })
                .build());

        // хватка: рывок -> укус -> взлёт с жертвой -> бросок оземь
        abilities.add(Ability.builder("grasp").animation("run_start").seconds(1.5).range(5.0, 14.0).cooldown(400).weight(2).moving()
                .at(1, (mob, target) -> mob.sound(SoundEvents.RAVAGER_ROAR, 1.2F, 1.2F))
                .every(4, 29, 1, (mob, target, tick) -> {
                    if (target == null)
                        return;
                    mob.lunge(target.position().subtract(mob.position()), 0.6D, 0.0D);
                    if (tick % 4 == 0)
                        mob.sound(SoundEvents.HORSE_GALLOP, 1.2F, 0.75F);
                    if (mob.distanceTo(target) < 2.7D && mob.isEnemy(target))
                        mob.forceAbility(bite, target);
                })
                .build());

        bite = Ability.builder("bite").animation("run_start_end").seconds(1.5).cooldown(0).when((mob, target) -> false)
                .at(1, (mob, target) -> mob.sound(SoundEvents.RAVAGER_ATTACK, 1.2F, 1.5F))
                .at(7, (mob, target) -> {
                    if (target != null && mob.distanceTo(target) < 4.0D && mob.isEnemy(target)) {
                        mob.hit(target, mob.damage(0.7), 0.0D, 0.0D);
                        mob.grab(target, HOLD);
                        mob.sound(SoundEvents.GOAT_LONG_JUMP, 1.2F, 0.75F);
                    }
                })
                .onEnd((mob, target) -> {
                    if (mob.held() != null)
                        mob.forceAbility(flyUp, target);
                })
                .build();
        abilities.add(bite);

        flyUp = Ability.builder("fly_up").seconds(1.6).cooldown(0).when((mob, target) -> false).unstoppable()
                .onStart((mob, target) -> mob.setNoGravity(true))
                .every(0, 31, 1, (mob, target, tick) -> {
                    mob.setDeltaMovement(0.0D, tick < 24 ? 0.32D : 0.0D, 0.0D);
                    mob.hurtMarked = true;
                    if (tick == 10)
                        mob.sound(SoundEvents.ENDER_DRAGON_FLAP, 1.5F, 0.8F);
                })
                .onEnd((mob, target) -> {
                    if (mob.held() != null)
                        mob.forceAbility(smash, target);
                    else
                        mob.setNoGravity(false);
                })
                .build();
        abilities.add(flyUp);

        smash = Ability.builder("smash").seconds(1.0).cooldown(0).when((mob, target) -> false).unstoppable()
                .onStart((mob, target) -> landed = false)
                .at(4, (mob, target) -> mob.setNoGravity(false))
                .every(4, 19, 1, (mob, target, tick) -> {
                    if (landed)
                        return;
                    mob.setDeltaMovement(0.0D, -1.1D, 0.0D);
                    mob.hurtMarked = true;
                    if (tick >= 6 && mob.onGround())
                        impact(mob);
                })
                .at(19, (mob, target) -> {
                    if (!landed)
                        impact(mob);
                })
                .onEnd((mob, target) -> mob.forceAbility(smashEnd, target))
                .build();
        abilities.add(smash);

        smashEnd = Ability.builder("smash_end").seconds(1.3).cooldown(0).when((mob, target) -> false).build();
        abilities.add(smashEnd);
    }

    private void impact(AbilityMobEntity mob) {
        landed = true;
        mob.release(1.8D, 0.7D);
        mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), 5.0D, mob.damage(0.5), 1.4D, 0.7D);
        mob.particles(ParticleTypes.EXPLOSION, mob.position().add(0.0D, 0.3D, 0.0D), 3, 0.8D, 0.0D);
        mob.particleRing(ParticleTypes.CLOUD, mob.position().add(0.0D, 0.25D, 0.0D), 4.0D, 40);
        mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.8F);
    }

    @Override
    protected void onAbilityFinished(@Nullable Ability ability, boolean interrupted) {
        if (interrupted || ability == null || !"fly_up".equals(ability.name()))
            setNoGravity(false);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(2.0);
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected String id() {
        return "blossom_drake";
    }
}
