package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.PoisonSpiderEntity;
import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class SpiderPassItemGoal extends Goal {
    private static final double PASS_DISTANCE_SQR = 2.0D * 2.0D;
    private static final double SEARCH_RANGE = 8.0D;
    private static final double PROGRESS_EPSILON = 0.25D;

    private final RegularSpiderEntity spider;
    @Nullable
    private RegularSpiderEntity receiverSpider;
    @Nullable
    private SpiderSpawnerEntity spiderSpawner;
    private int pathUpdateCooldown;
    private double lastNestDistanceSqr = -1.0D;

    public SpiderPassItemGoal(RegularSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (spider.getCarriedExperience() <= 0 || spider.getTarget() != null)
            return false;
        if (!spider.canPassExperience())
            return false;
        spiderSpawner = findSpawner();
        if (spiderSpawner == null)
            return false;
        if (isProgressingTowardNest())
            return false;
        receiverSpider = findCloserEmptyNestmate(spiderSpawner);
        return receiverSpider != null;
    }

    @Override
    public boolean canContinueToUse() {
        return spider.getCarriedExperience() > 0
                && spider.getTarget() == null
                && spider.canPassExperience()
                && spiderSpawner != null
                && receiverSpider != null
                && receiverSpider.isAlive()
                && receiverSpider.canPassExperience()
                && receiverSpider.getCarriedExperience() <= 0
                && receiverSpider.getTarget() == null
                && !isProgressingTowardNest();
    }

    @Override
    public void start() {
        pathUpdateCooldown = 0;
        if (spiderSpawner != null)
            lastNestDistanceSqr = spider.distanceToSqr(spiderSpawner);
    }

    @Override
    public void stop() {
        receiverSpider = null;
        spiderSpawner = null;
        lastNestDistanceSqr = -1.0D;
        spider.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (receiverSpider == null)
            return;

        spider.getLookControl().setLookAt(receiverSpider, 30.0F, 30.0F);
        if (--pathUpdateCooldown <= 0) {
            pathUpdateCooldown = 8;
            spider.getNavigation().moveTo(receiverSpider, 1.25D);
        }

        if (spider.distanceToSqr(receiverSpider) <= PASS_DISTANCE_SQR)
            passExperience();
    }

    private boolean isProgressingTowardNest() {
        if (spiderSpawner == null)
            return false;
        final double currentDistance = spider.distanceToSqr(spiderSpawner);
        if (lastNestDistanceSqr < 0.0D) {
            lastNestDistanceSqr = currentDistance;
            return false;
        }
        final boolean progressing = currentDistance < lastNestDistanceSqr - PROGRESS_EPSILON;
        lastNestDistanceSqr = currentDistance;
        return progressing;
    }

    private void passExperience() {
        if (receiverSpider == null || receiverSpider.getCarriedExperience() > 0)
            return;
        if (!spider.canPassExperience() || !receiverSpider.canPassExperience())
            return;
        final int carriedExperience = spider.getCarriedExperience();
        if (carriedExperience <= 0)
            return;
        receiverSpider.setCarriedExperience(carriedExperience);
        spider.setCarriedExperience(0);
        spider.markExperiencePassed();
        receiverSpider.markExperiencePassed();
        spider.getNavigation().stop();
        receiverSpider = null;
    }

    @Nullable
    private RegularSpiderEntity findCloserEmptyNestmate(SpiderSpawnerEntity nestSpawner) {
        final double ownDistance = spider.distanceToSqr(nestSpawner);
        final List<RegularSpiderEntity> nestmates = spider.level().getEntitiesOfClass(
                RegularSpiderEntity.class,
                spider.getBoundingBox().inflate(SEARCH_RANGE),
                nestmate -> nestmate != spider
                        && nestmate.isAlive()
                        && !(nestmate instanceof PoisonSpiderEntity)
                        && spider.isSameNest(nestmate)
                        && nestmate.getCarriedExperience() <= 0
                        && nestmate.canPassExperience()
                        && nestmate.getTarget() == null
        );

        RegularSpiderEntity bestReceiver = null;
        double bestDistance = ownDistance;
        for (RegularSpiderEntity nestmate : nestmates) {
            final double nestmateDistance = nestmate.distanceToSqr(nestSpawner);
            if (nestmateDistance + 4.0D < bestDistance) {
                bestDistance = nestmateDistance;
                bestReceiver = nestmate;
            }
        }
        return bestReceiver;
    }

    @Nullable
    private SpiderSpawnerEntity findSpawner() {
        if (spider.getSpawnerUUID().isEmpty() || !(spider.level() instanceof ServerLevel serverLevel))
            return null;
        final Entity spawnerEntity = serverLevel.getEntity(spider.getSpawnerUUID().get());
        if (spawnerEntity instanceof SpiderSpawnerEntity nestSpawner && nestSpawner.isAlive())
            return nestSpawner;
        return null;
    }
}
