package net.mcskill.mobexpansion.event;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.mcskill.mobexpansion.entity.SpiderSpawnerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

@EventBusSubscriber(modid = Core.MODID)
public final class MobExCobwebAlarmEvents {
    private static final int CHECK_INTERVAL_TICKS = 5;
    private static final double SPAWNER_SEARCH_RANGE = 48.0D;

    private MobExCobwebAlarmEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity))
            return;
        if (livingEntity.level().isClientSide())
            return;
        if (livingEntity.tickCount % CHECK_INTERVAL_TICKS != 0)
            return;
        if (livingEntity instanceof RegularSpiderEntity || livingEntity instanceof SpiderSpawnerEntity)
            return;
        if (!livingEntity.isAlive() || livingEntity.isSpectator())
            return;
        if (!isTouchingCobweb(livingEntity))
            return;

        final List<SpiderSpawnerEntity> nearbySpawners = livingEntity.level().getEntitiesOfClass(
                SpiderSpawnerEntity.class,
                livingEntity.getBoundingBox().inflate(SPAWNER_SEARCH_RANGE),
                spiderSpawner -> spiderSpawner.isActivated()
                        && !spiderSpawner.isDeadOrDying()
                        && spiderSpawner.getCollectionArea().intersects(livingEntity.getBoundingBox())
        );
        for (SpiderSpawnerEntity spiderSpawner : nearbySpawners)
            spiderSpawner.onCobwebDisturbed(livingEntity);
    }

    private static boolean isTouchingCobweb(LivingEntity livingEntity) {
        final AABB boundingBox = livingEntity.getBoundingBox();
        final BlockPos minPos = BlockPos.containing(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        final BlockPos maxPos = BlockPos.containing(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
        for (BlockPos blockPos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (livingEntity.level().getBlockState(blockPos).is(Blocks.COBWEB))
                return true;
        }
        return false;
    }
}
