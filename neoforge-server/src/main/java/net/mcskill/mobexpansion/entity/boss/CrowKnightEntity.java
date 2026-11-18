package net.mcskill.mobexpansion.entity.boss;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Вороний рыцарь — рыцарь в чёрных перьях с двумя клинками (пак Raven Knights), ~3,4 блока.
 * <ul>
 *     <li>серии взмахов перед собой, удары иссушают и подбрасывают; после серии в воздухе вокруг остаётся
 *     вороний туман (иссушение тем, кто в нём стоит);</li>
 *     <li>с 5–10 блоков — рывок к цели с серией;</li>
 *     <li>«взрыв перьев»: сильный удар, сковывает, затем 4 волны иссушения вокруг;</li>
 *     <li>каждые 12 с из земли вокруг и под врагами бьют шипы, каждые 11 с — вороньи стаи.</li>
 * </ul>
 * Свой — {@link CrowReaperEntity} (одна фракция). Блоки не ломает.
 */
public class CrowKnightEntity extends AbilityMobEntity {
    public static final String FACTION = "raven";
    private static final int SPIKES_PERIOD = 240;
    private static final int SWARM_PERIOD = 220;

    private int spikesTimer = SPIKES_PERIOD / 2;
    private int swarmTimer = SWARM_PERIOD;

    public CrowKnightEntity(EntityType<? extends CrowKnightEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 130;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 750.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public String faction() {
        return FACTION;
    }

    // ---------------------------------------------------------------- общие помощники воронов

    static void slash(AbilityMobEntity mob, double multiplier, float cone, double knockback, double lift, int witherTicks) {
        for (LivingEntity hit : mob.hitCone(5.0D, cone, mob.damage(multiplier), knockback, lift))
            if (witherTicks > 0)
                mob.effect(hit, MobEffects.WITHER, witherTicks, 0);
        mob.particles(ParticleTypes.SWEEP_ATTACK, mob.forwardPos(2.0D, 1.4D), 3, 0.8, 0.0);
        mob.particles(ParticleTypes.SQUID_INK, mob.forwardPos(2.0D, 1.4D), 10, 0.8, 0.02);
        mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 0.6F);
    }

    /** Вороний туман на 8 с: раз в секунду слабый урон и иссушение тем, кто внутри. */
    static void fog(AbilityMobEntity mob, Vec3 spot) {
        final float tick = mob.damage(0.12);
        mob.spawnEffect("crow_fog", mob.groundAt(spot), 160).animation("idle").glowing(false).ticker((effect, age) -> {
            if (age % 20 != 0)
                return;
            for (LivingEntity hit : mob.hitArea(effect.position().add(0.0D, 0.5D, 0.0D), 1.5D, tick, 0.0, 0.0))
                mob.effect(hit, MobEffects.WITHER, 30, 0);
        });
    }

