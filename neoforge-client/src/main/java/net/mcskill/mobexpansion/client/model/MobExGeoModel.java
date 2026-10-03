package net.mcskill.mobexpansion.client.model;

import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.entity.IModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class MobExGeoModel<T extends Entity & GeoAnimatable & IModel> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Core.MODID, "geo/" + animatable.modelName() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Core.MODID, "textures/entity/" + animatable.textureName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Core.MODID, "animations/" + animatable.animationName() + ".animation.json");
    }
}
