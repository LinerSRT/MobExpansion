package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class SpiderNearestAttackableTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    private final RegularSpiderEntity spider;

    public SpiderNearestAttackableTargetGoal(RegularSpiderEntity spider, Class<T> targetType, int randomInterval, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(spider, targetType, randomInterval, mustSee, mustReach, targetPredicate);
        this.spider = spider;
    }

    @Override
    @NotNull
    protected AABB getTargetSearchArea(double followDistance) {
        return spider.getBoundingBox().inflate(followDistance, RegularSpiderEntity.TARGET_SEARCH_VERTICAL, followDistance);
    }
}
