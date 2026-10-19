package net.mcskill.mobexpansion.entity.robot;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Божья коровка-миномёт: чтобы стрелять, должна развернуться (set) — тогда стоит на месте, получает меньше урона
 * и бьёт навесом по площади, даже без прямой видимости. Если цель подошла слишком близко или ушла далеко —
 * сворачивается (move) и меняет позицию.
 */
public class LadybugMortarEntity extends RobotBugEntity {
    private static final int MOBILE = 0;
    private static final int DEPLOYED = 1;

    private int deployedTicks;

    public LadybugMortarEntity(EntityType<? extends LadybugMortarEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 16;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    private boolean deployed() {
        return getStance() == DEPLOYED;
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("set").seconds(3.32).range(7.0, 34.0).cooldown(40).weight(5).ignoreSight()
                .when((mob, target) -> !deployed())
                .at(10, (mob, target) -> mob.sound(SoundEvents.PISTON_EXTEND, 1.0F, 0.7F))
                .onEnd((mob, target) -> {
                    setStance(DEPLOYED);
                    deployedTicks = 0;
                })
                .build());

        abilities.add(Ability.builder("fire").seconds(2.08).range(5.0, 40.0).cooldown(36).weight(5).ignoreSight()
                .when((mob, target) -> deployed())
                .at(26, (mob, target) -> {
                    if (target == null)
                        return;
                    final Vec3 barrel = mob.position().add(0.0D, 1.6D, 0.0D);
                    // упреждение: куда цель придёт к моменту падения снаряда
                    final int flight = 22 + (int) (mob.distanceTo(target) * 1.1D);
                    final Vec3 landing = target.position().add(target.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).scale(flight * 0.6D));
                    mob.lobAt(mob.projectile("bullet", barrel).scale(2.4F).gravity(0.05D).damage(mob.damage(1.0))
                            .explosion(3.0D, mob.damage(1.2)).life(flight + 40)
                            .trail(ParticleTypes.SMOKE, 2).impact(ParticleTypes.LARGE_SMOKE, null), landing, flight);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.6F);
                    mob.particles(ParticleTypes.CLOUD, barrel, 8, 0.2, 0.05);
                })
                .build());

        abilities.add(Ability.builder("move").seconds(3.0).range(0.0, 64.0).cooldown(30).weight(10).ignoreSight()
                .when((mob, target) -> deployed() && (mob.distanceTo(target) < 6.0D || mob.distanceTo(target) > 40.0D || deployedTicks > 400))
                .at(10, (mob, target) -> mob.sound(SoundEvents.PISTON_CONTRACT, 1.0F, 0.7F))
                .onEnd((mob, target) -> setStance(MOBILE))
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && deployed()) {
            deployedTicks++;
            getNavigation().stop();
            // без цели долго стоять развёрнутой незачем
            if (getTarget() == null && deployedTicks > 200 && !isUsingAbility())
                forceAbility(abilities().get(2), null);
        }
    }

    @Override
    protected CombatStyle combatStyle() {
        return deployed() ? CombatStyle.STATIONARY : CombatStyle.KITE;
    }

    @Override
    public double preferredMinRange() {
        return 10.0D;
    }

    @Override
    public double preferredMaxRange() {
        return 26.0D;
    }

    @Override
    protected String idleAnimation() {
        return deployed() ? "set_idle" : "idle";
    }

    @Override
    protected String walkAnimation() {
        return deployed() ? "set_idle" : "walk";
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // развёрнутая — закрыта панцирем
        return super.hurt(source, deployed() ? amount * 0.65F : amount);
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(2.08);
    }

    @Override
    public String modelName() {
        return "ladybug_mortar";
    }

    @Override
    public String textureName() {
        return "ladybug_mortar";
    }

    @Override
    public String animationName() {
        return "ladybug_mortar";
    }
}
