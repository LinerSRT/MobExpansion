package net.mcskill.mobexpansion.event;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.clearance.Clearance;
import net.mcskill.mobexpansion.clearance.MobClearance;
import net.mcskill.mobexpansion.command.ClearanceCommand;
import net.mcskill.mobexpansion.command.MobExpansionCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Уровень допуска: игрок с уровнем ниже требуемого не может нанести мобу урон (ни в ближнем бою, ни снарядами),
 * плюс синхронизация уровня и таблицы требований клиенту.
 */
@EventBusSubscriber(modid = Core.MODID)
public final class MobExClearanceEvents {
    private static final int NOTIFY_COOLDOWN_TICKS = 40;
    private static final Map<ServerPlayer, Integer> LAST_NOTIFY = new WeakHashMap<>();

    private MobExClearanceEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ClearanceCommand.register(event.getDispatcher());
        MobExpansionCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MobClearance.syncTo(player);
            Clearance.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            Clearance.sync(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            Clearance.sync(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            LAST_NOTIFY.remove(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isDenied(player, event.getTarget()))
            event.setCanceled(true);
    }

    /**
     * Ловит всё, что не прошло через AttackEntityEvent: снаряды, взмах меча, TNT и т.п. — любой урон,
     * у которого виновник (источник-владелец) — игрок.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && isDenied(player, event.getEntity()))
            event.setCanceled(true);
    }

    private static boolean isDenied(ServerPlayer player, Entity target) {
        final int required = MobClearance.getRequired(target);
        if (required <= 0)
            return false;
        final int level = Clearance.get(player);
        if (level >= required)
            return false;
        notifyDenied(player, required, level);
        return true;
    }

    private static void notifyDenied(ServerPlayer player, int required, int level) {
        final int now = player.server.getTickCount();
        final Integer last = LAST_NOTIFY.get(player);
        if (last != null && now - last < NOTIFY_COOLDOWN_TICKS)
            return;
        LAST_NOTIFY.put(player, now);
        player.sendSystemMessage(Component.translatable("message.mobexpansion.clearance.denied", required, level)
                .withStyle(ChatFormatting.RED));
    }
}
