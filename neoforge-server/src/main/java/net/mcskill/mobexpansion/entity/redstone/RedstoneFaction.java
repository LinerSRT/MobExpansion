package net.mcskill.mobexpansion.entity.redstone;

import net.mcskill.mobexpansion.entity.MobExTargeting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class RedstoneFaction {
    private RedstoneFaction() {
    }

    public static boolean isAlly(Entity entity) {
        return entity instanceof RedstoneEngineerEntity || entity instanceof RedstoneConstructEntity || entity instanceof RedstoneProjectileEntity || entity instanceof WrenchProjectileEntity;
    }

    public static boolean isValidTarget(LivingEntity livingEntity) {
        if (livingEntity == null || !livingEntity.isAlive() || isAlly(livingEntity))
            return false;
        return MobExTargeting.isValidHostileTarget(livingEntity);
    }
}
