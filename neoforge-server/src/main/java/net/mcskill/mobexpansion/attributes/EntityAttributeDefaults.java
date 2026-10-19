package net.mcskill.mobexpansion.attributes;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.drops.DropDestination;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityAttributeDefaults {
    private static final Map<ResourceLocation, EntityAttributeConfig> DEFAULTS;

    static {
        final Map<ResourceLocation, EntityAttributeConfig> defaults = new LinkedHashMap<>();
        put(defaults, "regular_spider", 20, 1, 0, 5, 0.32, 0.2, 14);
        put(defaults, "poison_spider", 16, 0, 0, 4, 0.34, 0.2, 16);
        put(defaults, "spider_spawn", 50, 0, 0, 0, 0.0, 1.0, 0);
        put(defaults, "redstone_engineer", 70, 5, 0, 6, 0.27, 0.35, 32);
        put(defaults, "redstone_automaton", 52, 8, 0, 9, 0.30, 0.7, 22);
        put(defaults, "redstone_drone", 14, 1, 0, 4, 0.38, 0.2, 28);
        put(defaults, "redstone_tower", 65, 10, 0, 8, 0.14, 0.9, 24);
        put(defaults, "sentinel_statue", 50, 20, 0, 0, 0.0, 1.0, 0);
        put(defaults, "sentinel_golem", 300, 12, 0, 10, 0.28, 1.0, 32);
        put(defaults, "rat", 30, 0, 0, 5, 0.28, 0.9, 40);
        put(defaults, "mosquito", 20, 0, 0, 5, 0.23, 1.0, 40);
        put(defaults, "leech", 30, 0, 0, 1, 0.18, 1.0, 40);
        put(defaults, "rat_king", 500, 4, 0, 10, 0.29, 1.0, 40);
        put(defaults, "swarmer_ant", 14, 2, 0, 3, 0.33, 0.0, 24);
        put(defaults, "beetle_trooper", 40, 6, 0, 5, 0.25, 0.5, 32);
        put(defaults, "gunship_moth", 60, 4, 0, 6, 0.3, 0.6, 40);
        put(defaults, "huntsman_spider", 45, 4, 0, 8, 0.3, 0.4, 48);
        put(defaults, "ladybug_mortar", 50, 8, 0, 7, 0.22, 0.6, 40);
        put(defaults, "woodlouse_tank", 120, 12, 0, 8, 0.2, 0.9, 32);
        put(defaults, "sculk_crabby", 18, 4, 0, 4, 0.3, 0.2, 24);
        put(defaults, "sculked_crustacean", 40, 10, 0, 7, 0.24, 0.6, 24);
        put(defaults, "sculk_screamer", 60, 4, 0, 7, 0.27, 0.5, 32);
        put(defaults, "sculk_sentinel", 220, 10, 0, 10, 0.25, 1.0, 40);
        put(defaults, "echo_enshrouder", 90, 4, 0, 7, 0.26, 0.5, 40);
        put(defaults, "soul_bearer", 30, 2, 0, 5, 0.25, 0.2, 32);
        put(defaults, "possessed_cinder_armor", 260, 14, 0, 11, 0.27, 0.8, 40);
        put(defaults, "possessed_ruin_armor", 320, 18, 0, 13, 0.24, 1.0, 40);
        put(defaults, "possessed_viking_armor", 280, 14, 0, 12, 0.27, 0.8, 40);
        put(defaults, "apocalypse_type_a", 600, 16, 0, 14, 0.3, 1.0, 48);
        DEFAULTS = Collections.unmodifiableMap(defaults);
    }

    private EntityAttributeDefaults() {
    }

    public static Map<ResourceLocation, EntityAttributeConfig> all() {
        return DEFAULTS;
    }

    public static boolean isModDefault(ResourceLocation entityId) {
        return entityId != null && DEFAULTS.containsKey(entityId);
    }

    public static EntityAttributeConfig get(ResourceLocation entityId) {
        if (isModDefault(entityId))
            return DEFAULTS.get(entityId);
        final EntityAttributeConfig fromType = fromEntityType(entityId);
        if (fromType != null)
            return fromType;
        return new EntityAttributeConfig(30, 2, 0, 6, 0.28, 0.2, 24, EntityExtraParametersCatalog.defaultExtras(entityId));
    }

    @SuppressWarnings("unchecked")
    private static EntityAttributeConfig fromEntityType(ResourceLocation entityId) {
        if (entityId == null)
            return null;
        final EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (entityType == null)
            return null;
        final EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) entityType;
        if (!DefaultAttributes.hasSupplier(livingType))
            return null;
        final AttributeSupplier supplier = DefaultAttributes.getSupplier(livingType);
        return new EntityAttributeConfig(
                attributeValue(supplier, Attributes.MAX_HEALTH, 20.0D),
                attributeValue(supplier, Attributes.ARMOR, 0.0D),
                attributeValue(supplier, Attributes.ARMOR_TOUGHNESS, 0.0D),
                attributeValue(supplier, Attributes.ATTACK_DAMAGE, 0.0D),
                attributeValue(supplier, Attributes.MOVEMENT_SPEED, 0.25D),
                attributeValue(supplier, Attributes.KNOCKBACK_RESISTANCE, 0.0D),
                attributeValue(supplier, Attributes.FOLLOW_RANGE, 16.0D),
                EntityExtraParametersCatalog.defaultExtras(entityId),
                DropDestination.WORLD
        );
    }

    private static double attributeValue(AttributeSupplier supplier, Holder<Attribute> attribute, double fallback) {
        if (!supplier.hasAttribute(attribute))
            return fallback;
        return supplier.getValue(attribute);
    }

    private static void put(Map<ResourceLocation, EntityAttributeConfig> defaults, String path, double maxHealth, double armor, double armorToughness, double attackDamage, double movementSpeed, double knockbackResistance, double followRange) {
        final ResourceLocation entityId = Core.loc(path);
        defaults.put(entityId, new EntityAttributeConfig(
                maxHealth,
                armor,
                armorToughness,
                attackDamage,
                movementSpeed,
                knockbackResistance,
                followRange,
                EntityExtraParametersCatalog.defaultExtras(entityId)
        ));
    }
}
