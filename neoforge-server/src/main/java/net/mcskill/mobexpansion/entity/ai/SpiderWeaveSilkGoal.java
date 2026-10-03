package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

public class SpiderWeaveSilkGoal extends Goal {
    private static final int[][] THREAD_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final int PLACE_INTERVAL_TICKS = 45;
    private static final double REACH_HORIZONTAL_SQR = 2.25D * 2.25D;
    private static final double REACH_VERTICAL = 2.0D;
    private static final int STUCK_TIMEOUT_TICKS = 60;
    private static final double PROGRESS_EPSILON = 0.35D;
    private static final int REPATH_INTERVAL_TICKS = 25;

    private final RegularSpiderEntity spider;
    @Nullable
    private SpiderSpawnerEntity spiderSpawner;
    @Nullable
    private BlockPos weaveTarget;
    private int directionIndex = -1;
    private int placeCooldown;
    private int repathCooldown;
    private int failedAttempts;
    private int stuckTicks;
    private double lastDistanceSqr = -1.0D;

    public SpiderWeaveSilkGoal(RegularSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!spider.canWeaveSilk())
            return false;
        if (!spider.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
            return false;
        spiderSpawner = findSpawner();
        if (spiderSpawner == null || !spiderSpawner.isActivated())
            return false;
        if (spiderSpawner.shouldReduceOutsideSpiders())
            return false;
        return findBestWeaveTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (!spider.canWeaveSilk()
                || spiderSpawner == null
                || !spiderSpawner.isAlive()
                || weaveTarget == null
                || failedAttempts >= 4
                || stuckTicks >= STUCK_TIMEOUT_TICKS
                || spiderSpawner.shouldReduceOutsideSpiders()
                || !canPlaceCobwebAt(weaveTarget))
            return false;
        if (directionIndex >= 0 && !spiderSpawner.isWeaveThreadAvailable(directionIndex, spider.getUUID()))
            return false;
        return true;
    }

    @Override
    public void start() {
        placeCooldown = 10;
        repathCooldown = 0;
        failedAttempts = 0;
        stuckTicks = 0;
        lastDistanceSqr = -1.0D;
        if (!claimCurrentWeaveTarget() && !pickAndClaimWeaveTarget()) {
            weaveTarget = null;
            directionIndex = -1;
            return;
        }
        moveToWeaveTarget();
    }

    @Override
    public void stop() {
        releaseCurrentThreadClaim();
        weaveTarget = null;
        directionIndex = -1;
        spiderSpawner = null;
        stuckTicks = 0;
        lastDistanceSqr = -1.0D;
        spider.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (spiderSpawner == null || weaveTarget == null)
            return;

        spider.getLookControl().setLookAt(
                weaveTarget.getX() + 0.5D,
                weaveTarget.getY() + 0.5D,
                weaveTarget.getZ() + 0.5D,
                30.0F,
                30.0F
        );

        updateStuckTracking();
        if (weaveTarget == null)
            return;

        if (isInWeaveRange(weaveTarget)) {
            spider.getNavigation().stop();
            if (--placeCooldown > 0)
                return;

            if (!placeCobweb(weaveTarget)) {
                failedAttempts++;
                if (!pickAndClaimWeaveTarget())
                    weaveTarget = null;
                placeCooldown = 15;
                stuckTicks = 0;
                return;
            }

            spider.consumeSilk(1);
            placeCooldown = PLACE_INTERVAL_TICKS;
            failedAttempts = 0;
            stuckTicks = 0;
            lastDistanceSqr = -1.0D;
            if (!spider.canWeaveSilk() || !advanceAlongCurrentThread()) {
                if (!pickAndClaimWeaveTarget())
                    weaveTarget = null;
            } else {
                moveToWeaveTarget();
            }
            return;
        }

        if (spider.isInCobweb()) {
            spider.getMoveControl().setWantedPosition(
                    weaveTarget.getX() + 0.5D,
                    weaveTarget.getY(),
                    weaveTarget.getZ() + 0.5D,
                    0.95D
            );
            return;
        }

        if (--repathCooldown <= 0) {
            repathCooldown = REPATH_INTERVAL_TICKS;
            if (!spider.getNavigation().isInProgress())
                moveToWeaveTarget();
        }
    }

