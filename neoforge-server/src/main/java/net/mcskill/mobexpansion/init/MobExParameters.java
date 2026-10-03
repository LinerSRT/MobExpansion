package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.attributes.EntityAttributeConfig;
import net.mcskill.mobexpansion.attributes.EntityAttributesConfigIO;
import net.mcskill.mobexpansion.attributes.EntityExtraParametersCatalog;
import net.mcskill.mobexpansion.attributes.ExtraParameterDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class MobExParameters {
    private MobExParameters() {
    }

    public static ResourceLocation idOf(Entity entity) {
        if(entity == null)
            return null;
        return idOf(entity.getType());
    }

    public static ResourceLocation idOf(EntityType<?> entityType) {
        if (entityType == null || !BuiltInRegistries.ENTITY_TYPE.containsValue(entityType))
            return null;
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
    }

    public static double get(Entity entity, String key, double fallback) {
        return get(idOf(entity), key, fallback);
    }

    public static double get(ResourceLocation entityId, String key, double fallback) {
        if (entityId == null)
            return fallback;
        final EntityAttributeConfig attributeConfig = EntityAttributesConfigIO.getCached(entityId);
        if (attributeConfig.extras().containsKey(key))
            return attributeConfig.extra(key, fallback);
        final ExtraParameterDefinition definition = EntityExtraParametersCatalog.find(entityId, key);
        return definition != null ? definition.defaultValue() : fallback;
    }

    public static float getFloat(Entity entity, String key, float fallback) {
        return (float) get(entity, key, fallback);
    }

    public static int getInt(Entity entity, String key, int fallback) {
        return (int) Math.round(get(entity, key, fallback));
    }
}
