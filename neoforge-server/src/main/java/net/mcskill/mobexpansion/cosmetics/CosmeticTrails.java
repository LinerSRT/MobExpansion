package net.mcskill.mobexpansion.cosmetics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import net.mcskill.mobexpansion.init.MobExAttachments;
import net.mcskill.mobexpansion.network.CosmeticsStatePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Шлейфы MSEss без зависимости от MSEss: всё через его команду {@code /trail}.
 * <ul>
 *     <li>весь каталог — подсказки {@code /trail grant <ник> } от консоли;</li>
 *     <li>доступные игроку — подсказки {@code /trail } от имени игрока (MSEss сам учитывает права, премиум и фракцию);</li>
 *     <li>надеть/снять — {@code /trail <id>} / {@code /trail off} от имени игрока (проверки и сообщения — MSEss).</li>
 * </ul>
 * Что надето, MSEss наружу не отдаёт, поэтому GUI помнит свой последний выбор ({@link MobExAttachments#COSMETIC_TRAIL}).
 * Если MSEss нет (команды {@code trail} нет) — шлейфов нет.
 */
public final class CosmeticTrails {
    private static final String COMMAND = "trail";
    /** Подкоманды /trail, которые попадают в подсказки рядом с id шлейфов. */
    private static final Set<String> SUBCOMMANDS = Set.of("list", "off", "disable", "set", "grant", "revoke", "equip");
    private static final long TIMEOUT_SECONDS = 5;

    private CosmeticTrails() {
    }

    public static boolean available(MinecraftServer server) {
        return server.getCommands().getDispatcher().getRoot().getChild(COMMAND) != null;
    }

    /** Каталог шлейфов с доступностью для игрока. Завершается не на серверном потоке (подсказки MSEss асинхронные). */
    public static CompletableFuture<List<CosmeticsStatePacket.Entry>> catalog(ServerPlayer player) {
        final MinecraftServer server = player.server;
        if (!available(server))
            return CompletableFuture.completedFuture(List.of());
        final CompletableFuture<List<String>> all = suggest(server, server.createCommandSourceStack(),
                COMMAND + " grant " + player.getGameProfile().getName() + " ");
        final CompletableFuture<List<String>> usable = suggest(server, player.createCommandSourceStack(), COMMAND + " ");
        return all.thenCombine(usable, (catalog, open) -> {
                    // если консоли подсказки не дали — хотя бы доступные игроку
                    final Set<String> ids = new LinkedHashSet<>(catalog.isEmpty() ? open : catalog);
                    ids.addAll(open);
                    final List<CosmeticsStatePacket.Entry> entries = new ArrayList<>();
                    for (String id : ids)
                        entries.add(new CosmeticsStatePacket.Entry(id, open.contains(id)));
                    return entries;
                })
                .completeOnTimeout(List.of(), TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(error -> List.of());
    }

    private static CompletableFuture<List<String>> suggest(MinecraftServer server, CommandSourceStack source, String input) {
        try {
            final CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            final ParseResults<CommandSourceStack> parse = dispatcher.parse(input, source);
            return dispatcher.getCompletionSuggestions(parse).thenApply(suggestions -> {
                final List<String> ids = new ArrayList<>();
                for (Suggestion suggestion : suggestions.getList()) {
                    final String id = suggestion.getText();
                    if (!SUBCOMMANDS.contains(id) && !ids.contains(id))
                        ids.add(id);
                }
                return ids;
            });
        } catch (RuntimeException error) {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    public static String current(ServerPlayer player) {
        return player.getData(MobExAttachments.COSMETIC_TRAIL);
    }

    /** Надеть шлейф ({@code id} пустой — снять). Доступ проверяет MSEss и сам пишет игроку результат. */
    public static void select(ServerPlayer player, String id) {
        if (!available(player.server))
            return;
        final String command = id.isEmpty() ? COMMAND + " off" : COMMAND + " " + id;
        player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
        player.setData(MobExAttachments.COSMETIC_TRAIL, id);
    }
}
