package net.mcskill.mobexpansion.entity.boss;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.entity.effect.MobExProjectileEntity;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Древний страж — каменный исполин ~5,8 блока (модель из пака Guardian).
 * <ul>
 *     <li>появляется спящей статуей без головы и просыпается (3 с), когда враг подходит ближе 10 блоков
 *     или бьёт его;</li>
 *     <li>вблизи — удар перед собой, тройная серия (первый удар оглушает, последний подбрасывает),
 *     удар оземь на 4 блока вперёд с подбросом;</li>
 *     <li>12 сфер веером во все стороны;</li>
 *     <li>«Небесный меч»: круг 10 блоков у цели, через 5 с в него падает меч — урон и отброс, вокруг на 20 с
 *     встают 6 кристаллов: кто их коснётся, оглушён на 3 с;</li>
 *     <li>захват: держит цель перед собой и бьёт лучом, потом отбрасывает;</li>
 *     <li>ниже 30% здоровья — над ним на 20 с встаёт парящее оружие и раз в 2,5 с пускает самонаводящиеся ракеты
 *     во всех врагов в 25 блоках.</li>
 * </ul>
 * Блоки не ломает.
 */
public class AncientGuardianEntity extends AbilityMobEntity {
    public static final String FACTION = "ancient";
    /** Где держит схваченную цель: чуть правее, на высоте 2,3 блока, в 3,4 блока перед собой. */
    private static final Vec3 HOLD = new Vec3(0.6D, 2.3D, 3.4D);
    private static final double AWAKE_DISTANCE = 10.0D;

