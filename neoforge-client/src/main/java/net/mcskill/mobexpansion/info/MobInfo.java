package net.mcskill.mobexpansion.info;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сводка о мобе для EMI: характеристики (из конфига характеристик MobExpansion), уровень допуска и дроп.
 * Собирается на сервере ({@code info.MobInfoSync}) и уходит клиенту пакетом {@code MobInfoPacket}.
 * Класс одинаковый в {@code neoforge-server} и {@code neoforge-client}.
 *
 * @param abilityDamage урон способностей: ключ доп. параметра ({@code *_damage}) → значение
 * @param xpMax         -1 — опыт в конфиге не задан
 * @param location      текст «Расположение» из настройщика мобов (пусто — не задан)
 * @param respawn       текст «До респавна» из настройщика мобов (пусто — не задан)
 */
public record MobInfo(
        ResourceLocation entityId,
        float maxHealth,
        float armor,
        float attackDamage,
        float movementSpeed,
        Map<String, Float> abilityDamage,
        int clearance,
        List<Drop> drops,
        int xpMin,
        int xpMax,
        float xpChance,
        String location,
        String respawn
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MobInfo> STREAM_CODEC = StreamCodec.of(MobInfo::write, MobInfo::read);

    public MobInfo {
        abilityDamage = abilityDamage == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(abilityDamage));
        drops = drops == null ? List.of() : List.copyOf(drops);
        location = location == null ? "" : location;
        respawn = respawn == null ? "" : respawn;
    }

    public boolean hasExperience() {
        return xpMax >= 0;
    }

    private static void write(RegistryFriendlyByteBuf buf, MobInfo info) {
        buf.writeResourceLocation(info.entityId);
        buf.writeFloat(info.maxHealth);
        buf.writeFloat(info.armor);
        buf.writeFloat(info.attackDamage);
        buf.writeFloat(info.movementSpeed);
        buf.writeVarInt(info.abilityDamage.size());
        for (Map.Entry<String, Float> entry : info.abilityDamage.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
        buf.writeVarInt(info.clearance);
        buf.writeVarInt(info.drops.size());
        for (Drop drop : info.drops)
            Drop.write(buf, drop);
        buf.writeVarInt(info.xpMin);
        buf.writeVarInt(info.xpMax);
        buf.writeFloat(info.xpChance);
        buf.writeUtf(info.location);
        buf.writeUtf(info.respawn);
    }

    private static MobInfo read(RegistryFriendlyByteBuf buf) {
        final ResourceLocation entityId = buf.readResourceLocation();
        final float maxHealth = buf.readFloat();
        final float armor = buf.readFloat();
        final float attackDamage = buf.readFloat();
        final float movementSpeed = buf.readFloat();
        final int abilityCount = buf.readVarInt();
        final Map<String, Float> abilityDamage = new LinkedHashMap<>();
        for (int i = 0; i < abilityCount; i++)
            abilityDamage.put(buf.readUtf(), buf.readFloat());
        final int clearance = buf.readVarInt();
        final int dropCount = buf.readVarInt();
        final List<Drop> drops = new ArrayList<>(dropCount);
        for (int i = 0; i < dropCount; i++)
            drops.add(Drop.read(buf));
        return new MobInfo(entityId, maxHealth, armor, attackDamage, movementSpeed, abilityDamage, clearance, drops,
                buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readUtf(), buf.readUtf());
    }

    /**
     * Один предмет дропа.
     *
     * @param chance      0..1
     * @param conditional выпадает только при доп. условиях (убит игроком, условия конфига и т.п.)
     * @param lootTable   из ванильной loot-таблицы моба (а не из конфига дропа MobExpansion)
     */
    public record Drop(ResourceLocation itemId, int minCount, int maxCount, float chance, boolean conditional, boolean lootTable) {
        private static void write(RegistryFriendlyByteBuf buf, Drop drop) {
            buf.writeResourceLocation(drop.itemId);
            buf.writeVarInt(drop.minCount);
            buf.writeVarInt(drop.maxCount);
            buf.writeFloat(drop.chance);
            buf.writeBoolean(drop.conditional);
            buf.writeBoolean(drop.lootTable);
        }

        private static Drop read(RegistryFriendlyByteBuf buf) {
            return new Drop(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readBoolean(), buf.readBoolean());
        }
    }
}
