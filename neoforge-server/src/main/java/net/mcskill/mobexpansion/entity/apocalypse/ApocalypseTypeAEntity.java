package net.mcskill.mobexpansion.entity.apocalypse;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.init.MobExSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Apocalypse Type A — босс пустоты. Ближний бой (удары, комбо с отметками, апперкот, захват и бросок),
 * перемещения (телепорт за спину, исчезновение с падением сверху) и заклинания пустоты на моделях эффектов:
 * луч, вихрь пустоты, поле чёрных звёзд, обрушение зданий. На половине здоровья — вторая фаза:
 * кулдауны идут вдвое быстрее, открываются чёрная дыра, «чёрный ящик» (ловушка) и белая пустота.
 */
public class ApocalypseTypeAEntity extends AbilityMobEntity {
    public static final String FACTION = "void";
    private static final int PHASE_ONE = 0;
    private static final int PHASE_TWO = 1;
    private static final Vec3 HOLD = new Vec3(0.0D, 1.6D, 1.4D);

    private Ability throwAbility;
    private Ability vanish;
    @Nullable
    private Vec3 markedSpot;
    @Nullable
    private Vec3 beamDirection;

    public ApocalypseTypeAEntity(EntityType<? extends ApocalypseTypeAEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    private boolean phaseTwo() {
        return getStance() == PHASE_TWO;
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // ---------------- ближний бой
        abilities.add(Ability.builder("basic_attack").seconds(1.45).range(0, 3.5).cooldown(15).weight(4)
                .at(8, (mob, target) -> {
                    swing(mob, 1.0);
                    mob.sound(MobExSounds.APOCALYPSE_BASIC_ATTACK1.get(), 3.0F, 1.0F);
                })
                .build());
        abilities.add(Ability.builder("basic_attack2").seconds(1.8).range(0, 3.5).cooldown(20).weight(4)
                .at(8, (mob, target) -> {
                    swing(mob, 0.7);
                    mob.sound(MobExSounds.APOCALYPSE_BASIC_ATTACK1.get(), 3.0F, 1.0F);
                })
                .at(21, (mob, target) -> {
                    swing(mob, 0.9);
                    mob.sound(MobExSounds.APOCALYPSE_BASIC_ATTACK2.get(), 3.0F, 1.0F);
                })
                .build());
        abilities.add(Ability.builder("combo").seconds(3.15).range(0, 4.0).cooldown(90).weight(3).unstoppable()
                .at(9, (mob, target) -> comboHit(mob, target, "combo_first", 0.6, MobExSounds.APOCALYPSE_COMBO1.get()))
                .at(20, (mob, target) -> comboHit(mob, target, "combo_second", 0.6, MobExSounds.APOCALYPSE_COMBO2.get()))
                .at(33, (mob, target) -> comboHit(mob, target, "combo_third", 0.7, MobExSounds.APOCALYPSE_COMBO3.get()))
                .at(55, (mob, target) -> comboHit(mob, target, "combo_x", 1.5, MobExSounds.APOCALYPSE_COMBO_X.get()))
                .build());
        abilities.add(Ability.builder("uppercut").seconds(1.55).range(0, 3.0).cooldown(70).weight(2)
                .at(22, (mob, target) -> {
                    mob.hitCone(3.0, 90.0F, mob.damage(1.3), 0.2, 1.5);
                    mob.sound(MobExSounds.APOCALYPSE_UPPERCUT.get(), 3.0F, 1.0F);
                })
                .build());

        throwAbility = Ability.builder("throw").seconds(1.15).cooldown(0).when((mob, target) -> false)
                .at(13, (mob, target) -> {
                    final LivingEntity victim = mob.held();
                    if (victim != null) {
                        victim.invulnerableTime = 0;
                        victim.hurt(mob.damageSources().mobAttack(mob), mob.damage(1.0));
                    }
                    mob.release(2.4D, 0.8D);
                    mob.sound(MobExSounds.APOCALYPSE_THROW.get(), 3.0F, 1.0F);
                })
                .build();
        abilities.add(throwAbility);
        abilities.add(Ability.builder("grab").seconds(2.0).range(0, 3.0).cooldown(180).weight(2)
                .at(6, (mob, target) -> mob.sound(MobExSounds.APOCALYPSE_GRAB.get(), 3.0F, 1.0F))
                .at(27, (mob, target) -> {
                    if (target != null && mob.distanceTo(target) < 3.6D && mob.isEnemy(target))
                        mob.grab(target, HOLD);
                })
                .onEnd((mob, target) -> {
                    if (mob.held() != null)
                        mob.forceAbility(throwAbility, mob.held());
                })
                .build());

        // ---------------- перемещения
        abilities.add(Ability.builder("teleport").seconds(1.35).range(7.0, 30.0).cooldown(110).weight(2).ignoreSight()
                .at(6, (mob, target) -> mob.sound(MobExSounds.APOCALYPSE_TELEPORT.get(), 3.0F, 1.0F))
                .at(8, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize().scale(2.5D));
                    blink(mob, mob.groundAt(behind));
                    mob.faceTowards(target);
                })
                .build());
        vanish = Ability.builder("vanish").seconds(3.25).range(8.0, 30.0).cooldown(220).weight(1).unstoppable().ignoreSight()
                .at(7, (mob, target) -> {
                    mob.particles(ParticleTypes.REVERSE_PORTAL, mob.position().add(0, 1.5, 0), 40, 0.6, 0.1);
                    mob.setInvisible(true);
                    mob.setInvulnerable(true);
                })
                .at(9, (mob, target) -> mob.sound(MobExSounds.APOCALYPSE_VANISH.get(), 3.0F, 1.0F))
                .at(40, (mob, target) -> {
                    if (target != null)
                        mob.teleportTo(target.getX(), target.getY(), target.getZ());
                    mob.setInvisible(false);
                    mob.setInvulnerable(false);
                })
                .at(44, (mob, target) -> {
                    mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), 4.0D, mob.damage(1.5), 1.2D, 0.6D);
                    mob.particles(ParticleTypes.EXPLOSION_EMITTER, mob.position(), 1, 0.0, 0.0);
                    mob.particleRing(ParticleTypes.REVERSE_PORTAL, mob.position().add(0, 0.2, 0), 4.0D, 40);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.6F);
                })
                .build();
        abilities.add(vanish);

        // ---------------- заклинания пустоты
        abilities.add(Ability.builder("beam").seconds(5.45).range(4.0, 24.0).cooldown(200).weight(2).unstoppable().fixedFacing()
                .onStart((mob, target) -> {
                    if (target != null)
                        mob.faceTowards(target);
                })
                .at(9, (mob, target) -> {
                    final float yaw = target != null ? mob.yawTowards(target.position()) : mob.yBodyRot;
                    final float rad = yaw * Mth.DEG_TO_RAD;
                    beamDirection = new Vec3(-Mth.sin(rad), 0.0D, Mth.cos(rad));
                    // луч смотрит туда же, куда «смотрит» эффект (проверено в игре по стойке-мишени)
                    mob.spawnEffect("beam", mob.position().add(0.0D, 0.6D, 0.0D), 100, yaw)
                            .follow(mob, new Vec3(0.0D, 0.6D, 0.0D));
                })
                .at(31, (mob, target) -> mob.sound(MobExSounds.APOCALYPSE_BEAM.get(), 3.0F, 1.0F))
                .every(36, 94, 5, (mob, target, tick) -> {
                    final Vec3 from = mob.position().add(0.0D, 2.0D, 0.0D);
                    final Vec3 direction = beamDirection != null ? beamDirection : mob.forward();
                    for (LivingEntity hit : mob.hitLine(from, from.add(direction.scale(24.0D)), 1.4D, mob.damage(0.5), 0.3D, 0.0D))
                        mob.effect(hit, MobEffects.WITHER, 40, 0);
                })
                .build());

        abilities.add(Ability.builder("void_storm").seconds(2.65).range(0.0, 10.0).cooldown(240).weight(2).ignoreSight()
                .at(9, (mob, target) -> {
                    mob.spawnEffect("void_storm", mob.position(), 137).follow(mob, Vec3.ZERO).ticker((effect, age) -> {
                        if (age % 10 != 0)
                            return;
                        for (LivingEntity enemy : mob.enemiesNear(mob.position(), 9.0D))
                            pull(enemy, mob.position(), 0.25D);
                        mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 6.0D, mob.damage(0.5), 0.0D, 0.0D);
                        mob.particleRing(ParticleTypes.REVERSE_PORTAL, mob.position().add(0, 0.5, 0), 6.0D, 30);
                    });
                    mob.sound(MobExSounds.APOCALYPSE_VOID_STORM.get(), 3.0F, 1.0F);
                })
                .build());

        abilities.add(Ability.builder("black_star_field").seconds(2.9).range(0.0, 14.0).cooldown(240).weight(2).ignoreSight()
                .at(1, (mob, target) -> mob.sound(MobExSounds.APOCALYPSE_BLACK_STAR.get(), 3.0F, 1.0F))
                .at(19, (mob, target) -> {
                    final Vec3 center = mob.position();
                    mob.spawnEffect("black_star_field", center.add(0.0D, 0.05D, 0.0D), 58).ticker((effect, age) -> {
                        if (age < 10 || age % 10 != 0)
                            return;
                        for (LivingEntity enemy : mob.enemiesNear(center, 8.0D)) {
                            if (Math.abs(enemy.getY() - center.y) < 1.5D && mob.hit(enemy, mob.damage(0.7), 0.0D, 0.3D))
                                mob.effect(enemy, MobEffects.MOVEMENT_SLOWDOWN, 30, 1);
                        }
                    });
                })
                .build());

        abilities.add(Ability.builder("buildings_collapse").seconds(3.2).range(4.0, 20.0).cooldown(260).weight(2)
                .onStart((mob, target) -> {
                    markedSpot = target != null ? mob.groundAt(target.position()) : mob.forwardPos(6.0D, 0.0D);
                    mob.spawnEffect("buildings_collapse", markedSpot, 55, mob.yawTowards(markedSpot));
                })
                .every(0, 36, 6, (mob, target, tick) -> {
                    if (markedSpot != null)
                        mob.particleRing(ParticleTypes.LARGE_SMOKE, markedSpot.add(0.0D, 0.2D, 0.0D), 5.0D, 30);
                })
                .at(32, (mob, target) -> mob.sound(MobExSounds.APOCALYPSE_BUILDINGS_COLLAPSE.get(), 3.0F, 1.0F))
                .at(39, (mob, target) -> {
                    if (markedSpot == null)
                        return;
                    for (LivingEntity hit : mob.hitArea(markedSpot.add(0.0D, 1.0D, 0.0D), 5.5D, mob.damage(2.0), 0.5D, 0.2D))
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
                    mob.particles(ParticleTypes.EXPLOSION_EMITTER, markedSpot, 1, 0.0, 0.0);
                })
                .build());

        // ---------------- только вторая фаза
        abilities.add(Ability.builder("black_hole").seconds(9.5).range(6.0, 24.0).cooldown(420).weight(3).unstoppable()
                .when((mob, target) -> phaseTwo())
                .at(40, (mob, target) -> {
                    final Vec3 center = (target != null ? target.position() : mob.forwardPos(8.0D, 0.0D)).add(0.0D, 1.5D, 0.0D);
                    mob.spawnEffect("black_hole", center.subtract(0.0D, 1.5D, 0.0D), 150).ticker((effect, age) -> {
                        for (LivingEntity enemy : mob.enemiesNear(center, 10.0D))
                            pull(enemy, center, 0.12D);
                        if (age % 10 == 0)
                            mob.hitArea(center, 2.5D, mob.damage(0.6), 0.0D, 0.0D);
                        if (age == 140) {
                            mob.hitArea(center, 5.0D, mob.damage(1.5), 1.6D, 0.6D);
                            mob.particles(ParticleTypes.EXPLOSION_EMITTER, center, 1, 0.0, 0.0);
                        }
                    });
                    mob.sound(MobExSounds.APOCALYPSE_BLACK_HOLE.get(), 3.0F, 1.0F);
                })
                .build());

        abilities.add(Ability.builder("blackbox").seconds(9.55).range(4.0, 20.0).cooldown(500).weight(2).unstoppable()
                .when((mob, target) -> phaseTwo())
                .at(20, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 center = mob.groundAt(target.position());
                    mob.spawnEffect("blackbox", center, 210).glowing(false).ticker((effect, age) -> {
                        // мечи, вылетающие из ящика: тайминги звука из blackbox_skills оригинала
                        if (age == 150 || age == 155 || age == 160 || age == 177 || age == 206)
                            mob.level().playSound(null, center.x, center.y, center.z, MobExSounds.APOCALYPSE_SWORDS_WHOOSH.get(), mob.getSoundSource(), 3.0F, 1.0F);
                        for (LivingEntity enemy : mob.enemiesNear(center.add(0.0D, 1.0D, 0.0D), 6.0D)) {
                            final double distance = enemy.position().multiply(1.0D, 0.0D, 1.0D).distanceTo(center.multiply(1.0D, 0.0D, 1.0D));
                            if (distance > 2.8D && distance < 5.0D)
                                pull(enemy, center, 0.4D); // стенки ящика не выпускают
                            if (age % 10 == 0 && distance <= 3.5D && mob.hit(enemy, mob.damage(0.4), 0.0D, 0.0D))
                                mob.effect(enemy, MobEffects.MOVEMENT_SLOWDOWN, 20, 2);
                        }
                    });
                    mob.sound(MobExSounds.APOCALYPSE_BLACKBOX_BUILD.get(), 3.0F, 1.0F);
                })
                .build());

        abilities.add(Ability.builder("white_void").seconds(7.55).range(0.0, 16.0).cooldown(520).weight(2).unstoppable().ignoreSight()
                .when((mob, target) -> phaseTwo())
                .at(0, (mob, target) -> mob.spawnEffect("white_void", mob.position(), 151).follow(mob, Vec3.ZERO))
                .at(60, (mob, target) -> mob.sound(MobExSounds.APOCALYPSE_WHITE_VOID.get(), 3.0F, 1.0F))
                .every(10, 78, 6, (mob, target, tick) -> mob.particleRing(ParticleTypes.END_ROD, mob.position().add(0.0D, 1.5D, 0.0D), 16.0D * (1.0D - tick / 80.0D), 40))
                .at(80, (mob, target) -> {
                    for (LivingEntity hit : mob.hitArea(mob.position().add(0.0D, 1.5D, 0.0D), 16.0D, mob.damage(1.8), 1.5D, 0.5D))
                        mob.effect(hit, MobEffects.BLINDNESS, 60, 0);
                    mob.particles(ParticleTypes.FLASH, mob.position().add(0, 1.5, 0), 3, 1.0, 0.0);
                })
                .build());
    }

    private static void swing(AbilityMobEntity mob, double multiplier) {
        mob.hitCone(3.5, 120.0F, mob.damage(multiplier), 0.6, 0.2);
    }

    private static void comboHit(AbilityMobEntity mob, @Nullable LivingEntity target, String effect, double multiplier, SoundEvent sound) {
        final List<LivingEntity> hits = mob.hitCone(4.0, 110.0F, mob.damage(multiplier), multiplier > 1.0 ? 1.4 : 0.3, multiplier > 1.0 ? 0.5 : 0.05);
        final Vec3 at = target != null ? target.position() : mob.forwardPos(2.0D, 0.0D);
        mob.spawnEffect(effect, at, 15, mob.yBodyRot).animation("combo");
        mob.sound(sound, 3.0F, 1.0F);
        if (!hits.isEmpty())
            mob.particles(ParticleTypes.REVERSE_PORTAL, at.add(0.0D, 1.0D, 0.0D), 10, 0.3, 0.05);
    }

    private static void blink(AbilityMobEntity mob, Vec3 to) {
        mob.particles(ParticleTypes.REVERSE_PORTAL, mob.position().add(0, 1.5, 0), 30, 0.5, 0.1);
        mob.teleportTo(to.x, to.y, to.z);
        mob.particles(ParticleTypes.REVERSE_PORTAL, to.add(0, 1.5, 0), 30, 0.5, 0.1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying())
            return;
        // переход во вторую фазу: исчезает и обрушивается на цель
        if (!phaseTwo() && healthBelow(0.5)) {
            setStance(PHASE_TWO);
            particles(ParticleTypes.REVERSE_PORTAL, position().add(0, 1.5, 0), 80, 1.0, 0.2);
            if (getTarget() != null)
                forceAbility(vanish, getTarget());
        }
    }

    @Override
    protected int cooldownSpeed() {
        return phaseTwo() ? 2 : 1;
    }

    @Override
    protected void onAbilityFinished(@Nullable Ability ability, boolean interrupted) {
        if (ability != null && "vanish".equals(ability.name())) {
            setInvisible(false);
            setInvulnerable(false);
        }
        markedSpot = null;
        if (ability != null && "beam".equals(ability.name()))
            beamDirection = null;
    }

    @Override
    @Nullable
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.WHITE;
    }

    @Override
    public String faction() {
        return FACTION;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(11.05);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // в оригинале моб «Silent: true»: у босса только фирменные звуки способностей, без ванильных голосов
    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected void playHurtSound(@NotNull DamageSource source) {
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public String modelName() {
        return "apocalypse_type_a";
    }

    @Override
    public String textureName() {
        return "apocalypse_type_a";
    }

    @Override
    public String animationName() {
        return "apocalypse_type_a";
    }
}
