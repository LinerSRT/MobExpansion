package net.mcskill.mobexpansion.entity.ai.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.redstone.RedstoneConstructEntity;
import net.mcskill.mobexpansion.entity.redstone.RedstoneEngineerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ConstructLeashGoal extends Goal {
    private static final double LEASH_DISTANCE = 18.0D;
    private static final double RETURN_DISTANCE_SQR = 8.0D * 8.0D;

    private final RedstoneConstructEntity construct;
    private final double speedModifier;
    private int repathCooldown;

    public ConstructLeashGoal(RedstoneConstructEntity construct, double speedModifier) {
        this.construct = construct;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (construct.isBusy() || construct.isSpawning())
            return false;
        final RedstoneEngineerEntity owner = construct.getOwnerEngineer();
        if (owner == null || !owner.isAlive())
            return false;
        final double leashDistance = MobExParameters.get(owner, "constructLeashDistance", LEASH_DISTANCE);
        return construct.distanceToSqr(owner) > leashDistance * leashDistance;
    }

    @Override
    public boolean canContinueToUse() {
        if (construct.isBusy())
            return false;
        final RedstoneEngineerEntity owner = construct.getOwnerEngineer();
        if (owner == null || !owner.isAlive())
            return false;
        return construct.distanceToSqr(owner) > RETURN_DISTANCE_SQR;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        construct.setTarget(null);
    }

    @Override
    public void stop() {
        construct.getNavigation().stop();
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        final RedstoneEngineerEntity owner = construct.getOwnerEngineer();
        if (owner == null)
            return;
        if (--repathCooldown > 0)
            return;
        repathCooldown = 10;
        construct.getNavigation().moveTo(owner, speedModifier);
    }
}
