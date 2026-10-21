package net.mcskill.mobexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.mcskill.mobexpansion.skin.PlayerSkins;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * {@code /skin <скин> [игроки]} — надеть скин, {@code /skin reset [игроки]} — вернуть обычный вид.
 * Без игроков — на себя. Только для операторов (уровень 2).
 */
public final class SkinCommand {
    private static final String RESET = "reset";
    private static final DynamicCommandExceptionType UNKNOWN_SKIN = new DynamicCommandExceptionType(
            skin -> Component.translatable("commands.mobexpansion.skin.unknown", skin));

    private SkinCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal(RESET)
                        .executes(ctx -> apply(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), PlayerSkins.NONE))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> apply(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), PlayerSkins.NONE))))
                .then(Commands.argument("skin", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PlayerSkins.SKINS, builder))
                        .executes(ctx -> apply(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), skin(ctx.getArgument("skin", String.class))))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> apply(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), skin(ctx.getArgument("skin", String.class)))))));
    }

    private static String skin(String skin) throws CommandSyntaxException {
        if (!PlayerSkins.exists(skin))
            throw UNKNOWN_SKIN.create(skin);
        return skin;
    }

    private static int apply(CommandSourceStack source, Collection<ServerPlayer> targets, String skin) {
        for (ServerPlayer player : targets)
            PlayerSkins.set(player, skin);
        final boolean reset = skin.isEmpty();
        if (targets.size() == 1) {
            final ServerPlayer player = targets.iterator().next();
            source.sendSuccess(() -> reset
                    ? Component.translatable("commands.mobexpansion.skin.reset.single", player.getDisplayName())
                    : Component.translatable("commands.mobexpansion.skin.set.single", player.getDisplayName(), skin), true);
        } else {
            final int count = targets.size();
            source.sendSuccess(() -> reset
                    ? Component.translatable("commands.mobexpansion.skin.reset.multiple", count)
                    : Component.translatable("commands.mobexpansion.skin.set.multiple", skin, count), true);
        }
        return targets.size();
    }
}
