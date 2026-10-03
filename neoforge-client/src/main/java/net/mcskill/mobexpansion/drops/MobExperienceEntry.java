package net.mcskill.mobexpansion.drops;

import net.minecraft.util.Mth;

import java.util.List;

public record MobExperienceEntry(int minAmount, int maxAmount, float chance, List<LootConditionEntry> conditions) {
    public static final int MIN_XP = 0;
    public static final int MAX_XP = 10000;

    public MobExperienceEntry {
        minAmount = Mth.clamp(minAmount, MIN_XP, MAX_XP);
        maxAmount = Mth.clamp(Math.max(maxAmount, minAmount), MIN_XP, MAX_XP);
        chance = Mth.clamp(chance, MobDropEntry.MIN_CHANCE, MobDropEntry.MAX_CHANCE);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public MobExperienceEntry(int minAmount, int maxAmount, float chance) {
        this(minAmount, maxAmount, chance, List.of());
    }

    public static MobExperienceEntry createDefault() {
        return new MobExperienceEntry(5, 5, 1.0F, List.of());
    }

    public MobExperienceEntry withConditions(List<LootConditionEntry> nextConditions) {
        return new MobExperienceEntry(minAmount, maxAmount, chance, nextConditions);
    }

    public String displayLabel() {
        final String amountText = minAmount == maxAmount ? String.valueOf(minAmount) : minAmount + "-" + maxAmount;
        final String chanceText = Math.round(chance * 100.0F) + "%";
        final String conditionText = conditions.isEmpty() ? "" : " +" + conditions.size();
        return amountText + " XP (" + chanceText + ")" + conditionText;
    }
}
