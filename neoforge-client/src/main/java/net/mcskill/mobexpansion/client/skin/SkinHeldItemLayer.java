package net.mcskill.mobexpansion.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Предметы в руках скина — как ванильный ItemInHandLayer, только точка хвата берётся из кисти скина
 * ({@link SkinDefinition#rightGrip()}), а масштаб модели снимается, чтобы предмет был обычного размера.
 */
public class SkinHeldItemLayer extends GeoRenderLayer<PlayerSkinAnimatable> {
    private final PlayerSkinRenderer skinRenderer;
    private final SkinDefinition definition;

    public SkinHeldItemLayer(PlayerSkinRenderer renderer, SkinDefinition definition) {
        super(renderer);
        this.skinRenderer = renderer;
        this.definition = definition;
    }

    @Override
    public void renderForBone(PoseStack poseStack, PlayerSkinAnimatable animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        final HumanoidArm arm;
        if (bone.getName().equals(definition.rightHand()))
            arm = HumanoidArm.RIGHT;
        else if (bone.getName().equals(definition.leftHand()))
            arm = HumanoidArm.LEFT;
        else
            return;
        final AbstractClientPlayer player = skinRenderer.getCurrentEntity();
        if (player == null || player.isSpectator())
            return;
        final ItemStack stack = player.getMainArm() == arm ? player.getMainHandItem() : player.getOffhandItem();
        if (stack.isEmpty())
            return;
        final boolean left = arm == HumanoidArm.LEFT;
        final Vector3f grip = left ? definition.leftGrip() : definition.rightGrip();

        poseStack.pushPose();
        // стек сейчас в системе кости (после её поворота), начало — в 0,0,0 модели: возвращаемся в pivot кисти
        RenderUtil.translateToPivotPoint(poseStack, bone);
        poseStack.translate(grip.x() / 16F, grip.y() / 16F, grip.z() / 16F);
        final float unscale = 1.0F / definition.scale();
        poseStack.scale(unscale, unscale, unscale);
        // оси GeckoLib -> оси ванильной модели (там Y вниз и X зеркален), дальше — как ItemInHandLayer
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
