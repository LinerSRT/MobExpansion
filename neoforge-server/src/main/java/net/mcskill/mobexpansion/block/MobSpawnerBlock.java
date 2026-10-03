package net.mcskill.mobexpansion.block;

import com.mojang.serialization.MapCodec;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.mcskill.mobexpansion.init.MobExCodecs;
import net.mcskill.mobexpansion.init.MobExBlockEntities;
import net.mcskill.mobexpansion.menu.MobSpawnerMenu;
import net.mcskill.mobexpansion.spawner.MobSpawnerMask;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobSpawnerBlock extends BaseEntityBlock {

    public MobSpawnerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MobExCodecs.MOB_SPAWNER_CODEC;
    }

    @Override
    @NotNull
    protected RenderShape getRenderShape(@NotNull BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new MobSpawnerBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState blockState, @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, MobExBlockEntities.MOB_SPAWNER.get(), MobSpawnerBlockEntity::serverTick);
    }

    @Override
    @NotNull
    protected InteractionResult useWithoutItem(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.CONSUME;
        if (!serverPlayer.hasPermissions(4))
            return InteractionResult.CONSUME;
        if (!(level.getBlockEntity(blockPos) instanceof MobSpawnerBlockEntity spawnerBlockEntity))
            return InteractionResult.CONSUME;
        serverPlayer.openMenu(spawnerBlockEntity, buffer -> MobSpawnerMenu.writeInitialData(buffer, spawnerBlockEntity));
        return InteractionResult.CONSUME;
    }

    @Override
    @NotNull
    public SoundType getSoundType(@NotNull BlockState blockState, LevelReader level, @NotNull BlockPos blockPos, @Nullable Entity entity) {
        if (level.getBlockEntity(blockPos) instanceof MobSpawnerBlockEntity spawner) {
            final BlockState maskState = spawner.getMaskBlockState();
            if (maskState != null)
                return maskState.getSoundType(level, blockPos, entity);
        }
        return super.getSoundType(blockState, level, blockPos, entity);
    }

    @Override
    @NotNull
    protected VoxelShape getShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        final VoxelShape maskShape = MobSpawnerMask.maskShape(level, blockPos, context);
        return maskShape == null ? super.getShape(blockState, level, blockPos, context) : maskShape;
    }

    @Override
    @NotNull
    protected VoxelShape getCollisionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        final VoxelShape maskShape = MobSpawnerMask.maskCollisionShape(level, blockPos, context);
        return maskShape == null ? super.getCollisionShape(blockState, level, blockPos, context) : maskShape;
    }
}
