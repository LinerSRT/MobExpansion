package net.mcskill.mobexpansion.registry;

import net.mcskill.mobexpansion.datagen.ITagHolder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemHolder<T extends Item> extends DeferredItem<T> implements ITagHolder<Item> {
    protected Set<TagKey<Item>> itemTags = new HashSet<>();

    public ItemHolder(ResourceKey<Item> key) {
        super(key);
    }

    @SafeVarargs
    public final ItemHolder<T> addTags(TagKey<Item>... tags) {
        itemTags.addAll(new HashSet<>(List.of(tags)));
        return this;
    }

    @Override
    public Set<TagKey<Item>> tagSet() {
        return itemTags;
    }

    public static <T extends Item> ItemHolder<T> createItem(ResourceKey<Item> key) {
        return new ItemHolder<>(key);
    }
}
