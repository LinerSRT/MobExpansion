package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.MosquitoEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class MosquitoAttackGoal extends Goal {
    private final MosquitoEntity mosquito;
    private int cooldownTicks;

    public MosquitoAttackGoal(MosquitoEntity mosquito) {
        this.mosquito = mosquito;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final LivingEntity target = mosquito.getTarget();
        return target != null && target.isAlive() && !mosquito.isBusy();
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
        final LivingEntity target = mosquito.getTarget();
        if (target == null)
            return;

        mosquito.getLookControl().setLookAt(target, 40.0F, 40.0F);
        if (mosquito.isBusy())
            return;
        if (--cooldownTicks > 0)
            return;
        final double attackReach = MobExParameters.get(mosquito, "attackReach", 2.4D);
        if (mosquito.distanceToSqr(target) > attackReach * attackReach)
            return;
        if (mosquito.tryStartAttack(target))
            cooldownTicks = 10;
    }
}
