package net.mcskill.mobexpansion.cosmetics;

import net.mcskill.mobexpansion.network.CosmeticsRequestPacket;
import net.mcskill.mobexpansion.network.CosmeticsSelectPacket;
import net.mcskill.mobexpansion.network.CosmeticsStatePacket;
import net.mcskill.mobexpansion.network.OpenCosmeticsPacket;
import net.mcskill.mobexpansion.skin.PlayerSkins;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI кастомизации персонажа, серверная часть: что игроку доступно и выбор карточек.
 * Скины — свои ({@link PlayerSkins}, права {@code cosmetics.skin.<id>}), шлейфы — MSEss ({@link CosmeticTrails}).
 * Новая строка GUI (скины предметов, питомцы) — новая {@link CosmeticsSelectPacket.Category}, список в
 * {@link CosmeticsStatePacket} и ветка в {@link #select}.
 */
public final class Cosmetics {
    private Cosmetics() {
    }

    /** Подставляет серверные обработчики в пакеты GUI (вызывается при старте сервера). */
    public static void installHandlers() {
        CosmeticsRequestPacket.serverHandler = Cosmetics::sendState;
        CosmeticsSelectPacket.serverHandler = (player, packet) -> select(player, packet.category(), packet.id());
    }

    /** /cosmetics — открыть GUI у игрока. */
    public static void open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, OpenCosmeticsPacket.INSTANCE);
    }

    /** Отправить игроку состояние GUI. Шлейфы собираются асинхронно, пакет уходит с серверного потока. */
    public static void sendState(ServerPlayer player) {
        final MinecraftServer server = player.server;
        CosmeticTrails.catalog(player).thenAccept(trails -> server.execute(() -> {
            if (player.hasDisconnected())
                return;
            PacketDistributor.sendToPlayer(player, new CosmeticsStatePacket(skins(player), trails,
                    PlayerSkins.get(player), CosmeticTrails.current(player), CosmeticTrails.available(server)));
        }));
    }

    private static List<CosmeticsStatePacket.Entry> skins(ServerPlayer player) {
        final List<CosmeticsStatePacket.Entry> entries = new ArrayList<>();
        for (String skin : PlayerSkins.SKINS)
            entries.add(new CosmeticsStatePacket.Entry(skin, CosmeticPermissions.canUseSkin(player, skin)));
        return entries;
    }

    public static void select(ServerPlayer player, CosmeticsSelectPacket.Category category, String id) {
        switch (category) {
            case SKIN -> {
                if (id.isEmpty())
                    PlayerSkins.set(player, PlayerSkins.NONE);
                else if (PlayerSkins.exists(id) && CosmeticPermissions.canUseSkin(player, id))
                    PlayerSkins.set(player, id);
                else
                    denied(player);
                sendState(player);
            }
            case TRAIL -> {
                if (id.isEmpty()) {
                    CosmeticTrails.select(player, id);
                    sendState(player);
                    return;
                }
                // доступ — по свежему каталогу MSEss: иначе GUI запомнит шлейф, который MSEss не надел
                final MinecraftServer server = player.server;
                CosmeticTrails.catalog(player).thenAccept(trails -> server.execute(() -> {
                    if (player.hasDisconnected())
                        return;
                    if (trails.contains(new CosmeticsStatePacket.Entry(id, true)))
                        CosmeticTrails.select(player, id);
                    else
                        denied(player);
                    sendState(player);
                }));
            }
        }
    }

    private static void denied(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.mobexpansion.cosmetics.locked").withStyle(ChatFormatting.RED), true);
    }
}
