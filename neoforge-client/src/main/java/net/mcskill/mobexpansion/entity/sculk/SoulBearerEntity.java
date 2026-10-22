package net.mcskill.mobexpansion.entity.sculk;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.entity.ability.FlyingAbilityMobEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Носитель душ: парящий фонарь скалка. Стреляет веером из трёх сгустков душевного огня, а в бою
 * подпитывает раненых союзников-скалков. Погибая, выпускает души — вспышка душевного огня вокруг.
 */
public class SoulBearerEntity extends FlyingAbilityMobEntity {
    public SoulBearerEntity(EntityType<? extends SoulBearerEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("shoot").seconds(2.5).range(3.0, 20.0).cooldown(30).moving()
                .at(17, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 core = mob.position().add(0.0D, 1.0D, 0.0D);
                    final Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(core);
                    for (int i = -1; i <= 1; i++) {
                        final double angle = Math.toRadians(12.0D * i);
                        final Vec3 dir = new Vec3(aim.x * Math.cos(angle) - aim.z * Math.sin(angle), aim.y, aim.x * Math.sin(angle) + aim.z * Math.cos(angle));
                        final var flame = mob.projectile("", core).damage(mob.damage(0.8)).life(60)
                                .trail(ParticleTypes.SOUL_FIRE_FLAME, 2).impact(ParticleTypes.SOUL, null);
                        flame.shoot(dir.x, dir.y, dir.z, 0.85F, 0.5F);
                        mob.level().addFreshEntity(flame);
                    }
                    mob.sound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
                })
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        // подпитка союзников: раз в 2 секунды лечит самого раненого скалка рядом
        if (!level().isClientSide() && tickCount % 40 == 0 && getTarget() != null && !isDeadOrDying()) {
            level().getEntitiesOfClass(AbilityMobEntity.class, getBoundingBox().inflate(8.0D),
                            mob -> mob != this && SculkCreatureEntity.FACTION.equals(mob.faction()) && mob.getHealth() < mob.getMaxHealth())
                    .stream().min((a, b) -> Float.compare(a.getHealth() / a.getMaxHealth(), b.getHealth() / b.getMaxHealth()))
                    .ifPresent(ally -> {
                        ally.heal(Mth.clamp(ally.getMaxHealth() * 0.05F, 2.0F, 10.0F));
                        particleLine(ParticleTypes.SOUL, position().add(0.0D, 1.0D, 0.0D), ally.position().add(0.0D, ally.getBbHeight() * 0.6D, 0.0D), 0.5D);
                    });
        }
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (!dead && !level().isClientSide()) {
            final Vec3 core = position().add(0.0D, 1.0D, 0.0D);
            hitArea(core, 3.5D, damage(1.0), 0.6D, 0.3D);
            particles(ParticleTypes.SOUL_FIRE_FLAME, core, 40, 1.2, 0.05);
            sound(SoundEvents.SOUL_ESCAPE.value(), 2.0F, 0.6F);
        }
        super.die(source);
    }

    @Override
    public String faction() {
        return SculkCreatureEntity.FACTION;
    }

    @Override
    public double hoverHeight() {
        return 3.0D;
    }

    @Override
    public double preferredMinRange() {
        return 5.0D;
    }

    @Override
    public double preferredMaxRange() {
        return 9.0D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(8.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SCULK_BLOCK_BREAK;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SCULK_SHRIEKER_BREAK;
    }

    @Override
    public String modelName() {
        return "soul_bearer";
    }

    @Override
    public String textureName() {
        return "soul_bearer";
    }

    @Override
    public String animationName() {
        return "soul_bearer";
    }
}
