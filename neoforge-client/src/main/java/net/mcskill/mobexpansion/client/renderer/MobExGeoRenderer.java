package net.mcskill.mobexpansion.client.renderer;

import net.mcskill.mobexpansion.entity.IModel;
import net.mcskill.mobexpansion.client.model.MobExGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MobExGeoRenderer<T extends Entity & GeoAnimatable & IModel> extends GeoEntityRenderer<T> {
    public MobExGeoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MobExGeoModel<>());
        this.shadowRadius = 0.7F;
    }
}
