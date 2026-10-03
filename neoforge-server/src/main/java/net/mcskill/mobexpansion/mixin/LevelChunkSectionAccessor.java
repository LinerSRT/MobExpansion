package net.mcskill.mobexpansion.mixin;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelChunkSection.class)
public interface LevelChunkSectionAccessor {
    @Accessor("states")
    PalettedContainer<BlockState> chunkStates();

    @Accessor("biomes")
    PalettedContainerRO<Holder<Biome>> chunkBiomes();

    @Accessor("nonEmptyBlockCount")
    short chunNonEmptyBlockCount();
}
