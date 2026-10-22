package net.mcskill.mobexpansion.event;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.spawner.MobHome;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Привязка мобов к спавнеру (радиус дома из конфигуратора мобов, см. {@link MobHome}): моб не берёт целью то, что вне дома,
 * бросает цель, выйдя за границу, и возвращается домой. Проверка — раз в {@link #CHECK_INTERVAL} тиков,
 * у мобов без дома цена — одно сравнение поля.
 */
@EventBusSubscriber(modid = Core.MODID)
public final class MobExHomeEvents {
    private static final int CHECK_INTERVAL = 5;
    /** Дальше этого запаса за границей (блоков) моб считается унесённым и телепортируется домой. */
    private static final double TELEPORT_MARGIN = 8.0D;

    private MobExHomeEvents() {
    }

    /** Ванильное ограничение не сохраняется в NBT — восстанавливаем при загрузке моба. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Mob mob)
            MobHome.applyTo(mob);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        final LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || !(event.getEntity() instanceof Mob mob) || mob.level().isClientSide)
            return;
        // Быстрый отсев: у мобов без ограничения дома нет.
        if (!mob.hasRestriction())
            return;
        final MobHome home = MobHome.get(mob);
        if (home != null && !home.contains(target.getX(), target.getY(), target.getZ()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !mob.hasRestriction() || mob.level().isClientSide)
            return;
        if ((mob.tickCount + mob.getId()) % CHECK_INTERVAL != 0 || !mob.isAlive())
            return;
        final MobHome home = MobHome.get(mob);
        if (home == null)
            return;

        final LivingEntity target = mob.getTarget();
        if (target != null && !home.contains(target.getX(), target.getY(), target.getZ()))
            mob.setTarget(null);

        if (home.contains(mob.getX(), mob.getY(), mob.getZ()))
            return;

        // Вышел за границу: бросаем погоню и идём к спавнеру.
        mob.setTarget(null);
        final double limit = home.radius() + TELEPORT_MARGIN;
        final double dx = mob.getX() - (home.center().getX() + 0.5D);
        final double dy = mob.getY() - (home.center().getY() + 0.5D);
        final double dz = mob.getZ() - (home.center().getZ() + 0.5D);
        if (dx * dx + dy * dy + dz * dz > limit * limit && mob.getVehicle() == null) {
            mob.getNavigation().stop();
            mob.teleportTo(home.center().getX() + 0.5D, home.center().getY() + 1.0D, home.center().getZ() + 0.5D);
            return;
        }
        mob.getNavigation().moveTo(home.center().getX() + 0.5D, home.center().getY(), home.center().getZ() + 0.5D, 1.0D);
    }
}
