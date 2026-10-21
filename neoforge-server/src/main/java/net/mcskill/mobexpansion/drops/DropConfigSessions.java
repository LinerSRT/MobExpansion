package net.mcskill.mobexpansion.drops;

import net.mcskill.mobexpansion.attributes.EntityAttributeConfig;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Что сервер отправил игроку при открытии конфигуратора дропа. GUI присылает при сохранении все сущности
 * разом, поэтому без этого снимка окно, открытое раньше чужого (или своего) сохранения, откатывало бы
 * сделанные с тех пор правки. С ним перезаписываются только сущности, которые игрок реально изменил.
 */
public final class DropConfigSessions {
    public record Snapshot(Map<ResourceLocation, EntityLootConfig> loot, Map<ResourceLocation, EntityAttributeConfig> attributes) {
    }

    private static final Map<UUID, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private DropConfigSessions() {
    }

    public static void open(UUID playerId, Map<ResourceLocation, EntityLootConfig> loot, Map<ResourceLocation, EntityAttributeConfig> attributes) {
        SNAPSHOTS.put(playerId, new Snapshot(Map.copyOf(loot), Map.copyOf(attributes)));
    }

    @Nullable
    public static Snapshot take(UUID playerId) {
        return SNAPSHOTS.remove(playerId);
    }

    public static void clear(UUID playerId) {
        SNAPSHOTS.remove(playerId);
    }

    /**
     * Трёхстороннее слияние: берём текущее состояние и накладываем только то, что игрок изменил
     * относительно снимка при открытии.
     */
    public static <V> Map<ResourceLocation, V> merge(
            Map<ResourceLocation, V> current,
            Map<ResourceLocation, V> base,
            Map<ResourceLocation, V> incoming,
            Function<ResourceLocation, V> fallback
    ) {
        final Map<ResourceLocation, V> result = new LinkedHashMap<>(current);
        final Set<ResourceLocation> ids = new LinkedHashSet<>(base.keySet());
        ids.addAll(incoming.keySet());
        for (ResourceLocation id : ids) {
            final V next = incoming.containsKey(id) ? incoming.get(id) : fallback.apply(id);
            final V before = base.containsKey(id) ? base.get(id) : fallback.apply(id);
            if (!next.equals(before))
                result.put(id, next);
        }
        return result;
    }
}
