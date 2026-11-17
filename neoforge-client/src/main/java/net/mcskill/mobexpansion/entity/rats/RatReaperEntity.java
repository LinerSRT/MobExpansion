package net.mcskill.mobexpansion.entity.rats;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Крыса-жнец с двумя клинками: удары правым и левым клинком с подшагом и рывок с ударом обоими.
 * Каждый удар — двойной урон и пробой щита.
 */
public class RatReaperEntity extends RatClanEntity {
    private final Ability spawn = Ability.builder("spawn").seconds(1.0).cooldown(0).when((mob, target) -> false).unstoppable().build();

    public RatReaperEntity(EntityType<? extends RatReaperEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("right_attack").seconds(1.75).range(0, 4.0).cooldown(100).weight(3)
                .at(10, (mob, target) -> woosh(120))
                .every(10, 14, 1, (mob, target, tick) -> step(0.35D))
                .at(12, (mob, target) -> slash(mob.localPos(0.6D, 1.1D, 1.6D), 2.0D, 2.0D, 50, 0.3D, 0.1D))
                .build());

        abilities.add(Ability.builder("left_attack").seconds(1.75).range(0, 4.0).cooldown(100).weight(3)
                .at(10, (mob, target) -> woosh(120))
                .every(10, 14, 1, (mob, target, tick) -> step(0.35D))
                .at(12, (mob, target) -> slash(mob.localPos(-0.6D, 1.1D, 1.6D), 2.0D, 2.0D, 50, 0.3D, 0.1D))
                .build());

        // рывок: подскок к цели и удар обоими клинками подряд
        abilities.add(Ability.builder("charge_attack").seconds(1.85).range(2.0, 6.0).cooldown(160).weight(2)
                .at(11, (mob, target) -> woosh(120))
                .at(13, (mob, target) -> woosh(100))
                .every(13, 18, 1, (mob, target, tick) -> step(0.55D))
                .at(15, (mob, target) -> slash(mob.localPos(-0.6D, 1.1D, 1.6D), 2.0D, 2.0D, 50, 0.4D, 0.1D))
                .at(17, (mob, target) -> slash(mob.localPos(0.6D, 1.1D, 1.6D), 2.0D, 2.0D, 50, 0.4D, 0.1D, true))
                .build());
    }

    @Override
    @Nullable
    protected Ability spawnAbility() {
        return spawn;
    }

    @Override
    public double chaseSpeed() {
        return 1.1D;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(3.0);
    }
}
