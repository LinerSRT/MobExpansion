package net.mcskill.mobexpansion.entity.sea;

import net.mcskill.mobexpansion.entity.ability.AbilityCombatGoal;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Морское существо (паки Sea Depth / Sea Turtle): плавает как дельфин (SmoothSwimmingMoveControl + путь по воде),
 * под водой не тонет и не всплывает, на суше задыхается, как рыба. Фракция общая — «sea».
 * <p>
 * MOVEMENT_SPEED у морских мобов — крейсерская скорость в воде в блоках за тик.
 * Имена geo/animation/texture по умолчанию — id типа сущности.
 */
public abstract class SeaCreatureEntity extends AbilityMobEntity {
    public static final String FACTION = "sea";
    /** Предел размеров для поиска пути (как у летунов): большие мобы иначе проверяют сотни блоков на узел. */
    private static final int MAX_PATHFINDING_SIZE = 3;
    private static final int MAX_AIR = 300;

    @Nullable
    private String id;

    protected SeaCreatureEntity(EntityType<? extends SeaCreatureEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, maxPitch(), maxTurn(), 0.1F, 0.1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        setPathfindingMalus(PathType.WATER, 0.0F);
    }

    /** id моба (= имя ассетов по умолчанию). */
    public String id() {
        if (id == null)
            id = BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
        return id;
    }

    /** Наибольший наклон корпуса при всплытии/погружении, градусов. */
    protected int maxPitch() {
        return 60;
    }

    /** Скорость поворота по горизонтали, градусов за тик. */
    protected int maxTurn() {
        return 10;
    }

    /** Наклонять ли модель по тангажу (у медуз и мин — нет). */
    public boolean pitchesModel() {
        return true;
    }

    /** Задыхается ли на суше. */
    protected boolean suffocatesOutOfWater() {
        return true;
    }

    @Override
    public String faction() {
        return FACTION;
    }

    @Override
    public String modelName() {
        return id();
    }

    @Override
    public String textureName() {
        return id();
    }

    @Override
    public String animationName() {
        return id();
    }

    // ---------------------------------------------------------------- цели и движение

    @Override
    protected void registerGoals() {
        // без FloatGoal: он выталкивает моба к поверхности
        goalSelector.addGoal(2, new AbilityCombatGoal(this));
        registerIdleGoals();
        registerTargetGoals();
    }

    protected void registerTargetGoals() {
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isEnemy));
    }

    @Override
    protected void registerIdleGoals() {
        goalSelector.addGoal(7, new RandomSwimmingGoal(this, 0.8D, 40));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        return new WaterBoundPathNavigation(this, level) {
            @Override
            @NotNull
            protected PathFinder createPathFinder(int maxVisitedNodes) {
                nodeEvaluator = new CappedSwimNodeEvaluator();
                return new PathFinder(nodeEvaluator, maxVisitedNodes);
            }
        };
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (isControlledByLocalInstance() && isInWater()) {
            // ввод нормализуется: разгон = getSpeed(), при сопротивлении 0.9 предельная скорость = 10 × getSpeed()
            // = скорость атрибута × модификатор (SmoothSwimmingMoveControl умножает на 0.1)
            final Vec3 input = travelVector.lengthSqr() > 1.0E-7D ? travelVector.normalize() : Vec3.ZERO;
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9D));
            calculateEntityAnimation(true);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    protected boolean isVisuallyMoving() {
        return getDeltaMovement().lengthSqr() > 4.0E-4D;
    }

    /** Рывок в любом направлении (в том числе вверх/вниз), задаёт скорость напрямую. */
    public void swimLunge(Vec3 direction, double speed) {
        if (direction.lengthSqr() < 1.0E-6D)
            return;
        final Vec3 velocity = direction.normalize().scale(speed);
        setDeltaMovement(velocity);
        hurtMarked = true;
        final double horizontal = velocity.horizontalDistance();
        setXRot((float) -(Mth.atan2(velocity.y, horizontal) * Mth.RAD_TO_DEG) * 0.6F);
    }

    /** Направление от пасти к центру цели. */
    public Vec3 directionTo(Entity target, double mouthHeight) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(position().add(0.0D, mouthHeight, 0.0D));
    }

    /** Остановиться на месте (в воде инерция гасится не сразу). */
    public void halt() {
        setDeltaMovement(Vec3.ZERO);
        hurtMarked = true;
    }

    // ---------------------------------------------------------------- дыхание и вода

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public int getMaxAirSupply() {
        return MAX_AIR;
    }

    @Override
    public void baseTick() {
        final int air = getAirSupply();
        super.baseTick();
        // под водой воздух всегда полный (не тонет); вне воды, как у WaterAnimal, кончается — и рыба задыхается
        if (isAlive() && !isInWaterOrBubble() && suffocatesOutOfWater()) {
            setAirSupply(air - 1);
            if (getAirSupply() == -20) {
                setAirSupply(0);
                hurt(damageSources().drown(), 2.0F);
            }
        } else {
            setAirSupply(MAX_AIR);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }

    /** {@link SwimNodeEvaluator} с ограничением размеров моба, см. {@link #MAX_PATHFINDING_SIZE}. */
    private static final class CappedSwimNodeEvaluator extends SwimNodeEvaluator {
        private CappedSwimNodeEvaluator() {
            super(false);
        }

        @Override
        public void prepare(@NotNull PathNavigationRegion level, @NotNull net.minecraft.world.entity.Mob mob) {
            super.prepare(level, mob);
            entityWidth = Math.min(entityWidth, MAX_PATHFINDING_SIZE);
            entityHeight = Math.min(entityHeight, MAX_PATHFINDING_SIZE);
            entityDepth = Math.min(entityDepth, MAX_PATHFINDING_SIZE);
        }
    }
}
