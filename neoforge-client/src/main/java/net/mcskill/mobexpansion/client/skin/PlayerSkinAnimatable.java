package net.mcskill.mobexpansion.client.skin;

import com.google.common.collect.MapMaker;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.player.LocalPlayer;
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

import java.util.Iterator;
import java.util.Map;

/**
 * Анимируемый объект скина. Один на скин, но состояние анимаций GeckoLib (контроллеры, снимки костей, время старта) —
 * своё у каждого игрока: менеджер берётся по {@link #instanceId}, который выдаётся каждому объекту игрока заново.
 * <p>
 * Почему не id сущности (как у GeckoLib по умолчанию): id повторяются после перехода между серверами сети (лобби → сервер)
 * и смены измерения, а кеш GeckoLib их никогда не забывает — новый игрок получал чужой менеджер со стартом по чужим часам,
 * время анимации уходило в минус, ключевые кадры экстраполировались (кости «разлетались») или стояли на месте.
 * Здесь менеджеры живут, пока игрока рисуют, и через {@link #IDLE_EXPIRE_MS} без отрисовки удаляются.
 * <p>
 * Часы — игровое время мира, общие для всех игроков: GeoModel хранит один счётчик animTime на модель скина и прибавляет
 * к нему разницу между кадрами разных игроков, так что с личным {@code tickCount} время прыгало бы туда-сюда. Смена
 * мира (и скачок его времени) приходит вместе с новыми объектами игроков — а значит, с новыми менеджерами.
 * <p>
 * Анимации — только для «декоративных» костей (крылья, хвост, волосы, корона); руки/ноги/голова/тело берут позу
 * из ванильной модели. Основной контроллер выбирает: glide (элитры), flap (полёт), flare (только что ударил),
 * run (спринт), walk, idle. Плюс постоянные петли скина ({@link SkinDefinition#loops()}).
 * Выбор считается только по тому, что клиент знает о любом игроке (флаги, позиция, onGround): способность летать
 * ({@code abilities.flying}) с сервера приходит только своему игроку, у чужих она всегда false.
 * <p>
 * Неподвижная фигура (статуи топов MSEss — помечены тегом {@link #STATIC_POSE_TAG}) не анимируется вовсе: основной
 * контроллер и петли стоят, а контроллер {@code static} держит первый кадр glide (крылья раскрыты) без перехода и
 * с нулевой скоростью. Без этого фигура, которая никогда не касается земли, вечно махала бы крыльями (flap).
 */
public final class PlayerSkinAnimatable implements GeoAnimatable {
    /** Сколько тиков после удара играет всплеск (flare). */
    private static final int FLARE_TICKS = 10;
    /** Чужой игрок считается летящим, если столько тиков не касался земли (обычный прыжок — около 12). */
    private static final int AIRBORNE_FLAP_TICKS = 15;
    /** Падение быстрее этого (блоков за тик) — уже не полёт. */
    private static final double FALL_SPEED = -0.6;
    /** Горизонтальный сдвиг за тик (в квадрате), с которого игрок идёт. */
    private static final double MOVE_SQR = 1.0E-4;
    /** Менеджер анимаций игрока, которого столько не рисовали, удаляется. */
    private static final long IDLE_EXPIRE_MS = 30_000L;
    private static final long PURGE_EVERY_MS = 10_000L;
    /**
     * Метка неподвижной фигуры: тег сущности ({@code Entity#addTag}), который ставит мод фигуры (MSEss — статуи топов).
     * Ванильный тег, а не класс — без зависимости от MSEss; у настоящих игроков на клиенте тегов нет.
     * Та же строка — в MsBunker ({@code PlayerAnimations.STATIC_POSE_TAG}).
     */
    public static final String STATIC_POSE_TAG = "mcskill.static_pose";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("flap");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation FLARE = RawAnimation.begin().thenPlay("flare");

    /**
     * Состояние игрока, общее для всех скинов: id экземпляра для GeckoLib и когда он последний раз стоял на земле.
     * Слабые ключи (по ссылке) — объект игрока, ушедшего из мира, сборщик мусора уберёт сам. Только поток рендера.
     */
    // MapMaker со слабыми ключами сравнивает по ссылке: Entity#equals сравнивает id сущности, а id повторяются
    // после смены сервера/измерения — обычный WeakHashMap отдал бы новому объекту игрока старое состояние.
    private static final Map<Player, PlayerState> PLAYERS = new MapMaker().weakKeys().makeMap();
    private static long nextInstanceId = 1;

    private final SkinDefinition definition;
    private final PerPlayerCache cache = new PerPlayerCache(this);

    public PlayerSkinAnimatable(SkinDefinition definition) {
        this.definition = definition;
    }

    /** Id экземпляра анимации для этого объекта игрока; новый объект (вход, респаун, смена сервера) — новый id. */
    static long instanceId(Player player) {
        return state(player).instanceId;
    }

