package net.mcskill.mobexpansion.entity.ability;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Одна способность моба: анимация + длительность + таймлайн действий по тикам.
 * Описывается декларативно через {@link #builder(String)} и исполняется {@link AbilityMobEntity}.
 */
public final class Ability {
    @FunctionalInterface
    public interface Action {
        void run(AbilityMobEntity mob, @Nullable LivingEntity target);
    }

    @FunctionalInterface
    public interface TickAction {
        void run(AbilityMobEntity mob, @Nullable LivingEntity target, int tick);
    }

    @FunctionalInterface
    public interface Condition {
        boolean test(AbilityMobEntity mob, LivingEntity target);
    }

    final String name;
    @Nullable
    final String animation;
    final int duration;
    final int cooldown;
    final double minRange;
    final double maxRange;
    final int weight;
    final boolean stopsMovement;
    final boolean tracksTarget;
    final boolean unstoppable;
    final boolean needsSight;
    @Nullable
    final Condition condition;
    @Nullable
    final Action onStart;
    @Nullable
    final Action onEnd;
    final Map<Integer, List<Action>> timeline;
    final List<Window> windows;

    record Window(int from, int to, int period, TickAction action) {
    }

    private Ability(Builder builder) {
        this.name = builder.name;
        this.animation = builder.animation;
        this.duration = Math.max(1, builder.duration);
        this.cooldown = builder.cooldown;
        this.minRange = builder.minRange;
        this.maxRange = builder.maxRange;
        this.weight = Math.max(1, builder.weight);
        this.stopsMovement = builder.stopsMovement;
        this.tracksTarget = builder.tracksTarget;
        this.unstoppable = builder.unstoppable;
        this.needsSight = builder.needsSight;
        this.condition = builder.condition;
        this.onStart = builder.onStart;
        this.onEnd = builder.onEnd;
        this.timeline = builder.timeline;
        this.windows = builder.windows;
    }

    public String name() {
        return name;
    }

    @Nullable
    public String animation() {
        return animation;
    }

    public int duration() {
        return duration;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Секунды анимации -> тики (так проще брать длину прямо из animation.json).
     */
    public static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0D);
    }

    public static final class Builder {
        private final String name;
        @Nullable
        private String animation;
        private int duration = 20;
        private int cooldown = 40;
        private double minRange = 0.0D;
        private double maxRange = 3.0D;
        private int weight = 1;
        private boolean stopsMovement = true;
        private boolean tracksTarget = true;
        private boolean unstoppable;
        private boolean needsSight = true;
        @Nullable
        private Condition condition;
        @Nullable
        private Action onStart;
        @Nullable
        private Action onEnd;
        private final Map<Integer, List<Action>> timeline = new HashMap<>();
        private final List<Window> windows = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
            this.animation = name;
        }

        public Builder animation(@Nullable String animation) {
            this.animation = animation;
            return this;
        }

        public Builder seconds(double seconds) {
            this.duration = ticks(seconds);
            return this;
        }

        public Builder duration(int ticks) {
            this.duration = ticks;
            return this;
        }

        public Builder cooldown(int ticks) {
            this.cooldown = ticks;
            return this;
        }

        public Builder range(double min, double max) {
            this.minRange = min;
            this.maxRange = max;
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        /** Моб может двигаться во время способности (рывки, перекаты, полёт). */
        public Builder moving() {
            this.stopsMovement = false;
            return this;
        }

        /** Не поворачиваться к цели во время способности (рывок по прямой и т.п.). */
        public Builder fixedFacing() {
            this.tracksTarget = false;
            return this;
        }

        /** Отбрасывание и толчки не сбивают способность. */
        public Builder unstoppable() {
            this.unstoppable = true;
            return this;
        }

        /** Можно применять без прямой видимости цели. */
        public Builder ignoreSight() {
            this.needsSight = false;
            return this;
        }

        public Builder when(Condition condition) {
            this.condition = this.condition == null ? condition : and(this.condition, condition);
            return this;
        }

        public Builder onStart(Action action) {
            this.onStart = action;
            return this;
        }

        public Builder onEnd(Action action) {
            this.onEnd = action;
            return this;
        }

        /** Действие ровно на указанном тике способности. */
        public Builder at(int tick, Action action) {
            timeline.computeIfAbsent(tick, key -> new ArrayList<>()).add(action);
            return this;
        }

        /** Действие на тике, заданном долей длительности (0..1) — удобно, когда тайминг «в середине замаха». */
        public Builder atFraction(double fraction, Action action) {
            return at((int) Math.round(duration * fraction), action);
        }

        /** Действие каждый period-й тик в окне [from, to]. */
        public Builder every(int from, int to, int period, TickAction action) {
            windows.add(new Window(from, to, Math.max(1, period), action));
            return this;
        }

        public Ability build() {
            return new Ability(this);
        }

        private static Condition and(Condition first, Condition second) {
            return (mob, target) -> first.test(mob, target) && second.test(mob, target);
        }
    }
}
