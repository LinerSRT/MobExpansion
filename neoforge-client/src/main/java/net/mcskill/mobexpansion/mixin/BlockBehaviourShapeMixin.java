package net.mcskill.mobexpansion.mixin;

import net.mcskill.mobexpansion.client.IClientBlockExtensionsEx;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourShapeMixin {
    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    @Unique
    private void extraShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        final VoxelShape shape = extraShape(state, level, pos, context, false);
        if (shape != null)
            cir.setReturnValue(shape);
    }

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    @Unique
    private void extraCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        final VoxelShape shape = extraShape(state, level, pos, context, true);
        if (shape != null)
            cir.setReturnValue(shape);
    }

    @Inject(method = "getOcclusionShape", at = @At("HEAD"), cancellable = true)
    @Unique
    private void extraOcclusionShape(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        Block block = (Block) (Object) this;
        if (!(IClientBlockExtensions.of(block) instanceof IClientBlockExtensionsEx extra))
            return;
        final VoxelShape shape = extra.getOcclusionShape(state, level, pos);
        if (shape != null)
            cir.setReturnValue(shape);
    }

    @Nullable
    @Unique
    private VoxelShape extraShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, boolean collision) {
        Block block = (Block) (Object) this;
        if (!(IClientBlockExtensions.of(block) instanceof IClientBlockExtensionsEx extra))
            return null;
        return collision
                ? extra.getCollisionShape(state, level, pos, context)
                : extra.getShape(state, level, pos, context);
    }
}
