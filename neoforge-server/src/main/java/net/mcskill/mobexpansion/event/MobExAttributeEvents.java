package net.mcskill.mobexpansion.event;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.attributes.EntityAttributesConfigIO;
import net.mcskill.mobexpansion.attributes.VanillaAttributeLimits;
import net.mcskill.mobexpansion.drops.EntityLootConfigIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Core.MODID)
public final class MobExAttributeEvents {
    private MobExAttributeEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        VanillaAttributeLimits.expand();
        EntityAttributesConfigIO.reload();
        EntityLootConfigIO.reload();
    }
    @SubscribeEvent
    public static void onServerStartedEvent(ServerStartedEvent event) {
        EntityAttributesConfigIO.reload();
        EntityLootConfigIO.reload();
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof Mob mob))
            return;
        final ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (!EntityAttributesConfigIO.hasConfigured(entityId))
            return;
        EntityAttributesConfigIO.getCached(entityId).applyTo(mob);
    }
}
