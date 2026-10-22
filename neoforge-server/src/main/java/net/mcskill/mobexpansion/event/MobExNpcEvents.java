package net.mcskill.mobexpansion.event;

import net.mcskill.mobexpansion.Core;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/** Мобы мода не берут целью NPC из CustomNPCs (кто бы ни пытался назначить цель). */
@EventBusSubscriber(modid = Core.MODID)
public final class MobExNpcEvents {
    private static final String NPC_NAMESPACE = "customnpcs";

    private MobExNpcEvents() {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        final LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || !(event.getEntity() instanceof Mob mob))
            return;
        if (isFromNamespace(mob, Core.MODID) && isFromNamespace(target, NPC_NAMESPACE))
            event.setCanceled(true);
    }

    private static boolean isFromNamespace(Entity entity, String namespace) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(namespace);
    }
}
