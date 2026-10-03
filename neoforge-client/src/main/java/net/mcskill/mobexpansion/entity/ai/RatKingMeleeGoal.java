package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.RatKingEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class RatKingMeleeGoal extends Goal {
    private final RatKingEntity ratKing;
    private int pathCooldownTicks;

    public RatKingMeleeGoal(RatKingEntity ratKing) {
        this.ratKing = ratKing;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final LivingEntity target = ratKing.getTarget();
        return target != null && target.isAlive() && ratKing.prefersMeleeCombat(target);
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
    public void tick() {
        final LivingEntity target = ratKing.getTarget();
        if (target == null)
            return;

        ratKing.getLookControl().setLookAt(target, 40.0F, 40.0F);
        if (ratKing.isBusy()) {
            ratKing.getNavigation().stop();
            return;
        }

        final double meleeReach = MobExParameters.get(ratKing, "meleeReach", 3.2D);
        final double distanceSquared = ratKing.distanceToSqr(target);
        if (distanceSquared <= meleeReach * meleeReach) {
            ratKing.getNavigation().stop();
            ratKing.tryStartMeleeAttack(target);
            return;
        }

        if (--pathCooldownTicks <= 0) {
            pathCooldownTicks = 6;
            ratKing.getNavigation().moveTo(target, 1.1D);
        }
    }
}
