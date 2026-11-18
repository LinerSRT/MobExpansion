package net.mcskill.mobexpansion.client.cosmetics;

import net.mcskill.mobexpansion.client.skin.ClientSkins;
import net.mcskill.mobexpansion.network.CosmeticsSelectPacket;
import net.mcskill.mobexpansion.network.CosmeticsStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Скины и шлейфы для GUI других модов: вкладка «Кастомизация» меню игрока бункера (MsBunker) вызывает это рефлексией —
 * зависимости на MobExpansion у неё нет, поэтому сигнатуры только из простых и ванильных типов, и менять их нельзя
 * без правки MsBunker ({@code client/MobExCosmetics}). Категории — {@code "skin"} и {@code "trail"}.
 * <p>
 * Состояние то же, что у GUI на H: запрос {@link #request()}, ответ приходит асинхронно (шлейфы собираются из MSEss),
 * {@link #version()} растёт с каждым ответом. Права и сам выбор проверяет сервер ({@code cosmetics.Cosmetics}).
 */
public final class CosmeticsApi {
    private static final Map<ResourceLocation, Boolean> TEXTURES = new HashMap<>();

    private CosmeticsApi() {
    }

    /** Запросить у сервера, что игрок может надеть. */
    public static void request() {
        CosmeticsClient.request();
    }

    /** Номер последнего состояния с сервера; не меняется — нечего перечитывать. Пока ответа не было, {@link #loaded()} — false. */
    public static int version() {
        return CosmeticsClient.version();
    }

    public static boolean loaded() {
        return CosmeticsClient.state() != null;
    }

    /** id, доступные игроку, в порядке каталога. */
    public static List<String> unlocked(String category) {
        final List<String> ids = new ArrayList<>();
        for (CosmeticsStatePacket.Entry entry : entries(category))
            if (entry.unlocked())
                ids.add(entry.id());
        return ids;
    }

    /** Что надето ({@code ""} — ничего). */
    public static String current(String category) {
        final CosmeticsStatePacket state = CosmeticsClient.state();
        final CosmeticsSelectPacket.Category parsed = parse(category);
        if (state == null || parsed == null)
            return "";
        return switch (parsed) {
            case SKIN -> state.skin();
            case TRAIL -> state.trail();
            case PREFIX -> state.prefix();
        };
    }

    /** Надеть ({@code id} пустой — снять). Новое состояние придёт с сервера. */
    public static void select(String category, String id) {
        final CosmeticsSelectPacket.Category parsed = parse(category);
        if (parsed != null)
            CosmeticsClient.select(parsed, id);
    }

    public static Component name(String category, String id) {
        return Component.translatableWithFallback("cosmetics.mobexpansion." + category + "." + id, id);
    }

    /** Нарисовать игрока в скине {@code skin} ({@code ""} — обычный вид), пока выполняется {@code render}. */
    public static void renderWithSkin(Player player, String skin, Runnable render) {
        ClientSkins.renderWithSkin(player, skin, render);
    }

    /** Текстура шлейфа MSEss (1024x1024) или null, если её нет. */
    @Nullable
    public static ResourceLocation trailTexture(String id) {
        final ResourceLocation texture = ResourceLocation.tryParse("msess:trails/" + id + ".png");
        if (texture == null)
            return null;
        return TEXTURES.computeIfAbsent(texture,
                location -> Minecraft.getInstance().getResourceManager().getResource(location).isPresent()) ? texture : null;
    }

    private static List<CosmeticsStatePacket.Entry> entries(String category) {
        final CosmeticsStatePacket state = CosmeticsClient.state();
        final CosmeticsSelectPacket.Category parsed = parse(category);
        if (state == null || parsed == null)
            return List.of();
        return switch (parsed) {
            case SKIN -> state.skins();
            case TRAIL -> state.trails();
            case PREFIX -> state.prefixes();
        };
    }

    @Nullable
    private static CosmeticsSelectPacket.Category parse(String category) {
        try {
            return CosmeticsSelectPacket.Category.valueOf(category.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
