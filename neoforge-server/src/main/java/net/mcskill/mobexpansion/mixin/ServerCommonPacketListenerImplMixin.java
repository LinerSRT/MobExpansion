package net.mcskill.mobexpansion.mixin;

import net.mcskill.mobexpansion.spawner.MobSpawnerMask;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
    private void pushTarget(Packet<?> packet, PacketSendListener listener, CallbackInfo callbackInfo) {
        ServerCommonPacketListenerImpl packetListener = (ServerCommonPacketListenerImpl) (Object) this;
        if (packetListener instanceof ServerGamePacketListenerImpl gamePacketListener) {
            final Packet<?> filtered = MobSpawnerMask.filterOutgoing(gamePacketListener.player, packet);
            if (filtered != packet) {
                callbackInfo.cancel();
                if (filtered != null)
                    packetListener.send(filtered, listener);
                return;
            }
            MobSpawnerMask.pushTarget(gamePacketListener.player);
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("RETURN"))
    private void popTarget(Packet<?> packet, PacketSendListener listener, CallbackInfo callbackInfo) {
        MobSpawnerMask.popTarget();
    }
}
