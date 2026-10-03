package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SpiderFleeToNestGoal extends Goal {
    private final RegularSpiderEntity spider;
    private final double speedModifier;
    @Nullable
    private Vec3 fleePos;

    public SpiderFleeToNestGoal(RegularSpiderEntity spider, double speedModifier) {
        this.spider = spider;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!spider.shouldFleeToNest())
            return false;
        if (spider.isCarryingLoot())
            return false;
        final SpiderSpawnerEntity spiderSpawner = findSpawner();
        if (spiderSpawner == null)
            return false;
        if (spider.distanceToSqr(spiderSpawner) < 9.0D)
            return false;
        fleePos = DefaultRandomPos.getPosTowards(spider, 12, 4, spiderSpawner.position(), (float) Math.PI / 2.0F);
        return fleePos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return fleePos != null
                && !spider.getNavigation().isDone()
                && spider.getHealth() <= spider.getMaxHealth() * 0.45F;
    }

    @Override
    public void start() {
        if (fleePos != null)
            spider.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, speedModifier);
        spider.setTarget(null);
    }

    @Override
    public void stop() {
        fleePos = null;
        spider.getNavigation().stop();
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