    private void updateStuckTracking() {
        if (weaveTarget == null)
            return;
        if (isInWeaveRange(weaveTarget)) {
            stuckTicks = 0;
            lastDistanceSqr = spider.distanceToSqr(
                    weaveTarget.getX() + 0.5D,
                    weaveTarget.getY() + 0.5D,
                    weaveTarget.getZ() + 0.5D
            );
            return;
        }
        final double distanceSqr = spider.distanceToSqr(
                weaveTarget.getX() + 0.5D,
                weaveTarget.getY() + 0.5D,
                weaveTarget.getZ() + 0.5D
        );
        if (lastDistanceSqr < 0.0D || distanceSqr + PROGRESS_EPSILON < lastDistanceSqr) {
            stuckTicks = 0;
            lastDistanceSqr = distanceSqr;
            return;
        }
        stuckTicks++;
        lastDistanceSqr = Math.min(lastDistanceSqr, distanceSqr);
        if (stuckTicks < STUCK_TIMEOUT_TICKS)
            return;
        failedAttempts++;
        stuckTicks = 0;
        lastDistanceSqr = -1.0D;
        if (!pickAndClaimWeaveTarget())
            weaveTarget = null;
    }

    private boolean isInWeaveRange(BlockPos targetPos) {
        final double deltaX = spider.getX() - (targetPos.getX() + 0.5D);
        final double deltaY = spider.getY() - targetPos.getY();
        final double deltaZ = spider.getZ() - (targetPos.getZ() + 0.5D);
        return deltaX * deltaX + deltaZ * deltaZ <= REACH_HORIZONTAL_SQR && Math.abs(deltaY) <= REACH_VERTICAL;
    }

    private void moveToWeaveTarget() {
        if (weaveTarget == null)
            return;
        if (spider.isInCobweb()) {
            spider.getMoveControl().setWantedPosition(
                    weaveTarget.getX() + 0.5D,
                    weaveTarget.getY(),
                    weaveTarget.getZ() + 0.5D,
                    0.95D
            );
            return;
        }
        spider.getNavigation().moveTo(
                weaveTarget.getX() + 0.5D,
                weaveTarget.getY(),
                weaveTarget.getZ() + 0.5D,
                0.85D
        );
    }

    private void releaseCurrentThreadClaim() {
        if (spiderSpawner == null || directionIndex < 0)
            return;
        spiderSpawner.releaseWeaveThread(directionIndex, spider.getUUID());
    }

    private boolean findBestWeaveTarget() {
        if (spiderSpawner == null)
            return false;

        if (spider.shouldAvoidBrightLight()) {
            final BlockPos torchPos = findNearbyTorchToSmother();
            if (torchPos != null) {
                directionIndex = -1;
                weaveTarget = torchPos;
                return true;
            }
        }

        BlockPos bestTip = null;
        int bestDirection = -1;
        double bestDistance = Double.MAX_VALUE;
        final UUID spiderId = spider.getUUID();
        final int startIndex = spider.getRandom().nextInt(THREAD_DIRECTIONS.length);
        for (int offset = 0; offset < THREAD_DIRECTIONS.length; offset++) {
            final int index = (startIndex + offset) % THREAD_DIRECTIONS.length;
            if (!spiderSpawner.isWeaveThreadAvailable(index, spiderId))
                continue;
            final BlockPos tipPos = findThreadTip(index);
            if (tipPos == null || !canPlaceCobwebAt(tipPos))
                continue;
            final double distance = spider.distanceToSqr(tipPos.getX() + 0.5D, tipPos.getY() + 0.5D, tipPos.getZ() + 0.5D);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTip = tipPos;
                bestDirection = index;
            }
        }

        if (bestTip == null || bestDirection < 0) {
            weaveTarget = null;
            directionIndex = -1;
            return false;
        }

