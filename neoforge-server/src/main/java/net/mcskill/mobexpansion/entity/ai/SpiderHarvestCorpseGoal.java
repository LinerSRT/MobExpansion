package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.NestCorpseLoot;
import net.mcskill.mobexpansion.entity.PoisonSpiderEntity;
import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SpiderHarvestCorpseGoal extends Goal {
    private static final double HARVEST_DISTANCE_SQR = 2.5D * 2.5D;
    private static final int HARVEST_TIME_TICKS = 40;
    private static final double FAR_FROM_NEST_SQR = 8.0D * 8.0D;

    private final RegularSpiderEntity spider;
    @Nullable
    private SpiderSpawnerEntity spiderSpawner;
    @Nullable
    private NestCorpseLoot targetCorpse;
    private int harvestTicks;
    private int pathUpdateCooldown;

    public SpiderHarvestCorpseGoal(RegularSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (spider instanceof PoisonSpiderEntity)
            return false;
        if (spider.getTarget() != null || spider.isCarryingLoot())
            return false;
        spiderSpawner = findSpawner();
        if (spiderSpawner == null)
            return false;
        targetCorpse = spiderSpawner.findNearestCorpse(spider.blockPosition());
        return targetCorpse != null;
    }

    @Override
    public boolean canContinueToUse() {
        return spiderSpawner != null
                && targetCorpse != null
                && spider.getTarget() == null
                && !spider.isCarryingLoot()
                && spiderSpawner.hasCorpse(targetCorpse);
    }

    @Override
    public void start() {
        harvestTicks = 0;
        pathUpdateCooldown = 0;
    }

    @Override
    public void stop() {
        targetCorpse = null;
        spiderSpawner = null;
        harvestTicks = 0;
        spider.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (spiderSpawner == null || targetCorpse == null)
            return;

        final BlockPos corpsePos = targetCorpse.getPosition();
        spider.getLookControl().setLookAt(corpsePos.getX() + 0.5D, corpsePos.getY() + 0.5D, corpsePos.getZ() + 0.5D, 30.0F, 30.0F);

        if (spider.distanceToSqr(corpsePos.getX() + 0.5D, corpsePos.getY() + 0.5D, corpsePos.getZ() + 0.5D) > HARVEST_DISTANCE_SQR) {
            harvestTicks = 0;
            if (--pathUpdateCooldown <= 0) {
                pathUpdateCooldown = 8;
                spider.getNavigation().moveTo(corpsePos.getX() + 0.5D, corpsePos.getY(), corpsePos.getZ() + 0.5D, 1.3D);
            }
            return;
        }

        spider.getNavigation().stop();
        harvestTicks++;
        if (harvestTicks < HARVEST_TIME_TICKS)
            return;

        final int harvestedExperience = spiderSpawner.harvestCorpse(targetCorpse);
        if (harvestedExperience > 0)
            spider.setCarriedExperience(spider.getCarriedExperience() + harvestedExperience);
        targetCorpse = null;
    }

    public static boolean isFarFromNest(RegularSpiderEntity spider, SpiderSpawnerEntity spiderSpawner) {
        return spider.distanceToSqr(spiderSpawner) >= FAR_FROM_NEST_SQR;
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
