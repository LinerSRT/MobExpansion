package net.mcskill.mobexpansion.entity.ai;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.PoisonSpiderEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class PoisonSpiderRangedGoal extends Goal {
    private final PoisonSpiderEntity spider;
    private final RangedAttackMob rangedAttackMob;
    private final double speedModifier;

    @Nullable
    private LivingEntity target;
    private int attackTime = -1;
    private int repathCooldown;
    private boolean movingAway;

    public PoisonSpiderRangedGoal(PoisonSpiderEntity spider, double speedModifier) {
        this.spider = spider;
        this.rangedAttackMob = spider;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final LivingEntity attackTarget = spider.getTarget();
        if (attackTarget == null || !attackTarget.isAlive())
            return false;
        target = attackTarget;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || (target != null && target.isAlive());
    }

    @Override
    public void stop() {
        target = null;
        attackTime = -1;
        movingAway = false;
        spider.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        final LivingEntity attackTarget = target;
        if (attackTarget == null)
            return;

        final double distanceSqr = spider.distanceToSqr(attackTarget);
        final double distance = Math.sqrt(distanceSqr);
        final boolean canSee = spider.getSensing().hasLineOfSight(attackTarget);
        final float attackRadius = MobExParameters.getFloat(spider, "spitAttackRadius", 12.0F);

        spider.getLookControl().setLookAt(attackTarget, 30.0F, 30.0F);
        if (spider.isPreparingSpit()) {
            spider.getNavigation().stop();
            return;
        }

        updateMovement(attackTarget, distance, canSee);

        if (!canSee)
            return;

        if (--attackTime <= 0 && distance <= attackRadius) {
            final float velocityScale = Mth.clamp((float) (distance / attackRadius), 0.1F, 1.0F);
            rangedAttackMob.performRangedAttack(attackTarget, velocityScale);
            final int attackIntervalMin = MobExParameters.getInt(spider, "spitMinInterval", 120);
            final int attackIntervalMax = MobExParameters.getInt(spider, "spitMaxInterval", 160);
            attackTime = spider.getRandom().nextInt(attackIntervalMin, Math.max(attackIntervalMin, attackIntervalMax) + 1);
        }
    }

    private void updateMovement(LivingEntity attackTarget, double distance, boolean canSee) {
        final double approachDistance = MobExParameters.get(spider, "approachDistance", 11.0D);
        final double retreatDistance = MobExParameters.get(spider, "retreatDistance", 6.0D);
        final double holdDistance = MobExParameters.get(spider, "holdDistance", 8.5D);

        if (!canSee || distance > approachDistance) {
            movingAway = false;
            if (--repathCooldown <= 0) {
                repathCooldown = 10;
                spider.getNavigation().moveTo(attackTarget, speedModifier);
            }
            return;
        }

        if (distance < retreatDistance || movingAway && distance < holdDistance) {
            movingAway = true;
            if (--repathCooldown <= 0) {
                repathCooldown = 8;
                final Vec3 retreatPos = DefaultRandomPos.getPosAway(spider, 10, 5, attackTarget.position());
                if (retreatPos != null)
                    spider.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, speedModifier);
            }
            return;
        }

        movingAway = false;
        spider.getNavigation().stop();
        repathCooldown = 0;
    }
}
