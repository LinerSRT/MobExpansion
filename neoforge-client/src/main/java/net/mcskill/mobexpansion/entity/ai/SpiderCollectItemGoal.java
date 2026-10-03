package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class SpiderCollectItemGoal extends Goal {
    private static final double PICKUP_DISTANCE_SQR = 1.75D * 1.75D;

    private final RegularSpiderEntity spider;
    @Nullable
    private ExperienceOrb targetOrb;
    private int pathUpdateCooldown;

    public SpiderCollectItemGoal(RegularSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (spider.getTarget() != null || spider.isCarryingLoot() || findSpawner() == null)
            return false;
        targetOrb = findNearestExperienceOrb();
        return targetOrb != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetOrb != null
                && targetOrb.isAlive()
                && targetOrb.getValue() > 0
                && spider.getTarget() == null
                && !spider.isCarryingLoot()
                && isOrbInCollectionZone(targetOrb);
    }

    @Override
    public void start() {
        pathUpdateCooldown = 0;
    }

    @Override
    public void stop() {
        targetOrb = null;
        spider.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetOrb == null)
            return;
        spider.getLookControl().setLookAt(targetOrb, 30.0F, 30.0F);
        if (--pathUpdateCooldown <= 0) {
            pathUpdateCooldown = 10;
            spider.getNavigation().moveTo(targetOrb, 1.15D);
        }
        if (spider.distanceToSqr(targetOrb) <= PICKUP_DISTANCE_SQR)
            pickUpOrb(targetOrb);
    }

    private void pickUpOrb(ExperienceOrb experienceOrb) {
        spider.setCarriedExperience(spider.getCarriedExperience() + experienceOrb.getValue());
        experienceOrb.discard();
        targetOrb = null;
        spider.getNavigation().stop();
    }

    @Nullable
    private ExperienceOrb findNearestExperienceOrb() {
        final AABB searchArea = getCollectionArea();
        if (searchArea == null)
            return null;
        final List<ExperienceOrb> experienceOrbs = spider.level().getEntitiesOfClass(ExperienceOrb.class, searchArea, experienceOrb -> experienceOrb.isAlive() && experienceOrb.getValue() > 0);
        ExperienceOrb nearestOrb = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ExperienceOrb experienceOrb : experienceOrbs) {
            final double distance = spider.distanceToSqr(experienceOrb);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestOrb = experienceOrb;
            }
        }
        return nearestOrb;
    }

    private boolean isOrbInCollectionZone(ExperienceOrb experienceOrb) {
        final AABB searchArea = getCollectionArea();
        return searchArea != null && searchArea.contains(experienceOrb.position());
    }

    @Nullable
    private AABB getCollectionArea() {
        final SpiderSpawnerEntity spiderSpawner = findSpawner();
        if (spiderSpawner != null)
            return spiderSpawner.getCollectionArea();
        if (!spider.hasRestriction())
            return null;
        return new AABB(spider.getRestrictCenter()).inflate(spider.getRestrictRadius());
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
