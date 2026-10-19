package net.mcskill.mobexpansion.entity.robot;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Муравей-роевик: мелкий быстрый боец. Сила в числе — чем больше сородичей рядом, тем больнее кусает
 * и быстрее бегает; если одного ударили, на обидчика сбегается весь рой поблизости.
 */
public class SwarmerAntEntity extends RobotBugEntity {
    private int swarmSize;

    public SwarmerAntEntity(EntityType<? extends SwarmerAntEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("attack").seconds(0.9).range(0, 2.2).cooldown(10)
                .at(7, (mob, target) -> {
                    final float bonus = 1.0F + 0.15F * Math.min(swarmSize, 4);
                    if (!mob.hitCone(2.0, 110.0F, mob.damage(bonus), 0.15, 0.05).isEmpty())
                        mob.sound(SoundEvents.SPIDER_HURT, 0.6F, 1.8F);
                })
                .build());
    }

    @Override
    public double chaseSpeed() {
        return 1.15D + 0.05D * Math.min(swarmSize, 4);
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(4.95);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 20 == 0) {
            swarmSize = level().getEntitiesOfClass(SwarmerAntEntity.class, getBoundingBox().inflate(8.0D), ant -> ant != this && ant.isAlive()).size();
            if (swarmSize >= 3 && getTarget() != null)
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false));
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        final boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && isEnemy(attacker)) {
            for (SwarmerAntEntity ant : level().getEntitiesOfClass(SwarmerAntEntity.class, getBoundingBox().inflate(16.0D), ant -> ant != this && ant.getTarget() == null))
                ant.setTarget(attacker);
            particles(ParticleTypes.ELECTRIC_SPARK, position().add(0, 0.5, 0), 4, 0.2, 0.1);
        }
        return hurt;
    }

    @Override
    public String modelName() {
        return "swarmer_ant";
    }

    @Override
    public String textureName() {
        return "swarmer_ant";
    }

    @Override
    public String animationName() {
        return "swarmer_ant";
    }
}
