package net.mcskill.mobexpansion.drops;

import java.util.ArrayList;
import java.util.List;


public record EntityLootConfig(List<MobDropEntry> drops, List<MobExperienceEntry> experience, boolean replaceVanilla) {
    public static final EntityLootConfig EMPTY = new EntityLootConfig(List.of(), List.of(), false);

    public EntityLootConfig(List<MobDropEntry> drops, List<MobExperienceEntry> experience) {
        this(drops, experience, false);
    }

    public EntityLootConfig {
        drops = drops == null ? List.of() : List.copyOf(drops);
        experience = experience == null ? List.of() : List.copyOf(experience);
    }

    public boolean isEmpty() {
        return drops.isEmpty() && experience.isEmpty() && !replaceVanilla;
    }

    public EntityLootConfig withDrops(List<MobDropEntry> nextDrops) {
        return new EntityLootConfig(nextDrops, experience, replaceVanilla);
    }

    public EntityLootConfig withExperience(List<MobExperienceEntry> nextExperience) {
        return new EntityLootConfig(drops, nextExperience, replaceVanilla);
    }

    public EntityLootConfig withReplaceVanilla(boolean nextReplaceVanilla) {
        return new EntityLootConfig(drops, experience, nextReplaceVanilla);
    }

    public static EntityLootConfig ofDrops(List<MobDropEntry> dropEntries) {
        return new EntityLootConfig(dropEntries, List.of(), false);
    }

    public EntityLootConfig mutableCopy() {
        return new EntityLootConfig(new ArrayList<>(drops), new ArrayList<>(experience), replaceVanilla);
    }
}
