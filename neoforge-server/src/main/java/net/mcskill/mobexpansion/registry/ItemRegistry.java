package net.mcskill.mobexpansion.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class ItemRegistry extends DeferredRegister.Items {
    public ItemRegistry(String namespace) {
        super(namespace);
    }

    public <I extends Item> @NotNull ItemHolder<I> registerItem(@NotNull String name, @NotNull Function<Item.Properties, ? extends I> func, Item.@NotNull Properties props) {
        return (ItemHolder<I>) this.register(name, () -> func.apply(props));
    }

    public <I extends Item> @NotNull ItemHolder<I> registerItem(@NotNull String name, @NotNull Function<Item.Properties, ? extends I> func) {
        return this.registerItem(name, func, new Item.Properties());
    }

    public ItemHolder<Item> registerItem(String name, Item.Properties props) {
        return this.registerItem(name, Item::new, props);
    }

    public ItemHolder<Item> registerItem(String name) {
        return this.registerItem(name, Item::new, new Item.Properties());
    }

    private <I extends BlockItem, U extends Block> ItemHolder<I> registerBlockItem(String name, Function<ResourceLocation, I> func, BlockHolder<U> block) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(func);
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(getNamespace(), name);
        DeferredItem<I> ret = createHolder(getRegistryKey(), key);
        var entries = DeferredRegistryReflect.getEntries(this);
        if(entries == null)
            throw new RuntimeException("Cannot obtain entries in "+getClass().getSimpleName());
        if (entries.putIfAbsent(ret, () -> func.apply(key)) != null) {
            throw new IllegalArgumentException("Duplicate registration " + name);
        }
        return (ItemHolder<I>) ret;
    }

    public <I extends BlockItem, U extends Block> ItemHolder<I> registerBlockItem(String name, BlockHolder<U> block, Supplier<I> sup) {
        return this.registerBlockItem(name, key -> sup.get(), block);
    }


    public <U extends Block> ItemHolder<BlockItem> registerBlockItem(String name, BlockHolder<U> block, Item.Properties properties) {
        return this.registerBlockItem(name, key -> new BlockItem(block.get(), properties), block);
    }

    public <U extends Block> ItemHolder<BlockItem> registerBlockItem(String name, BlockHolder<U> block) {
        return this.registerBlockItem(name, block, new Item.Properties());
    }

    public <U extends Block> ItemHolder<BlockItem> registerBlockItem(BlockHolder<U> block, Item.Properties properties) {
        return this.registerBlockItem(block.unwrapKey().orElseThrow().location().getPath(), block, properties);
    }

    public <U extends Block> ItemHolder<BlockItem> registerBlockItem(BlockHolder<U> block) {
        return this.registerBlockItem(block, new Item.Properties());
    }

    @Override
    @NotNull
    protected <I extends Item> DeferredItem<I> createHolder(@NotNull ResourceKey<? extends Registry<Item>> registryKey, @NotNull ResourceLocation location) {
        ResourceKey<Item> itemKey = ResourceKey.create(registryKey, location);
        return (DeferredItem<I>) new ItemHolder<>(itemKey);
    }

    public List<DeferredHolder<Item, ? extends Item>> getItems() {
        return getEntries().stream().toList();
    }
}
