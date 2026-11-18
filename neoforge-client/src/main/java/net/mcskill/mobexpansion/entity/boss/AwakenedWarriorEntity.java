package net.mcskill.mobexpansion.entity.boss;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.init.MobExSounds;
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
 * Пробуждённый воин — быстрый дуэлянт ростом ~2 блока (модель и звуки из пака RPG Class Boss Warrior).
 * <ul>
 *     <li>до боя стоит в стойке ожидания, при появлении — пробуждение;</li>
 *     <li>серии c1–c3 и взлёт с ударом оземь (c4–c5), рывки в стороны и к цели, прыжок берсерка, вихрь с выпадом,
 *     «Инстинкт бастиона» (сопротивление урону);</li>
 *     <li>ниже 60% здоровья — вторая фаза: кулдауны идут вдвое быстрее, добавляются «Кровавый барьер»
 *     (−70% урона и волна в конце), «Порочный удар» и «Удар ярости»;</li>
 *     <li>сильный удар иногда сбивает его (откат 2,5 с), в это время он получает на 30% больше урона.</li>
 * </ul>
 */
public class AwakenedWarriorEntity extends AbilityMobEntity {
    public static final String FACTION = "awakened";
    private static final int STANCE_REST = 0;
    private static final int STANCE_COMBAT = 1;

    private static final Ability START = Ability.builder("start_combat").seconds(1.5).cooldown(0).when((mob, target) -> false).build();

    private Ability flinch;
    private int flinchCooldown;
    private int barrierTicks;

