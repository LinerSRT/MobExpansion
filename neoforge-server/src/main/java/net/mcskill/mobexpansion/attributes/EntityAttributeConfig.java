package net.mcskill.mobexpansion.attributes;

import net.mcskill.mobexpansion.drops.DropDestination;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EntityAttributeConfig(
        double maxHealth,
        double armor,
        double armorToughness,
        double attackDamage,
        double movementSpeed,
        double knockbackResistance,
        double followRange,
        Map<String, Double> extras,
        DropDestination dropDestination
) {
    public static final double MAX_HEALTH_CAP = 1_000_000.0D;
    public static final double ATTACK_DAMAGE_CAP = 1_000_000.0D;

    public EntityAttributeConfig(double maxHealth, double armor, double armorToughness, double attackDamage, double movementSpeed, double knockbackResistance, double followRange) {
        this(maxHealth, armor, armorToughness, attackDamage, movementSpeed, knockbackResistance, followRange, Map.of());
    }

    public EntityAttributeConfig(double maxHealth, double armor, double armorToughness, double attackDamage, double movementSpeed, double knockbackResistance, double followRange, Map<String, Double> extras) {
        this(maxHealth, armor, armorToughness, attackDamage, movementSpeed, knockbackResistance, followRange, extras, DropDestination.KILLER_INVENTORY);
    }

    public EntityAttributeConfig {
        extras = extras == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(extras));
        dropDestination = dropDestination == null ? DropDestination.KILLER_INVENTORY : dropDestination;
    }

    public EntityAttributeConfig sanitized() {
        return sanitized(null);
    }

    public EntityAttributeConfig sanitized(ResourceLocation entityId) {
        final Map<String, Double> sanitizedExtras = entityId == null ? Map.copyOf(extras) : EntityExtraParametersCatalog.sanitizeExtras(entityId, extras);
        return new EntityAttributeConfig(clamp(maxHealth, 1.0D, MAX_HEALTH_CAP), clamp(armor, 0.0D, 100.0D), clamp(armorToughness, 0.0D, 100.0D), clamp(attackDamage, 0.0D, ATTACK_DAMAGE_CAP), clamp(movementSpeed, 0.0D, 4.0D), clamp(knockbackResistance, 0.0D, 1.0D), clamp(followRange, 0.0D, 128.0D), sanitizedExtras, dropDestination);
    }

    public EntityAttributeConfig withExtras(Map<String, Double> nextExtras) {
        return new EntityAttributeConfig(maxHealth, armor, armorToughness, attackDamage, movementSpeed, knockbackResistance, followRange, nextExtras, dropDestination);
    }

    public EntityAttributeConfig withDropDestination(DropDestination nextDestination) {
        return new EntityAttributeConfig(maxHealth, armor, armorToughness, attackDamage, movementSpeed, knockbackResistance, followRange, extras, nextDestination);
    }

    public double extra(String key, double fallback) {
        return extras.getOrDefault(key, fallback);
    }

    public void applyTo(LivingEntity livingEntity) {
        final EntityAttributeConfig config = sanitized();
        final float previousMaxHealth = livingEntity.getMaxHealth();
        final float previousHealth = livingEntity.getHealth();
        final boolean wasAtFullHealth = previousHealth >= previousMaxHealth - 1.0E-3F;
        setBase(livingEntity, Attributes.MAX_HEALTH, config.maxHealth);
        setBase(livingEntity, Attributes.ARMOR, config.armor);
        setBase(livingEntity, Attributes.ARMOR_TOUGHNESS, config.armorToughness);
        setBase(livingEntity, Attributes.ATTACK_DAMAGE, config.attackDamage);
        setBase(livingEntity, Attributes.MOVEMENT_SPEED, config.movementSpeed);
        setBase(livingEntity, Attributes.KNOCKBACK_RESISTANCE, config.knockbackResistance);
        setBase(livingEntity, Attributes.FOLLOW_RANGE, config.followRange);
        final float newMaxHealth = livingEntity.getMaxHealth();
        if (wasAtFullHealth || previousHealth > newMaxHealth)
            livingEntity.setHealth(newMaxHealth);
    }

    private static void setBase(LivingEntity livingEntity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        final AttributeInstance instance = livingEntity.getAttribute(attribute);
        if (instance != null)
            instance.setBaseValue(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
