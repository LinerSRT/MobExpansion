package net.mcskill.mobexpansion.client;

import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MobSpawnerClientExtensions implements IClientBlockExtensionsEx {
    public static final MobSpawnerClientExtensions INSTANCE = new MobSpawnerClientExtensions();

    private MobSpawnerClientExtensions() {
    }

    @Nullable
    private static BlockState maskAt(BlockGetter level, BlockPos pos) {
        if (level instanceof Level world && world.isClientSide() && MobSpawnerRevealHandler.isRevealing())
            return null;
        if (!(level.getBlockEntity(pos) instanceof MobSpawnerBlockEntity spawner))
            return null;
        return spawner.getMaskBlockState();
    }

    @Override
    @Nullable
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        final BlockState maskState = maskAt(level, pos);
        return maskState == null ? null : maskState.getShape(level, pos, context);
    }

    @Override
    @Nullable
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        final BlockState maskState = maskAt(level, pos);
        return maskState == null ? null : maskState.getCollisionShape(level, pos, context);
    }

    @Override
    public boolean addHitEffects(@NotNull BlockState blockState, @NotNull Level level, @NotNull HitResult hitResult, @NotNull ParticleEngine particleEngine) {
        if (!(level instanceof ClientLevel clientLevel) || !(hitResult instanceof BlockHitResult hit))
            return false;
        final BlockState maskState = maskAt(level, hit.getBlockPos());
        if (maskState == null)
            return false;
        spawnCrackParticles(clientLevel, hit.getBlockPos(), hit.getDirection(), maskState, particleEngine);
        return true;
    }

    @Override
    public boolean addDestroyEffects(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull ParticleEngine particleEngine) {
        final BlockState maskState = maskAt(level, blockPos);
        if (maskState == null)
            return false;
        particleEngine.destroy(blockPos, maskState);
        return true;
    }

    @Override
    public boolean playBreakSound(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos) {
        final BlockState maskState = maskAt(level, blockPos);
        if (maskState == null)
            return false;
        final SoundType soundType = maskState.getSoundType(level, blockPos, null);
        level.playLocalSound(blockPos, soundType.getBreakSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false);
        return true;
    }

    @Override
    public boolean areBreakingParticlesTinted(@NotNull BlockState state, @NotNull ClientLevel level, @NotNull BlockPos blockPos) {
        final BlockState maskState = maskAt(level, blockPos);
        if (maskState == null)
            return IClientBlockExtensionsEx.super.areBreakingParticlesTinted(state, level, blockPos);
        return IClientBlockExtensions.of(maskState).areBreakingParticlesTinted(maskState, level, blockPos);
    }

    private static void spawnCrackParticles(ClientLevel level, BlockPos pos, Direction side, BlockState maskState, ParticleEngine manager) {
        if (maskState.getRenderShape() == RenderShape.INVISIBLE)
            return;
        final VoxelShape shape = maskState.getShape(level, pos);
        if (shape.isEmpty())
            return;
        final AABB bounds = shape.bounds();
        double particleX = pos.getX() + level.random.nextDouble() * (bounds.maxX - bounds.minX - 0.2F) + 0.1F + bounds.minX;
        double particleY = pos.getY() + level.random.nextDouble() * (bounds.maxY - bounds.minY - 0.2F) + 0.1F + bounds.minY;
        double particleZ = pos.getZ() + level.random.nextDouble() * (bounds.maxZ - bounds.minZ - 0.2F) + 0.1F + bounds.minZ;
        switch (side) {
            case DOWN -> particleY = pos.getY() + bounds.minY - 0.1F;
            case UP -> particleY = pos.getY() + bounds.maxY + 0.1F;
            case NORTH -> particleZ = pos.getZ() + bounds.minZ - 0.1F;
            case SOUTH -> particleZ = pos.getZ() + bounds.maxZ + 0.1F;
            case WEST -> particleX = pos.getX() + bounds.minX - 0.1F;
            case EAST -> particleX = pos.getX() + bounds.maxX + 0.1F;
        }
        manager.add(
                new TerrainParticle(level, particleX, particleY, particleZ, 0.0D, 0.0D, 0.0D, maskState, pos)
                        .updateSprite(maskState, pos)
                        .setPower(0.2F)
                        .scale(0.6F)
        );
    }
}
