package net.mcskill.mobexpansion.client.skin;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Анимируемый объект скина. Один на скин, состояние анимаций GeckoLib хранит отдельно для каждого игрока
 * (по id сущности, см. GeoReplacedEntityRenderer#getInstanceId).
 * <p>
 * Анимации — только для «декоративных» костей (крылья, хвост, волосы, корона); руки/ноги/голова/тело берут позу
 * из ванильной модели. Основной контроллер выбирает: glide (элитры), flap (полёт), flare (только что ударил),
 * run (спринт), walk, idle. Плюс постоянные петли скина ({@link SkinDefinition#loops()}).
 */
public final class PlayerSkinAnimatable implements GeoAnimatable {
    /** Сколько тиков после удара играет всплеск (flare). */
    private static final int FLARE_TICKS = 10;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("flap");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation FLARE = RawAnimation.begin().thenPlay("flare");

    private final SkinDefinition definition;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Какой удар (время в тиках) уже показан всплеском — чтобы перезапускать анимацию на каждый удар. */
    private final Int2LongMap shownAttack = new Int2LongOpenHashMap();

    public PlayerSkinAnimatable(SkinDefinition definition) {
        this.definition = definition;
        shownAttack.defaultReturnValue(-1);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::main));
        for (String loop : definition.loops()) {
            final RawAnimation animation = RawAnimation.begin().thenLoop(loop);
            controllers.add(new AnimationController<>(this, loop, 0, state -> state.setAndContinue(animation)));
        }
    }

    private PlayState main(AnimationState<PlayerSkinAnimatable> state) {
        final Entity entity = state.getData(DataTickets.ENTITY);
        if (!(entity instanceof Player player))
            return state.setAndContinue(IDLE);
        if (player.isFallFlying())
            return state.setAndContinue(GLIDE);
        if (player.getAbilities().flying)
            return state.setAndContinue(FLAP);
        final long attack = SkinAttackFx.lastAttackTick(player.getId());
        if (attack >= 0 && player.level().getGameTime() - attack < FLARE_TICKS) {
            if (shownAttack.put(player.getId(), attack) != attack)
                state.getController().forceAnimationReset();
            return state.setAndContinue(FLARE);
        }
        if (!state.isMoving())
            return state.setAndContinue(IDLE);
        return state.setAndContinue(player.isSprinting() ? RUN : WALK);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return object instanceof Entity entity ? entity.tickCount : 0;
    }
}
