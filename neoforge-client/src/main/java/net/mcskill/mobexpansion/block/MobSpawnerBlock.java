package net.mcskill.mobexpansion.block;

import com.mojang.serialization.MapCodec;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.mcskill.mobexpansion.client.MobSpawnerRevealHandler;
import net.mcskill.mobexpansion.init.MobExCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
        return null;
    }

    @Override
    @NotNull
    protected InteractionResult useWithoutItem(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull RandomSource random) {
        if (!MobSpawnerRevealHandler.isRevealing() && level.getBlockEntity(blockPos) instanceof MobSpawnerBlockEntity spawner) {
            final BlockState maskState = spawner.getMaskBlockState();
            if (maskState != null) {
                maskState.getBlock().animateTick(maskState, level, blockPos, random);
                return;
            }
        }
        super.animateTick(blockState, level, blockPos, random);
    }

    @Override
    @NotNull
    public SoundType getSoundType(@NotNull BlockState blockState, LevelReader level, BlockPos blockPos, @Nullable Entity entity) {
        if (!MobSpawnerRevealHandler.isRevealing() && level.getBlockEntity(blockPos) instanceof MobSpawnerBlockEntity spawner) {
            final BlockState maskState = spawner.getMaskBlockState();
            if (maskState != null)
                return maskState.getSoundType(level, blockPos, entity);
        }
        return super.getSoundType(blockState, level, blockPos, entity);
    }
}
