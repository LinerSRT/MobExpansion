package net.mcskill.mobexpansion.entity.rats;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.init.MobExSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

import java.util.List;
import java.util.Set;

/**
 * Крысиный владыка (в оригинале — Rat King; имя занято нашим Королём крыс). Босс с длинным мечом.
 * Спит на троне (храп «zzz»), пока игрок не подойдёт на 20 блоков; проснувшись, бьёт сериями:
 * три комбо, удар когтями, выпад и вихрь. Снаряды наносят ему четверть урона, все удары пробивают щит.
 */
public class RatOverlordEntity extends RatClanEntity {
    private static final int DORMANT = 0;
    private static final int AWAKE = 1;
    private static final double WAKE_RADIUS = 20.0D;
    private static final List<String> TOGGLED = List.of("vfx_slash", "vfx_claw", "h_zzz", "h_eye");
    private static final Set<String> ATTACKS = Set.of("combo", "combo2", "combo3", "claw", "pierce", "spin");

    private Ability awaken;

    public RatOverlordEntity(EntityType<? extends RatOverlordEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public boolean isDormant() {
        return getStance() == DORMANT;
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        awaken = Ability.builder("active").seconds(1.0).cooldown(0).when((mob, target) -> false).unstoppable()
                .at(5, (mob, target) -> mob.sound(MobExSounds.RATS_ACTIVE, 2.5F, 1.2F))
                .build();
        abilities.add(awaken);

        // двойной взмах мечом с подшагом
        abilities.add(Ability.builder("combo").seconds(2.7).range(0, 6.0).cooldown(320).weight(3)
                .at(16, (mob, target) -> cloth(80))
                .at(18, (mob, target) -> woosh(100))
                .at(28, (mob, target) -> woosh(80))
                .every(20, 24, 1, (mob, target, tick) -> step(0.4D))
                .at(22, (mob, target) -> slash(mob.localPos(0.0D, 1.5D, 4.0D), 3.6D, 4.0D, 60, 1.0D, 0.3D))
                .at(31, (mob, target) -> slash(mob.localPos(0.0D, 1.5D, 4.0D), 3.6D, 4.0D, 60, 1.0D, 0.3D, true))
                .build());

        // хвостом по ногам (замедление), затем мощный взмах, отбрасывающий далеко
        abilities.add(Ability.builder("combo2").seconds(2.5).range(0, 6.0).cooldown(320).weight(3)
                .at(12, (mob, target) -> woosh(100))
                .at(16, (mob, target) -> cloth(70))
                .at(25, (mob, target) -> woosh(80))
                .every(11, 15, 1, (mob, target, tick) -> step(0.4D))
                .at(17, (mob, target) -> {
                    for (LivingEntity hit : slash(mob.localPos(0.0D, 0.8D, 0.5D), 3.0D, 2.0D, 60, 0.2D, 0.0D))
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 10, 2);
                })
                .at(28, (mob, target) -> slash(mob.localPos(0.0D, 1.5D, 4.0D), 3.5D, 4.0D, 100, 1.6D, 0.3D, true))
                .build());

        // два выпада подряд: ближний и дальний удар, затем сокрушительный взмах
        abilities.add(Ability.builder("combo3").seconds(3.0).range(0, 6.0).cooldown(320).weight(3)
                .at(11, (mob, target) -> woosh(80))
                .at(13, (mob, target) -> cloth(80))
                .at(17, (mob, target) -> swordSound(100))
                .at(30, (mob, target) -> cloth(90))
                .at(31, (mob, target) -> woosh(120))
                .every(12, 17, 1, (mob, target, tick) -> step(0.4D))
                .every(30, 35, 1, (mob, target, tick) -> step(0.6D))
                .at(18, (mob, target) -> {
                    final List<LivingEntity> near = slash(mob.localPos(0.0D, 1.5D, 2.0D), 2.8D, 4.0D, 80, 0.2D, 0.0D);
                    final List<LivingEntity> far = slash(mob.localPos(0.0D, 1.5D, 8.0D), 2.8D, 4.0D, 80, 0.2D, 0.0D);
                    for (LivingEntity hit : near)
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 10, 1);
                    for (LivingEntity hit : far)
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 10, 1);
                })
                .at(33, (mob, target) -> slash(mob.localPos(0.0D, 1.5D, 3.0D), 3.5D, 5.0D, 80, 1.6D, 0.3D, true))
                .build());

        // когти: удар снизу, подбрасывает
        abilities.add(Ability.builder("claw").seconds(1.25).range(0, 4.0).cooldown(320).weight(3)
                .at(2, (mob, target) -> {
                    woosh(130);
                    cloth(120);
                })
                .at(4, (mob, target) -> slash(mob.localPos(0.0D, 1.5D, 2.0D), 3.0D, 4.0D, 80, 0.6D, 1.0D))
                .build());

        // выпад: рывок на 4 блока и укол
        abilities.add(Ability.builder("pierce").seconds(3.0).range(4.0, 10.0).cooldown(320).weight(3)
                .at(17, (mob, target) -> cloth(70))
                .at(20, (mob, target) -> swordSound(120))
                .at(37, (mob, target) -> swordSound(170))
                .every(16, 20, 1, (mob, target, tick) -> step(0.9D))
                .at(21, (mob, target) -> {
                    for (LivingEntity hit : slash(mob.localPos(0.0D, 1.5D, 5.0D), 3.0D, 5.0D, 100, 0.4D, 0.1D))
                        mob.effect(hit, MobEffects.MOVEMENT_SLOWDOWN, 20, 2);
                })
                .build());

        // вихрь: три секунды крутится, идя на цель, и разбрасывает всех вокруг; в конце — добивающий удар
        abilities.add(Ability.builder("spin").seconds(4.5).range(0, 8.0).cooldown(320).weight(2).moving().unstoppable()
                .every(5, 61, 1, (mob, target, tick) -> {
                    if (target != null && tick % 10 == 5)
                        mob.getNavigation().moveTo(target, 1.4D);
                })
                .every(5, 60, 10, (mob, target, tick) -> {
                    woosh(100);
                    cloth(80);
                    slash(mob.position().add(0.0D, 1.5D, 0.0D), 3.2D, 2.0D, 80, 1.4D, 0.2D, true);
                })
                .at(53, (mob, target) -> woosh(80))
                .at(58, (mob, target) -> swordSound(100))
                .at(62, (mob, target) -> {
                    mob.getNavigation().stop();
                    slash(mob.localPos(0.0D, 1.5D, 2.0D), 2.8D, 2.0D, 80, 2.0D, 0.3D, true);
                    slash(mob.localPos(0.0D, 1.5D, 7.0D), 2.8D, 2.0D, 80, 2.0D, 0.3D, true);
                })
                .build());
    }

    /** Проснуться (игрок рядом или удар). */
    public void wakeUp(@Nullable LivingEntity target) {
        if (!isDormant() || level().isClientSide() || isDeadOrDying())
            return;
        abilities(); // способности регистрируются лениво
        setStance(AWAKE);
        if (target != null)
            setTarget(target);
        forceAbility(awaken, null);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying() || !isDormant())
            return;
        getNavigation().stop();
        if (tickCount > 10 && tickCount % 10 == 0) {
            final Player nearest = level().getNearestPlayer(this, WAKE_RADIUS);
            if (nearest != null && isEnemy(nearest))
                wakeUp(nearest);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // спящий не берёт цель сам — только через wakeUp
        if (isDormant() && target != null && !level().isClientSide())
            return;
        super.setTarget(target);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isProjectile(source))
            amount *= 0.25F;
        else if (source.is(DamageTypes.FALL))
            amount *= 0.1F;
        final boolean hurt = super.hurt(source, amount);
        if (hurt && isDormant() && source.getEntity() instanceof LivingEntity attacker && isEnemy(attacker))
            wakeUp(attacker);
        return hurt;
    }

    @Override
    protected PlayState handleAnimation(AnimationState<AbilityMobEntity> state) {
        if (isDormant() && !isDeadOrDying())
            return state.setAndContinue(loopAnim("not_active"));
        return super.handleAnimation(state);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (!dead && !level().isClientSide()) {
            sound(MobExSounds.RATS_CLOTH, 2.5F, 1.0F);
            sound(SoundEvents.BASALT_STEP, 2.5F, 0.5F);
        }
        super.die(source);
    }

    @Override
    @Nullable
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected void playStepSound(@NotNull net.minecraft.core.BlockPos pos, @NotNull net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.BASALT_STEP, 1.0F, 0.7F + random.nextFloat() * 0.5F);
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(4.0);
    }

    @Override
    public List<String> toggledBones() {
        return TOGGLED;
    }

    @Override
    public boolean isBoneVisible(String bone) {
        return switch (bone) {
            case "h_zzz" -> isDormant() && !isDeadOrDying();
            // закрытый глаз: во сне и после смерти
            case "h_eye" -> isDormant() || isDeadOrDying();
            default -> {
                // вспышки удара видны только в атаках (их масштаб ведёт сама анимация)
                final String animation = playingAnimation();
                yield !isDeadOrDying() && animation != null && ATTACKS.contains(animation);
            }
        };
    }
}
