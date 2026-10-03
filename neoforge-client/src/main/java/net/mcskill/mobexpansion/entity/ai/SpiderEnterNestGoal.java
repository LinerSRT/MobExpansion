package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SpiderEnterNestGoal extends Goal {
    private static final double ENTER_DISTANCE_SQR = 4.0D * 4.0D;

    private final RegularSpiderEntity spider;
    @Nullable
    private SpiderSpawnerEntity targetSpawner;
    private int pathUpdateCooldown;

    public SpiderEnterNestGoal(RegularSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (spider.getTarget() != null || spider.isCarryingLoot())
            return false;
        targetSpawner = findSpawner();
        if (targetSpawner == null || !targetSpawner.isActivated())
            return false;
        return targetSpawner.shouldReduceOutsideSpiders();
    }

    @Override
    public boolean canContinueToUse() {
        return targetSpawner != null
                && targetSpawner.isAlive()
                && spider.getTarget() == null
                && !spider.isCarryingLoot()
                && targetSpawner.shouldReduceOutsideSpiders();
    }

    @Override
    public void start() {
        pathUpdateCooldown = 0;
        spider.getNavigation().moveTo(targetSpawner, 1.25D);
    }

    @Override
    public void stop() {
        targetSpawner = null;
        spider.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetSpawner == null)
            return;

        spider.getLookControl().setLookAt(targetSpawner, 30.0F, 30.0F);
        if (--pathUpdateCooldown <= 0) {
            pathUpdateCooldown = 8;
            spider.getNavigation().moveTo(targetSpawner, 1.25D);
        }

        if (spider.distanceToSqr(targetSpawner) <= ENTER_DISTANCE_SQR)
            targetSpawner.absorbSpider(spider);
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
