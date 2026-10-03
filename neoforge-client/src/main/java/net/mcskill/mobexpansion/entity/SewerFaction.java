package net.mcskill.mobexpansion.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class SewerFaction {
    private SewerFaction() {
    }

    public static boolean isAlly(Entity entity) {
        return entity instanceof SewerMobEntity || entity instanceof SewerPotionProjectileEntity || entity instanceof SewerPoisonAreaEntity || entity instanceof SewerBoltProjectileEntity;
    }

    public static boolean isValidTarget(LivingEntity livingEntity) {
        if (livingEntity == null || !livingEntity.isAlive() || isAlly(livingEntity))
            return false;
        return MobExTargeting.isValidHostileTarget(livingEntity);
    }
}
