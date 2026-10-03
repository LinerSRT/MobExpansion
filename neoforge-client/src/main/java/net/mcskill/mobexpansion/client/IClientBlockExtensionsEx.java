package net.mcskill.mobexpansion.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;

public interface IClientBlockExtensionsEx extends IClientBlockExtensions {
    @Nullable
    default VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return null;
    }

    @Nullable
    default VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Nullable
    default VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, CollisionContext.empty());
    }
}
