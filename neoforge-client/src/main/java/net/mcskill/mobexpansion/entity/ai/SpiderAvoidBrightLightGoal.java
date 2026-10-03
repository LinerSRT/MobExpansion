package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SpiderAvoidBrightLightGoal extends Goal {
    private static final int MAX_LIGHT_LEVEL = 12;
    private static final int SEARCH_RADIUS = 6;
    private static final int REPATH_COOLDOWN_TICKS = 20;

    private final RegularSpiderEntity spider;
    @Nullable
    private BlockPos darkTarget;
    private int repathCooldown;

    public SpiderAvoidBrightLightGoal(RegularSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!spider.shouldAvoidBrightLight())
            return false;
        if (spider.getTarget() != null || spider.isCarryingLoot())
            return false;
        if (spider.level().getMaxLocalRawBrightness(spider.blockPosition()) <= MAX_LIGHT_LEVEL)
            return false;
        darkTarget = findDarkerPosition();
        return darkTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return darkTarget != null && spider.shouldAvoidBrightLight() && spider.getTarget() == null && spider.level().getMaxLocalRawBrightness(spider.blockPosition()) > MAX_LIGHT_LEVEL;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        moveToDarkTarget();
    }

    @Override
    public void stop() {
        darkTarget = null;
        spider.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (darkTarget == null)
            return;
        if (--repathCooldown > 0)
            return;
        repathCooldown = REPATH_COOLDOWN_TICKS;
        if (spider.level().getMaxLocalRawBrightness(darkTarget) > MAX_LIGHT_LEVEL)
            darkTarget = findDarkerPosition();
        moveToDarkTarget();
    }

    private void moveToDarkTarget() {
        if (darkTarget == null)
            return;
        spider.getNavigation().moveTo(darkTarget.getX() + 0.5D, darkTarget.getY(), darkTarget.getZ() + 0.5D, 1.05D);
    }

    @Nullable
    private BlockPos findDarkerPosition() {
        final Level level = spider.level();
        final BlockPos origin = spider.blockPosition();
        final int originLight = level.getMaxLocalRawBrightness(origin);
        BlockPos bestPos = null;
        int bestLight = originLight;
        double bestDistance = Double.MAX_VALUE;

        for (int attempt = 0; attempt < 16; attempt++) {
            final int offsetX = spider.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            final int offsetY = spider.getRandom().nextInt(3) - 1;
            final int offsetZ = spider.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            final BlockPos candidatePos = origin.offset(offsetX, offsetY, offsetZ);
            if (spider.hasRestriction() && !spider.isWithinRestriction(candidatePos))
                continue;
            final int candidateLight = level.getMaxLocalRawBrightness(candidatePos);
            if (candidateLight >= bestLight)
                continue;
            if (!level.getBlockState(candidatePos).getCollisionShape(level, candidatePos).isEmpty())
                continue;
            final BlockPos supportPos = candidatePos.below();
            if (!level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP))
                continue;
            final double distance = Vec3.atCenterOf(candidatePos).distanceToSqr(spider.position());
            if (candidateLight < bestLight || distance < bestDistance) {
                bestLight = candidateLight;
                bestDistance = distance;
                bestPos = candidatePos;
            }
        }
        return bestPos;
    }
}
