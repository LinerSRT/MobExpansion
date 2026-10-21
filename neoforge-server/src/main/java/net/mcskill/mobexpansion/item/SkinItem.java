package net.mcskill.mobexpansion.item;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Предмет-заглушка скина игрока: в мире и в инвентаре рисуется 3D-моделью самого скина
 * ({@code geo/skin/<skin>.geo.json}, см. клиентский {@code SkinItemRenderer}). Из кейсов будет падать вместо настоящего скина.
 */
public class SkinItem extends Item implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private final String skin;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SkinItem(String skin, Properties properties) {
        super(properties);
        this.skin = skin;
    }

    public String skin() {
        return skin;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
