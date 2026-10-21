package net.mcskill.mobexpansion.event;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.command.SkinCommand;
import net.mcskill.mobexpansion.network.SkinAttackFxPacket;
import net.mcskill.mobexpansion.skin.PlayerSkins;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Скины игроков: команда, рассылка скина клиентам и эффект удара игрока со скином.
 */
@EventBusSubscriber(modid = Core.MODID)
public final class MobExSkinEvents {
    private MobExSkinEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SkinCommand.register(event.getDispatcher());
    }

    /** Игрок {@code viewer} начал видеть игрока со скином — отправляем ему скин. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer owner && event.getEntity() instanceof ServerPlayer viewer
                && PlayerSkins.hasSkin(owner))
            PlayerSkins.sendTo(owner, viewer);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSkins.hasSkin(player))
            PlayerSkins.broadcast(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSkins.hasSkin(player))
            PlayerSkins.broadcast(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSkins.hasSkin(player))
            PlayerSkins.broadcast(player);
    }

    /**
     * Удар игрока со скином: клиенты рисуют эффект. Низкий приоритет — отменённые удары (например, без допуска)
     * сюда не доходят. Событие приходит до самой атаки, поэтому заряд удара ещё не сброшен.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !PlayerSkins.hasSkin(player))
            return;
        if (!event.getTarget().isAttackable() || event.getTarget().skipAttackInteraction(player))
            return;
        final float strength = player.getAttackStrengthScale(0.5F);
        // те же условия, что у ванильного крита в Player#attack
        final boolean critical = strength > 0.9F && player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable()
                && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger()
                && event.getTarget() instanceof LivingEntity && !player.isSprinting();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SkinAttackFxPacket(player.getId(), strength, critical));
    }
}
