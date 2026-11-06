package net.mcskill.mobexpansion.entity.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Летающий моб со способностями (по образцу MosquitoEntity): FlyingMoveControl, без гравитации и урона от падения.
 * При смерти падает на землю, чтобы анимация смерти не висела в воздухе.
 */
public abstract class FlyingAbilityMobEntity extends AbilityMobEntity {
    /**
     * Предел размеров моба (блоков) для поиска пути. Ванильный FlyNodeEvaluator на каждую позицию пути проверяет все блоки
     * хитбокса: у моли 11×12 это ~1900 блоков на позицию и секунды на путь. Летун в открытом воздухе и так проходит,
     * путь — лишь ориентир, столкновения остаются за физикой.
     */
    private static final int MAX_PATHFINDING_SIZE = 3;

    protected FlyingAbilityMobEntity(EntityType<? extends FlyingAbilityMobEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    /** Высота зависания над целью в бою. */
    public double hoverHeight() {
        return 4.0D;
    }

    @Override
    protected CombatStyle combatStyle() {
        return CombatStyle.HOVER;
    }

    @Override
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        final FlyingPathNavigation navigation = new FlyingPathNavigation(this, level) {
            @Override
            @NotNull
            protected PathFinder createPathFinder(int maxVisitedNodes) {
                nodeEvaluator = new CappedFlyNodeEvaluator();
                nodeEvaluator.setCanPassDoors(true);
                return new PathFinder(nodeEvaluator, maxVisitedNodes);
            }
        };
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerIdleGoals() {
        goalSelector.addGoal(7, new WaterAvoidingRandomFlyingGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        // мёртвый летун падает вниз, живой — парит
        setNoGravity(!isDeadOrDying());
        super.tick();
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (isDeadOrDying()) {
            super.travel(travelVector);
            return;
        }
        if (isEffectiveAi() || isControlledByLocalInstance()) {
            final double drag = isInWater() ? 0.8D : isInLava() ? 0.5D : 0.91D;
            moveRelative(isInWater() || isInLava() ? 0.02F : getSpeed(), travelVector);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(drag));
        }
        calculateEntityAnimation(false);
    }

    @Override
    protected boolean isVisuallyMoving() {
        return getDeltaMovement().lengthSqr() > 0.002D;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    /** {@link FlyNodeEvaluator} с ограничением размеров моба, см. {@link #MAX_PATHFINDING_SIZE}. */
    private static final class CappedFlyNodeEvaluator extends FlyNodeEvaluator {
        @Override
        public void prepare(@NotNull PathNavigationRegion level, @NotNull Mob mob) {
            super.prepare(level, mob);
            entityWidth = Math.min(entityWidth, MAX_PATHFINDING_SIZE);
            entityHeight = Math.min(entityHeight, MAX_PATHFINDING_SIZE);
            entityDepth = Math.min(entityDepth, MAX_PATHFINDING_SIZE);
        }
    }
}
