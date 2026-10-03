package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.LeechEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.EnumSet;

public class LeechAttackGoal extends Goal {
    private final LeechEntity leech;
    private int pathCooldownTicks;

    public LeechAttackGoal(LeechEntity leech) {
        this.leech = leech;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final LivingEntity target = leech.getTarget();
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
        final LivingEntity target = leech.getTarget();
        if (target == null)
            return;

        leech.getLookControl().setLookAt(target, 30.0F, 30.0F);
        final double distanceSquared = leech.distanceToSqr(target);
        final double holdDistance = MobExParameters.get(leech, "holdDistance", 4.0D);
        final double attackReach = MobExParameters.get(leech, "attackReach", 4.5D);

        if (leech.isBusy()) {
            leech.getNavigation().stop();
            leech.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 124, false, false));
            return;
        }

        if (distanceSquared <= holdDistance * holdDistance) {
            leech.getNavigation().stop();
            leech.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 124, false, false));
            if (distanceSquared <= attackReach * attackReach)
                leech.tryStartAttack(target);
            return;
        }

        if (--pathCooldownTicks <= 0) {
            pathCooldownTicks = 8;
            leech.getNavigation().moveTo(target, 1.0D);
        }
    }
}
