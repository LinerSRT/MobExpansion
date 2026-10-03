package net.mcskill.mobexpansion.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData$BlockEntityInfo")
public interface ChunkBlockEntityInfoAccessor {
    @Accessor("type")
    BlockEntityType<?> entityType();

    @Accessor("tag")
    CompoundTag entityTag();
}
