package net.mcskill.mobexpansion.datagen;

import net.minecraft.tags.TagKey;

import java.util.Set;

public interface ITagHolder<T> {
    Set<TagKey<T>> tagSet();
}
