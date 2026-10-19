package net.mcskill.mobexpansion.entity.possessed;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Доспех викинга: ледяной берсерк. Рубит топором правой и левой рукой (замораживая), бьёт серией,
 * метает топор, прыгает на цель с ударом оземь, а ледяные шипы бегут по земле к врагу.
 * «Безрассудный удар» — очень сильный, но после него доспех на пару секунд открыт (получает больше урона).
 */
public class PossessedVikingArmorEntity extends PossessedArmorEntity {
    private int exposedTicks;

    public PossessedVikingArmorEntity(EntityType<? extends PossessedVikingArmorEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 280.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    /** Мороз: замедление + эффект обморожения как в рыхлом снегу. */
    private static void frost(AbilityMobEntity mob, List<LivingEntity> hits, int ticks) {
        for (LivingEntity hit : hits) {
            mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, ticks, 1);
            hit.setTicksFrozen(Math.min(hit.getTicksRequiredToFreeze() + ticks, hit.getTicksFrozen() + ticks));
        }
        if (!hits.isEmpty())
            mob.particles(ParticleTypes.SNOWFLAKE, hits.getFirst().position().add(0.0D, 1.0D, 0.0D), 12, 0.4, 0.02);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("headbutt").seconds(1.0).range(0, 2.5).cooldown(40).weight(2)
                .at(8, (mob, target) -> {
                    for (LivingEntity hit : mob.hitCone(2.5, 80.0F, mob.damage(0.8), 1.3, 0.4))
                        mob.effect(hit, MobEffects.CONFUSION, 60, 0);
                    mob.sound(SoundEvents.ANVIL_LAND, 0.7F, 0.8F);
                })
                .build());
        abilities.add(chop("right_chop", 1.25, 5));
        abilities.add(chop("left_chop", 1.5, 10));
        abilities.add(Ability.builder("axe_combo").seconds(2.5).range(0, 3.5).cooldown(60).weight(2)
                .at(15, (mob, target) -> frost(mob, mob.hitCone(3.4, 120.0F, mob.damage(0.9), 0.3, 0.1), 40))
                .at(21, (mob, target) -> {
                    frost(mob, mob.hitCone(3.4, 120.0F, mob.damage(1.2), 1.0, 0.2), 60);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
                })
                .build());

        abilities.add(Ability.builder("axe_throw").seconds(3.0).range(6.0, 22.0).cooldown(110).weight(2)
                .at(23, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 hand = mob.localPos(-0.5D, 2.0D, 0.6D);
                    mob.shootAt(mob.projectile("", hand).item(new ItemStack(Items.IRON_AXE)).spin().scale(1.6F).gravity(0.02D)
                                    .damage(mob.damage(1.3)).knockback(0.6).effect(MobEffects.MOVEMENT_SLOWDOWN, 80, 2).pierce().life(60)
                                    .trail(ParticleTypes.SNOWFLAKE, 2),
                            target, 1.4F, 1.0F);
                    mob.sound(SoundEvents.TRIDENT_THROW.value(), 1.2F, 0.6F);
                })
                .build());

        abilities.add(Ability.builder("reckless_strike").seconds(1.5).range(0, 3.0).cooldown(80).weight(2)
                .at(7, (mob, target) -> {
                    frost(mob, mob.hitCone(3.2, 90.0F, mob.damage(2.0), 1.3, 0.3), 80);
                    mob.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.2F, 0.6F);
                    exposedTicks = 50;
                })
                .build());

        abilities.add(Ability.builder("reckless_leap").seconds(2.0).range(5.0, 14.0).cooldown(130).weight(2).moving().unstoppable()
                .at(4, (mob, target) -> {
                    if (target != null)
                        mob.leapTo(target.position(), 14);
                    mob.sound(SoundEvents.RAVAGER_ROAR, 0.6F, 1.4F);
                })
                .at(18, (mob, target) -> {
                    frost(mob, mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), 3.5D, mob.damage(1.6), 1.0, 0.6), 60);
                    mob.particleRing(ParticleTypes.SNOWFLAKE, mob.position().add(0.0D, 0.2D, 0.0D), 3.0D, 30);
                    mob.particles(ParticleTypes.EXPLOSION, mob.position(), 1, 0.0, 0.0);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.9F);
                })
                .build());

        // ледяные шипы бегут по земле от доспеха к цели
        abilities.add(Ability.builder("ice_spikes").seconds(3.0).range(3.0, 16.0).cooldown(150).weight(2)
                .every(26, 52, 2, (mob, target, tick) -> {
                    final double distance = 1.5D + (tick - 26) * 0.6D;
                    final Vec3 spot = mob.groundAt(mob.forwardPos(distance, 0.0D));
                    frost(mob, mob.hitAreaOnce(spot.add(0.0D, 0.5D, 0.0D), 1.3D, mob.damage(1.0), 0.1, 0.9), 80);
                    mob.particles(ParticleTypes.SNOWFLAKE, spot.add(0.0D, 0.5D, 0.0D), 10, 0.3, 0.05);
                    mob.particles(ParticleTypes.ITEM_SNOWBALL, spot.add(0.0D, 0.3D, 0.0D), 6, 0.3, 0.1);
                    mob.sound(SoundEvents.GLASS_BREAK, 0.6F, 1.4F);
                })
                .build());
    }

    private Ability chop(String name, double seconds, int hitTick) {
        return Ability.builder(name).seconds(seconds).range(0, 3.5).cooldown(14).weight(4)
                .at(hitTick, (mob, target) -> {
                    frost(mob, mob.hitCone(3.4, 100.0F, mob.damage(1.0), 0.5, 0.1), 40);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.7F);
                })
                .build();
    }

    @Override
    public void tick() {
        super.tick();
        if (exposedTicks > 0)
            exposedTicks--;
    }

    @Override
    public boolean hurt(@org.jetbrains.annotations.NotNull net.minecraft.world.damagesource.DamageSource source, float amount) {
        return super.hurt(source, exposedTicks > 0 ? amount * 1.5F : amount);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public String modelName() {
        return "possessed_viking_armor";
    }

    @Override
    public String textureName() {
        return "possessed_viking_armor";
    }

    @Override
    public String animationName() {
        return "possessed_viking_armor";
    }
}
