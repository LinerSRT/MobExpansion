package net.mcskill.mobexpansion.entity.ai.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.redstone.RedstoneTowerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class TowerCombatGoal extends Goal {
    private static final double MELEE_RANGE = 2.5D;
    private static final double RANGED_RANGE = 14.0D;
    private static final int ATTACK_COOLDOWN_TICKS = 40;

    private final RedstoneTowerEntity tower;
    private int cooldownTicks;
    private int repathCooldown;

    public TowerCombatGoal(RedstoneTowerEntity tower) {
        this.tower = tower;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (tower.isBusy())
            return false;
        final LivingEntity target = tower.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        tower.getNavigation().stop();
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        final LivingEntity target = tower.getTarget();
        if (target == null)
            return;

        tower.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (tower.isBusy()) {
            tower.getNavigation().stop();
            return;
        }

        final double meleeRange = MobExParameters.get(tower, "meleeRange", MELEE_RANGE);
        final double rangedRange = MobExParameters.get(tower, "rangedRange", RANGED_RANGE);
        final double meleeRangeSqr = meleeRange * meleeRange;
        final double rangedRangeSqr = rangedRange * rangedRange;
        final double distanceSqr = tower.distanceToSqr(target);
        if (distanceSqr > meleeRangeSqr) {
            if (--repathCooldown <= 0) {
                repathCooldown = 12;
                tower.getNavigation().moveTo(target, 0.85D);
            }
        } else {
            tower.getNavigation().stop();
        }

        if (--cooldownTicks > 0)
            return;
        if (!tower.getSensing().hasLineOfSight(target))
            return;
        if (distanceSqr > rangedRangeSqr)
            return;

        if (tower.tryStartAttack(target, distanceSqr <= meleeRangeSqr))
            cooldownTicks = MobExParameters.getInt(tower, "attackCooldownTicks", ATTACK_COOLDOWN_TICKS);
    }
}
