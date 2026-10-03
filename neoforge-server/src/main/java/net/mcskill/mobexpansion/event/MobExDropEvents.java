package net.mcskill.mobexpansion.event;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.attributes.EntityAttributesConfigIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Iterator;

@EventBusSubscriber(modid = Core.MODID)
public final class MobExDropEvents {
    private MobExDropEvents() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide())
            return;
        if (!(event.getEntity() instanceof Mob))
            return;

        final ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (!EntityAttributesConfigIO.hasConfigured(entityId))
            return;
        if (!EntityAttributesConfigIO.getCached(entityId).dropDestination().givesToKiller())
            return;

        final Player killer = findKillerPlayer(event);
        if (killer == null)
            return;

        final Iterator<ItemEntity> dropIterator = event.getDrops().iterator();
        while (dropIterator.hasNext()) {
            final ItemEntity itemEntity = dropIterator.next();
            if (itemEntity.getItem().isEmpty()) {
                dropIterator.remove();
                continue;
            }
            itemEntity.setPickUpDelay(0);
            itemEntity.playerTouch(killer);
            if (!itemEntity.isAlive() || itemEntity.getItem().isEmpty())
                dropIterator.remove();
        }
    }

    private static Player findKillerPlayer(LivingDropsEvent event) {
        final Entity source = event.getSource().getEntity();
        if (source instanceof Player player)
            return player;
        final LivingEntity credit = event.getEntity().getKillCredit();
        if (credit instanceof Player player)
            return player;
        return null;
    }
}
