package net.mcskill.mobexpansion.entity.ai.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.redstone.RedstoneEngineerEntity;
import net.mcskill.mobexpansion.entity.redstone.RedstoneEngineerEntity.SummonType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class EngineerCombatGoal extends Goal {
    private static final double MELEE_RANGE = 2.5D;
    private static final double HOLD_DISTANCE = 9.0D;
    private static final double RETREAT_DISTANCE = 6.0D;
    private static final double THROW_RANGE = 14.0D;
    private static final int MELEE_COOLDOWN_TICKS = 22;
    private static final int THROW_COOLDOWN_TICKS = 32;

    private final RedstoneEngineerEntity engineer;
    private int actionCooldown;
    private int repathCooldown;

    public EngineerCombatGoal(RedstoneEngineerEntity engineer) {
        this.engineer = engineer;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (engineer.isBuilding() || engineer.isPerformingAction())
            return false;
        final LivingEntity target = engineer.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || engineer.isBuilding() || engineer.isPerformingAction();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        engineer.getNavigation().stop();
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        final LivingEntity target = engineer.getTarget();
        if (target == null)
            return;

        engineer.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (engineer.isBuilding() || engineer.isPerformingAction()) {
            engineer.getNavigation().stop();
            return;
        }

        if (trySummonMissingConstruct()) {
            actionCooldown = MobExParameters.getInt(engineer, "meleeCooldownTicks", MELEE_COOLDOWN_TICKS);
            return;
        }

        final double meleeRange = MobExParameters.get(engineer, "meleeRange", MELEE_RANGE);
        final double throwRange = MobExParameters.get(engineer, "throwRange", THROW_RANGE);
        final double meleeRangeSqr = meleeRange * meleeRange;
        final double throwRangeSqr = throwRange * throwRange;
        final double distanceSqr = engineer.distanceToSqr(target);
        final boolean hasFrontline = engineer.hasLivingAutomaton() || engineer.hasLivingTower();
        updateMovement(target, distanceSqr, hasFrontline, meleeRangeSqr);

        if (--actionCooldown > 0)
            return;
        if (!engineer.getSensing().hasLineOfSight(target))
            return;

        if (hasFrontline) {
            if (distanceSqr <= throwRangeSqr && distanceSqr > meleeRangeSqr && engineer.tryThrowKey(target)) {
                actionCooldown = MobExParameters.getInt(engineer, "throwCooldownTicks", THROW_COOLDOWN_TICKS);
                return;
            }
            if (distanceSqr <= meleeRangeSqr && engineer.tryMeleeAttack(target))
                actionCooldown = MobExParameters.getInt(engineer, "meleeCooldownTicks", MELEE_COOLDOWN_TICKS);
            return;
        }

        if (distanceSqr <= meleeRangeSqr) {
            if (engineer.tryMeleeAttack(target))
                actionCooldown = MobExParameters.getInt(engineer, "meleeCooldownTicks", MELEE_COOLDOWN_TICKS);
            return;
        }

        if (distanceSqr <= throwRangeSqr && engineer.tryThrowKey(target))
            actionCooldown = MobExParameters.getInt(engineer, "throwCooldownTicks", THROW_COOLDOWN_TICKS);
    }

    private void updateMovement(LivingEntity target, double distanceSqr, boolean hasFrontline, double meleeRangeSqr) {
        final double distance = Math.sqrt(distanceSqr);
        final double retreatDistance = MobExParameters.get(engineer, "retreatDistance", RETREAT_DISTANCE);
        final double holdDistance = MobExParameters.get(engineer, "holdDistance", HOLD_DISTANCE);
        if (hasFrontline) {
            if (distance < retreatDistance) {
                if (--repathCooldown <= 0) {
                    repathCooldown = 8;
                    final Vec3 retreatPos = DefaultRandomPos.getPosAway(engineer, 10, 5, target.position());
                    if (retreatPos != null)
                        engineer.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.15D);
                }
                return;
            }
            if (distance > holdDistance + 2.0D) {
                if (--repathCooldown <= 0) {
                    repathCooldown = 10;
                    engineer.getNavigation().moveTo(target, 0.95D);
                }
                return;
            }
            engineer.getNavigation().stop();
            repathCooldown = 0;
            return;
        }

        if (distanceSqr > meleeRangeSqr) {
            if (--repathCooldown <= 0) {
                repathCooldown = 10;
                engineer.getNavigation().moveTo(target, 1.05D);
            }
            return;
        }
        engineer.getNavigation().stop();
    }

    private boolean trySummonMissingConstruct() {
        if (!engineer.canStartSummon())
            return false;
        if (engineer.tryBeginSummon(SummonType.AUTOMATON))
            return true;
        if (engineer.tryBeginSummon(SummonType.DRONE))
            return true;
        return engineer.tryBeginSummon(SummonType.TOWER);
    }
}
