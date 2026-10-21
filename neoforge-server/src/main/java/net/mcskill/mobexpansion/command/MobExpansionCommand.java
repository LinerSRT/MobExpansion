package net.mcskill.mobexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /mobexpansion spawnall <радиус>} — для тестов: все спавнеры мобов в радиусе (в загруженных чанках)
 * сразу спавнят волну, не дожидаясь КД.
 */
public final class MobExpansionCommand {
    public static final int MAX_RADIUS = 256;

    private MobExpansionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mobexpansion")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawnall")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                                .executes(ctx -> spawnAll(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static int spawnAll(CommandSourceStack source, int radius) {
        final ServerLevel level = source.getLevel();
        final BlockPos center = BlockPos.containing(source.getPosition());
        final double radiusSqr = (double) radius * radius;
        final List<MobSpawnerBlockEntity> spawners = new ArrayList<>();
        final int minChunkX = (center.getX() - radius) >> 4, maxChunkX = (center.getX() + radius) >> 4;
        final int minChunkZ = (center.getZ() - radius) >> 4, maxChunkZ = (center.getZ() + radius) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null)
                    continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof MobSpawnerBlockEntity spawner
                            && spawner.getBlockPos().distSqr(center) <= radiusSqr)
                        spawners.add(spawner);
                }
            }
        }
        int active = 0;
        for (MobSpawnerBlockEntity spawner : spawners) {
            if (spawner.forceSpawn(level) > 0)
                active++;
        }
        final int total = spawners.size(), spawned = active;
        source.sendSuccess(() -> Component.translatable("commands.mobexpansion.spawnall", spawned, total, radius), true);
        return spawned;
    }
}
