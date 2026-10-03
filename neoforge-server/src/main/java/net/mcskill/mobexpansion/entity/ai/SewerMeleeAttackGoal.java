package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.SewerMobEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SewerMeleeAttackGoal extends Goal {
    private final SewerMobEntity sewerMob;
    private final double speedModifier;
    private final double defaultAttackReach;
    private final AttackStarter attackStarter;
    private int pathCooldownTicks;

    public SewerMeleeAttackGoal(SewerMobEntity sewerMob, double speedModifier, double attackReach, AttackStarter attackStarter) {
        this.sewerMob = sewerMob;
        this.speedModifier = speedModifier;
        this.defaultAttackReach = attackReach;
        this.attackStarter = attackStarter;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final LivingEntity target = sewerMob.getTarget();
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
    public void tick() {
        final LivingEntity target = sewerMob.getTarget();
        if (target == null)
            return;

        sewerMob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (sewerMob.isBusy()) {
            sewerMob.getNavigation().stop();
            return;
        }

        final double attackReach = MobExParameters.get(sewerMob, "attackReach", defaultAttackReach);
        final double distanceSquared = sewerMob.distanceToSqr(target);
        final double reachSquared = attackReach * attackReach;
        if (distanceSquared <= reachSquared) {
            attackStarter.startAttack(target);
            return;
        }

        if (--pathCooldownTicks <= 0) {
            pathCooldownTicks = 8;
            sewerMob.getNavigation().moveTo(target, speedModifier);
        }
    }

    @FunctionalInterface
    public interface AttackStarter {
        void startAttack(LivingEntity target);
    }
}
