package net.mcskill.mobexpansion.entity.sculk;

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

import java.util.List;

/**
 * Скалк-страж: огромный мини-босс. Бьёт с размаху, топает (ударная волна + облако пыли), языком
 * притягивает далёких врагов, а схватив цель — съедает её, восстанавливая себе здоровье.
 */
public class SculkSentinelEntity extends SculkCreatureEntity {
    private static final Vec3 HOLD = new Vec3(0.0D, 2.0D, 1.3D);

    private Ability eat;
    private float damageWhileHolding;

    public SculkSentinelEntity(EntityType<? extends SculkSentinelEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 220.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("smash").seconds(3.0).range(0.0, 3.5).cooldown(30).weight(4)
                .at(17, (mob, target) -> {
                    mob.hitCone(3.6, 100.0F, mob.damage(1.3), 1.0, 0.4);
                    mob.particles(ParticleTypes.SCULK_SOUL, mob.forwardPos(2.0D, 0.2D), 10, 0.5, 0.05);
                    mob.sound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.2F, 0.8F);
                })
                .build());

        abilities.add(Ability.builder("stomp").seconds(2.5).range(0.0, 5.0).cooldown(100).weight(2).unstoppable()
                .at(10, (mob, target) -> {
                    mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), 5.0D, mob.damage(1.0), 0.9, 0.6);
                    mob.spawnEffect("heavy_dust_cloud", mob.position(), 60).animation("spawn").scale(2.5F).glowing(false);
                    mob.particleRing(ParticleTypes.CLOUD, mob.position().add(0.0D, 0.2D, 0.0D), 4.0D, 32);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.6F);
                })
                .build());

        // язык: притянуть врага издалека
        abilities.add(Ability.builder("tongue_attack").seconds(4.0).range(4.0, 12.0).cooldown(130).weight(3)
                .at(20, (mob, target) -> {
                    if (target == null || !mob.getSensing().hasLineOfSight(target) || mob.distanceTo(target) > 13.0D)
                        return;
                    final Vec3 mouth = mob.localPos(0.0D, 2.4D, 0.8D);
                    mob.particleLine(ParticleTypes.SCULK_SOUL, mouth, target.position().add(0.0D, 1.0D, 0.0D), 0.4D);
                    if (mob.hit(target, mob.damage(0.5), 0.0D, 0.0D)) {
                        pull(target, mob.forwardPos(1.5D, 0.5D), 1.6D);
                        mob.effect(target, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
                    }
                    mob.sound(SoundEvents.WARDEN_TENDRIL_CLICKS, 1.5F, 0.6F);
                })
                .build());

        eat = Ability.builder("eat").seconds(5.5).cooldown(0).unstoppable()
                .when((mob, target) -> false)
                .every(20, 100, 20, (mob, target, tick) -> {
                    final LivingEntity victim = mob.held();
                    if (victim == null) {
                        mob.interruptAbility();
                        return;
                    }
                    victim.invulnerableTime = 0;
                    if (victim.hurt(mob.damageSources().mobAttack(mob), mob.damage(0.6)))
                        mob.heal(5.0F);
                    mob.particles(ParticleTypes.SCULK_SOUL, victim.position().add(0.0D, 1.0D, 0.0D), 8, 0.3, 0.05);
                    mob.sound(SoundEvents.GENERIC_EAT, 1.2F, 0.5F);
                })
                .onEnd((mob, target) -> mob.release(1.2D, 0.5D))
                .build();
        abilities.add(eat);

        // захват: если поймал — переходит к поеданию
        abilities.add(Ability.builder("grab").seconds(4.0).range(0.0, 3.2).cooldown(260).weight(2).unstoppable()
                .at(25, (mob, target) -> {
                    if (target != null && mob.distanceTo(target) < 3.8D && mob.isEnemy(target)) {
                        mob.grab(target, HOLD);
                        mob.sound(SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.2F);
                    }
                })
                .onEnd((mob, target) -> {
                    final LivingEntity victim = mob.held();
                    if (victim != null)
                        mob.forceAbility(eat, victim);
                })
                .build());
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        final boolean hurt = super.hurt(source, amount);
        // схваченного можно отбить: достаточно нанести стражу 15% здоровья, пока он держит цель
        if (hurt && held() != null) {
            damageWhileHolding += amount;
            if (damageWhileHolding >= getMaxHealth() * 0.15F) {
                damageWhileHolding = 0.0F;
                release(0.6D, 0.4D);
                interruptAbility();
                playSound(SoundEvents.WARDEN_HURT, 1.5F, 0.8F);
            }
        } else if (held() == null) {
            damageWhileHolding = 0.0F;
        }
        return hurt;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(12.0);
    }

    @Override
    public String modelName() {
        return "sculk_sentinel";
    }

    @Override
    public String textureName() {
        return "sculk_sentinel";
    }

    @Override
    public String animationName() {
        return "sculk_sentinel";
    }
}
