package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.entity.sea.SeaMineEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Морская мина: модель, цепь до дна (ванильная модель блока цепи, мир не меняется) и раскалённый слой
 * (sea_mine_hot), который включается на вспышках перед взрывом.
 */
public class SeaMineRenderer extends SeaCreatureRenderer<SeaMineEntity> {
    private static final ResourceLocation HOT = ResourceLocation.fromNamespaceAndPath(Core.MODID, "textures/entity/sea_mine_hot.png");
    private static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();

    public SeaMineRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0F, 0.5F);
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, SeaMineEntity mine, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (mine.getStance() != SeaMineEntity.HOT)
                    return;
                final RenderType hot = RenderType.eyes(HOT);
                getRenderer().reRender(bakedModel, poseStack, bufferSource, mine, hot, bufferSource.getBuffer(hot), partialTick, 0xF000F0, packedOverlay, 0xFFFFFFFF);
            }
        });
    }

    @Override
    public void render(@NotNull SeaMineEntity mine, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(mine, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        renderChain(mine, poseStack, bufferSource);
    }

    private static void renderChain(SeaMineEntity mine, PoseStack poseStack, MultiBufferSource bufferSource) {
        final float length = mine.chainLength();
        if (length <= 0.05F || mine.isDeadOrDying())
            return;
        final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        final int segments = (int) Math.ceil(length);
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        poseStack.pushPose();
        for (int i = 0; i < segments; i++) {
            // последнее звено уходит в дно — его прячет сам блок
            final double top = SeaMineEntity.CHAIN_TOP + i;
            cursor.set(mine.getX(), mine.getY() - top - 0.5D, mine.getZ());
            final int light = LevelRenderer.getLightColor(mine.level(), cursor);
            poseStack.pushPose();
            poseStack.translate(-0.5D, -top - 1.0D, -0.5D);
            dispatcher.renderSingleBlock(CHAIN, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** Мина видна, пока видна её цепь: иначе цепь пропадала бы, едва мина уходит из кадра. */
    @Override
    public boolean shouldRender(@NotNull SeaMineEntity mine, @NotNull net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(mine, frustum, camX, camY, camZ))
            return true;
        final float length = mine.chainLength();
        return length > 0.0F && frustum.isVisible(new AABB(mine.getX() - 0.5D, mine.getY() - length - SeaMineEntity.CHAIN_TOP, mine.getZ() - 0.5D,
                mine.getX() + 0.5D, mine.getY(), mine.getZ() + 0.5D));
    }
}
