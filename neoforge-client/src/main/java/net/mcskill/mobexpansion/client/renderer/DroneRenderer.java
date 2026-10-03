package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.redstone.RedstoneDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class DroneRenderer extends MobExGeoRenderer<RedstoneDroneEntity> {
    public DroneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.35F;
    }
}