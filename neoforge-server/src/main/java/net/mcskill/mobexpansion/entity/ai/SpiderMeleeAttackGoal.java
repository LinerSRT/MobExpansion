package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.RegularSpiderEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class SpiderMeleeAttackGoal extends MeleeAttackGoal {
    private final RegularSpiderEntity spider;

    public SpiderMeleeAttackGoal(RegularSpiderEntity spider, double speedModifier, boolean followUnseenTarget) {
        super(spider, speedModifier, followUnseenTarget);
        this.spider = spider;
    }

    @Override
    public boolean canUse() {
        final LivingEntity attackTarget = spider.getTarget();
        if (attackTarget != null && !spider.isWithinCombatLeash(attackTarget.blockPosition()))
            return false;
        if (attackTarget != null && !spider.isWithinVerticalEngageRange(attackTarget))
            return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        final LivingEntity attackTarget = spider.getTarget();
        if (attackTarget != null && !spider.isWithinCombatLeash(attackTarget.blockPosition()))
            return false;
        if (attackTarget != null && !spider.isWithinVerticalEngageRange(attackTarget))
            return false;
        return super.canContinueToUse();
    }

    @Override
    protected int adjustedTickDelay(int delay) {
        if (spider.hasEffect(MobEffects.DIG_SPEED))
            return super.adjustedTickDelay(Math.max(8, delay / 2));
        return super.adjustedTickDelay(delay);
    }
}
