package net.mcskill.mobexpansion.entity.meadows;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Страж безмятежности: каменный великан, дремлющий, пока рядом нет игроков. Проснувшись, бьёт кулаком с
 * ударной волной, топает расходящимся кольцом, кидает глыбу, а щитом усиливает всех существ Луга.
 * Раненый в ядро, иногда закрывается и почти не получает урона.
 */
public class SerenitySentinelEntity extends MeadowsCreatureEntity {
    private static final int ASLEEP = 0;
    private static final int AWAKE = 1;
    private static final double WAKE_RADIUS = 16.0D;
    private static final DustColorTransitionOptions WAVE = new DustColorTransitionOptions(new Vector3f(1.0F, 1.0F, 1.0F), new Vector3f(0.416F, 0.451F, 0.82F), 4.0F);

    private Ability awaken;

    public SerenitySentinelEntity(EntityType<? extends SerenitySentinelEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public boolean isAsleep() {
        return getStance() == ASLEEP;
    }

    /** Проснуться и сразу взять цель (вызывается и при призыве королевой). */
    public void wakeUp(@Nullable LivingEntity target) {
        if (!isAsleep() || level().isClientSide())
            return;
        abilities(); // способности регистрируются лениво: у свежепризванного awaken ещё null
        setStance(AWAKE);
        sound(SoundEvents.IRON_GOLEM_REPAIR, 2.0F, 0.5F);
        if (target != null)
            setTarget(target);
        forceAbility(awaken, null);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        awaken = Ability.builder("awaken").seconds(1.5).cooldown(0).when((mob, target) -> false).unstoppable()
                .at(10, (mob, target) -> mob.particles(ParticleTypes.POOF, mob.position().add(0.0D, 4.0D, 0.0D), 25, 1.2, 0.05))
                .build();
        abilities.add(awaken);

        // кулак с размаху + ударная волна
        abilities.add(Ability.builder("punch").seconds(2.5).range(0, 7.0).cooldown(80).weight(4)
                .at(25, (mob, target) -> {
                    mob.hitCone(7.0, 120.0F, mob.damage(1.0), 0.9, 0.9);
                    mob.particles(ParticleTypes.EXPLOSION, mob.forwardPos(4.0D, 0.3D), 3, 1.0, 0.0);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.6F);
                })
                .every(25, 35, 1, (mob, target, tick) -> shockRing(mob, tick - 25, 10, 12.0D, 0.4))
                .build());

        // топот: кольцо расходится до 20 блоков
        abilities.add(Ability.builder("shock").seconds(3.0).range(0, 20.0).cooldown(220).weight(2).unstoppable().ignoreSight()
                .at(33, (mob, target) -> {
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.5F);
                    mob.particles(ParticleTypes.EXPLOSION_EMITTER, mob.position(), 1, 0.0, 0.0);
                })
                .every(33, 44, 1, (mob, target, tick) -> shockRing(mob, tick - 33, 11, 20.0D, 1.0))
                .build());

        // щит: усиливает существ Луга вокруг
        abilities.add(Ability.builder("shield").seconds(3.0).range(0, 20.0).cooldown(420).weight(1).ignoreSight()
                .when((mob, target) -> mob.healthBelow(0.8) || alliesNear(mob).size() > 1)
                .at(27, (mob, target) -> {
                    for (AbilityMobEntity ally : alliesNear(mob)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                        mob.particles(ParticleTypes.END_ROD, ally.position().add(0.0D, ally.getBbHeight() * 0.5D, 0.0D), 20, 0.6, 0.05);
                    }
                    mob.sound(SoundEvents.BEACON_ACTIVATE, 2.0F, 0.8F);
                })
                .build());

        // метание глыбы
        abilities.add(Ability.builder("throw_stone").seconds(4.0).range(8.0, 25.0).cooldown(180).weight(3)
                .at(25, (mob, target) -> mob.sound(SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.5F))
                .at(60, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 destination = target.position();
                    mob.lobAt(mob.projectile("stone", mob.localPos(0.0D, 7.5D, 1.0D))
                            .item(new ItemStack(Items.MOSSY_COBBLESTONE)).scale(3.0F).spin()
                            .gravity(0.05D).life(80).damage(0.0F)
                            .explosion(5.0D, mob.damage(1.0))
                            .effect(MobEffects.MOVEMENT_SLOWDOWN, 40, 2)
                            .impact(ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE.value()), destination, 24);
                    mob.sound(SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.7F);
                })
                .build());

        // защитная стойка: включается при ударе в ядро
        abilities.add(Ability.builder("block").seconds(7.0).cooldown(420).when((mob, target) -> false).unstoppable()
                .at(1, (mob, target) -> mob.sound(SoundEvents.IRON_GOLEM_REPAIR, 2.0F, 0.6F))
                .every(10, 130, 20, (mob, target, tick) -> mob.particles(ParticleTypes.GLOW, mob.position().add(0.0D, 4.5D, 0.0D), 15, 0.8, 0.05))
                .build());
    }

    /** Ударная волна: кольцо радиусом от 3 блоков до maxRadius за steps тиков, каждого врага бьёт один раз. */
    private static void shockRing(AbilityMobEntity mob, int step, int steps, double maxRadius, double multiplier) {
        final double radius = 3.0D + (maxRadius - 3.0D) * step / steps;
        final Vec3 center = mob.position();
        mob.particleRing(WAVE, center.add(0.0D, 0.3D, 0.0D), radius, (int) (radius * 8));
        for (LivingEntity victim : mob.enemiesNear(center.add(0.0D, 1.0D, 0.0D), radius + 0.5D)) {
            final double distance = victim.position().subtract(center).multiply(1.0D, 0.0D, 1.0D).length();
            if (distance < radius - 1.5D || Math.abs(victim.getY() - center.y) > 3.0D || !mob.firstHit(victim))
                continue;
            if (mob.hit(victim, mob.damage(multiplier), 0.8D, 0.6D))
                mob.effect(victim, MobEffects.MOVEMENT_SLOWDOWN, 15, 4);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying())
            return;
        if (isAsleep()) {
            getNavigation().stop();
            final Player nearest = level().getNearestPlayer(this, WAKE_RADIUS);
            if (tickCount > 10 && nearest != null && !nearest.isSpectator() && !nearest.isCreative())
                wakeUp(nearest);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // спящий не берёт цель сам — только через wakeUp
        if (isAsleep() && target != null)
            return;
        super.setTarget(target);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isAsleep() && !level().isClientSide() && source.getEntity() instanceof LivingEntity attacker)
            wakeUp(attacker);
        float taken = amount;
        if (isUsingAbility("block"))
            taken *= 0.25F;
        final boolean hurt = super.hurt(source, taken);
        // удар в ядро иногда заставляет закрыться
        if (hurt && !level().isClientSide() && !isAsleep() && !isBusy() && !isOnCooldown("block") && random.nextFloat() < 0.12F) {
            for (Ability ability : abilities()) {
                if ("block".equals(ability.name()))
                    forceAbility(ability, getTarget());
            }
        }
        return hurt;
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected PlayState handleAnimation(AnimationState<AbilityMobEntity> state) {
        if (!isDeadOrDying() && isAsleep())
            return state.setAndContinue(RawAnimation.begin().thenLoop("slumber"));
        return super.handleAnimation(state);
    }

    @Override
    protected String idleAnimation() {
        return "idle";
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(4.5);
    }

    @Override
    @Nullable
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.GREEN;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected String id() {
        return "serenity_sentinel";
    }
}