        directionIndex = bestDirection;
        weaveTarget = bestTip;
        return true;
    }

    private boolean claimCurrentWeaveTarget() {
        if (spiderSpawner == null || weaveTarget == null)
            return false;
        if (directionIndex < 0)
            return true;
        return spiderSpawner.tryClaimWeaveThread(directionIndex, spider.getUUID());
    }

    private boolean pickAndClaimWeaveTarget() {
        releaseCurrentThreadClaim();
        if (!findBestWeaveTarget())
            return false;
        if (!claimCurrentWeaveTarget()) {
            weaveTarget = null;
            directionIndex = -1;
            return false;
        }
        stuckTicks = 0;
        lastDistanceSqr = -1.0D;
        repathCooldown = 0;
        return true;
    }

    @Nullable
    private BlockPos findNearbyTorchToSmother() {
        final int searchRadius = 8;
        final BlockPos origin = spider.blockPosition();
        BlockPos nearestTorch = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
            for (int offsetY = -2; offsetY <= 2; offsetY++) {
                for (int offsetZ = -searchRadius; offsetZ <= searchRadius; offsetZ++) {
                    final BlockPos candidatePos = origin.offset(offsetX, offsetY, offsetZ);
                    if (spider.hasRestriction() && !spider.isWithinRestriction(candidatePos))
                        continue;
                    if (!isSmotherableLightSource(spider.level().getBlockState(candidatePos)))
                        continue;
                    final double distance = spider.distanceToSqr(candidatePos.getX() + 0.5D, candidatePos.getY() + 0.5D, candidatePos.getZ() + 0.5D);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestTorch = candidatePos;
                    }
                }
            }
        }
        return nearestTorch;
    }

    private boolean advanceAlongCurrentThread() {
        if (directionIndex < 0)
            return false;
        final BlockPos nextTip = findThreadTip(directionIndex);
        if (nextTip == null || !canPlaceCobwebAt(nextTip))
            return false;
        weaveTarget = nextTip;
        return true;
    }

    @Nullable
    private BlockPos findThreadTip(int threadDirectionIndex) {
        if (spiderSpawner == null)
            return null;

        final int[] direction = THREAD_DIRECTIONS[threadDirectionIndex];
        final BlockPos origin = spiderSpawner.blockPosition();
        final int maxSteps = Math.max(4, spiderSpawner.getPatrolRadius()) * 2;
        BlockPos currentPos = findThreadStart(origin);
        if (currentPos == null)
            currentPos = origin.above();

        for (int step = 0; step < maxSteps; step++) {
            final BlockPos nextPos = findNextThreadSegment(currentPos, direction);
            if (nextPos == null)
                return null;
            if (spider.hasRestriction() && !spider.isWithinRestriction(nextPos))
                return null;

            final BlockState nextState = spider.level().getBlockState(nextPos);
            if (nextState.is(Blocks.COBWEB)) {
                currentPos = nextPos;
                continue;
            }
            if (canPlaceCobwebAt(nextPos))
                return nextPos;
            return null;
        }
        return null;
    }

    @Nullable
    private BlockPos findThreadStart(BlockPos origin) {
        final BlockPos aboveOrigin = origin.above();
        if (spider.level().getBlockState(aboveOrigin).is(Blocks.COBWEB) || canPlaceCobwebAt(aboveOrigin))
            return aboveOrigin;
        if (spider.level().getBlockState(origin).is(Blocks.COBWEB) || canOccupyWeaveCell(origin))
            return origin;
        for (Direction direction : Direction.values()) {
            final BlockPos neighborPos = origin.relative(direction);
            if (spider.level().getBlockState(neighborPos).is(Blocks.COBWEB))
                return neighborPos;
        }
        if (hasFloorSupport(aboveOrigin) && canOccupyWeaveCell(aboveOrigin))
            return aboveOrigin;
        if (hasFloorSupport(origin) && canOccupyWeaveCell(origin))
            return origin;
        return aboveOrigin;
    }

    @Nullable
    private BlockPos findNextThreadSegment(BlockPos fromPos, int[] direction) {
        final BlockPos forwardPos = fromPos.offset(direction[0], 0, direction[1]);
        final BlockPos forwardUpPos = fromPos.offset(direction[0], 1, direction[1]);
        final BlockPos forwardDownPos = fromPos.offset(direction[0], -1, direction[1]);

        if (isWallBlocking(forwardPos)) {
            if (isThreadCell(forwardUpPos))
                return forwardUpPos;
            if (isAlreadyWallClimbSegment(fromPos, direction))
                return null;
            final BlockPos upPos = fromPos.above();
            if (isThreadCell(upPos))
                return upPos;
            return null;
        }

        if (isReplaceableWeaveCell(forwardPos) && !hasFloorSupport(forwardPos) && !spider.level().getBlockState(forwardPos).is(Blocks.COBWEB)) {
            if (isAlreadyCliffDropSegment(fromPos, direction))
                return null;
            final BlockPos downPos = fromPos.below();
            if (isThreadCell(downPos))
                return downPos;
            return null;
        }

        if (isThreadCell(forwardPos))
            return forwardPos;
        if (isThreadCell(forwardUpPos))
            return forwardUpPos;
        if (isThreadCell(forwardDownPos))
            return forwardDownPos;
        return null;
    }

    private boolean isAlreadyWallClimbSegment(BlockPos fromPos, int[] direction) {
        if (!spider.level().getBlockState(fromPos.below()).is(Blocks.COBWEB))
            return false;
        return isWallBlocking(fromPos.offset(direction[0], 0, direction[1]));
    }

    private boolean isAlreadyCliffDropSegment(BlockPos fromPos, int[] direction) {
        if (!spider.level().getBlockState(fromPos.above()).is(Blocks.COBWEB))
            return false;
        final BlockPos forwardPos = fromPos.offset(direction[0], 0, direction[1]);
        return isReplaceableWeaveCell(forwardPos) && !hasFloorSupport(forwardPos);
    }

    private boolean isThreadCell(BlockPos blockPos) {
        final BlockState blockState = spider.level().getBlockState(blockPos);
        if (blockState.is(Blocks.COBWEB))
            return true;
        return canPlaceCobwebAt(blockPos);
    }

    private boolean isReplaceableWeaveCell(BlockPos blockPos) {
        final BlockState blockState = spider.level().getBlockState(blockPos);
        return canPlaceCobweb(blockState, blockPos) || blockState.is(Blocks.COBWEB);
    }

    private boolean canOccupyWeaveCell(BlockPos blockPos) {
        final BlockState blockState = spider.level().getBlockState(blockPos);
        return blockState.is(Blocks.COBWEB) || canPlaceCobweb(blockState, blockPos);
    }

    private boolean isWallBlocking(BlockPos blockPos) {
        final BlockState blockState = spider.level().getBlockState(blockPos);
        if (blockState.is(Blocks.COBWEB) || canPlaceCobweb(blockState, blockPos))
            return false;
        return !blockState.getCollisionShape(spider.level(), blockPos).isEmpty();
    }

    private boolean placeCobweb(BlockPos cobwebPos) {
        if (!canPlaceCobwebAt(cobwebPos))
            return false;
        spider.level().setBlock(cobwebPos, Blocks.COBWEB.defaultBlockState(), 3);
        spider.level().gameEvent(GameEvent.BLOCK_PLACE, cobwebPos, GameEvent.Context.of(spider, Blocks.COBWEB.defaultBlockState()));
        spider.playSound(SoundEvents.SPIDER_STEP, 0.6F, 1.4F);
        return true;
    }

    private boolean canPlaceCobwebAt(BlockPos cobwebPos) {
        final BlockState currentState = spider.level().getBlockState(cobwebPos);
        if (isSmotherableLightSource(currentState))
            return spider.shouldAvoidBrightLight();
        return canPlaceCobweb(currentState, cobwebPos) && hasWeaveSupport(cobwebPos);
    }

    private boolean hasWeaveSupport(BlockPos cobwebPos) {
        if (hasFloorSupport(cobwebPos))
            return true;
        if (hasAdjacentCobweb(cobwebPos))
            return true;
        return hasWallClingSupport(cobwebPos);
    }

    private boolean hasAdjacentCobweb(BlockPos cobwebPos) {
        for (Direction direction : Direction.values()) {
            if (spider.level().getBlockState(cobwebPos.relative(direction)).is(Blocks.COBWEB))
                return true;
        }
        return false;
    }

    private boolean hasWallClingSupport(BlockPos cobwebPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            final BlockPos neighborPos = cobwebPos.relative(direction);
            final BlockState neighborState = spider.level().getBlockState(neighborPos);
            if (neighborState.isFaceSturdy(spider.level(), neighborPos, direction.getOpposite()))
                return true;
        }
        return false;
    }

    private boolean hasFloorSupport(BlockPos cobwebPos) {
        final BlockPos supportPos = cobwebPos.below();
        final BlockState supportState = spider.level().getBlockState(supportPos);
        return supportState.isFaceSturdy(spider.level(), supportPos, Direction.UP);
    }

    private boolean canPlaceCobweb(BlockState blockState, BlockPos blockPos) {
        if (blockState.is(Blocks.COBWEB))
            return false;
        if (blockState.isAir() || blockState.canBeReplaced() || blockState.is(BlockTags.REPLACEABLE))
            return true;
        if (!blockState.getFluidState().isEmpty())
            return false;
        return blockState.getCollisionShape(spider.level(), blockPos).isEmpty();
    }

    private static boolean isSmotherableLightSource(BlockState blockState) {
        return blockState.is(Blocks.TORCH)
                || blockState.is(Blocks.WALL_TORCH)
                || blockState.is(Blocks.SOUL_TORCH)
                || blockState.is(Blocks.SOUL_WALL_TORCH);
    }

    @Nullable
    private SpiderSpawnerEntity findSpawner() {
        if (spider.getSpawnerUUID().isEmpty() || !(spider.level() instanceof ServerLevel serverLevel))
            return null;
        final Entity spawnerEntity = serverLevel.getEntity(spider.getSpawnerUUID().get());
        if (spawnerEntity instanceof SpiderSpawnerEntity spiderSpawner && spiderSpawner.isAlive())
            return spiderSpawner;
        return null;
    }
}