    private static final Ability DORMANT = Ability.builder("dormant").animation("spawn").duration(20 * 60 * 20).cooldown(0)
            .unstoppable().when((mob, target) -> false).build();
    private static final Ability AWAKEN = Ability.builder("awaken").animation("spawn_anime").seconds(3.0).cooldown(0)
            .unstoppable().when((mob, target) -> false)
            .at(10, (mob, target) -> mob.sound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 2.0F, 0.5F))
            .at(40, (mob, target) -> {
                mob.particleRing(ParticleTypes.SOUL_FIRE_FLAME, mob.position().add(0.0D, 0.3D, 0.0D), 4.0D, 40);
                mob.sound(SoundEvents.WITHER_SPAWN, 1.2F, 1.4F);
            })
            .build();

    private boolean weaponCalled;

    public AncientGuardianEntity(EntityType<? extends AncientGuardianEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 170;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ARMOR, 18.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public String faction() {
        return FACTION;
    }

    // ---------------------------------------------------------------- помощники

    private static void smash(AbilityMobEntity mob, Vec3 spot, double radius, double multiplier, double knockback, double lift) {
        mob.hitArea(spot, radius, mob.damage(multiplier), knockback, lift);
        mob.particles(ParticleTypes.EXPLOSION, spot, 2, radius * 0.3, 0.0);
        mob.particleRing(ParticleTypes.CAMPFIRE_COSY_SMOKE, spot, radius, 28);
        mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.6F);
    }

    // ---------------------------------------------------------------- способности

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("smash").animation("skill1").seconds(5.0).range(0, 7.0).cooldown(200).weight(3).unstoppable()
                .at(42, (mob, target) -> smash(mob, mob.forwardPos(2.0D, 0.3D), 3.5D, 1.0, 0.8, 0.4))
                .build());
        abilities.add(Ability.builder("quake").animation("skill2").seconds(5.5).range(0, 15.0).cooldown(300).weight(2).unstoppable()
                .at(43, (mob, target) -> smash(mob, mob.forwardPos(4.0D, 0.3D), 5.0D, 1.0, 0.3, 1.5))
                .build());
        abilities.add(Ability.builder("triple_strike").animation("skill3").seconds(5.0).range(0, 7.0).cooldown(320).weight(2).unstoppable()
                .at(42, (mob, target) -> {
                    for (LivingEntity hit : mob.hitArea(mob.forwardPos(3.0D, 0.5D), 3.5D, mob.damage(1.0), 0.2, 0.0))
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 20, 9);
                    mob.sound(SoundEvents.ANVIL_LAND, 1.4F, 0.6F);
                })
                .at(62, (mob, target) -> smash(mob, mob.forwardPos(3.0D, 0.3D), 3.5D, 1.0, 0.6, 0.2))
                .at(84, (mob, target) -> smash(mob, mob.forwardPos(3.0D, 0.3D), 3.5D, 1.6, 0.4, 1.5))
                .build());
        // 12 сфер во все стороны
        abilities.add(Ability.builder("orbs").animation("skill4").seconds(5.5).range(0, 20.0).cooldown(480).weight(2).unstoppable().ignoreSight()
                .at(67, (mob, target) -> {
                    final Vec3 from = mob.position().add(0.0D, 1.5D, 0.0D);
                    final float orbDamage = mob.damage(1.0);
                    for (int i = 0; i < 12; i++) {
                        final double angle = Math.PI * 2.0D * i / 12.0D;
                        final MobExProjectileEntity orb = mob.projectile("guardian_orb", from).damage(orbDamage).knockback(0.6).life(40)
                                .trail(ParticleTypes.END_ROD, 1).impact(ParticleTypes.FLASH, SoundEvents.AMETHYST_BLOCK_BREAK);
                        orb.shoot(Math.cos(angle), 0.0D, Math.sin(angle), 0.7F, 0.0F);
                        mob.level().addFreshEntity(orb);
                    }
                    mob.sound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.6F);
                })
                .build());
        // небесный меч
        abilities.add(Ability.builder("sky_sword").animation("skill5").seconds(5.5).range(6.0, 20.0).cooldown(600).weight(2).unstoppable()
                .at(52, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 spot = mob.groundAt(target.position());
                    final float impact = mob.damage(1.4);
                    mob.spawnEffect("guardian_sword", spot, 520, 0.0F).animation("spawn").ticker((effect, age) -> {
                        if (age < 100 && age % 10 == 0)
                            mob.particleRing(ParticleTypes.END_ROD, spot.add(0.0D, 0.2D, 0.0D), 10.0D, 60);
                        if (age == 100) {
                            mob.hitArea(spot.add(0.0D, 1.0D, 0.0D), 10.0D, impact, 1.5, 0.8);
                            mob.particles(ParticleTypes.EXPLOSION_EMITTER, spot, 2, 2.0, 0.0);
                            mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.5F);
                            for (int i = 0; i < 6; i++) {
                                final double angle = Math.PI * 2.0D * i / 6.0D + mob.getRandom().nextDouble();
                                final double distance = 3.0D + mob.getRandom().nextDouble() * 6.0D;
                                crystal(mob, mob.groundAt(spot.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance)));
                            }
                        }
                    });
                    mob.sound(SoundEvents.BEACON_ACTIVATE, 2.0F, 0.6F);
                })
                .build());
        // захват и луч
        abilities.add(Ability.builder("grab").animation("skill6").seconds(7.0).range(0, 6.0).cooldown(600).weight(2).unstoppable()
                .at(47, (mob, target) -> {
                    if (target != null && mob.distanceTo(target) < 7.0D && mob.isEnemy(target)) {
                        mob.grab(target, HOLD);
                        mob.sound(SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.5F);
                    }
                })
                .every(60, 90, 5, (mob, target, tick) -> {
                    final LivingEntity victim = mob.held();
                    if (victim != null)
                        mob.particleLine(ParticleTypes.END_ROD, mob.position().add(0.0D, 4.5D, 0.0D), victim.position().add(0.0D, 1.0D, 0.0D), 0.5D);
                })
                .at(92, (mob, target) -> {
                    final LivingEntity victim = mob.held();
                    mob.release(1.4D, 1.5D);
                    if (victim != null) {
                        victim.invulnerableTime = 0;
                        victim.hurt(mob.damageSources().mobAttack(mob), mob.damage(2.5));
                        mob.particles(ParticleTypes.FLASH, victim.position().add(0.0D, 1.0D, 0.0D), 1, 0.0, 0.0);
                    }
                    mob.sound(SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.5F);
                })
                .build());
        // парящее оружие (один раз, ниже 30%)
        abilities.add(Ability.builder("weapon").animation("skill7").seconds(6.5).range(0, 50.0).cooldown(20).weight(100).unstoppable().ignoreSight()
                .when((mob, target) -> mob instanceof AncientGuardianEntity guardian && !guardian.weaponCalled && mob.healthBelow(0.3D))
                .at(63, (mob, target) -> {
                    if (mob instanceof AncientGuardianEntity guardian)
                        guardian.weaponCalled = true;
                    final float missile = mob.damage(0.8);
                    mob.spawnEffect("guardian_weapon", mob.position(), 480).animation("active").ticker((effect, age) -> {
                        if (age < 90 || age > 380 || age % 50 != 0)
                            return;
                        final Vec3 from = effect.position().add(0.0D, 11.0D, 0.0D);
                        for (LivingEntity enemy : mob.enemiesNear(effect.position(), 25.0D)) {
                            final MobExProjectileEntity rocket = mob.projectile("guardian_missile", from).damage(missile).life(80)
                                    .homing(enemy, 0.12D).trail(ParticleTypes.SOUL_FIRE_FLAME, 1).impact(ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE.value());
                            mob.shootAt(rocket, enemy, 0.8F, 2.0F);
                        }
                        mob.sound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.0F, 0.6F);
                    });
                    mob.sound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.5F);
                })
                .build());
    }

    /** Кристалл у упавшего меча: кто подойдёт вплотную — оглушён на 3 с; живёт 20 с. */
    private static void crystal(AbilityMobEntity mob, Vec3 spot) {
        mob.spawnEffect("guardian_cry", spot, 400).animation("idle").ticker((effect, age) -> {
            if (age % 5 != 0)
                return;
            for (LivingEntity enemy : mob.enemiesNear(effect.position().add(0.0D, 0.5D, 0.0D), 1.2D)) {
                if (enemy.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
                    continue;
                mob.effect(enemy, MobEffects.MOVEMENT_SLOWDOWN, 60, 9);
                mob.effect(enemy, MobEffects.WEAKNESS, 60, 1);
                mob.spawnEffect("guardian_stun", enemy.position(), 60).animation("spawn").follow(enemy, Vec3.ZERO);
                mob.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 0.6F);
            }
        });
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        return DORMANT;
    }

    // ---------------------------------------------------------------- сон и пробуждение

    private boolean dormant() {
        return isUsingAbility("dormant");
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !dormant() || tickCount % 5 != 0)
            return;
        final LivingEntity target = getTarget();
        if (target != null && distanceTo(target) < AWAKE_DISTANCE)
            forceAbility(AWAKEN, target);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        final boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && dormant() && !isDeadOrDying())
            forceAbility(AWAKEN, getTarget());
        return hurt;
    }

    @Override
    protected String idleAnimation() {
        return dormant() ? "spawn" : "idle";
    }

    // ---------------------------------------------------------------- прочее

    @Override
    protected int deathTicks() {
        return Ability.ticks(3.5) + 10;
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
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public String modelName() {
        return "ancient_guardian";
    }

    @Override
    public String textureName() {
        return "ancient_guardian";
    }

    @Override
    public String animationName() {
        return "ancient_guardian";
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.2F, 0.5F);
    }
}
