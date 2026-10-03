package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.SentinelGolemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SentinelGolemCombatGoal extends Goal {
    private final SentinelGolemEntity golem;
    private int cooldownTicks;
    private int repathCooldown;

    public SentinelGolemCombatGoal(SentinelGolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.isBusy())
            return false;
        final LivingEntity target = golem.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || golem.isBusy();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        final LivingEntity target = golem.getTarget();
        if (target == null)
            return;

        golem.getLookControl().setLookAt(target, 40.0F, 40.0F);
        if (golem.isBusy()) {
            golem.getNavigation().stop();
            return;
        }

        final double meleeRange = MobExParameters.get(golem, "meleeRange", 4.0D);
        final double meleeRangeSqr = meleeRange * meleeRange;
        final double lungeRange = MobExParameters.get(golem, "lungeRange", 10.0D);
        final double lungeRangeSqr = lungeRange * lungeRange;
        final double jumpMinRange = MobExParameters.get(golem, "jumpMinRange", 8.0D);
        final double jumpMinRangeSqr = jumpMinRange * jumpMinRange;
        final double jumpMaxRange = MobExParameters.get(golem, "jumpMaxRange", 29.0D);
        final double jumpMaxRangeSqr = jumpMaxRange * jumpMaxRange;
        final int attackCooldownTicks = MobExParameters.getInt(golem, "attackCooldownTicks", 20);

        final double distanceSqr = golem.distanceToSqr(target);
        if (distanceSqr > meleeRangeSqr) {
            if (--repathCooldown <= 0) {
                repathCooldown = 8;
                golem.getNavigation().moveTo(target, 1.05D);
            }
        } else {
            golem.getNavigation().stop();
        }

        if (--cooldownTicks > 0)
            return;
        if (!golem.getSensing().hasLineOfSight(target))
            return;

        if (distanceSqr >= jumpMinRangeSqr && distanceSqr <= jumpMaxRangeSqr && golem.tryJumpSlam(target)) {
            cooldownTicks = attackCooldownTicks + 40;
            return;
        }
        if (distanceSqr <= lungeRangeSqr && distanceSqr > meleeRangeSqr && golem.tryEstocada(target)) {
            cooldownTicks = attackCooldownTicks + 10;
            return;
        }
        if (distanceSqr <= meleeRangeSqr && golem.tryMeleeAttack(target))
            cooldownTicks = attackCooldownTicks;
    }
}
