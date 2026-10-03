package net.mcskill.mobexpansion.datagen;

import net.mcskill.mobexpansion.Core;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "rawtypes"})
public class GenericTagProvider<T> extends IntrinsicHolderTagsProvider<T> {
    private final List<DeferredHolder<T, ? extends T>> registered;

    public GenericTagProvider(PackOutput output, ResourceKey<? extends Registry<T>> key, Function<T, ResourceKey<T>> func, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper, List<DeferredHolder<T, ? extends T>> registered) {
        super(output, key, lookupProvider, func, Core.MODID, existingFileHelper);
        this.registered = registered;
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        for (DeferredHolder<T, ? extends T> entry : registered) {
            if (entry instanceof ITagHolder tags) {
                Set<TagKey<T>> tag = tags.tagSet();
                if (tag != null)
                    tag.forEach(t -> tag(t).add(entry.get()));
            }
        }
    }
}