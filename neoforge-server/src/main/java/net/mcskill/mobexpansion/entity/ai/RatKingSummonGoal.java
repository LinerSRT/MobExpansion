package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RatKingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class RatKingSummonGoal extends Goal {
    private final RatKingEntity ratKing;

    public RatKingSummonGoal(RatKingEntity ratKing) {
        this.ratKing = ratKing;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return ratKing.shouldSummonMinions() && !ratKing.isBusy();
    }

    @Override
    public boolean canContinueToUse() {
        return ratKing.isAttacking() && "summon".equals(ratKing.getActiveAttackNamePublic());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        ratKing.tryStartSummon();
    }

    @Override
    public void tick() {
        ratKing.getNavigation().stop();
    }
}
