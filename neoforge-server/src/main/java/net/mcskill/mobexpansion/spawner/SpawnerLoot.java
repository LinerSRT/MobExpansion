package net.mcskill.mobexpansion.spawner;

import net.mcskill.mobexpansion.Core;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Configurator loot belongs to a spawner-created instance, not its entity type. */
@EventBusSubscriber(modid = Core.MODID)
public final class SpawnerLoot {
    private static final String KEY = "mobexpansion_spawner_loot_owner";

    private SpawnerLoot() {}

    public static void mark(Mob mob) {
        mob.getPersistentData().putUUID(KEY, mob.getUUID());
    }

    public static boolean isEligible(Entity entity) {
        return entity instanceof Mob && entity.getPersistentData().hasUUID(KEY)
                && entity.getUUID().equals(entity.getPersistentData().getUUID(KEY));
    }

    public static void clear(Entity entity) {
        // A tombstone prevents legacy home migration from re-enabling loot after reload.
        entity.getPersistentData().putBoolean(KEY, false);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob))
            return;
        // Upgrade already saved spawner mobs. Never infer provenance for a fresh summon/egg.
        if (event.loadedFromDisk() && !mob.getPersistentData().contains(KEY)
                && mob.getPersistentData().contains("mobexpansion_home", Tag.TAG_COMPOUND))
            mark(mob);
    }
}
