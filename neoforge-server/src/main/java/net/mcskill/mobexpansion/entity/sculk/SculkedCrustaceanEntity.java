package net.mcskill.mobexpansion.entity.sculk;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Заражённый скалком рак: медленный танк с панцирем. Удары и снаряды спереди наполовину гасятся панцирем —
 * его надо обходить. Клешнёй давит так, что ослабляет цель; каждый третий щипок — дробящий.
 */
public class SculkedCrustaceanEntity extends SculkCreatureEntity {
    private int pinchCount;

    public SculkedCrustaceanEntity(EntityType<? extends SculkedCrustaceanEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("pinch").seconds(1.75).range(0, 2.6).cooldown(18)
                .at(12, (mob, target) -> {
                    final boolean crushing = ++pinchCount % 3 == 0;
                    for (LivingEntity hit : mob.hitCone(2.4, 90.0F, mob.damage(crushing ? 1.6 : 1.0), crushing ? 0.8 : 0.3, 0.1)) {
                        mob.effect(hit, MobEffects.WEAKNESS, crushing ? 100 : 40, 0);
                        if (crushing)
                            mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
                    }
                    mob.sound(crushing ? SoundEvents.ANVIL_LAND : SoundEvents.TURTLE_EGG_CRACK, 0.6F, crushing ? 0.6F : 1.0F);
                })
                .build());
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        final Entity attacker = source.getDirectEntity();
        if (attacker != null && !source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.EXPLOSION)) {
            final Vec3 to = attacker.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (to.lengthSqr() > 1.0E-4D && to.normalize().dot(forward()) > 0.5D) {
                amount *= 0.5F;
                playSound(SoundEvents.SHIELD_BLOCK, 0.8F, 0.8F);
                particles(ParticleTypes.SCULK_SOUL, position().add(forward()).add(0, 1.0, 0), 3, 0.2, 0.02);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(12.0);
    }

    @Override
    public String modelName() {
        return "sculked_crustacean";
    }

    @Override
    public String textureName() {
        return "sculked_crustacean";
    }

    @Override
    public String animationName() {
        return "sculked_crustacean";
    }
}