    public AwakenedWarriorEntity(EntityType<? extends AwakenedWarriorEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 650.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    public String faction() {
        return FACTION;
    }

    private boolean secondPhase() {
        return healthBelow(0.6D);
    }

    // ---------------------------------------------------------------- помощники

    private static void closeIn(AbilityMobEntity mob, @Nullable LivingEntity target, double speed) {
        if (target != null && mob.distanceTo(target) > 2.5D)
            mob.lunge(target.position().subtract(mob.position()), speed, 0.0D);
    }

    private static void swing(AbilityMobEntity mob, double multiplier, double range, float cone, double knockback, double lift, SoundEvent sound) {
        mob.hitCone(range, cone, mob.damage(multiplier), knockback, lift);
        mob.particles(ParticleTypes.SWEEP_ATTACK, mob.forwardPos(1.6D, 1.1D), 2, 0.4, 0.0);
        mob.sound(sound, 1.4F, 1.0F);
    }

    private static void slam(AbilityMobEntity mob, double multiplier, double radius, double knockback, double lift) {
        mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), radius, mob.damage(multiplier), knockback, lift);
        mob.particleRing(ParticleTypes.CAMPFIRE_COSY_SMOKE, mob.position().add(0.0D, 0.2D, 0.0D), radius, 28);
        mob.particles(ParticleTypes.EXPLOSION, mob.position(), 1, 0.0, 0.0);
        mob.sound(MobExSounds.AWAKENED_WARRIOR_STOMP.get(), 1.8F, 1.0F);
        mob.sound(MobExSounds.AWAKENED_WARRIOR_RUPTURE.get(), 1.6F, 1.0F);
    }

    private static Vec3 side(AbilityMobEntity mob, double sign) {
        final Vec3 fwd = mob.forward();
        return new Vec3(-fwd.z * sign, 0.0D, fwd.x * sign);
    }

    // ---------------------------------------------------------------- способности

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // c1 -> c2 -> c3: три выпада с подшагом к цели
        abilities.add(Ability.builder("brutal_combo").animation("c1").duration(40).range(0, 6.0).cooldown(40).weight(4)
                .at(4, (mob, target) -> closeIn(mob, target, 0.6D))
                .at(6, (mob, target) -> swing(mob, 0.8, 3.5, 110.0F, 0.4, 0.05, MobExSounds.AWAKENED_WARRIOR_SLASH1.get()))
                .at(13, (mob, target) -> mob.playAnimation("c2"))
                .at(17, (mob, target) -> closeIn(mob, target, 0.6D))
                .at(19, (mob, target) -> swing(mob, 0.8, 3.5, 110.0F, 0.4, 0.05, MobExSounds.AWAKENED_WARRIOR_SLASH2.get()))
                .at(26, (mob, target) -> mob.playAnimation("c3"))
                .at(30, (mob, target) -> closeIn(mob, target, 0.7D))
                .at(32, (mob, target) -> swing(mob, 1.1, 5.0, 60.0F, 0.9, 0.1, MobExSounds.AWAKENED_WARRIOR_PIERCE.get()))
                .build());
        // c4 -> c5: подброс, взлёт и удар сверху
        abilities.add(Ability.builder("rising_combo").animation("c4").duration(40).range(0, 4.5).cooldown(100).weight(2).unstoppable()
                .at(10, (mob, target) -> swing(mob, 1.0, 3.0, 120.0F, 0.2, 0.9, MobExSounds.AWAKENED_WARRIOR_SLASH1.get()))
                .at(11, (mob, target) -> mob.lunge(mob.forward(), 0.3D, 0.6D))
                .at(16, (mob, target) -> swing(mob, 0.8, 3.0, 140.0F, 0.2, 0.4, MobExSounds.AWAKENED_WARRIOR_SLASH2.get()))
                .at(20, (mob, target) -> mob.playAnimation("c5"))
                .at(26, (mob, target) -> {
                    if (target != null)
                        mob.lunge(target.position().subtract(mob.position()), 0.4D, 0.0D);
                    mob.setDeltaMovement(mob.getDeltaMovement().x, -1.2D, mob.getDeltaMovement().z);
                    mob.hurtMarked = true;
                })
                .at(30, (mob, target) -> slam(mob, 1.4, 3.0, 0.6, 0.2))
                .at(34, (mob, target) -> mob.hitArea(mob.position().add(0.0D, 0.5D, 0.0D), 4.0D, mob.damage(0.8), 0.8, 0.5))
                .build());
        // уклонения в стороны и назад
        abilities.add(dash("dash_left", 0.65, -1.0));
        abilities.add(dash("dash_right", 0.65, 1.0));
        abilities.add(Ability.builder("dash_back").seconds(0.8).range(0, 3.0).cooldown(80).weight(1).moving().fixedFacing()
                .at(2, (mob, target) -> {
                    mob.lunge(mob.forward().scale(-1.0D), 1.1D, 0.15D);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_AIRDASH.get(), 1.2F, 1.0F);
                })
                .build());
        // рывок вперёд с ударом — сокращает дистанцию
        abilities.add(Ability.builder("dash_front").seconds(0.65).range(5.0, 12.0).cooldown(80).weight(2).moving().unstoppable()
                .at(2, (mob, target) -> {
                    if (target != null)
                        mob.lunge(target.position().subtract(mob.position()), 1.4D, 0.0D);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_AIRDASH.get(), 1.2F, 1.0F);
                })
                .every(4, 12, 2, (mob, target, tick) -> mob.hitAreaOnce(mob.forwardPos(1.2D, 1.0D), 1.8D, mob.damage(0.9), 0.6, 0.2))
                .build());
        // прыжок берсерка: прыгает к цели и бьёт оземь
        abilities.add(Ability.builder("berserker_leap").seconds(2.0).range(5.0, 16.0).cooldown(140).weight(2).moving().unstoppable()
                .at(2, (mob, target) -> mob.sound(MobExSounds.AWAKENED_WARRIOR_STOMP_CHARGE.get(), 1.4F, 1.0F))
                .at(6, (mob, target) -> {
                    if (target != null)
                        mob.leapTo(target.position(), 14);
                })
                .at(20, (mob, target) -> slam(mob, 1.5, 3.5, 1.2, 0.6))
                .build());
        // вихрь (2 с, задевает всех вокруг и тянется к цели), затем выпад
        abilities.add(Ability.builder("relentless_whirlwind").animation("relentless_whirlwind_spin_long").duration(61).range(0, 8.0)
                .cooldown(160).weight(2).moving().unstoppable()
                .every(0, 38, 2, (mob, target, tick) -> {
                    if (target != null && mob.distanceTo(target) > 2.0D)
                        mob.lunge(target.position().subtract(mob.position()), 0.35D, 0.0D);
                })
                .every(4, 38, 5, (mob, target, tick) -> {
                    mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 2.6D, mob.damage(0.35), 0.3, 0.05);
                    mob.particleRing(ParticleTypes.SWEEP_ATTACK, mob.position().add(0.0D, 1.0D, 0.0D), 1.8D, 6);
                    if (tick % 10 == 4)
                        mob.sound(MobExSounds.AWAKENED_WARRIOR_SPIN.get(), 1.2F, 1.0F);
                })
                .at(40, (mob, target) -> mob.playAnimation("relentless_whirlwind_pierce"))
                .at(46, (mob, target) -> {
                    if (target != null)
                        mob.faceTowards(target);
                    mob.lunge(mob.forward(), 1.2D, 0.0D);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_PIERCE.get(), 1.6F, 0.9F);
                })
                .every(48, 56, 2, (mob, target, tick) -> mob.hitAreaOnce(mob.forwardPos(1.5D, 1.0D), 2.0D, mob.damage(1.3), 1.0, 0.3))
                .build());
        // сопротивление урону на 10 с
        abilities.add(Ability.builder("bulwark_instinct").seconds(1.5).range(0, 20.0).cooldown(400).weight(1).ignoreSight()
                .when((mob, target) -> mob.healthBelow(0.85D))
                .at(12, (mob, target) -> {
                    mob.effect(mob, MobEffects.DAMAGE_RESISTANCE, 200, 1);
                    mob.particles(ParticleTypes.ENCHANTED_HIT, mob.position().add(0.0D, 1.0D, 0.0D), 30, 0.6, 0.1);
                    mob.sound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.6F, 0.8F);
                })
                .build());

        // ---------------- вторая фаза (ниже 60%)
        abilities.add(Ability.builder("bloodbound_barrier").animation("bloodbound_barrier_long").seconds(2.4).range(0, 12.0)
                .cooldown(360).weight(2).ignoreSight()
                .when((mob, target) -> ((AwakenedWarriorEntity) mob).secondPhase())
                .at(0, (mob, target) -> ((AwakenedWarriorEntity) mob).barrierTicks = 60)
                .every(0, 46, 8, (mob, target, tick) -> mob.particleRing(ParticleTypes.DAMAGE_INDICATOR, mob.position().add(0.0D, 1.0D, 0.0D), 1.4D, 12))
                .at(47, (mob, target) -> {
                    mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 4.0D, mob.damage(1.0), 1.0, 0.4);
                    mob.heal(mob.getMaxHealth() * 0.03F);
                    mob.particleRing(ParticleTypes.CRIMSON_SPORE, mob.position().add(0.0D, 0.6D, 0.0D), 4.0D, 40);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_IMPACT.get(), 1.6F, 1.0F);
                })
                .build());
        abilities.add(Ability.builder("vicious_strike").seconds(4.75).range(0, 6.0).cooldown(240).weight(2).unstoppable()
                .when((mob, target) -> ((AwakenedWarriorEntity) mob).secondPhase())
                .at(10, (mob, target) -> mob.sound(MobExSounds.AWAKENED_WARRIOR_CHARGE.get(), 1.6F, 1.0F))
                .every(10, 60, 6, (mob, target, tick) -> mob.particleRing(ParticleTypes.ENCHANTED_HIT, mob.forwardPos(2.5D, 0.2D), 2.5D, 16))
                .at(66, (mob, target) -> {
                    mob.hitCone(6.0, 140.0F, mob.damage(2.2), 1.6, 0.5);
                    mob.particles(ParticleTypes.SWEEP_ATTACK, mob.forwardPos(2.5D, 1.0D), 6, 1.0, 0.0);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_ULT3.get(), 1.8F, 1.0F);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_IMPACT.get(), 1.6F, 0.9F);
                })
                .build());
        abilities.add(Ability.builder("strike_of_fury").animation("strike_of_fury_1").duration(113).range(0, 5.0).cooldown(260).weight(2)
                .when((mob, target) -> ((AwakenedWarriorEntity) mob).secondPhase())
                .at(12, (mob, target) -> closeIn(mob, target, 0.6D))
                .at(14, (mob, target) -> swing(mob, 0.9, 3.5, 120.0F, 0.3, 0.05, MobExSounds.AWAKENED_WARRIOR_ULT1.get()))
                .at(24, (mob, target) -> closeIn(mob, target, 0.6D))
                .at(26, (mob, target) -> swing(mob, 0.9, 3.5, 120.0F, 0.3, 0.05, MobExSounds.AWAKENED_WARRIOR_ULT2.get()))
                .at(38, (mob, target) -> closeIn(mob, target, 0.7D))
                .at(40, (mob, target) -> swing(mob, 1.2, 4.0, 140.0F, 0.8, 0.2, MobExSounds.AWAKENED_WARRIOR_ULT3.get()))
                .at(58, (mob, target) -> mob.playAnimation("strike_of_fury_2"))
                .at(70, (mob, target) -> {
                    if (target != null)
                        mob.leapTo(target.position(), 14);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_STOMP_CHARGE.get(), 1.4F, 1.0F);
                })
                .at(86, (mob, target) -> {
                    slam(mob, 1.8, 4.0, 1.2, 0.6);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_STOMP_ULT.get(), 1.8F, 1.0F);
                })
                .build());

        flinch = Ability.builder("flinch").seconds(2.5).cooldown(0).when((mob, target) -> false).build();
        abilities.add(flinch);
    }

    private static Ability dash(String name, double seconds, double sign) {
        return Ability.builder(name).seconds(seconds).range(0, 6.0).cooldown(60).weight(1).moving().fixedFacing()
                .at(2, (mob, target) -> {
                    mob.lunge(side(mob, sign), 1.1D, 0.1D);
                    mob.sound(MobExSounds.AWAKENED_WARRIOR_AIRDASH.get(), 1.2F, 1.0F);
                })
                .build();
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        return START;
    }

    @Override
    protected List<String> extraAnimations() {
        return List.of("c2", "c3", "c5", "relentless_whirlwind_pierce", "strike_of_fury_2");
    }

    @Override
    protected int cooldownSpeed() {
        return secondPhase() ? 2 : 1;
    }

    // ---------------------------------------------------------------- стойка, урон

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide())
            return;
        final int stance = getTarget() != null ? STANCE_COMBAT : STANCE_REST;
        if (getStance() != stance)
            setStance(stance);
        if (flinchCooldown > 0)
            flinchCooldown--;
        if (barrierTicks > 0)
            barrierTicks--;
    }

    @Override
    protected String idleAnimation() {
        return getStance() == STANCE_REST ? "idle_prebattle" : "idle";
    }

    @Override
    protected String walkAnimation() {
        return getStance() == STANCE_REST ? "walk" : "run";
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (barrierTicks > 0)
            amount *= 0.3F;
        if (isUsingAbility("flinch"))
            amount *= 1.3F;
        final boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && flinchCooldown <= 0 && !isDeadOrDying() && barrierTicks <= 0
                && amount >= getMaxHealth() * 0.02F && getRandom().nextFloat() < 0.05F
                && !isUsingAbility("vicious_strike") && !isUsingAbility("strike_of_fury")) {
            flinchCooldown = 300;
            forceAbility(flinch, getTarget());
        }
        return hurt;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(3.85) + 10;
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
        return "awakened_warrior";
    }

    @Override
    public String textureName() {
        return "awakened_warrior";
    }

    @Override
    public String animationName() {
        return "awakened_warrior";
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.GOAT_RAM_IMPACT;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.5F, 0.9F);
    }
}
