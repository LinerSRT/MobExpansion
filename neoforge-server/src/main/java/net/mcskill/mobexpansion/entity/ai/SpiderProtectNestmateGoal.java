package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.PoisonSpiderEntity;
import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class SpiderProtectNestmateGoal extends Goal {
    private final PoisonSpiderEntity spider;
    @Nullable
    private LivingEntity protectTarget;

    public SpiderProtectNestmateGoal(PoisonSpiderEntity spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (spider.getTarget() != null && spider.getTarget().isAlive())
            return false;
        protectTarget = findThreatAgainstWoundedNestmate();
        return protectTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return protectTarget != null
                && protectTarget.isAlive()
                && (spider.getTarget() == null || spider.getTarget() == protectTarget);
    }

    @Override
    public void start() {
        spider.setTarget(protectTarget);
    }

    @Override
    public void stop() {
        if (spider.getTarget() == protectTarget)
            spider.setTarget(null);
        protectTarget = null;
    }

    @Nullable
    private LivingEntity findThreatAgainstWoundedNestmate() {
        final double searchRange = MobExParameters.get(spider, "homeRadius", 12.0D);
        final List<RegularSpiderEntity> nestmates = spider.level().getEntitiesOfClass(
                RegularSpiderEntity.class,
                spider.getBoundingBox().inflate(searchRange),
                nestmate -> nestmate != spider
                        && nestmate.isAlive()
                        && spider.isSameNest(nestmate)
                        && nestmate.getHealth() <= nestmate.getMaxHealth() * 0.5F
        );
        for (RegularSpiderEntity nestmate : nestmates) {
            final LivingEntity nestmateTarget = nestmate.getTarget();
            if (nestmateTarget != null && spider.isValidAttackTarget(nestmateTarget))
                return nestmateTarget;
            final LivingEntity lastHurtBy = nestmate.getLastHurtByMob();
            if (lastHurtBy != null && spider.isValidAttackTarget(lastHurtBy))
                return lastHurtBy;
        }
        return null;
    }
}
