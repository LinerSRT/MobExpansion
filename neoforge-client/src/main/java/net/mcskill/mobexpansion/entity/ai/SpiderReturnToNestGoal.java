package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SpiderReturnToNestGoal extends Goal {
    private static final double EDGE_FRACTION = 0.75D;

    private final RegularSpiderEntity spider;
    private final double speedModifier;
    @Nullable
    private Vec3 wantedPos;
    private int cooldownTicks;

    public SpiderReturnToNestGoal(RegularSpiderEntity spider, double speedModifier) {
        this.spider = spider;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }
        if (spider.getTarget() != null || spider.isCarryingLoot())
            return false;
        if (!spider.hasRestriction())
            return false;
        if (!isNearPatrolEdge())
            return false;
        if (spider.getRandom().nextInt(reducedTickDelay(100)) != 0)
            return false;

        final BlockPos homePos = spider.getRestrictCenter();
        final Vec3 nestPos = Vec3.atBottomCenterOf(homePos);
        wantedPos = DefaultRandomPos.getPosTowards(spider, 10, 4, nestPos, (float) Math.PI / 2.0F);
        return wantedPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return wantedPos != null
                && !spider.getNavigation().isDone()
                && spider.getTarget() == null
                && !spider.isCarryingLoot();
    }

    @Override
    public void start() {
        if (wantedPos != null)
            spider.getNavigation().moveTo(wantedPos.x, wantedPos.y, wantedPos.z, speedModifier);
    }

    @Override
    public void stop() {
        wantedPos = null;
        cooldownTicks = reducedTickDelay(120);
        spider.getNavigation().stop();
    }

    private boolean isNearPatrolEdge() {
        final double restrictRadius = spider.getRestrictRadius();
        if (restrictRadius <= 0.0D)
            return false;
        final double distanceSqr = spider.blockPosition().distSqr(spider.getRestrictCenter());
        final double edgeDistance = restrictRadius * EDGE_FRACTION;
        return distanceSqr >= edgeDistance * edgeDistance;
    }
}
