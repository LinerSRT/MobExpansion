package net.mcskill.mobexpansion.entity.meadows;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.effect.MobExProjectileEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Алая королева Эвербллум — вторая, разъярённая форма: быстрее рубит, носится бегом, с рёвом отбрасывает
 * всех вокруг и метает сквозные алые волны.
 */
public class ScarletQueenEverbloomEntity extends EverbloomQueenEntity {
    @Nullable
    private Ability spawnIntro;

    public ScarletQueenEverbloomEntity(EntityType<? extends ScarletQueenEverbloomEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.ATTACK_DAMAGE, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected boolean angry() {
        return true;
    }

    @Override
    protected void registerPhaseAbilities(List<Ability> abilities) {
        // рёв: отбрасывает всех в радиусе 20 и расходится кольцами огня
        abilities.add(Ability.builder("savage").animation("s3").seconds(2.5).range(0, 20.0).cooldown(400).weight(2).unstoppable().ignoreSight()
                .at(15, (mob, target) -> mob.sound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.6F))
                .at(23, (mob, target) -> {
                    for (LivingEntity hit : mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 20.0D, mob.damage(0.4), 2.2, 0.6))
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                    mob.particles(ParticleTypes.EXPLOSION_EMITTER, mob.position().add(0.0D, 1.0D, 0.0D), 1, 0.0, 0.0);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.6F);
                })
                .every(23, 41, 3, (mob, target, tick) ->
                        mob.particleRing(ParticleTypes.SOUL_FIRE_FLAME, mob.position().add(0.0D, 0.4D, 0.0D), 2.0D + (tick - 23) * 1.0D, 70))
                .build());

        // алая волна: сквозной снаряд вперёд
        abilities.add(Ability.builder("scarlet_wave").animation("s4_slash").seconds(2.0).range(3.0, 28.0).cooldown(120).weight(3)
                .at(15, (mob, target) -> {
                    final Vec3 direction = mob.forward();
                    final MobExProjectileEntity wave = mob.projectile("scarlet_wave", mob.localPos(0.0D, 1.2D, 1.2D))
                            .item(new ItemStack(Items.FIRE_CHARGE)).scale(3.0F).spin()
                            .damage(mob.damage(2.0)).knockback(1.2D).pierce().life(25)
                            .trail(ParticleTypes.FLAME, 3);
                    wave.shoot(direction.x, 0.02D, direction.z, 1.6F, 0.0F);
                    mob.level().addFreshEntity(wave);
                    mob.sound(SoundEvents.TRIDENT_THROW, 3.0F, 0.6F);
                })
                .build());

        spawnIntro = Ability.builder("spawn").seconds(1.5).cooldown(0).when((mob, target) -> false).unstoppable()
                .at(1, (mob, target) -> {
                    mob.particles(ParticleTypes.FLAME, mob.position().add(0.0D, 1.0D, 0.0D), 40, 1.0, 0.05);
                    mob.sound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.5F);
                })
                .build();
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        abilities();
        return spawnIntro;
    }

    @Override
    protected String walkAnimation() {
        return isAggressive() ? "s4_run_loop" : "walking";
    }

    @Override
    public double chaseSpeed() {
        return 1.25D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(7.0);
    }

    @Override
    @Nullable
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected String id() {
        return "scarlet_queen_everbloom";
    }
}
