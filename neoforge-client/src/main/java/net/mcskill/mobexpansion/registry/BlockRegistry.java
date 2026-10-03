package net.mcskill.mobexpansion.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class BlockRegistry extends DeferredRegister<Block> {
    public BlockRegistry(String namespace) {
        super(Registries.BLOCK, namespace);
    }

    @NotNull
    public <B extends Block> BlockHolder<B> registerBlock(@NotNull String name, @NotNull Function<BlockBehaviour.Properties, ? extends B> blockCreator, BlockBehaviour.@NotNull Properties properties) {
        return (BlockHolder<B>) this.register(name, () -> blockCreator.apply(properties));
    }

    @Override
    @NotNull
    protected <I extends Block> DeferredBlock<I> createHolder(@NotNull ResourceKey<? extends Registry<Block>> registryKey, @NotNull ResourceLocation location) {
        return BlockHolder.createBlock(ResourceKey.create(registryKey, location));
    }

    public List<DeferredHolder<Block, ? extends Block>> getBlocks() {
        return getEntries().stream().toList();
    }
}
