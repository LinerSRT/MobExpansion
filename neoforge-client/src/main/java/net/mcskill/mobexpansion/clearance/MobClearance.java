package net.mcskill.mobexpansion.clearance;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.network.MobClearancePacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр «тип моба → требуемый уровень допуска» (0 = без требования).
 * <p>
 * Ключ — id типа сущности, поэтому можно задавать требования и мобам других модов. Значения по умолчанию заданы
 * ниже в коде; на сервере их перекрывает поле «Уровень допуска» из конфига характеристик мобов
 * ({@code config/mobexpansion/entity_attributes.json}, вкладка «Характеристики» настройщика дропа).
 * Источник истины — сервер: таблица отправляется клиенту при входе
 * ({@link net.mcskill.mobexpansion.network.MobClearancePacket}).
 */
public final class MobClearance {
    private static final Map<ResourceLocation, Integer> DEFAULTS = new HashMap<>();
    private static final Map<ResourceLocation, Integer> REQUIRED = new ConcurrentHashMap<>();

    static {
        // Редстоун-фабрика
        put("redstone_engineer", 1);
        put("redstone_automaton", 1);
        put("redstone_drone", 1);
        put("redstone_tower", 1);
        // Скалк
        put("sculk_crabby", 1);
        put("sculked_crustacean", 1);
        put("sculk_screamer", 1);
        put("sculk_sentinel", 2);
        put("echo_enshrouder", 2);
        // Одержимые доспехи
        put("soul_bearer", 2);
        put("possessed_cinder_armor", 2);
        put("possessed_ruin_armor", 2);
        put("possessed_viking_armor", 2);
        // Боссы
        put("sentinel_golem", 3);
        put("rat_king", 3);
        put("apocalypse_type_a", 4);
        put("blossom_drake", 2);
        put("floral_serpent", 2);
        put("glimmering_unicorn", 2);
        put("serenity_sentinel", 3);
        put("queen_everbloom", 4);
        put("scarlet_queen_everbloom", 4);
        REQUIRED.putAll(DEFAULTS);
    }

    private MobClearance() {
    }

    private static void put(String path, int level) {
        DEFAULTS.put(Core.loc(path), level);
    }

    /** Уровень по умолчанию (из кода), без учёта конфига. */
    public static int getDefault(ResourceLocation entityId) {
        return entityId == null ? 0 : DEFAULTS.getOrDefault(entityId, 0);
    }

    public static int getRequired(Entity entity) {
        return entity == null ? 0 : getRequired(entity.getType());
    }

    public static int getRequired(EntityType<?> type) {
        return getRequired(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static int getRequired(ResourceLocation entityId) {
        return entityId == null ? 0 : REQUIRED.getOrDefault(entityId, 0);
    }

    /**
     * Задаёт требуемый уровень для типа сущности (0 — снять требование).
     * Изменения после входа игроков нужно разослать заново через {@link #syncToAll()}.
     */
    public static void setRequired(ResourceLocation entityId, int level) {
        final int clamped = Clearance.clamp(level);
        if (clamped <= 0)
            REQUIRED.remove(entityId);
        else
            REQUIRED.put(entityId, clamped);
    }

    /** Применяет уровни из конфига характеристик мобов (перекрывают значения по умолчанию). */
    public static void applyConfigured(Map<ResourceLocation, Integer> configured) {
        for (Map.Entry<ResourceLocation, Integer> entry : configured.entrySet())
            setRequired(entry.getKey(), entry.getValue());
    }

    public static Map<ResourceLocation, Integer> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(REQUIRED));
    }

    public static void syncTo(ServerPlayer player) {
        if (player.connection != null)
            PacketDistributor.sendToPlayer(player, new MobClearancePacket(snapshot()));
    }

    /** Разослать таблицу всем игрокам на сервере (только при запущенном сервере). */
    public static void syncToAll() {
        PacketDistributor.sendToAllPlayers(new MobClearancePacket(snapshot()));
    }
}
