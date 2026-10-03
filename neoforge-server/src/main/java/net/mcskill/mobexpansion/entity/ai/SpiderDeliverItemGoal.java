package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SpiderDeliverItemGoal extends Goal {
    private static final double DEPOSIT_DISTANCE_SQR = 2.25D * 2.25D;

    private final RegularSpiderEntity spider;
    @Nullable
    private SpiderSpawnerEntity targetSpawner;
    private int pathUpdateCooldown;

    public SpiderDeliverItemGoal(RegularSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (spider.getCarriedExperience() <= 0 || spider.getTarget() != null)
            return false;
        targetSpawner = findSpawner();
        return targetSpawner != null;
    }

    @Override
    public boolean canContinueToUse() {
        return spider.getCarriedExperience() > 0
                && spider.getTarget() == null
                && targetSpawner != null
                && targetSpawner.isAlive();
    }

    @Override
    public void start() {
        pathUpdateCooldown = 0;
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
            pathUpdateCooldown = 10;
            spider.getNavigation().moveTo(targetSpawner, 1.2D);
        }

        if (spider.distanceToSqr(targetSpawner) <= DEPOSIT_DISTANCE_SQR)
            depositCarriedExperience();
    }

    private void depositCarriedExperience() {
        if (targetSpawner == null)
            return;
        final int carriedExperience = spider.getCarriedExperience();
        if (carriedExperience <= 0)
            return;
        targetSpawner.depositCollectedExperience(carriedExperience);
        spider.clearCarriedLoot();
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
