package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RatKingEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class RatKingRangedGoal extends Goal {
    private static final int FAR_RANGED_INTERVAL_TICKS = 25;

    private final RatKingEntity ratKing;
    private int cooldownTicks;

    public RatKingRangedGoal(RatKingEntity ratKing) {
        this.ratKing = ratKing;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final LivingEntity target = ratKing.getTarget();
        return target != null && target.isAlive() && ratKing.prefersRangedCombat(target) && ratKing.canStartRangedAttack(target);
    }

    @Override
    public boolean canContinueToUse() {
        final LivingEntity target = ratKing.getTarget();
        return target != null && target.isAlive() && ratKing.prefersRangedCombat(target);
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
        ratKing.getNavigation().stop();

        if (ratKing.isBusy())
            return;

        if (--cooldownTicks > 0)
            return;

        if (!ratKing.tryStartRangedAttack(target))
            return;

        cooldownTicks = FAR_RANGED_INTERVAL_TICKS;
    }
}
