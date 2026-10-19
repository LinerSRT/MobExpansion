package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.entity.IModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Светящийся слой, который сам находит textures/entity/&lt;текстура&gt;_emissive.png.
 * Если такой текстуры нет — слой ничего не рисует. Так один рендерер подходит любой модели.
 */
public class AutoEmissiveLayer<T extends GeoAnimatable & IModel> extends GeoRenderLayer<T> {
    private static final Map<String, Optional<ResourceLocation>> CACHE = new ConcurrentHashMap<>();

    public AutoEmissiveLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    public static Optional<ResourceLocation> emissiveTexture(String textureName) {
        return CACHE.computeIfAbsent(textureName, name -> {
            final ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Core.MODID, "textures/entity/" + name + "_emissive.png");
            return Minecraft.getInstance().getResourceManager().getResource(location).isPresent() ? Optional.of(location) : Optional.empty();
        });
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        final Optional<ResourceLocation> texture = emissiveTexture(animatable.textureName());
        if (texture.isEmpty())
            return;
        final RenderType emissiveType = RenderType.eyes(texture.get());
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveType, bufferSource.getBuffer(emissiveType), partialTick, 0xF000F0, packedOverlay, 0xFFFFFFFF);
    }
}
