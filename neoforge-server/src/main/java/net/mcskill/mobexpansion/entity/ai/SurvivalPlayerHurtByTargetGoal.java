package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.entity.MobExTargeting;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

public class SurvivalPlayerHurtByTargetGoal extends HurtByTargetGoal {
    public SurvivalPlayerHurtByTargetGoal(PathfinderMob mob) {
        super(mob);
    }

    @Override
    public boolean canUse() {
        if (!MobExTargeting.isSurvivalPlayer(mob.getLastHurtByMob()))
            return false;
        return super.canUse();
    }
}
