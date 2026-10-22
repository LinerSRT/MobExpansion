package net.mcskill.mobexpansion.entity.meadows;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.entity.effect.MobExProjectileEntity;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Общая часть королев Луга: рубящие удары, большой взмах, сияющая область с отражающим пузырём, сияющий
 * снаряд с ослепляющей вспышкой, усиление союзников, притяжение, призыв стражей и отскок при ударе.
 * Алая форма ({@link #angry()}) быстрее и добавляет свои атаки через {@link #registerPhaseAbilities}.
 */
public abstract class EverbloomQueenEntity extends MeadowsCreatureEntity {
    private static final String MINION_TAG = "everbloom_minion";

    private boolean bubble;
    private int bubbleCooldown;
    private Ability jumpBack;

    protected EverbloomQueenEntity(EntityType<? extends EverbloomQueenEntity> entityType, Level level) {
        super(entityType, level);
    }

    /** Алая (разъярённая) форма: быстрее замахи, больше импульсов области, дополнительные атаки. */
    protected abstract boolean angry();

    /** Дополнительные способности формы. */
    protected void registerPhaseAbilities(List<Ability> abilities) {
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        final boolean angry = angry();

        abilities.add(slash("slash_right", "attack1", angry ? 8 : 12));
        abilities.add(slash("slash_left", "attack2", angry ? 9 : 13));

        abilities.add(Ability.builder("slash_huge").animation("s2").seconds(2.5).range(0, 7.5).cooldown(140).weight(2)
                .at(20, (mob, target) -> mob.sound(SoundEvents.EVOKER_PREPARE_ATTACK, 2.0F, 0.7F))
                .at(23, (mob, target) -> {
                    for (LivingEntity hit : mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), 6.5D, mob.damage(2.5), 1.5, 1.0)) {
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 30, 2);
                        mob.effect(hit, MobEffects.CONFUSION, 120, 0);
                    }
                    mob.particleRing(angry() ? ParticleTypes.FLAME : ParticleTypes.END_ROD, mob.position().add(0.0D, 0.4D, 0.0D), 5.5D, 48);
                    mob.particles(ParticleTypes.SWEEP_ATTACK, mob.position().add(0.0D, 1.2D, 0.0D), 6, 2.5, 0.0);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
                })
                .build());

        // сияющая область: пузырь отражает урон снарядами, вокруг бьют импульсы
        final int first = angry ? 20 : 15;
        final int pulses = angry ? 6 : 4;
        abilities.add(Ability.builder("luminous_area").animation("s1").duration(angry ? 100 : 70).range(0, 7.5).cooldown(140).weight(2).unstoppable()
                .at(first, (mob, target) -> {
                    bubble = true;
                    mob.sound(SoundEvents.BEACON_ACTIVATE, 2.0F, 1.4F);
                })
                .every(first, first + (pulses - 1) * 10, 10, (mob, target, tick) -> {
                    final Vec3 center = mob.position().add(0.0D, 1.0D, 0.0D);
                    for (LivingEntity hit : mob.hitArea(center, 7.0D, mob.damage(0.5), 0.5, 0.45)) {
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 10, 3);
                        mob.particles(ParticleTypes.END_ROD, hit.position(), 15, 0.3, 0.25);
                    }
                    mob.particleRing(ParticleTypes.END_ROD, mob.position().add(0.0D, 0.3D, 0.0D), 7.0D, 56);
                    mob.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 1.2F);
                })
                .build());

        abilities.add(Ability.builder("radiant_shot").animation("spell4").seconds(1.5).range(4.0, 22.0).cooldown(140).weight(2)
                .at(12, (mob, target) -> {
                    if (target == null)
                        return;
                    mob.sound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 1.2F);
                    mob.shootAt(mob.projectile("radiant_bullet", mob.localPos(0.0D, 2.6D, 0.8D))
                            .item(new ItemStack(Items.AMETHYST_SHARD)).scale(2.0F).spin()
                            .damage(mob.damage(1.0)).life(60)
                            .trail(ParticleTypes.END_ROD, 2)
                            .effect(MobEffects.MOVEMENT_SLOWDOWN, 20, 3)
                            .onImpact(bullet -> flash(mob, bullet.position())), target, 1.1F, 0.0F);
                })
                .build());

        abilities.add(Ability.builder("buff_armor").animation("spell3").seconds(1.5).range(0, 25.0).cooldown(240).weight(1).ignoreSight()
                .when((mob, target) -> mob.healthBelow(0.85))
                .at(15, (mob, target) -> {
                    for (AbilityMobEntity ally : alliesNear(mob)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                        mob.particles(ParticleTypes.HAPPY_VILLAGER, ally.position().add(0.0D, ally.getBbHeight() * 0.5D, 0.0D), 15, 0.5, 0.05);
                    }
                    mob.sound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.8F);
                })
                .build());

        abilities.add(Ability.builder("rose_pull").animation("spell2").seconds(1.5).range(0, 15.0).cooldown(220).weight(1)
                .at(15, (mob, target) -> {
                    for (LivingEntity victim : mob.enemiesNear(mob.position(), 15.0D)) {
                        pull(victim, mob.position(), 1.1D);
                        mob.effect(victim, MobEffects.MOVEMENT_SLOWDOWN, 50, 2);
                        mob.effect(victim, MobEffects.WEAKNESS, 50, 2);
                    }
                    mob.particleRing(ParticleTypes.CHERRY_LEAVES, mob.position().add(0.0D, 0.5D, 0.0D), 12.0D, 60);
                    mob.sound(SoundEvents.EVOKER_PREPARE_WOLOLO, 2.0F, 0.8F);
                })
                .build());

        // призыв стражей Луга (раз в минуту, пока прошлые не пали)
        abilities.add(Ability.builder("summon").animation("spell1").seconds(2.0).range(0, 24.0).cooldown(1200).weight(3).ignoreSight()
                .when((mob, target) -> mob.healthBelow(0.8) && minions(mob).isEmpty())
                .at(12, (mob, target) -> {
                    spawnMinion(mob, MobExEntities.FLORAL_SERPENT.get(), mob.localPos(0.0D, 0.0D, 3.5D), target);
                    spawnMinion(mob, MobExEntities.GLIMMERING_UNICORN.get(), mob.localPos(4.0D, 0.0D, 3.5D), target);
                    spawnMinion(mob, MobExEntities.SERENITY_SENTINEL.get(), mob.localPos(-4.0D, 0.0D, 3.5D), target);
                    mob.sound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.8F);
                })
                .build());

        jumpBack = Ability.builder("jump_back").duration(angry ? 30 : 40).cooldown(200).moving().when((mob, target) -> false)
                .at(7, (mob, target) -> mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F))
                .every(7, (angry ? 30 : 40) - 4, 1, (mob, target, tick) -> {
                    mob.lunge(mob.forward().scale(-1.0D), 0.42D, tick == 7 ? 0.3D : 0.0D);
                    mob.particles(ParticleTypes.CLOUD, mob.position().add(0.0D, 0.2D, 0.0D), 2, 0.3, 0.02);
                })
                .build();
        abilities.add(jumpBack);

        registerPhaseAbilities(abilities);
    }

    private Ability slash(String name, String animation, int hitTick) {
        return Ability.builder(name).animation(animation).duration(angry() ? 20 : 30).range(0, 4.5).cooldown(25).weight(4)
                .at(hitTick, (mob, target) -> {
                    mob.hitCone(4.5, 135.0F, mob.damage(1.0), 1.5, 0.15);
                    mob.particles(ParticleTypes.SWEEP_ATTACK, mob.forwardPos(2.0D, 1.4D), 2, 0.5, 0.0);
                    mob.particles(angry() ? ParticleTypes.FLAME : ParticleTypes.END_ROD, mob.forwardPos(2.0D, 1.4D), 10, 0.6, 0.05);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.8F);
                })
                .build();
    }

    /** Ослепляющая вспышка сияющего снаряда. */
    private static void flash(AbilityMobEntity mob, Vec3 at) {
        mob.particles(ParticleTypes.FLASH, at, 3, 0.2, 0.0);
        mob.particles(ParticleTypes.ELECTRIC_SPARK, at, 60, 1.5, 0.15);
        for (LivingEntity victim : mob.enemiesNear(at, 14.0D)) {
            mob.effect(victim, MobEffects.CONFUSION, 100, 1);
            mob.effect(victim, MobEffects.MOVEMENT_SLOWDOWN, 50, 2);
            mob.effect(victim, MobEffects.BLINDNESS, 30, 0);
        }
    }

    private static List<MeadowsCreatureEntity> minions(AbilityMobEntity mob) {
        return mob.level().getEntitiesOfClass(MeadowsCreatureEntity.class, mob.getBoundingBox().inflate(60.0D),
                entity -> entity.isAlive() && entity.getTags().contains(MINION_TAG));
    }

    private static <T extends MeadowsCreatureEntity> void spawnMinion(AbilityMobEntity mob, EntityType<T> type, Vec3 at, @Nullable LivingEntity target) {
        final Vec3 ground = mob.groundAt(at);
        final T minion = mob.summon(type, ground);
        if (minion == null)
            return;
        minion.addTag(MINION_TAG);
        if (minion instanceof SerenitySentinelEntity sentinel)
            sentinel.wakeUp(target);
        mob.particles(ParticleTypes.POOF, ground.add(0.0D, 0.5D, 0.0D), 20, 0.6, 0.05);
    }

    // ---------------------------------------------------------------- пузырь и отскок

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying())
            return;
        if (bubbleCooldown > 0)
            bubbleCooldown--;
        if (bubble && tickCount % 4 == 0)
            particles(dust(1.0F, 0.85F, 0.3F, 2.0F), position().add(0.0D, 1.8D, 0.0D), 12, 1.4D, 0.0D);
        setAggressive(getTarget() != null);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (bubble && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            reflect(source);
            return false;
        }
        final boolean hurt = super.hurt(source, amount);
        final LivingEntity target = getTarget();
        if (hurt && !level().isClientSide() && !isDeadOrDying() && !isBusy() && target != null && distanceTo(target) < 4.5F
                && !isOnCooldown("jump_back") && random.nextFloat() < 0.35F)
            forceAbility(jumpBack, target);
        return hurt;
    }

    /** Пузырь не пропускает урон и выпускает в обидчика три самонаводящихся шара. */
    private void reflect(DamageSource source) {
        if (level().isClientSide() || bubbleCooldown > 0 || !(source.getEntity() instanceof LivingEntity attacker) || !isEnemy(attacker))
            return;
        bubbleCooldown = 8;
        final float damage = damage(0.5);
        for (int i = 0; i < 3; i++) {
            final MobExProjectileEntity orb = projectile("bubble_orb", localPos((i - 1) * 0.8D, 3.0D, 0.2D))
                    .item(new ItemStack(Items.AMETHYST_SHARD)).damage(damage).life(60)
                    .homing(attacker, 0.25D).trail(ParticleTypes.END_ROD, 1)
                    .effect(MobEffects.MOVEMENT_SLOWDOWN, 10, 4);
            shootAt(orb, attacker, 0.9F, 8.0F);
        }
        sound(SoundEvents.ALLAY_THROW, 3.0F, 1.0F);
    }

    @Override
    protected void onAbilityFinished(@Nullable Ability ability, boolean interrupted) {
        bubble = false;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (!level().isClientSide())
            for (MeadowsCreatureEntity minion : minions(this))
                minion.discard();
        super.die(source);
    }

    @Override
    protected String idleAnimation() {
        return "idle";
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
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }
}
