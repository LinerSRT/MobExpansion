package net.mcskill.mobexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.mcskill.mobexpansion.clearance.Clearance;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.function.IntBinaryOperator;

/**
 * {@code /clearance get [игрок]}, {@code /clearance set <игроки> <уровень>}, {@code /clearance add <игроки> <число>}.
 */
public final class ClearanceCommand {
    private ClearanceCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clearance")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get")
                        .executes(ctx -> get(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("level", IntegerArgumentType.integer(Clearance.MIN_LEVEL, Clearance.MAX_LEVEL))
                                        .executes(ctx -> change(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                                IntegerArgumentType.getInteger(ctx, "level"), (current, value) -> value)))))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-Clearance.MAX_LEVEL, Clearance.MAX_LEVEL))
                                        .executes(ctx -> change(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                                IntegerArgumentType.getInteger(ctx, "amount"), Integer::sum))))));
    }

    private static int get(CommandSourceStack source, ServerPlayer player) {
        final int level = Clearance.get(player);
        source.sendSuccess(() -> Component.translatable("commands.mobexpansion.clearance.get", player.getDisplayName(), level, Clearance.MAX_LEVEL), false);
        return level;
    }

    private static int change(CommandSourceStack source, Collection<ServerPlayer> targets, int value, IntBinaryOperator operation) {
        int last = 0;
        for (ServerPlayer player : targets) {
            final int before = Clearance.get(player);
            last = Clearance.set(player, operation.applyAsInt(before, value));
            if (last != before)
                player.sendSystemMessage(Component.translatable("message.mobexpansion.clearance.changed", last)
                        .withStyle(ChatFormatting.GREEN));
        }
        if (targets.size() == 1) {
            final ServerPlayer player = targets.iterator().next();
            final int level = last;
            source.sendSuccess(() -> Component.translatable("commands.mobexpansion.clearance.set.single", player.getDisplayName(), level), true);
        } else {
            final int count = targets.size();
            source.sendSuccess(() -> Component.translatable("commands.mobexpansion.clearance.set.multiple", count), true);
        }
        return targets.size();
    }
}
