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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Фуракас — тяжёлый рыцарь с молотом и щитом (модель из пака Furacas), ~3,1 блока.
 * <ul>
 *     <li>вблизи — удар молотом (подбрасывает) и удар щитом (замедление и слепота);</li>
 *     <li>раз в 12 с парирует удар в упор: урон не проходит, атакующий получает ответный удар;</li>
 *     <li>издалека — вихрь с рывком к цели, «буря клинков» (прыжок и удар по всем в круге 12 блоков,
 *     круг виден заранее — можно выбежать) и цепи: притягивают задетых к рыцарю 3,5 с;</li>
 *     <li>«сила» — зелёный круг вокруг рыцаря на 10 с: урон каждую секунду всем внутри 8 блоков;</li>
 *     <li>ниже 40% здоровья — ярость (один раз): сопротивление и скорость до конца боя, вокруг встают
 *     8 столпов, которые жгут стоящих рядом 28 с.</li>
 * </ul>
 * Блоки не ломает, огня и падения не боится.
 */
public class FuracasEntity extends AbilityMobEntity {
    public static final String FACTION = "fallen";
    private static final double STORM_RADIUS = 12.0D;
    private static final int PARRY_COOLDOWN = 240;
    private static final Ability SPAWN = Ability.builder("spawn").seconds(3.6).cooldown(0).unstoppable().when((mob, target) -> false).build();

    private Ability parry;
    private int parryCooldown;
    private boolean enraged;
    /** Кого держат цепи текущего «Цепного броска». */
    private final List<LivingEntity> chained = new ArrayList<>();

