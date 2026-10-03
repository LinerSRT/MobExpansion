package net.mcskill.mobexpansion.registry;

import net.mcskill.mobexpansion.datagen.ITagHolder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockHolder<T extends Block> extends DeferredBlock<T> implements ITagHolder<Block> {
    private Set<TagKey<Block>> blockTags = Set.of();

    public BlockHolder(ResourceKey<Block> key) {
        super(key);
    }

    @SafeVarargs
    public final BlockHolder<T> addTags(TagKey<Block>... tags) {
        blockTags = new HashSet<>(List.of(tags));
        return this;
    }

    public final BlockHolder<T> addTags(Set<TagKey<Block>> tagSet) {
        blockTags = new HashSet<>(tagSet);
        return this;
    }

    @Override
    public Set<TagKey<Block>> tagSet() {
        return blockTags;
    }

    public static <T extends Block> BlockHolder<T> createBlock(ResourceKey<Block> key) {
        return new BlockHolder<>(key);
    }
}
