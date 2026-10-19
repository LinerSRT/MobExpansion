package net.mcskill.mobexpansion.entity.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Боевая цель для {@link AbilityMobEntity}: запускает способности и двигает моба между ними.
 */
public class AbilityCombatGoal extends Goal {
    private final AbilityMobEntity mob;
    private int repathCooldown;
    private int strafeTicks;
    private boolean strafeClockwise;
    private double hoverAngle;

    public AbilityCombatGoal(AbilityMobEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && !mob.isDeadOrDying();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || mob.isUsingAbility();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        hoverAngle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        final LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive())
            return;
        if (mob.isUsingAbility())
            return;

        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (!mob.isBusy()) {
            final Ability ability = mob.pickAbility(target);
            if (ability != null && mob.tryStartAbility(ability, target))
                return;
        }
        move(target);
    }

    private void move(LivingEntity target) {
        switch (mob.combatStyle()) {
            case MELEE -> chase(target);
            case KITE -> kite(target);
            case STATIONARY -> mob.getNavigation().stop();
            case HOVER -> hover(target);
        }
    }

    private void chase(LivingEntity target) {
        if (--repathCooldown > 0)
            return;
        repathCooldown = 8 + mob.getRandom().nextInt(5);
        mob.getNavigation().moveTo(target, mob.chaseSpeed());
    }

    private void kite(LivingEntity target) {
        final double distance = mob.distanceTo(target);
        final boolean canSee = mob.getSensing().hasLineOfSight(target);
        if (distance < mob.preferredMinRange()) {
            if (--repathCooldown > 0 && !mob.getNavigation().isDone())
                return;
            repathCooldown = 10;
            final Vec3 away = DefaultRandomPos.getPosAway(mob, 10, 5, target.position());
            if (away != null)
                mob.getNavigation().moveTo(away.x, away.y, away.z, mob.chaseSpeed() * 1.15D);
            return;
        }
        if (distance > mob.preferredMaxRange() || !canSee) {
            chase(target);
            return;
        }
        // в комфортной зоне — стрейф вокруг цели
        mob.getNavigation().stop();
        if (--strafeTicks <= 0) {
            strafeTicks = 20 + mob.getRandom().nextInt(30);
            strafeClockwise = mob.getRandom().nextBoolean();
        }
        mob.getMoveControl().strafe(0.0F, strafeClockwise ? 0.4F : -0.4F);
        mob.faceTowards(target);
    }

    private void hover(LivingEntity target) {
        hoverAngle += 0.03D;
        final double radius = (mob.preferredMinRange() + mob.preferredMaxRange()) * 0.5D;
        final double height = mob instanceof FlyingAbilityMobEntity flyer ? flyer.hoverHeight() : 5.0D;
        final Vec3 wanted = target.position().add(Math.cos(hoverAngle) * radius, height, Math.sin(hoverAngle) * radius);
        mob.getMoveControl().setWantedPosition(wanted.x, wanted.y, wanted.z, mob.chaseSpeed());
        mob.faceTowards(target);
    }
}
