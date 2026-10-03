package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.entity.redstone.RedstoneTowerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TowerRenderer extends MobExGeoRenderer<RedstoneTowerEntity> {
    private static final ResourceLocation EMISSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Core.MODID, "textures/entity/redstone_tower_emissive.png");

    public TowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.9F;
        //addRenderLayer(new EmissiveGeoLayer<>(this, EMISSIVE_TEXTURE));
    }
}