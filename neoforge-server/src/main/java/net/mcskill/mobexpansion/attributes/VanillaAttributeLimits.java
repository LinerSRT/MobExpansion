package net.mcskill.mobexpansion.attributes;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class VanillaAttributeLimits {
    private VanillaAttributeLimits() {
    }

    public static void expand() {
        raiseMax(Attributes.MAX_HEALTH, EntityAttributeConfig.MAX_HEALTH_CAP);
        raiseMax(Attributes.ATTACK_DAMAGE, EntityAttributeConfig.ATTACK_DAMAGE_CAP);
    }

    private static void raiseMax(Holder<Attribute> attribute, double maxValue) {
        if (!(attribute.value() instanceof RangedAttribute rangedAttribute))
            return;
        if (rangedAttribute.maxValue < maxValue)
            rangedAttribute.maxValue = maxValue;
    }
}