    public FuracasEntity(EntityType<? extends FuracasEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 160;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public String faction() {
        return FACTION;
    }

    // ---------------------------------------------------------------- способности

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // молот оземь перед собой: подбрасывает
        abilities.add(Ability.builder("hammer_slam").animation("skill1").seconds(4.5).range(0, 5.0).cooldown(160).weight(3).unstoppable()
                .at(37, (mob, target) -> {
                    final Vec3 spot = mob.forwardPos(1.5D, 0.3D);
                    mob.hitArea(spot, 3.0D, mob.damage(1.0), 0.6, 1.2);
                    mob.particles(ParticleTypes.EXPLOSION, spot, 2, 0.6, 0.0);
                    mob.particleRing(ParticleTypes.HAPPY_VILLAGER, spot, 3.0D, 24);
                    mob.sound(SoundEvents.ANVIL_LAND, 1.6F, 0.5F);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.8F);
                })
                .build());
        // удар щитом: замедление и слепота
        abilities.add(Ability.builder("shield_bash").animation("skill2").seconds(4.5).range(0, 5.0).cooldown(200).weight(3).unstoppable()
                .at(37, (mob, target) -> {
                    final Vec3 spot = mob.forwardPos(1.5D, 0.8D);
                    for (LivingEntity hit : mob.hitArea(spot, 2.5D, mob.damage(1.0), 1.0, 0.2)) {
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 100, 1);
                        mob.effect(hit, MobEffects.BLINDNESS, 60, 0);
                    }
                    mob.particles(ParticleTypes.SQUID_INK, spot, 20, 1.0, 0.05);
                    mob.sound(SoundEvents.SHIELD_BLOCK, 2.0F, 0.5F);
                })
                .build());
        // вихрь: рывок к цели и серия ударов вокруг себя
        abilities.add(Ability.builder("whirlwind").animation("skill4").seconds(4.75).range(6.0, 20.0).cooldown(300).weight(2).moving()
                .at(30, (mob, target) -> {
                    if (target != null)
                        mob.lunge(target.position().subtract(mob.position()), 1.4D, 0.0D);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
                })
                .every(35, 57, 5, (mob, target, tick) -> {
                    mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 3.0D, mob.damage(0.35), 0.2, 0.35);
                    mob.particleRing(ParticleTypes.SWEEP_ATTACK, mob.position().add(0.0D, 1.0D, 0.0D), 2.5D, 6);
                    mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.7F);
                })
                .build());
        // буря клинков: круг виден 1,5 с, потом удар по всем внутри
        abilities.add(Ability.builder("blade_storm").animation("skill5").seconds(5.6).range(8.0, 25.0).cooldown(320).weight(2).unstoppable().ignoreSight()
                .at(10, (mob, target) -> mob.sound(SoundEvents.WITHER_SHOOT, 1.6F, 0.5F))
                .every(20, 52, 4, (mob, target, tick) ->
                        mob.particleRing(ParticleTypes.HAPPY_VILLAGER, mob.position().add(0.0D, 0.2D, 0.0D), STORM_RADIUS, 72))
                .at(55, (mob, target) -> {
                    mob.spawnEffect("furacas_blades", mob.position(), 30).animation("spawn");
                    mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), STORM_RADIUS, mob.damage(1.2), 0.6, 0.4);
                    mob.particles(ParticleTypes.EXPLOSION_EMITTER, mob.position(), 1, 0.0, 0.0);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.6F);
                })
                .build());
        // цепи: задетых тянет к рыцарю
        abilities.add(Ability.builder("chains").animation("skill6").seconds(6.0).range(5.0, 20.0).cooldown(480).weight(2).unstoppable()
                .at(43, (mob, target) -> {
                    if (!(mob instanceof FuracasEntity furacas))
                        return;
                    furacas.chained.clear();
                    for (LivingEntity enemy : mob.enemiesNear(mob.position(), 20.0D)) {
                        final Vec3 at = enemy.position();
                        mob.spawnEffect("furacas_chain", at, 80, mob.yawTowards(at) + 180.0F).animation("spawn").glowing(false);
                        if (mob.hit(enemy, mob.damage(0.6), 0.0, 0.0))
                            furacas.chained.add(enemy);
                    }
                    mob.sound(SoundEvents.CHAIN_PLACE, 2.0F, 0.5F);
                })
                .every(45, 115, 5, (mob, target, tick) -> {
                    if (!(mob instanceof FuracasEntity furacas))
                        return;
                    for (LivingEntity victim : furacas.chained)
                        if (victim.isAlive() && victim.distanceTo(mob) > 3.0D) {
                            AbilityMobEntity.pull(victim, mob.position(), 0.35D);
                            mob.particleLine(ParticleTypes.SMOKE, mob.position().add(0.0D, 1.5D, 0.0D), victim.position().add(0.0D, 1.0D, 0.0D), 1.5D);
                        }
                })
                .build());
        // сила: круг 8 блоков на 10 с
        abilities.add(Ability.builder("power").animation("skill7").seconds(5.0).range(0, 12.0).cooldown(600).weight(1).unstoppable()
                .at(32, (mob, target) -> {
                    final float tick = mob.damage(0.3);
                    mob.spawnEffect("furacas_power", mob.position(), 200).animation("spawn").follow(mob, Vec3.ZERO).ticker((effect, age) -> {
                        if (age % 20 != 10)
                            return;
                        mob.hitArea(effect.position().add(0.0D, 0.5D, 0.0D), 8.0D, tick, 0.0, 0.0);
                        mob.particleRing(ParticleTypes.HAPPY_VILLAGER, effect.position().add(0.0D, 0.2D, 0.0D), 8.0D, 40);
                    });
                    mob.sound(SoundEvents.BEACON_ACTIVATE, 2.0F, 0.5F);
                })
                .build());
        // ярость: один раз ниже 40%
        abilities.add(Ability.builder("rage").animation("skill8").seconds(3.5).range(0, 64.0).cooldown(20).weight(100).unstoppable().ignoreSight()
                .when((mob, target) -> mob instanceof FuracasEntity furacas && !furacas.enraged && mob.healthBelow(0.4D))
                .at(15, (mob, target) -> {
                    if (!(mob instanceof FuracasEntity furacas))
                        return;
                    furacas.enraged = true;
                    mob.effect(mob, MobEffects.DAMAGE_RESISTANCE, 20 * 600, 0);
                    mob.effect(mob, MobEffects.MOVEMENT_SPEED, 20 * 600, 0);
                    final float burn = mob.damage(0.35);
                    for (int i = 0; i < 8; i++) {
                        final double angle = Math.PI * 2.0D * i / 8.0D;
                        final Vec3 spot = mob.groundAt(mob.position().add(Math.cos(angle) * 7.0D, 0.0D, Math.sin(angle) * 7.0D));
                        mob.spawnEffect("furacas_aura", spot, 560).animation("pillar").ticker((effect, age) -> {
                            if (age >= 40 && age % 10 == 0)
                                mob.hitArea(effect.position().add(0.0D, 1.0D, 0.0D), 1.8D, burn, 0.3, 0.1);
                        });
                    }
                    mob.particles(ParticleTypes.SOUL_FIRE_FLAME, mob.position().add(0.0D, 1.5D, 0.0D), 60, 1.5, 0.05);
                    mob.sound(SoundEvents.WITHER_SPAWN, 1.5F, 0.8F);
                })
                .build());

        parry = Ability.builder("parry").animation("skill3").seconds(2.0).cooldown(0).when((mob, target) -> false).build();
        abilities.add(parry);
    }

    @Override
    protected void onAbilityFinished(@Nullable Ability ability, boolean interrupted) {
        if (ability != null && "chains".equals(ability.name()))
            chained.clear();
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        return SPAWN;
    }

    // ---------------------------------------------------------------- парирование

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && parryCooldown > 0)
            parryCooldown--;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (!level().isClientSide() && parryCooldown <= 0 && !isUsingAbility() && !isDeadOrDying()
                && source.getDirectEntity() instanceof LivingEntity attacker && isEnemy(attacker) && attacker.distanceTo(this) < 5.0D) {
            parryCooldown = PARRY_COOLDOWN;
            faceTowards(attacker);
            forceAbility(parry, attacker);
            hit(attacker, damage(1.0), 1.2, 0.3);
            particles(ParticleTypes.CRIT, position().add(0.0D, 1.8D, 0.0D), 20, 0.6, 0.2);
            sound(SoundEvents.SHIELD_BLOCK, 2.0F, 0.7F);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    // ---------------------------------------------------------------- прочее

    @Override
    protected int deathTicks() {
        return Ability.ticks(2.0) + 10;
    }

    @Override
    public boolean fireImmune() {
        return true;
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
        return "furacas";
    }

    @Override
    public String textureName() {
        return "furacas";
    }

    @Override
    public String animationName() {
        return "furacas";
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 0.8F, 0.5F);
    }
}
