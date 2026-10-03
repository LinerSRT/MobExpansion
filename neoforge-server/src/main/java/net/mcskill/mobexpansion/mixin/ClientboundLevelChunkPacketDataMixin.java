package net.mcskill.mobexpansion.mixin;

import io.netty.buffer.Unpooled;
import net.mcskill.mobexpansion.spawner.MobSpawnerMask;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientboundLevelChunkPacketData.class)
public abstract class ClientboundLevelChunkPacketDataMixin {
    @Shadow
    @Final
    @Mutable
    private byte[] buffer;

    @Shadow
    @Final
    private List<?> blockEntitiesData;

    @Unique
    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("RETURN"))
    private void maskSpawner(LevelChunk chunk, CallbackInfo callbackInfo) {
        if (!MobSpawnerMask.shouldHide())
            return;
        final FriendlyByteBuf packed = new FriendlyByteBuf(Unpooled.buffer());
        MobSpawnerMask.writeCamouflagedChunk(packed, chunk);
        buffer = new byte[packed.readableBytes()];
        packed.readBytes(buffer);
        blockEntitiesData.removeIf(MobSpawnerMask::isMaskedSpawnerInfo);
    }
}
