package net.mcskill.mobexpansion.init;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.blockentity.MobSpawnerBlockEntity;
import net.mcskill.mobexpansion.client.MobSpawnerClientExtensions;
import net.mcskill.mobexpansion.client.MobSpawnerRevealHandler;
import net.mcskill.mobexpansion.client.model.MobSpawnerCamoBakedModel;
import net.mcskill.mobexpansion.client.renderer.RegularSpiderRenderer;
import net.mcskill.mobexpansion.client.renderer.PoisonSpiderRenderer;
import net.mcskill.mobexpansion.client.renderer.PoisonProjectileRenderer;
import net.mcskill.mobexpansion.client.renderer.SpiderSpawnerRenderer;
import net.mcskill.mobexpansion.client.renderer.EngineerRenderer;
import net.mcskill.mobexpansion.client.renderer.AutomatonRenderer;
import net.mcskill.mobexpansion.client.renderer.DroneRenderer;
import net.mcskill.mobexpansion.client.renderer.TowerRenderer;
import net.mcskill.mobexpansion.client.renderer.ProjectileRenderer;
import net.mcskill.mobexpansion.client.renderer.WrenchProjectileRenderer;
import net.mcskill.mobexpansion.client.renderer.StatueRenderer;
import net.mcskill.mobexpansion.client.renderer.GolemRenderer;
import net.mcskill.mobexpansion.client.renderer.RatRenderer;
import net.mcskill.mobexpansion.client.renderer.MosquitoRenderer;
import net.mcskill.mobexpansion.client.renderer.LeechRenderer;
import net.mcskill.mobexpansion.client.renderer.RatKingRenderer;
import net.mcskill.mobexpansion.client.renderer.PotionProjectileRenderer;
import net.mcskill.mobexpansion.client.renderer.InvisibleRenderer;
import net.mcskill.mobexpansion.client.renderer.AbilityMobRenderer;
import net.mcskill.mobexpansion.client.renderer.MobExEffectRenderer;
import net.mcskill.mobexpansion.client.renderer.MobExProjectileRenderer;
import net.mcskill.mobexpansion.client.screen.MobDropConfigScreen;
import net.mcskill.mobexpansion.client.screen.MobSpawnerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Core.MODID, value = Dist.CLIENT)
public class MobExClientInit {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MobExEntities.REGULAR_SPIDER.get(), RegularSpiderRenderer::new);
        event.registerEntityRenderer(MobExEntities.POISON_SPIDER.get(), PoisonSpiderRenderer::new);
        event.registerEntityRenderer(MobExEntities.POISON_PROJECTILE.get(), PoisonProjectileRenderer::new);
        event.registerEntityRenderer(MobExEntities.SPIDER_SPAWN.get(), SpiderSpawnerRenderer::new);
        event.registerEntityRenderer(MobExEntities.REDSTONE_ENGINEER.get(), EngineerRenderer::new);
        event.registerEntityRenderer(MobExEntities.REDSTONE_AUTOMATON.get(), AutomatonRenderer::new);
        event.registerEntityRenderer(MobExEntities.REDSTONE_DRONE.get(), DroneRenderer::new);
        event.registerEntityRenderer(MobExEntities.REDSTONE_TOWER.get(), TowerRenderer::new);
        event.registerEntityRenderer(MobExEntities.REDSTONE_PROJECTILE.get(), ProjectileRenderer::new);
        event.registerEntityRenderer(MobExEntities.WRENCH_PROJECTILE.get(), WrenchProjectileRenderer::new);
        event.registerEntityRenderer(MobExEntities.SENTINEL_STATUE.get(), StatueRenderer::new);
        event.registerEntityRenderer(MobExEntities.SENTINEL_GOLEM.get(), GolemRenderer::new);
        event.registerEntityRenderer(MobExEntities.RAT.get(), RatRenderer::new);
        event.registerEntityRenderer(MobExEntities.MOSQUITO.get(), MosquitoRenderer::new);
        event.registerEntityRenderer(MobExEntities.LEECH.get(), LeechRenderer::new);
        event.registerEntityRenderer(MobExEntities.RAT_KING.get(), RatKingRenderer::new);
        event.registerEntityRenderer(MobExEntities.SEWER_POTION_PROJECTILE.get(), PotionProjectileRenderer::new);
        event.registerEntityRenderer(MobExEntities.SEWER_BOLT_PROJECTILE.get(), InvisibleRenderer::new);
        event.registerEntityRenderer(MobExEntities.SEWER_POISON_AREA.get(), InvisibleRenderer::new);
        event.registerEntityRenderer(MobExEntities.SWARMER_ANT.get(), AbilityMobRenderer.noCull(1.0F, 0.5F));
        event.registerEntityRenderer(MobExEntities.BEETLE_TROOPER.get(), AbilityMobRenderer.noCull(1.0F, 1.2F));
        event.registerEntityRenderer(MobExEntities.GUNSHIP_MOTH.get(), AbilityMobRenderer.noCull(3.0F, 4.2F));
        event.registerEntityRenderer(MobExEntities.HUNTSMAN_SPIDER.get(), AbilityMobRenderer.noCull(1.0F, 1.4F));
        event.registerEntityRenderer(MobExEntities.LADYBUG_MORTAR.get(), AbilityMobRenderer.noCull(1.0F, 1.1F));
        event.registerEntityRenderer(MobExEntities.WOODLOUSE_TANK.get(), AbilityMobRenderer.noCull(1.0F, 1.6F));
        event.registerEntityRenderer(MobExEntities.SCULK_CRABBY.get(), AbilityMobRenderer.of(1.0F, 0.5F));
        event.registerEntityRenderer(MobExEntities.SCULKED_CRUSTACEAN.get(), AbilityMobRenderer.of(1.0F, 0.8F));
        event.registerEntityRenderer(MobExEntities.SCULK_SCREAMER.get(), AbilityMobRenderer.of(1.0F, 1.0F));
        event.registerEntityRenderer(MobExEntities.SCULK_SENTINEL.get(), AbilityMobRenderer.of(1.0F, 1.4F));
        event.registerEntityRenderer(MobExEntities.ECHO_ENSHROUDER.get(), AbilityMobRenderer.of(1.0F, 0.8F));
        event.registerEntityRenderer(MobExEntities.SOUL_BEARER.get(), AbilityMobRenderer.of(1.0F, 0.5F));
        event.registerEntityRenderer(MobExEntities.POSSESSED_CINDER_ARMOR.get(), AbilityMobRenderer.of(1.0F, 0.9F));
        event.registerEntityRenderer(MobExEntities.POSSESSED_RUIN_ARMOR.get(), AbilityMobRenderer.of(1.0F, 0.9F));
        event.registerEntityRenderer(MobExEntities.POSSESSED_VIKING_ARMOR.get(), AbilityMobRenderer.of(1.0F, 0.9F));
        event.registerEntityRenderer(MobExEntities.APOCALYPSE_TYPE_A.get(), AbilityMobRenderer.of(1.0F, 1.0F));
        event.registerEntityRenderer(MobExEntities.ABILITY_PROJECTILE.get(), MobExProjectileRenderer::new);
        event.registerEntityRenderer(MobExEntities.ABILITY_EFFECT.get(), MobExEffectRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MobExMenus.MOB_SPAWNER.get(), MobSpawnerScreen::new);
        event.register(MobExMenus.MOB_DROP_CONFIG.get(), MobDropConfigScreen::new);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        final var models = event.getModels();
        for (ModelResourceLocation location : models.keySet().toArray(ModelResourceLocation[]::new)) {
            if (!location.id().equals(Core.loc("mob_spawner")))
                continue;
            if (ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                continue;
            models.put(location, new MobSpawnerCamoBakedModel(models.get(location)));
        }
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(MobSpawnerClientExtensions.INSTANCE, MobExBlocks.MOB_SPAWNER.get());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, getter, pos, tintIndex) -> {
            if (getter == null || pos == null)
                return -1;
            if (!(getter.getBlockEntity(pos) instanceof MobSpawnerBlockEntity spawner))
                return -1;
            if (MobSpawnerRevealHandler.isRevealing())
                return -1;
            final BlockState maskState = spawner.getMaskBlockState();
            if (maskState == null)
                return -1;
            return Minecraft.getInstance().getBlockColors().getColor(maskState, getter, pos, tintIndex);
        }, MobExBlocks.MOB_SPAWNER.get());
    }
}
