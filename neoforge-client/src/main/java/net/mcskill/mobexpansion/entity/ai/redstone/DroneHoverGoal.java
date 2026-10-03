package net.mcskill.mobexpansion.entity.ai.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.redstone.RedstoneDroneEntity;
import net.mcskill.mobexpansion.entity.redstone.RedstoneEngineerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DroneHoverGoal extends Goal {
    private static final double MIN_HOVER_HEIGHT = 2.0D;
    private static final double MAX_HOVER_HEIGHT = 3.0D;
    private static final double ESCORT_RADIUS = 2.0D;
    private static final double COMBAT_RADIUS = 3.5D;

    private final RedstoneDroneEntity drone;
    private int repathCooldown;
    private double orbitAngle;

    public DroneHoverGoal(RedstoneDroneEntity drone) {
        this.drone = drone;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !drone.isSpawning();
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
        if (drone.isBusy()) {
            drone.getNavigation().stop();
            return;
        }

        if (--repathCooldown > 0)
            return;
        repathCooldown = 5;

        final Vec3 hoverTarget = resolveHoverTarget();
        drone.getMoveControl().setWantedPosition(hoverTarget.x, hoverTarget.y, hoverTarget.z, 1.15D);
    }

    private Vec3 resolveHoverTarget() {
        final LivingEntity combatTarget = drone.getTarget();
        final RedstoneEngineerEntity owner = drone.getOwnerEngineer();
        final double combatRadius = MobExParameters.get(drone, "combatHoverRadius", COMBAT_RADIUS);
        final double escortRadius = MobExParameters.get(drone, "escortRadius", ESCORT_RADIUS);
        final double minHoverHeight = MobExParameters.get(drone, "minHoverHeight", MIN_HOVER_HEIGHT);
        final double maxHoverHeight = MobExParameters.get(drone, "maxHoverHeight", MAX_HOVER_HEIGHT);

        if (owner == null) {
            if (combatTarget == null)
                return new Vec3(drone.getX(), drone.getY(), drone.getZ());
            return orbitAround(combatTarget, combatRadius, minHoverHeight, maxHoverHeight);
        }

        if (combatTarget != null && combatTarget.isAlive() && owner.isAlive()) {
            final Vec3 between = owner.position().add(combatTarget.position()).scale(0.5D);
            final double hoverHeight = Mth.lerp(0.55D, minHoverHeight, maxHoverHeight);
            orbitAngle += 0.22D;
            return new Vec3(
                    between.x + Math.cos(orbitAngle) * combatRadius,
                    owner.getY() + hoverHeight,
                    between.z + Math.sin(orbitAngle) * combatRadius
            );
        }

        return orbitAround(owner, escortRadius, minHoverHeight, maxHoverHeight);
    }

    private Vec3 orbitAround(LivingEntity anchor, double radius, double minHoverHeight, double maxHoverHeight) {
        orbitAngle += 0.2D;
        final double hoverHeight = Mth.lerp(0.5D, minHoverHeight, maxHoverHeight);
        return new Vec3(
                anchor.getX() + Math.cos(orbitAngle) * radius,
                anchor.getY() + hoverHeight,
                anchor.getZ() + Math.sin(orbitAngle) * radius
        );
    }
}
