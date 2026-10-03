package net.mcskill.mobexpansion.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class MobExTargeting {
    private MobExTargeting() {
    }

    public static boolean isSurvivalPlayer(LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player))
            return false;
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    public static boolean isValidHostileTarget(LivingEntity livingEntity) {
        return livingEntity != null && livingEntity.isAlive() && isSurvivalPlayer(livingEntity);
    }
}
