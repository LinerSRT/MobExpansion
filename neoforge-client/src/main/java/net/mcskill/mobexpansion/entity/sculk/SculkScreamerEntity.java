package net.mcskill.mobexpansion.entity.sculk;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Скалк-крикун: рвёт когтями (правая/левая лапа), а его долгий крик бьёт волнами — накладывает тьму
 * и замедление, будит всё скалк-окружение и вызывает из земли крабов.
 */
public class SculkScreamerEntity extends SculkCreatureEntity {
    public SculkScreamerEntity(EntityType<? extends SculkScreamerEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(slash("right_slash"));
        abilities.add(slash("left_slash"));

        abilities.add(Ability.builder("scream").seconds(7.0).range(0.0, 14.0).cooldown(320).weight(2).unstoppable()
                .onStart((mob, target) -> {
                    mob.sound(SoundEvents.WARDEN_ROAR, 2.0F, 1.3F);
                    if (target != null)
                        alertSculk(mob, target, 24.0D);
                })
                .every(5, 100, 20, (mob, target, tick) -> {
                    final Vec3 mouth = mob.localPos(0.0D, 1.6D, 0.8D);
                    for (LivingEntity hit : mob.hitArea(mouth, 10.0D, mob.damage(0.3), 0.4D, 0.1D)) {
                        mob.effect(hit, MobEffects.DARKNESS, 100, 0);
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 50, 1);
                    }
                    mob.particles(ParticleTypes.SONIC_BOOM, mouth, 1, 0.0, 0.0);
                    mob.particleRing(ParticleTypes.SCULK_SOUL, mob.position().add(0.0D, 0.2D, 0.0D), 3.0D + (tick / 20.0D) * 1.5D, 30);
                    mob.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.5F, 0.8F);
                })
                .at(40, (mob, target) -> {
                    // зов: из земли вылезают крабы, если их рядом мало
                    final int nearby = mob.level().getEntitiesOfClass(SculkCrabbyEntity.class, mob.getBoundingBox().inflate(16.0D)).size();
                    for (int i = nearby; i < 3; i++) {
                        final double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
                        mob.summon(MobExEntities.SCULK_CRABBY.get(), mob.groundAt(mob.position().add(Math.cos(angle) * 3.0D, 0.0D, Math.sin(angle) * 3.0D)));
                    }
                })
                .build());
    }

    private Ability slash(String name) {
        return Ability.builder(name).seconds(1.25).range(0.0, 3.0).cooldown(12).weight(3)
                .at(5, (mob, target) -> {
                    if (!mob.hitCone(3.0, 110.0F, mob.damage(1.0), 0.5, 0.2).isEmpty())
                        mob.sound(SoundEvents.WARDEN_ATTACK_IMPACT, 0.8F, 1.3F);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.7F);
                })
                .build();
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(8.0);
    }

    @Override
    public String modelName() {
        return "sculk_screamer";
    }

    @Override
    public String textureName() {
        return "sculk_screamer";
    }

    @Override
    public String animationName() {
        return "sculk_screamer";
    }
}