    static void fogRing(AbilityMobEntity mob, double radius, int count) {
        for (int i = 0; i < count; i++) {
            final double angle = Math.PI * 2.0D * i / count;
            fog(mob, mob.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius));
        }
    }

    /** Две вороньи стаи в 4–5 блоках от ворона, мордой к цели: 2 с клюют всех в 3,5 блоках. */
    static void swarms(AbilityMobEntity mob, LivingEntity target) {
        final float peck = mob.damage(0.15);
        for (int i = 0; i < 2; i++) {
            final double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
            final double distance = 4.0D + mob.getRandom().nextDouble();
            final Vec3 spot = mob.position().add(Math.cos(angle) * distance, 0.5D, Math.sin(angle) * distance);
            final Vec3 look = target.position().subtract(spot);
            final float yaw = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) - 90.0D);
            mob.spawnEffect("crow_swarm", spot, 40, yaw).animation("spawn").glowing(false).ticker((effect, age) -> {
                if (age % 5 == 0)
                    mob.hitArea(effect.position().add(0.0D, 1.0D, 0.0D), 3.5D, peck, 0.1, 0.0);
            });
        }
        mob.sound(SoundEvents.PHANTOM_SWOOP, 2.0F, 0.6F);
    }

    // ---------------------------------------------------------------- способности

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("simplecombo").seconds(1.9).range(0, 5.0).cooldown(40).weight(3)
                .at(20, (mob, target) -> slash(mob, 1.0, 180.0F, 0.5, 0.1, 0))
                .build());
        abilities.add(Ability.builder("simplecombo2").seconds(1.9).range(0, 5.0).cooldown(60).weight(2).moving()
                .at(15, (mob, target) -> mob.lunge(mob.forward(), 0.8D, 0.0D))
                .at(20, (mob, target) -> slash(mob, 1.1, 120.0F, 1.2, 0.5, 0))
                .build());
        abilities.add(Ability.builder("comboflyattack").seconds(4.25).range(0, 5.0).cooldown(140).weight(2)
                .at(18, (mob, target) -> slash(mob, 1.2, 180.0F, 0.3, 0.1, 100))
                .at(56, (mob, target) -> {
                    slash(mob, 1.2, 180.0F, 1.2, 0.5, 100);
                    fogRing(mob, 4.0D, 6);
                })
                .build());
        abilities.add(Ability.builder("explosion").seconds(6.0).range(0, 6.0).cooldown(320).weight(1).unstoppable()
                .at(36, (mob, target) -> {
                    for (LivingEntity hit : mob.hitCone(5.0D, 180.0F, mob.damage(2.0), 0.4, 0.2)) {
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
                        mob.effect(hit, MobEffects.WITHER, 22, 1);
                    }
                    mob.particles(ParticleTypes.EXPLOSION, mob.forwardPos(2.0D, 1.0D), 3, 1.0, 0.0);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.8F, 0.8F);
                })
                .every(46, 82, 12, (mob, target, tick) -> {
                    for (LivingEntity hit : mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 6.5D, mob.damage(0.4), 0.0, 0.0))
                        mob.effect(hit, MobEffects.WITHER, 22, 1);
                    mob.particleRing(ParticleTypes.SQUID_INK, mob.position().add(0.0D, 0.5D, 0.0D), 6.5D, 36);
                    mob.sound(SoundEvents.PHANTOM_FLAP, 1.6F, 0.5F);
                })
                .build());
        abilities.add(Ability.builder("fastcombo").seconds(4.35).range(5.0, 10.0).cooldown(100).weight(3).moving()
                .at(20, (mob, target) -> {
                    if (target != null) {
                        mob.faceTowards(target);
                        mob.lunge(target.position().subtract(mob.position()), 1.3D, 0.0D);
                    }
                    fog(mob, mob.position());
                    mob.sound(SoundEvents.PHANTOM_SWOOP, 1.6F, 0.8F);
                })
                .at(26, (mob, target) -> {
                    if (target != null && mob.distanceTo(target) < 4.0D)
                        mob.hit(target, mob.damage(1.0), 0.3, 0.1);
                })
                .at(36, (mob, target) -> slash(mob, 1.0, 180.0F, 0.3, 0.1, 0))
                .at(56, (mob, target) -> {
                    for (LivingEntity enemy : mob.enemiesNear(mob.position(), 5.0D))
                        AbilityMobEntity.push(enemy, mob.position(), 1.2D, 0.6D);
                    mob.sound(SoundEvents.PHANTOM_FLAP, 1.6F, 0.7F);
                })
                .build());
    }

    // ---------------------------------------------------------------- шипы и стаи по таймеру

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying())
            return;
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceTo(target) > 20.0D)
            return;
        if (--spikesTimer <= 0) {
            spikesTimer = SPIKES_PERIOD;
            spikes(target);
        }
        if (--swarmTimer <= 0) {
            swarmTimer = SWARM_PERIOD;
            swarms(this, target);
        }
    }

    /** 5 шипов в 4–10 блоках вокруг и по одному под каждым врагом в 10 блоках: через 1 с бьют снизу. */
    private void spikes(LivingEntity target) {
        final float stab = damage(0.8);
        for (int i = 0; i < 5; i++) {
            final double angle = getRandom().nextDouble() * Math.PI * 2.0D;
            final double distance = 4.0D + getRandom().nextDouble() * 6.0D;
            spike(groundAt(position().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance)), stab);
        }
        for (LivingEntity enemy : enemiesNear(position(), 10.0D))
            spike(groundAt(enemy.position()), stab);
        sound(SoundEvents.POINTED_DRIPSTONE_LAND, 2.0F, 0.5F);
    }

    private void spike(Vec3 spot, float stab) {
        particleRing(ParticleTypes.SQUID_INK, spot.add(0.0D, 0.1D, 0.0D), 1.2D, 12);
        spawnEffect("crow_spike", spot, 200, getRandom().nextFloat() * 360.0F).animation("strike").glowing(false).ticker((effect, age) -> {
            if (age == 20)
                hitArea(effect.position().add(0.0D, 0.5D, 0.0D), 1.5D, stab, 0.0, 0.6);
        });
    }

    // ---------------------------------------------------------------- прочее

    @Override
    protected int deathTicks() {
        return Ability.ticks(9.25);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public String modelName() {
        return "crow_knight";
    }

    @Override
    public String textureName() {
        return "crow_knight";
    }

    @Override
    public String animationName() {
        return "crow_knight";
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.6F, 0.6F);
    }
}
