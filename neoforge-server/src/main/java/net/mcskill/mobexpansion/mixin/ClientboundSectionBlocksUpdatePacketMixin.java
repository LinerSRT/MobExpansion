package net.mcskill.mobexpansion.mixin;

import net.mcskill.mobexpansion.spawner.MobSpawnerMask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public abstract class ClientboundSectionBlocksUpdatePacketMixin {
    @Shadow
    @Final
    private SectionPos sectionPos;

    @Shadow
    @Final
    private short[] positions;

    @Shadow
    @Final
    private BlockState[] states;

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void maskSpawners(FriendlyByteBuf buffer, CallbackInfo callbackInfo) {
        final var player = MobSpawnerMask.currentTarget();
        if (player == null || MobSpawnerMask.canReveal(player))
            return;
        buffer.writeLong(sectionPos.asLong());
        buffer.writeVarInt(positions.length);
        for (int index = 0; index < positions.length; index++) {
            final BlockPos pos = new BlockPos(sectionPos.relativeToBlockX(positions[index]), sectionPos.relativeToBlockY(positions[index]), sectionPos.relativeToBlockZ(positions[index]));
            final BlockState state = MobSpawnerMask.maskOriginalState(player.serverLevel(), pos, states[index]);
            buffer.writeVarLong((long) Block.getId(state) << 12 | (long) positions[index]);
        }
        callbackInfo.cancel();
    }
}
