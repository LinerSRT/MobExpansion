package net.mcskill.mobexpansion.mixin;

import net.mcskill.mobexpansion.spawner.MobSpawnerMask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {
    @Inject(method = "sendChunk", at = @At("HEAD"))
    private static void pushTargetPlayer(ServerGamePacketListenerImpl packetListener, ServerLevel level, LevelChunk chunk, CallbackInfo callbackInfo) {
        MobSpawnerMask.pushTarget(packetListener.player);
    }

    @Inject(method = "sendChunk", at = @At("RETURN"))
    private static void popTargetPlayer(ServerGamePacketListenerImpl packetListener, ServerLevel level, LevelChunk chunk, CallbackInfo callbackInfo) {
        MobSpawnerMask.popTarget();
    }
}