    private static PlayerState state(Player player) {
        return PLAYERS.computeIfAbsent(player, p -> new PlayerState(nextInstanceId++, p.tickCount));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::main));
        for (String loop : definition.loops()) {
            final RawAnimation animation = RawAnimation.begin().thenLoop(loop);
            controllers.add(new AnimationController<>(this, loop, 0,
                    state -> isStatic(state) ? PlayState.STOP : state.setAndContinue(animation)));
        }
        // Фигуры: без перехода (сразу кадр) и с нулевой скоростью — время анимации всегда 0.
        controllers.add(new AnimationController<>(this, "static", 0,
                state -> isStatic(state) ? state.setAndContinue(GLIDE) : PlayState.STOP).setAnimationSpeed(0));
    }

    private static boolean isStatic(AnimationState<PlayerSkinAnimatable> state) {
        return state.getData(DataTickets.ENTITY) instanceof Player player && player.getTags().contains(STATIC_POSE_TAG);
    }

    private PlayState main(AnimationState<PlayerSkinAnimatable> state) {
        final Entity entity = state.getData(DataTickets.ENTITY);
        if (!(entity instanceof Player player))
            return state.setAndContinue(IDLE);
        if (player.getTags().contains(STATIC_POSE_TAG))
            return PlayState.STOP;
        final PlayerState playerState = state(player);
        if (player.isFallFlying())
            return state.setAndContinue(GLIDE);
        if (isFlying(player, playerState))
            return state.setAndContinue(FLAP);
        final long attack = SkinAttackFx.lastAttackTick(player.getId());
        final long sinceAttack = player.level().getGameTime() - attack;
        // удар «из будущего» (время осталось от прошлого мира/сервера) — не всплеск, иначе крылья застынут в его конце
        if (attack >= 0 && sinceAttack >= 0 && sinceAttack < FLARE_TICKS) {
            final Slot slot = cache.slot(playerState.instanceId);
            if (slot != null && slot.shownAttack != attack) {
                slot.shownAttack = attack;
                state.getController().forceAnimationReset();
            }
            return state.setAndContinue(FLARE);
        }
        final double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
        if (dx * dx + dz * dz < MOVE_SQR)
            return state.setAndContinue(IDLE);
        return state.setAndContinue(player.isSprinting() ? RUN : WALK);
    }

    /** Свой игрок — по способности; чужой — долго в воздухе и не падает камнем. */
    private static boolean isFlying(Player player, PlayerState state) {
        if (player instanceof LocalPlayer)
            return player.getAbilities().flying;
        if (player.onGround() || player.isInWater() || player.isPassenger() || player.onClimbable()) {
            state.lastGroundTick = player.tickCount;
            return false;
        }
        return player.tickCount - state.lastGroundTick >= AIRBORNE_FLAP_TICKS && player.getY() - player.yo > FALL_SPEED;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Общие часы — игровое время мира (см. описание класса: animTime у GeoModel один на все экземпляры). */
    @Override
    public double getTick(Object object) {
        return object instanceof Entity entity ? entity.level().getGameTime() : 0;
    }

    private static final class PlayerState {
        final long instanceId;
        int lastGroundTick;

        PlayerState(long instanceId, int tickCount) {
            this.instanceId = instanceId;
            this.lastGroundTick = tickCount;
        }
    }

    private static final class Slot {
        final AnimatableManager<?> manager;
        long lastUsed;
        /** Какой удар (время в тиках) уже показан всплеском — чтобы перезапускать анимацию на каждый удар. */
        long shownAttack = -1;

        Slot(AnimatableManager<?> manager) {
            this.manager = manager;
        }
    }

    /** Менеджеры анимаций по id экземпляра; неиспользуемые удаляются, чтобы кеш не рос с каждым встреченным игроком. */
    private static final class PerPlayerCache extends AnimatableInstanceCache {
        private final Long2ObjectMap<Slot> slots = new Long2ObjectOpenHashMap<>();
        private long lastPurge;

        PerPlayerCache(GeoAnimatable animatable) {
            super(animatable);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends GeoAnimatable> AnimatableManager<T> getManagerForId(long uniqueId) {
            final long now = Util.getMillis();
            if (now - lastPurge > PURGE_EVERY_MS) {
                lastPurge = now;
                for (Iterator<Slot> it = slots.values().iterator(); it.hasNext(); )
                    if (now - it.next().lastUsed > IDLE_EXPIRE_MS)
                        it.remove();
            }
            Slot slot = slots.get(uniqueId);
            if (slot == null) {
                slot = new Slot(new AnimatableManager<>(animatable));
                slots.put(uniqueId, slot);
            }
            slot.lastUsed = now;
            return (AnimatableManager<T>) slot.manager;
        }

        Slot slot(long uniqueId) {
            return slots.get(uniqueId);
        }
    }
}
