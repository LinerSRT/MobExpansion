package net.mcskill.mobexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcskill.mobexpansion.client.model.MobExGeoModel;
import net.mcskill.mobexpansion.entity.effect.MobExProjectileEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Рисует {@link MobExProjectileEntity}: либо предмет (брошенный топор), либо GeckoLib-модель, либо ничего.
 * GeckoLib не поворачивает неживые сущности, поэтому поворот по направлению полёта делаем сами.
 */
public class MobExProjectileRenderer extends GeoEntityRenderer<MobExProjectileEntity> {
    private final ItemRenderer itemRenderer;

    public MobExProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, new MobExGeoModel<>());
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
        addRenderLayer(new AutoEmissiveLayer<>(this));
    }

    @Override
    public void render(MobExProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        final ItemStack stack = entity.getItem();
        if (!stack.isEmpty()) {
            renderItem(entity, stack, partialTick, poseStack, bufferSource, packedLight);
            return;
        }
        if (entity.hasModel())
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    private void renderItem(MobExProjectileEntity entity, ItemStack stack, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        final float scale = entity.getScale();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        if (entity.isSpinning())
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(entity.tickCount + partialTick) * 40.0F));
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
    }

    @Override
    public void preRender(PoseStack poseStack, MobExProjectileEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        final float scale = animatable.getScale();
        if (!isReRender && scale != 1.0F)
            poseStack.scale(scale, scale, scale);
    }

    @Override
    protected void applyRotations(MobExProjectileEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // у снарядов yRot = atan2(x, z): переводим в поворот модели, «нос» которой смотрит в -Z
        final float yaw = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        final float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F + yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        if (animatable.isSpinning())
            poseStack.mulPose(Axis.ZP.rotationDegrees(ageInTicks * 30.0F));
    }
}
