package net.mcskill.mobexpansion.entity.ai.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.redstone.RedstoneAutomatonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class AutomatonMeleeGoal extends Goal {
    private static final double MELEE_REACH = 3.0D;

    private final RedstoneAutomatonEntity automaton;
    private final double speedModifier;
    private int repathCooldown;

    public AutomatonMeleeGoal(RedstoneAutomatonEntity automaton, double speedModifier) {
        this.automaton = automaton;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (automaton.isBusy())
            return false;
        final LivingEntity target = automaton.getTarget();
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
        automaton.getNavigation().stop();
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        final LivingEntity target = automaton.getTarget();
        if (target == null)
            return;

        automaton.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (automaton.isBusy()) {
            automaton.getNavigation().stop();
            return;
        }

        final double distanceSqr = automaton.distanceToSqr(target);
        if (--repathCooldown <= 0) {
            repathCooldown = 8;
            automaton.getNavigation().moveTo(target, speedModifier);
        }

        final double meleeReach = MobExParameters.get(automaton, "meleeReach", MELEE_REACH);
        if (distanceSqr <= meleeReach * meleeReach && automaton.getSensing().hasLineOfSight(target))
            automaton.tryStartAttack(target);
    }
}
