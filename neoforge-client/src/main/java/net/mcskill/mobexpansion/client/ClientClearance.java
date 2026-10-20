package net.mcskill.mobexpansion.client;

import net.mcskill.mobexpansion.Core;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.Map;

/**
 * Клиентская копия уровня допуска локального игрока и таблицы требований мобов (приходят с сервера пакетами).
 */
@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public final class ClientClearance {
    private static int level;
    private static Map<ResourceLocation, Integer> required = Map.of();

    private ClientClearance() {
    }

    public static int getLevel() {
        return level;
    }

    public static void setLevel(int newLevel) {
        level = newLevel;
    }

    public static void setRequirements(Map<ResourceLocation, Integer> newRequired) {
        required = Map.copyOf(newRequired);
    }

    public static int getRequired(Entity entity) {
        return required.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), 0);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        level = 0;
        required = Map.of();
    }
}
