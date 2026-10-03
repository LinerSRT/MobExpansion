package net.mcskill.mobexpansion.entity.redstone;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.MobExGeckoEntity;
import net.mcskill.mobexpansion.entity.ai.redstone.EngineerCombatGoal;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class RedstoneEngineerEntity extends MobExGeckoEntity {
    public enum SummonType {
        AUTOMATON("build_automaton_box"),
        DRONE("build_drone_box"),
        TOWER("build_tower_box");

        private final String animationName;

        SummonType(String animationName) {
            this.animationName = animationName;
        }

        public String getAnimationName() {
            return animationName;
        }
    }

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation BUILD_AUTOMATON_ANIMATION = RawAnimation.begin().thenPlay("build_automaton_box");
    private static final RawAnimation BUILD_DRONE_ANIMATION = RawAnimation.begin().thenPlay("build_drone_box");
    private static final RawAnimation BUILD_TOWER_ANIMATION = RawAnimation.begin().thenPlay("build_tower_box");
    private static final RawAnimation THROW_ANIMATION = RawAnimation.begin().thenPlay("throw_projectile");
    private static final RawAnimation MELEE_ANIMATION = RawAnimation.begin().thenPlay("melee_attack");
    private static final RawAnimation ATTACK3_ANIMATION = RawAnimation.begin().thenPlay("attack3");
    private static final RawAnimation ATTACK4_ANIMATION = RawAnimation.begin().thenPlay("attack4");
    private static final RawAnimation DAMAGE_ANIMATION = RawAnimation.begin().thenPlay("damage");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");

    private static final int BUILD_DURATION_TICKS = 169;
    private static final int SUMMON_SPAWN_TICK = 107;
    private static final int THROW_DURATION_TICKS = 24;
    private static final int THROW_RELEASE_TICK = 10;
    private static final int MELEE_DURATION_TICKS = 24;
    private static final int MELEE_HIT_TICK = 10;
    private static final int SUMMON_COOLDOWN_TICKS = 55;
    private static final float PROJECTILE_SPEED = 1.25F;
    private static final float PROJECTILE_INACCURACY = 2.0F;

    private static final String AUTOMATON_IDS_TAG = "AutomatonIds";
    private static final String DRONE_IDS_TAG = "DroneIds";
    private static final String TOWER_IDS_TAG = "TowerIds";
    private static final String BUILD_TICKS_TAG = "BuildTicks";
    private static final String BUILD_TYPE_TAG = "BuildType";
    private static final String ACTION_TICKS_TAG = "ActionTicks";
    private static final String ACTION_NAME_TAG = "ActionName";
    private static final String SUMMON_COOLDOWN_TAG = "SummonCooldown";

    private final Set<UUID> automatonIds = new HashSet<>();
    private final Set<UUID> droneIds = new HashSet<>();
    private final Set<UUID> towerIds = new HashSet<>();

    private int buildTicksRemaining;
    private int summonAtTick = -1;
    @Nullable
    private SummonType pendingSummonType;
    private Vec3 buildOrigin = Vec3.ZERO;

    private int actionTicksRemaining;
    private int actionEffectTick = -1;
    @Nullable
    private String activeActionName;
    @Nullable
    private LivingEntity pendingActionTarget;
    private int summonCooldown;

    public RedstoneEngineerEntity(EntityType<? extends RedstoneEngineerEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 25;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 70.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new EngineerCombatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, RedstoneFaction::isValidTarget));
    }

    public boolean isBuilding() {
        return buildTicksRemaining > 0;
    }

    public boolean isPerformingAction() {
        return actionTicksRemaining > 0;
    }

    public boolean canStartSummon() {
        return !isBuilding() && !isPerformingAction() && summonCooldown <= 0;
    }

    public boolean hasLivingAutomaton() {
        cleanupConstructSets();
        return !automatonIds.isEmpty();
    }

    public boolean hasLivingDrone() {
        cleanupConstructSets();
        return !droneIds.isEmpty();
    }

    public boolean hasLivingTower() {
        cleanupConstructSets();
        return !towerIds.isEmpty();
    }

    public boolean tryBeginSummon(SummonType summonType) {
        if (!canStartSummon() || level().isClientSide())
            return false;
        cleanupConstructSets();
        if (summonType == SummonType.AUTOMATON && automatonIds.size() >= MobExParameters.getInt(this, "maxAutomatons", 1))
            return false;
        if (summonType == SummonType.DRONE && droneIds.size() >= MobExParameters.getInt(this, "maxDrones", 1))
            return false;
        if (summonType == SummonType.TOWER && towerIds.size() >= MobExParameters.getInt(this, "maxTowers", 1))
            return false;

        pendingSummonType = summonType;
        buildTicksRemaining = BUILD_DURATION_TICKS;
        summonAtTick = SUMMON_SPAWN_TICK;
        buildOrigin = position();
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        triggerAnim("main", summonType.getAnimationName());
        playSound(SoundEvents.ANVIL_USE, 0.7F, 1.2F);
        return true;
    }

    public boolean tryMeleeAttack(LivingEntity target) {
        if (isBuilding() || isPerformingAction() || target == null || !target.isAlive())
            return false;
        final String attackName = random.nextBoolean() ? "melee_attack" : (random.nextBoolean() ? "attack3" : "attack4");
        beginAction(attackName, MELEE_DURATION_TICKS, MELEE_HIT_TICK, target);
        return true;
    }

    public boolean tryThrowKey(LivingEntity target) {
        if (isBuilding() || isPerformingAction() || target == null || !target.isAlive())
            return false;
        beginAction("throw_projectile", THROW_DURATION_TICKS, THROW_RELEASE_TICK, target);
        return true;
    }

    private void beginAction(String actionName, int durationTicks, int effectTick, LivingEntity target) {
        activeActionName = actionName;
        actionTicksRemaining = durationTicks;
        actionEffectTick = effectTick;
        pendingActionTarget = target;
        getNavigation().stop();
        triggerAnim("main", actionName);
    }

    public void onConstructRemoved(RedstoneConstructEntity construct) {
        final UUID constructId = construct.getUUID();
        automatonIds.remove(constructId);
        droneIds.remove(constructId);
        towerIds.remove(constructId);
    }

    public void onConstructAttacked(RedstoneConstructEntity construct, LivingEntity attacker) {
        if (attacker == null || !attacker.isAlive() || !RedstoneFaction.isValidTarget(attacker))
            return;
        if (getTarget() == null || !getTarget().isAlive())
            setTarget(attacker);
        alertAllConstructs(attacker);
    }

    public void alertAllConstructs(LivingEntity threat) {
        if (!(level() instanceof ServerLevel serverLevel) || threat == null || !threat.isAlive())
            return;
        for (UUID constructId : collectAllConstructIds()) {
            final Entity constructEntity = serverLevel.getEntity(constructId);
            if (constructEntity instanceof RedstoneConstructEntity construct && construct.isAlive())
                construct.onPackAlert(threat);
        }
    }

    public int getLivingConstructCount() {
        cleanupConstructSets();
        return automatonIds.size() + droneIds.size() + towerIds.size();
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && summonCooldown > 0)
            summonCooldown--;

        if (isBuilding())
            tickBuildSequence();
        if (isPerformingAction())
            tickActionSequence();

        if (!level().isClientSide())
            cleanupConstructSets();

        shareTargetWithConstructs();
        super.tick();
    }

    private void tickBuildSequence() {
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        setPos(buildOrigin.x, buildOrigin.y, buildOrigin.z);

        if (summonAtTick == 0)
            spawnPendingConstruct();
        if (summonAtTick >= 0)
            summonAtTick--;

        buildTicksRemaining--;
        if (buildTicksRemaining <= 0) {
            pendingSummonType = null;
            summonAtTick = -1;
            summonCooldown = MobExParameters.getInt(this, "summonCooldownTicks", SUMMON_COOLDOWN_TICKS);
        }
    }

    private void tickActionSequence() {
        getNavigation().stop();
        if (pendingActionTarget != null && pendingActionTarget.isAlive())
            getLookControl().setLookAt(pendingActionTarget, 30.0F, 30.0F);

        if (actionEffectTick == 0)
            resolveActionEffect();
        if (actionEffectTick >= 0)
            actionEffectTick--;

        actionTicksRemaining--;
        if (actionTicksRemaining <= 0) {
            activeActionName = null;
            pendingActionTarget = null;
            actionEffectTick = -1;
        }
    }

    private void resolveActionEffect() {
        final LivingEntity target = pendingActionTarget;
        if (target == null || !target.isAlive())
            return;
        if ("throw_projectile".equals(activeActionName)) {
            throwKeyProjectile(target);
            return;
        }
        if (distanceToSqr(target) <= 9.0D)
            doHurtTarget(target);
    }

    private void throwKeyProjectile(LivingEntity target) {
        final WrenchProjectileEntity projectile = new WrenchProjectileEntity(level(), this);
        projectile.setDamage(MobExParameters.getFloat(this, "wrenchDamage", 7.0F));
        final double eyeY = getEyeY() - 0.1D;
        projectile.moveTo(getX(), eyeY, getZ(), getYRot(), getXRot());

        final double deltaX = target.getX() - getX();
        final double deltaY = target.getY(0.35D) - eyeY;
        final double deltaZ = target.getZ() - getZ();
        final double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        final float projectileSpeed = MobExParameters.getFloat(this, "projectileSpeed", PROJECTILE_SPEED);
        projectile.shoot(deltaX, deltaY + horizontalDistance * 0.12D, deltaZ, projectileSpeed, PROJECTILE_INACCURACY);
        playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        level().addFreshEntity(projectile);
    }

    private void spawnPendingConstruct() {
        if (!(level() instanceof ServerLevel serverLevel) || pendingSummonType == null)
            return;

        final RedstoneConstructEntity construct = createConstruct(serverLevel, pendingSummonType);
        if (construct == null)
            return;

        construct.moveTo(buildOrigin.x, buildOrigin.y, buildOrigin.z, getYRot(), 0.0F);
        construct.setOwnerUUID(getUUID());
        construct.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        serverLevel.addFreshEntityWithPassengers(construct);
        construct.beginSpawnFromBox();

        final LivingEntity target = getTarget();
        if (target != null && target.isAlive())
            construct.setTarget(target);

        registerConstruct(construct, pendingSummonType);
        playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.1F);
    }

    @Nullable
    private RedstoneConstructEntity createConstruct(ServerLevel serverLevel, SummonType summonType) {
        return switch (summonType) {
            case AUTOMATON -> MobExEntities.REDSTONE_AUTOMATON.get().create(serverLevel);
            case DRONE -> MobExEntities.REDSTONE_DRONE.get().create(serverLevel);
            case TOWER -> MobExEntities.REDSTONE_TOWER.get().create(serverLevel);
        };
    }

    private void registerConstruct(RedstoneConstructEntity construct, SummonType summonType) {
        final UUID constructId = construct.getUUID();
        switch (summonType) {
            case AUTOMATON -> automatonIds.add(constructId);
            case DRONE -> droneIds.add(constructId);
            case TOWER -> towerIds.add(constructId);
        }
    }

    private void shareTargetWithConstructs() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive())
            return;
        for (UUID constructId : collectAllConstructIds()) {
            final Entity constructEntity = serverLevel.getEntity(constructId);
            if (constructEntity instanceof RedstoneConstructEntity construct && construct.isAlive())
                construct.onPackAlert(target);
        }
    }

    private Set<UUID> collectAllConstructIds() {
        final Set<UUID> allIds = new HashSet<>();
        allIds.addAll(automatonIds);
        allIds.addAll(droneIds);
        allIds.addAll(towerIds);
        return allIds;
    }

    private void cleanupConstructSets() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        cleanupSet(serverLevel, automatonIds, RedstoneAutomatonEntity.class);
        cleanupSet(serverLevel, droneIds, RedstoneDroneEntity.class);
        cleanupSet(serverLevel, towerIds, RedstoneTowerEntity.class);
    }

    private <T extends RedstoneConstructEntity> void cleanupSet(ServerLevel serverLevel, Set<UUID> idSet, Class<T> constructClass) {
        final Iterator<UUID> idIterator = idSet.iterator();
        while (idIterator.hasNext()) {
            final Entity constructEntity = serverLevel.getEntity(idIterator.next());
            if (!constructClass.isInstance(constructEntity) || !constructEntity.isAlive())
                idIterator.remove();
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (RedstoneFaction.isAlly(target))
            return false;
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (isBuilding())
            return false;
        if (damageSource.is(DamageTypes.IN_WALL) || damageSource.is(DamageTypes.CRAMMING))
            return false;
        if (RedstoneFaction.isAlly(damageSource.getEntity()))
            return false;
        final boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt && !isDeadOrDying() && !isPerformingAction())
            triggerAnim("main", "damage");
        if (wasHurt && damageSource.getEntity() instanceof LivingEntity attacker && RedstoneFaction.isValidTarget(attacker)) {
            setTarget(attacker);
            alertAllConstructs(attacker);
        }
        return wasHurt;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (isBuilding())
            return;
        super.knockback(strength, x, z);
    }

    @Override
    public boolean isPushable() {
        return !isBuilding() && super.isPushable();
    }

    @Override
    public void push(@NotNull Entity entity) {
        if (isBuilding())
            return;
        super.push(entity);
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (isBuilding()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!isDeadOrDying())
            triggerAnim("main", "death");
        destroyAllConstructs();
        super.die(damageSource);
    }

    private void destroyAllConstructs() {
        pendingSummonType = null;
        buildTicksRemaining = 0;
        summonAtTick = -1;

        if (!(level() instanceof ServerLevel serverLevel))
            return;

        final Set<UUID> constructIds = new HashSet<>(collectAllConstructIds());
        for (UUID constructId : constructIds) {
            final Entity constructEntity = serverLevel.getEntity(constructId);
            if (constructEntity instanceof RedstoneConstructEntity construct && construct.isAlive())
                construct.kill();
        }

        automatonIds.clear();
        droneIds.clear();
        towerIds.clear();
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= 16 && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.put(AUTOMATON_IDS_TAG, writeUuidList(automatonIds));
        compoundTag.put(DRONE_IDS_TAG, writeUuidList(droneIds));
        compoundTag.put(TOWER_IDS_TAG, writeUuidList(towerIds));
        compoundTag.putInt(BUILD_TICKS_TAG, buildTicksRemaining);
        compoundTag.putInt(ACTION_TICKS_TAG, actionTicksRemaining);
        compoundTag.putInt(SUMMON_COOLDOWN_TAG, summonCooldown);
        compoundTag.putInt("SummonAtTick", summonAtTick);
        compoundTag.putDouble("BuildOriginX", buildOrigin.x);
        compoundTag.putDouble("BuildOriginY", buildOrigin.y);
        compoundTag.putDouble("BuildOriginZ", buildOrigin.z);
        if (pendingSummonType != null)
            compoundTag.putString(BUILD_TYPE_TAG, pendingSummonType.name());
        if (activeActionName != null)
            compoundTag.putString(ACTION_NAME_TAG, activeActionName);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        readUuidList(compoundTag.getList(AUTOMATON_IDS_TAG, Tag.TAG_INT_ARRAY), automatonIds);
        readUuidList(compoundTag.getList(DRONE_IDS_TAG, Tag.TAG_INT_ARRAY), droneIds);
        readUuidList(compoundTag.getList(TOWER_IDS_TAG, Tag.TAG_INT_ARRAY), towerIds);
        buildTicksRemaining = compoundTag.getInt(BUILD_TICKS_TAG);
        actionTicksRemaining = compoundTag.getInt(ACTION_TICKS_TAG);
        summonCooldown = compoundTag.getInt(SUMMON_COOLDOWN_TAG);
        summonAtTick = compoundTag.getInt("SummonAtTick");
        buildOrigin = new Vec3(
                compoundTag.getDouble("BuildOriginX"),
                compoundTag.getDouble("BuildOriginY"),
                compoundTag.getDouble("BuildOriginZ")
        );
        if (compoundTag.contains(BUILD_TYPE_TAG))
            pendingSummonType = SummonType.valueOf(compoundTag.getString(BUILD_TYPE_TAG));
        if (compoundTag.contains(ACTION_NAME_TAG))
            activeActionName = compoundTag.getString(ACTION_NAME_TAG);
    }

    private ListTag writeUuidList(Set<UUID> uuidSet) {
        final ListTag listTag = new ListTag();
        for (UUID uuid : uuidSet)
            listTag.add(NbtUtils.createUUID(uuid));
        return listTag;
    }

    private void readUuidList(ListTag listTag, Set<UUID> uuidSet) {
        uuidSet.clear();
        for (Tag entry : listTag)
            uuidSet.add(NbtUtils.loadUUID(entry));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("build_automaton_box", BUILD_AUTOMATON_ANIMATION)
                .triggerableAnim("build_drone_box", BUILD_DRONE_ANIMATION)
                .triggerableAnim("build_tower_box", BUILD_TOWER_ANIMATION)
                .triggerableAnim("throw_projectile", THROW_ANIMATION)
                .triggerableAnim("melee_attack", MELEE_ANIMATION)
                .triggerableAnim("attack3", ATTACK3_ANIMATION)
                .triggerableAnim("attack4", ATTACK4_ANIMATION)
                .triggerableAnim("damage", DAMAGE_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<RedstoneEngineerEntity> animationState) {
        if (isDeadOrDying())
            return animationState.setAndContinue(DEATH_ANIMATION);
        if (isBuilding() || isPerformingAction())
            return PlayState.CONTINUE;
        if (animationState.isMoving())
            return animationState.setAndContinue(WALK_ANIMATION);
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public String modelName() {
        return "redstone_engineer";
    }

    @Override
    public String textureName() {
        return "redstone_engineer";
    }

    @Override
    public String animationName() {
        return "redstone_engineer";
    }
}
