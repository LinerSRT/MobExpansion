package net.mcskill.mobexpansion.drops;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public record MobDropEntry(ResourceLocation itemId, int minCount, int maxCount, float chance, List<LootConditionEntry> conditions) {
    public static final int MIN_STACK = 1;
    public static final int MAX_STACK = 64;
    public static final float MIN_CHANCE = 0.0F;
    public static final float MAX_CHANCE = 1.0F;

    public MobDropEntry {
        minCount = Mth.clamp(minCount, MIN_STACK, MAX_STACK);
        maxCount = Mth.clamp(Math.max(maxCount, minCount), MIN_STACK, MAX_STACK);
        chance = Mth.clamp(chance, MIN_CHANCE, MAX_CHANCE);
        if (itemId == null)
            itemId = BuiltInRegistries.ITEM.getKey(Items.AIR);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public MobDropEntry(ResourceLocation itemId, int minCount, int maxCount, float chance) {
        this(itemId, minCount, maxCount, chance, List.of());
    }

    public static MobDropEntry createDefault() {
        return new MobDropEntry(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT), 1, 1, 1.0F, List.of());
    }

    public MobDropEntry withConditions(List<LootConditionEntry> nextConditions) {
        return new MobDropEntry(itemId, minCount, maxCount, chance, nextConditions);
    }

    public String displayLabel() {
        final Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        final String itemName = item == Items.AIR ? itemId.toString() : item.getDescription().getString();
        final String conditionText = conditions.isEmpty() ? "" : " +" + conditions.size();
        return itemName + " x" + minCount + "-" + maxCount + " (" + Math.round(chance * 100.0F) + "%)" + conditionText;
    }
}
