package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;


public class EmissiveGeoLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
	private final ResourceLocation emissiveTexture;

	public EmissiveGeoLayer(GeoRenderer<T> renderer, ResourceLocation emissiveTexture) {
		super(renderer);
		this.emissiveTexture = emissiveTexture;
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		poseStack.pushPose();
		poseStack.scale(1.005f, 1.005f, 1.005f);
		RenderType emissiveType = RenderType.eyes(emissiveTexture);
		getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveType, bufferSource.getBuffer(emissiveType), partialTick, 0xF000F0, packedOverlay, 0xFFFFFFFF);
		poseStack.popPose();
	}
}
