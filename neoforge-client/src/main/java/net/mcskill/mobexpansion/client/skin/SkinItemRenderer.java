package net.mcskill.mobexpansion.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mcskill.mobexpansion.item.SkinItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Рендер предмета-заглушки скина ({@link SkinItem}): рисует модель, текстуру и анимацию самого скина
 * ({@link SkinDefinition}), уменьшенные до размера предмета. Модель предмета — {@code builtin/entity}.
 */
public class SkinItemRenderer extends GeoItemRenderer<SkinItem> {
    public SkinItemRenderer() {
        super(new Model());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        final SkinDefinition definition = definition(stack);
        if (definition == null)
            return;
        // GeoItemRenderer ставит начало модели в середину блока по высоте, а ноги скина стоят на нуле — опускаем на пол-блока
        final float scale = ITEM_SIZE_PX / modelSizePx(definition);
        withScale(scale);
        poseStack.pushPose();
        poseStack.translate(0, -0.5, -modelCenterZPx(definition) * scale / 16.0F);
        super.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static SkinDefinition definition(ItemStack stack) {
        return stack.getItem() instanceof SkinItem item ? SkinRenderers.definition(item.skin()) : null;
    }

    /** Габарит модели скина (с крыльями) в пикселях: по нему модель вписывается в слот. */
    private static float modelSizePx(SkinDefinition definition) {
        return definition == SkinDefinition.CAPRA ? 86.0F : 62.0F;
    }

    /** Середина модели по оси Z (крылья и хвост уходят назад), чтобы модель стояла в слоте по центру. */
    private static float modelCenterZPx(SkinDefinition definition) {
        return definition == SkinDefinition.CAPRA ? 7.8F : 6.0F;
    }

    /** Размер модели на предмете, в пикселях (16 = один блок). */
    private static final float ITEM_SIZE_PX = 15.0F;

    private static final class Model extends GeoModel<SkinItem> {
        private static SkinDefinition of(SkinItem item) {
            return SkinRenderers.definition(item.skin());
        }

        @Override
        public ResourceLocation getModelResource(SkinItem item) {
            return of(item).model();
        }

        @Override
        public ResourceLocation getTextureResource(SkinItem item) {
            return of(item).texture();
        }

        @Override
        public ResourceLocation getAnimationResource(SkinItem item) {
            return of(item).animations();
        }
    }
}
