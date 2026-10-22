package net.mcskill.mobexpansion.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mcskill.mobexpansion.item.SkinItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
        // GeoItemRenderer сам масштабирует модель ДО сдвига в центр блока (translate 0.5, 0.51, 0.5 попадает под масштаб) —
        // поэтому свой масштаб применяем здесь и ставим ноги модели так, чтобы вся модель (рост + крылья) стояла в центре слота
        final float scale = ITEM_SIZE_PX / modelSizePx(definition);
        withScale(1.0F);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 - modelHeightPx(definition) * scale / 32.0F, 0.5 - modelCenterZPx(definition) * scale / 16.0F);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, -0.51, -0.5);
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

    /** Рост модели скина в пикселях (без крыльев). */
    private static float modelHeightPx(SkinDefinition definition) {
        return definition == SkinDefinition.CAPRA ? 81.0F : 40.0F;
    }

    /** Середина модели по оси Z (крылья и хвост уходят назад), чтобы модель стояла в слоте по центру. */
    private static float modelCenterZPx(SkinDefinition definition) {
        return definition == SkinDefinition.CAPRA ? 7.8F : 6.0F;
    }

    /** Размер модели на предмете, в пикселях (16 = один блок). */
    private static final float ITEM_SIZE_PX = 15.0F;

    private static final class Model extends GeoModel<SkinItem> {
        /**
         * Кости BakedGeoModel общие для всех, кто рисует эту модель, а рендерер игрока в скине пропускает расчёт анимации,
         * если уже считал его в этом кадре, — доверяя, что позу никто не трогал. Предмет (инвентарь, рука) двигал те же
         * кости, и анимация персонажа дёргалась. Поэтому предмет получает свою копию костей.
         */
        private final Map<BakedGeoModel, BakedGeoModel> copies = new IdentityHashMap<>();
        private BakedGeoModel active;

        @Override
        public BakedGeoModel getBakedModel(ResourceLocation location) {
            final BakedGeoModel shared = GeckoLibCache.getBakedModels().get(location);
            if (shared == null)
                return super.getBakedModel(location);
            final BakedGeoModel copy = copies.computeIfAbsent(shared, Model::copyOf);
            if (copy != active) {
                getAnimationProcessor().setActiveModel(copy);
                active = copy;
            }
            return copy;
        }

        private static BakedGeoModel copyOf(BakedGeoModel source) {
            final List<GeoBone> top = new ArrayList<>();
            for (GeoBone bone : source.topLevelBones())
                top.add(copyOf(bone, null));
            return new BakedGeoModel(top, source.properties());
        }

        private static GeoBone copyOf(GeoBone source, GeoBone parent) {
            final GeoBone bone = new GeoBone(parent, source.getName(), source.getMirror(), source.getInflate(), source.shouldNeverRender(), source.getReset());
            bone.updatePivot(source.getPivotX(), source.getPivotY(), source.getPivotZ());
            final BoneSnapshot rest = source.getInitialSnapshot();
            if (rest != null)
                bone.updateRotation(rest.getRotX(), rest.getRotY(), rest.getRotZ());
            else
                bone.updateRotation(source.getRotX(), source.getRotY(), source.getRotZ());
            bone.getCubes().addAll(source.getCubes());
            for (GeoBone child : source.getChildBones())
                bone.getChildBones().add(copyOf(child, bone));
            return bone;
        }

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
