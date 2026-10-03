package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SpiderPatrolGoal extends Goal {
    private static final double ARRIVE_DISTANCE_SQR = 2.0D * 2.0D;
    private static final double STANDING_SPEED_SQR = 0.0025D;
    private static final int GIVE_UP_TICKS = 100;

    private final RegularSpiderEntity spider;
    private final double speedModifier;
    @Nullable
    private Vec3 wantedPos;
    private int cooldownTicks;
    private int activeTicks;

    public SpiderPatrolGoal(RegularSpiderEntity spider, double speedModifier) {
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
        if (spider.getSilkReserve() > 0 && isNearNestCenter())
            return false;

        final boolean standingStill = spider.getDeltaMovement().horizontalDistanceSqr() < STANDING_SPEED_SQR;
        final boolean nearNestCenter = isNearNestCenter();
        if (!standingStill && !nearNestCenter && spider.getRandom().nextInt(reducedTickDelay(60)) != 0)
            return false;
        if (standingStill && !nearNestCenter && spider.getRandom().nextInt(reducedTickDelay(20)) != 0)
            return false;

        wantedPos = findPatrolPosition();
        return wantedPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return wantedPos != null
                && spider.getTarget() == null
                && !spider.isCarryingLoot()
                && activeTicks < GIVE_UP_TICKS;
    }

    @Override
    public void start() {
        activeTicks = 0;
        moveTowardWanted();
    }

    @Override
    public void stop() {
        wantedPos = null;
        activeTicks = 0;
        cooldownTicks = reducedTickDelay(40 + spider.getRandom().nextInt(60));
        spider.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (wantedPos == null)
            return;

        activeTicks++;
        if (spider.distanceToSqr(wantedPos) <= ARRIVE_DISTANCE_SQR) {
            wantedPos = null;
            return;
        }

        if (spider.isInCobweb() || activeTicks % 15 == 0)
            moveTowardWanted();
    }

    private void moveTowardWanted() {
        if (wantedPos == null)
            return;
        if (spider.isInCobweb()) {
            spider.getNavigation().stop();
            spider.getMoveControl().setWantedPosition(
                    wantedPos.x,
                    wantedPos.y,
                    wantedPos.z,
                    speedModifier
            );
            return;
        }
        spider.getNavigation().moveTo(wantedPos.x, wantedPos.y, wantedPos.z, speedModifier);
    }

    private boolean isNearNestCenter() {
        final BlockPos homePos = spider.getRestrictCenter();
        return spider.distanceToSqr(homePos.getX() + 0.5D, spider.getY(), homePos.getZ() + 0.5D) <= 36.0D;
    }

    @Nullable
    private Vec3 findPatrolPosition() {
        final BlockPos homePos = spider.getRestrictCenter();
        final double patrolRadius = Math.max(4.0D, spider.getRestrictRadius());
        final Level level = spider.level();

        for (int attempt = 0; attempt < 12; attempt++) {
            final double angle = spider.getRandom().nextDouble() * (Math.PI * 2.0D);
            final double distance = patrolRadius * (0.35D + spider.getRandom().nextDouble() * 0.55D);
            final int blockX = homePos.getX() + Mth.floor(Math.cos(angle) * distance);
            final int blockZ = homePos.getZ() + Mth.floor(Math.sin(angle) * distance);
            final BlockPos candidatePos = findStandablePos(level, blockX, homePos.getY(), blockZ);
            if (candidatePos == null)
                continue;
            if (!spider.isWithinRestriction(candidatePos))
                continue;
            if (spider.distanceToSqr(candidatePos.getX() + 0.5D, candidatePos.getY(), candidatePos.getZ() + 0.5D) < 9.0D)
                continue;
            return Vec3.atBottomCenterOf(candidatePos);
        }
        return null;
    }

    @Nullable
    private BlockPos findStandablePos(Level level, int blockX, int preferY, int blockZ) {
        for (int yOffset = 0; yOffset <= 3; yOffset++) {
            final int[] yCandidates = yOffset == 0
                    ? new int[]{preferY}
                    : new int[]{preferY + yOffset, preferY - yOffset};
            for (int blockY : yCandidates) {
                final BlockPos feetPos = new BlockPos(blockX, blockY, blockZ);
                if (canStandAt(level, feetPos))
                    return feetPos;
            }
        }
        return null;
    }

    private boolean canStandAt(Level level, BlockPos feetPos) {
        final BlockState feetState = level.getBlockState(feetPos);
        if (!feetState.getCollisionShape(level, feetPos).isEmpty() && !feetState.is(Blocks.COBWEB))
            return false;
        final BlockPos supportPos = feetPos.below();
        final BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, Direction.UP) || supportState.is(Blocks.COBWEB);
    }
}
