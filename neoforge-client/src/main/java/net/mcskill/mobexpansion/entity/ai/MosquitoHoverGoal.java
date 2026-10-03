package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.MosquitoEntity;
import net.mcskill.mobexpansion.init.MobExParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class MosquitoHoverGoal extends Goal {
    private static final int GROUND_SEARCH_DEPTH = 24;

    private final MosquitoEntity mosquito;
    private int repathCooldown;
    private double orbitAngle;

    public MosquitoHoverGoal(MosquitoEntity mosquito) {
        this.mosquito = mosquito;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (mosquito.isBusy()) {
            mosquito.getNavigation().stop();
            mosquito.setDeltaMovement(mosquito.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
            return;
        }
        if (--repathCooldown > 0)
            return;
        repathCooldown = 4;
        final LivingEntity target = mosquito.getTarget();
        final double combatRadius = MobExParameters.get(mosquito, "hoverCombatRadius", 2.2D);
        final Vec3 hoverTarget = target != null && target.isAlive() ? orbitAround(target.getX(), target.getY(), target.getZ(), combatRadius) : wanderAround();
        mosquito.getMoveControl().setWantedPosition(hoverTarget.x, hoverTarget.y, hoverTarget.z, 1.0D);
    }

    private Vec3 wanderAround() {
        final double baseY = findGroundY();
        final double wanderRadius = MobExParameters.get(mosquito, "hoverWanderRadius", 3.0D);
        return orbitAround(mosquito.getX(), baseY, mosquito.getZ(), wanderRadius * 0.35D);
    }

    private Vec3 orbitAround(double centerX, double baseY, double centerZ, double radius) {
        orbitAngle += 0.18D;
        final double minHoverHeight = MobExParameters.get(mosquito, "minHoverHeight", 1.2D);
        final double maxHoverHeight = MobExParameters.get(mosquito, "maxHoverHeight", 2.4D);
        final double hoverHeight = Mth.lerp(0.5D, minHoverHeight, maxHoverHeight);
        return new Vec3(centerX + Math.cos(orbitAngle) * radius, baseY + hoverHeight, centerZ + Math.sin(orbitAngle) * radius);
    }

    private double findGroundY() {
        final Level level = mosquito.level();
        final BlockPos.MutableBlockPos checkPos = mosquito.blockPosition().mutable();
        for (int depth = 0; depth < GROUND_SEARCH_DEPTH; depth++) {
            if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty())
                return checkPos.getY() + 1.0D;
            checkPos.move(Direction.DOWN);
        }
        return mosquito.getY() - MobExParameters.get(mosquito, "minHoverHeight", 1.2D);
    }
}
