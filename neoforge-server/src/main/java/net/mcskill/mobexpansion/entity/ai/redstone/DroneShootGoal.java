package net.mcskill.mobexpansion.entity.ai.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.redstone.RedstoneDroneEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class DroneShootGoal extends Goal {
    private static final double MAX_SHOOT_DISTANCE = 18.0D;
    private static final int SHOOT_COOLDOWN_TICKS = 14;

    private final RedstoneDroneEntity drone;
    private int cooldownTicks;

    public DroneShootGoal(RedstoneDroneEntity drone) {
        this.drone = drone;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (drone.isBusy())
            return false;
        final LivingEntity target = drone.getTarget();
        if (target == null || !target.isAlive())
            return false;
        final double maxShootDistance = MobExParameters.get(drone, "maxShootDistance", MAX_SHOOT_DISTANCE);
        return drone.distanceToSqr(target) <= maxShootDistance * maxShootDistance;
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
        final LivingEntity target = drone.getTarget();
        if (target == null)
            return;

        drone.getLookControl().setLookAt(target, 40.0F, 40.0F);
        if (drone.isBusy())
            return;

        if (--cooldownTicks > 0)
            return;
        if (!drone.getSensing().hasLineOfSight(target))
            return;

        if (drone.tryStartShoot(target))
            cooldownTicks = MobExParameters.getInt(drone, "shootCooldownTicks", SHOOT_COOLDOWN_TICKS);
    }
}
