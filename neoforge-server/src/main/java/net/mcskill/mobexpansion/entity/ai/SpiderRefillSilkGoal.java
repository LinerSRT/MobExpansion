package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SpiderRefillSilkGoal extends Goal {
    private static final double REFILL_DISTANCE_SQR = 2.75D * 2.75D;
    private static final int REFILL_TIME_TICKS = 35;

    private final RegularSpiderEntity spider;
    @Nullable
    private SpiderSpawnerEntity spiderSpawner;
    private int refillTicks;
    private int pathUpdateCooldown;

    public SpiderRefillSilkGoal(RegularSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (spider.getTarget() != null || spider.isCarryingLoot())
            return false;
        if (!spider.needsSilkRefill())
            return false;
        spiderSpawner = findSpawner();
        if (spiderSpawner == null || !spiderSpawner.isActivated())
            return false;
        return !spiderSpawner.shouldReduceOutsideSpiders();
    }

    @Override
    public boolean canContinueToUse() {
        return spiderSpawner != null
                && spiderSpawner.isAlive()
                && spider.getTarget() == null
                && !spider.isCarryingLoot()
                && spider.needsSilkRefill()
                && !spiderSpawner.shouldReduceOutsideSpiders();
    }

    @Override
    public void start() {
        refillTicks = 0;
        pathUpdateCooldown = 0;
    }

    @Override
    public void stop() {
        spiderSpawner = null;
        refillTicks = 0;
        spider.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (spiderSpawner == null)
            return;

        spider.getLookControl().setLookAt(spiderSpawner, 30.0F, 30.0F);
        if (spider.distanceToSqr(spiderSpawner) > REFILL_DISTANCE_SQR) {
            refillTicks = 0;
            if (--pathUpdateCooldown <= 0) {
                pathUpdateCooldown = 8;
                spider.getNavigation().moveTo(spiderSpawner, 1.15D);
            }
            return;
        }

        spider.getNavigation().stop();
        refillTicks++;
        if (refillTicks >= REFILL_TIME_TICKS)
            spider.refillSilk();
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
