package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.entity.ai.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"deprecation"})
public class RegularSpiderEntity extends MobExGeckoEntity {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(RegularSpiderEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_CLIMB_WALL = SynchedEntityData.defineId(RegularSpiderEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<ItemStack> DATA_CARRIED_ITEM = SynchedEntityData.defineId(RegularSpiderEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_CARRIED_EXPERIENCE = SynchedEntityData.defineId(RegularSpiderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SILK_RESERVE = SynchedEntityData.defineId(RegularSpiderEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("atk");
    private static final RawAnimation POISON_ANIMATION = RawAnimation.begin().thenPlay("pois");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private static final int DEATH_ANIMATION_TICKS = 50;
    public static final int HOME_RADIUS = 12;
    public static final int MAX_SILK_RESERVE = 10;
    public static final double TARGET_SEARCH_VERTICAL = 2.5D;
    private static final double MELEE_MAX_HEIGHT_ABOVE = 1.0D;
    private static final double TARGET_MAX_HEIGHT_BELOW = 3.0D;
    private static final double STANDING_SPEED_SQR = 0.01D;
    private static final double SEPARATION_STRENGTH = 0.2D;
    private static final String SPAWNER_UUID_TAG = "SpawnerUUID";
    private static final String SPAWN_ANIM_PLAYED_TAG = "SpawnAnimPlayed";
    private static final String CARRIED_ITEM_TAG = "CarriedItem";
    private static final String CARRIED_EXPERIENCE_TAG = "CarriedExperience";
    private static final String SILK_RESERVE_TAG = "SilkReserve";
    private static final ResourceLocation COBWEB_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("mobexpansion", "cobweb_speed");
    private static final AttributeModifier COBWEB_SPEED_MODIFIER =
            new AttributeModifier(COBWEB_SPEED_MODIFIER_ID, 0.55D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final int EXPERIENCE_PASS_COOLDOWN_TICKS = 60;

    @Nullable
    private UUID spawnerUUID;
    private boolean spawnAnimationPlayed;
    private boolean notifiedSpawner;
    private int spawnClimbGraceTicks;
    private boolean inCobweb;
    private int experiencePassCooldown;
    private boolean suppressDeathExperience;

    public RegularSpiderEntity(EntityType<? extends RegularSpiderEntity> entityType, Level level) {
        super(entityType, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 14.0D)
                .add(Attributes.ARMOR, 1.0D);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        final SpawnGroupData spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        restrictToHome(blockPosition());
        return spawnData;
    }

    public int getHomeRadius() {
        return MobExParameters.getInt(this, "homeRadius", HOME_RADIUS);
    }

    public int getMaxSilkReserve() {
        return MobExParameters.getInt(this, "maxSilkReserve", MAX_SILK_RESERVE);
    }

    public int getCombatLeashBuffer() {
        return MobExParameters.getInt(this, "combatLeashBuffer", 8);
    }

    public int getMaxComfortableLight() {
        return MobExParameters.getInt(this, "maxComfortableLight", 12);
    }

    public void restrictToHome(BlockPos homePos) {
        restrictToHome(homePos, getHomeRadius());
    }

    public void restrictToHome(BlockPos homePos, int radius) {
        restrictTo(homePos, Math.max(getHomeRadius(), radius));
    }

    public boolean isWithinPatrolZone(BlockPos blockPos) {
        return !hasRestriction() || isWithinRestriction(blockPos);
    }

    public boolean isWithinCombatLeash(BlockPos blockPos) {
        if (!hasRestriction())
            return true;
        final double leashRadius = getRestrictRadius() + getCombatLeashBuffer();
        return getRestrictCenter().distSqr(blockPos) <= leashRadius * leashRadius;
    }

    public boolean isOutsideSoftLeash() {
        return hasRestriction() && !isWithinCombatLeash(blockPosition());
    }

    public void refreshHomeFromSpawner() {
        final SpiderSpawnerEntity spiderSpawner = findLinkedSpawner();
        if (spiderSpawner == null)
            return;
        restrictToHome(spiderSpawner.blockPosition(), spiderSpawner.getPatrolRadius());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS_ID, (byte) 0);
        builder.define(DATA_CLIMB_WALL, (byte) 0);
        builder.define(DATA_CARRIED_ITEM, ItemStack.EMPTY);
        builder.define(DATA_CARRIED_EXPERIENCE, 0);
        builder.define(DATA_SILK_RESERVE, getMaxSilkReserve());
    }

    public int getSilkReserve() {
        return entityData.get(DATA_SILK_RESERVE);
    }

    public void setSilkReserve(int silkAmount) {
        entityData.set(DATA_SILK_RESERVE, Mth.clamp(silkAmount, 0, getMaxSilkReserve()));
    }

    public void consumeSilk(int amount) {
        if (amount <= 0)
            return;
        setSilkReserve(getSilkReserve() - amount);
    }

    public void refillSilk() {
        setSilkReserve(getMaxSilkReserve());
    }

    public boolean needsSilkRefill() {
        return getSilkReserve() <= 0;
    }

    public boolean canWeaveSilk() {
        return getTarget() == null && !isCarryingLoot() && getSilkReserve() > 0;
    }

    public ItemStack getCarriedItem() {
        return entityData.get(DATA_CARRIED_ITEM);
    }

    public void setCarriedItem(ItemStack itemStack) {
        entityData.set(DATA_CARRIED_ITEM, itemStack == null ? ItemStack.EMPTY : itemStack.copy());
    }

    public int getCarriedExperience() {
        return entityData.get(DATA_CARRIED_EXPERIENCE);
    }

    public void setCarriedExperience(int experienceAmount) {
        entityData.set(DATA_CARRIED_EXPERIENCE, Math.max(0, experienceAmount));
    }

    public boolean canPassExperience() {
        return experiencePassCooldown <= 0;
    }

    public void markExperiencePassed() {
        experiencePassCooldown = EXPERIENCE_PASS_COOLDOWN_TICKS;
    }

    public boolean shouldAvoidBrightLight() {
        return !level().isClientSide() && !level().isDay();
    }

    public boolean isTooBright(BlockPos blockPos) {
        return shouldAvoidBrightLight() && level().getMaxLocalRawBrightness(blockPos) > getMaxComfortableLight();
    }

    @Override
    public float getWalkTargetValue(@NotNull BlockPos blockPos) {
        return getWalkTargetValue(blockPos, level());
    }

    @Override
    public float getWalkTargetValue(@NotNull BlockPos blockPos, @NotNull LevelReader levelReader) {
        final float walkValue = super.getWalkTargetValue(blockPos, levelReader);
        if (shouldAvoidBrightLight() && levelReader.getMaxLocalRawBrightness(blockPos) > getMaxComfortableLight())
            return walkValue - 50.0F;
        return walkValue;
    }

    public boolean isCarryingItem() {
        return false;
    }

    public boolean isCarryingLoot() {
        return getCarriedExperience() > 0;
    }

    public void clearCarriedLoot() {
        setCarriedItem(ItemStack.EMPTY);
        setCarriedExperience(0);
    }

    public void setSpawnClimbGrace(int ticks) {
        spawnClimbGraceTicks = Math.max(0, ticks);
    }

    public void markAbsorbedIntoNest() {
        notifiedSpawner = true;
        spawnerUUID = null;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean wantsToPickUp(@NotNull ItemStack itemStack) {
        return false;
    }

    @Override
    protected void pickUpItem(@NotNull ItemEntity itemEntity) {
    }

    @Override
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        return new WallClimberNavigation(this, level);
    }

    public void setSpawnerUUID(@Nullable UUID spawnerUUID) {
        this.spawnerUUID = spawnerUUID;
    }

    public Optional<UUID> getSpawnerUUID() {
        return Optional.ofNullable(spawnerUUID);
    }

    public boolean isSameNest(RegularSpiderEntity otherSpider) {
        return spawnerUUID != null && spawnerUUID.equals(otherSpider.spawnerUUID);
    }

    public void alertNestmates(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || spawnerUUID == null || level().isClientSide())
            return;
        final List<RegularSpiderEntity> nestmates = level().getEntitiesOfClass(
                RegularSpiderEntity.class,
                getBoundingBox().inflate(16.0D),
                nestmate -> nestmate != this && nestmate.isAlive() && isSameNest(nestmate)
        );
        for (RegularSpiderEntity nestmate : nestmates) {
            if (nestmate.getTarget() != null)
                continue;
            if (nestmate.hasRestriction() && !nestmate.isWithinCombatLeash(target.blockPosition()))
                continue;
            if (!nestmate.isWithinVerticalEngageRange(target))
                continue;
            nestmate.setTarget(target);
        }
    }

    @Nullable
    public SpiderSpawnerEntity findLinkedSpawner() {
        if (spawnerUUID == null || !(level() instanceof ServerLevel serverLevel))
            return null;
        final Entity spawnerEntity = serverLevel.getEntity(spawnerUUID);
        if (spawnerEntity instanceof SpiderSpawnerEntity spiderSpawner && spiderSpawner.isAlive())
            return spiderSpawner;
        return null;
    }

    public void playSpawnAnimation() {
        if (level().isClientSide() || spawnAnimationPlayed)
            return;
        spawnAnimationPlayed = true;
        triggerAnim("main", "spawn");
    }

    public boolean isClimbing() {
        return (entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public boolean isOnCeiling() {
        return (entityData.get(DATA_FLAGS_ID) & 2) != 0;
    }

    public void setClimbing(boolean climbing) {
        byte flags = entityData.get(DATA_FLAGS_ID);
        if (climbing)
            flags = (byte) (flags | 1);
        else
            flags = (byte) (flags & ~1);
        entityData.set(DATA_FLAGS_ID, flags);
    }

    public void setOnCeiling(boolean onCeiling) {
        byte flags = entityData.get(DATA_FLAGS_ID);
        if (onCeiling)
            flags = (byte) (flags | 2);
        else
            flags = (byte) (flags & ~2);
        entityData.set(DATA_FLAGS_ID, flags);
    }

    @Nullable
    public Direction getClimbWallDirection() {
        final byte wallValue = entityData.get(DATA_CLIMB_WALL);
        if (wallValue == 0)
            return null;
        final Direction direction = Direction.from3DDataValue(wallValue);
        return direction.getAxis().isHorizontal() ? direction : null;
    }

    public void setClimbWallDirection(@Nullable Direction direction) {
        if (direction == null || !direction.getAxis().isHorizontal()) {
            entityData.set(DATA_CLIMB_WALL, (byte) 0);
            return;
        }
        entityData.set(DATA_CLIMB_WALL, (byte) direction.get3DDataValue());
    }

    @Override
    public boolean onClimbable() {
        return isClimbing() || inCobweb;
    }

    public boolean isInCobweb() {
        return inCobweb;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SpiderFleeToNestGoal(this, 1.35D));
        goalSelector.addGoal(2, new SpiderMeleeAttackGoal(this, 1.1D, true));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(4, new SpiderHarvestCorpseGoal(this));
        goalSelector.addGoal(5, new SpiderPassItemGoal(this));
        goalSelector.addGoal(6, new SpiderDeliverItemGoal(this));
        goalSelector.addGoal(7, new SpiderCollectItemGoal(this));
        goalSelector.addGoal(8, new SpiderRefillSilkGoal(this));
        goalSelector.addGoal(9, new SpiderEnterNestGoal(this));
        goalSelector.addGoal(10, new SpiderWeaveSilkGoal(this));
        goalSelector.addGoal(11, new SpiderAvoidBrightLightGoal(this));
        goalSelector.addGoal(12, new SpiderPatrolGoal(this, 1.0D));
        goalSelector.addGoal(13, new SpiderReturnToNestGoal(this, 1.0D));
        goalSelector.addGoal(14, new MoveTowardsRestrictionGoal(this, 1.0D));
        goalSelector.addGoal(15, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(16, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(17, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new SpiderNearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isValidAttackTarget));
    }

    public boolean isValidAttackTarget(LivingEntity target) {
        if (!target.isAlive() || target == this)
            return false;
        if (target instanceof RegularSpiderEntity || target instanceof SpiderSpawnerEntity)
            return false;
        if (!isWithinPatrolZone(target.blockPosition()))
            return false;
        if (!isWithinVerticalEngageRange(target))
            return false;
        return MobExTargeting.isValidHostileTarget(target);
    }

    protected double getMaxTargetHeightAbove() {
        return MELEE_MAX_HEIGHT_ABOVE;
    }

    protected double getMaxTargetHeightBelow() {
        return TARGET_MAX_HEIGHT_BELOW;
    }

    public boolean isWithinVerticalEngageRange(LivingEntity target) {
        final double deltaY = target.getY() - getY();
        if (deltaY > getMaxTargetHeightAbove())
            return false;
        return !(deltaY < -getMaxTargetHeightBelow());
    }

    public float getFleeHealthRatio() {
        return MobExParameters.getFloat(this, "fleeHealthRatio", 0.35F);
    }

    public boolean shouldFleeToNest() {
        return getHealth() <= getMaxHealth() * getFleeHealthRatio();
    }

    protected void applyMeleeAttackEffects(LivingEntity livingTarget) {
        if (random.nextFloat() < MobExParameters.getFloat(this, "meleePoisonChance", 0.25F)) {
            triggerAnim("main", "pois");
            livingTarget.addEffect(new MobEffectInstance(
                    MobEffects.POISON,
                    MobExParameters.getInt(this, "meleePoisonDuration", 60),
                    MobExParameters.getInt(this, "meleePoisonAmplifier", 0)
            ));
            return;
        }
        triggerAnim("main", "atk");
    }

    public void requestPoisonCover(@Nullable LivingEntity threat) {
        if (threat == null || !threat.isAlive() || level().isClientSide() || spawnerUUID == null)
            return;
        final List<PoisonSpiderEntity> poisonNestmates = level().getEntitiesOfClass(
                PoisonSpiderEntity.class,
                getBoundingBox().inflate(16.0D),
                nestmate -> nestmate != this && nestmate.isAlive() && isSameNest(nestmate)
        );
        for (PoisonSpiderEntity poisonNestmate : poisonNestmates) {
            poisonNestmate.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
            poisonNestmate.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
            if (poisonNestmate.getTarget() != null)
                continue;
            if (poisonNestmate.hasRestriction() && !poisonNestmate.isWithinCombatLeash(threat.blockPosition()))
                continue;
            poisonNestmate.setTarget(threat);
        }
    }

    @Override
    public void makeStuckInBlock(BlockState blockState, @NotNull Vec3 speedMultiplier) {
        if (!blockState.is(Blocks.COBWEB))
            super.makeStuckInBlock(blockState, speedMultiplier);
    }

    @Override
    public boolean canCollideWith(@NotNull Entity entity) {
        return !(entity instanceof RegularSpiderEntity) && super.canCollideWith(entity);
    }

    @Override
    public boolean isPushable() {
        return getTarget() != null && super.isPushable();
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        if (getTarget() == null || entity instanceof RegularSpiderEntity)
            return;
        super.doPush(entity);
    }

    @Override
    public void push(@NotNull Entity entity) {
        if (getTarget() == null || entity instanceof RegularSpiderEntity)
            return;
        super.push(entity);
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.is(DamageTypes.IN_WALL))
            return true;
        return super.isInvulnerableTo(damageSource);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
    }

    @Override
    public void tick() {
        inCobweb = detectInsideCobweb();
        if (!level().isClientSide())
            updateCobwebSpeedBoost(inCobweb);
        super.tick();
        if (inCobweb) {
            crawlInsideCobweb();
            resetFallDistance();
        }
        if (!level().isClientSide()) {
            if (experiencePassCooldown > 0)
                experiencePassCooldown--;
            if (!hasRestriction())
                refreshHomeFromSpawner();
            if (tickCount % 40 == 0)
                refreshHomeFromSpawner();
            if (!hasRestriction())
                restrictToHome(blockPosition());
            if (!spawnAnimationPlayed) {
                spawnAnimationPlayed = true;
                triggerAnim("main", "spawn");
            }
            if (isOutsideSoftLeash())
                setTarget(null);
            clearTargetIfOutsideNestZone();
            clearTargetIfOutOfVerticalReach();
            final boolean canClimb = spawnClimbGraceTicks <= 0;
            if (spawnClimbGraceTicks > 0)
                spawnClimbGraceTicks--;

            final boolean touchingCeiling = canClimb && isTouchingCeiling() && !isInsideSolidBlock();
            setOnCeiling(touchingCeiling);
            setClimbWallDirection(null);
            setClimbing(canClimb && (horizontalCollision || touchingCeiling || inCobweb));
            if (touchingCeiling) {
                final Vec3 movement = getDeltaMovement();
                if (movement.y < 0.0D)
                    setDeltaMovement(movement.x, 0.0D, movement.z);
                resetFallDistance();
            }
            separateFromStandingSpiders();
        }
    }

    private void crawlInsideCobweb() {
        final Vec3 movement = getDeltaMovement();
        double verticalSpeed = movement.y;
        if (verticalSpeed < 0.0D)
            verticalSpeed = 0.0D;

        final BlockPos climbTarget = getNavigation().getTargetPos();
        final boolean shouldClimbTowardTarget = horizontalCollision
                && getNavigation().isInProgress()
                && climbTarget != null
                && climbTarget.getY() > getBlockY();
        if (shouldClimbTowardTarget)
            verticalSpeed = Math.max(verticalSpeed, 0.22D);

        setDeltaMovement(movement.x, verticalSpeed, movement.z);
    }

    private void updateCobwebSpeedBoost(boolean insideCobweb) {
        final AttributeInstance movementSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null)
            return;
        if (insideCobweb) {
            movementSpeed.addOrUpdateTransientModifier(COBWEB_SPEED_MODIFIER);
            return;
        }
        movementSpeed.removeModifier(COBWEB_SPEED_MODIFIER_ID);
    }

    private boolean detectInsideCobweb() {
        final AABB boundingBox = getBoundingBox();
        final BlockPos minPos = BlockPos.containing(boundingBox.minX + 0.001D, boundingBox.minY + 0.001D, boundingBox.minZ + 0.001D);
        final BlockPos maxPos = BlockPos.containing(boundingBox.maxX - 0.001D, boundingBox.maxY - 0.001D, boundingBox.maxZ - 0.001D);
        final BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int blockX = minPos.getX(); blockX <= maxPos.getX(); blockX++) {
            for (int blockY = minPos.getY(); blockY <= maxPos.getY(); blockY++) {
                for (int blockZ = minPos.getZ(); blockZ <= maxPos.getZ(); blockZ++) {
                    checkPos.set(blockX, blockY, blockZ);
                    if (level().getBlockState(checkPos).is(Blocks.COBWEB))
                        return true;
                }
            }
        }
        return false;
    }

    private void clearTargetIfOutsideNestZone() {
        final LivingEntity attackTarget = getTarget();
        if (attackTarget == null)
            return;
        if (!attackTarget.isAlive()) {
            setTarget(null);
            return;
        }
        if (!hasRestriction())
            return;
        if (isWithinCombatLeash(attackTarget.blockPosition()))
            return;
        setTarget(null);
        getNavigation().stop();
    }

    private void clearTargetIfOutOfVerticalReach() {
        final LivingEntity attackTarget = getTarget();
        if (attackTarget == null)
            return;
        if (isWithinVerticalEngageRange(attackTarget))
            return;
        setTarget(null);
        getNavigation().stop();
    }

    private void separateFromStandingSpiders() {
        if (getDeltaMovement().horizontalDistanceSqr() > STANDING_SPEED_SQR)
            return;

        final List<RegularSpiderEntity> overlappingSpiders = level().getEntitiesOfClass(
                RegularSpiderEntity.class,
                getBoundingBox(),
                spider -> spider != this && spider.isAlive() && getBoundingBox().intersects(spider.getBoundingBox())
        );
        if (overlappingSpiders.isEmpty())
            return;

        for (RegularSpiderEntity otherSpider : overlappingSpiders) {
            if (otherSpider.getDeltaMovement().horizontalDistanceSqr() > STANDING_SPEED_SQR)
                continue;

            double offsetX = getX() - otherSpider.getX();
            double offsetZ = getZ() - otherSpider.getZ();
            double distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
            if (distanceSquared < 1.0E-4D) {
                offsetX = random.nextDouble() - 0.5D;
                offsetZ = random.nextDouble() - 0.5D;
                distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
            }

            final double distance = Math.sqrt(distanceSquared);
            setDeltaMovement(getDeltaMovement().add(
                    offsetX / distance * SEPARATION_STRENGTH,
                    0.0D,
                    offsetZ / distance * SEPARATION_STRENGTH
            ));
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        final boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt && !level().isClientSide()) {
            notifySpawnerOfHurt();
            final Entity attacker = damageSource.getEntity();
            if (attacker instanceof LivingEntity livingAttacker) {
                alertNestmates(livingAttacker);
                if (shouldFleeToNest() && !(this instanceof PoisonSpiderEntity))
                    requestPoisonCover(livingAttacker);
                final SpiderSpawnerEntity spiderSpawner = findLinkedSpawner();
                if (spiderSpawner != null)
                    spiderSpawner.alertAllSpiders(livingAttacker);
            }
        }
        return wasHurt;
    }

    private void notifySpawnerOfHurt() {
        if (spawnerUUID == null || !(level() instanceof ServerLevel serverLevel))
            return;
        final Entity spawnerEntity = serverLevel.getEntity(spawnerUUID);
        if (spawnerEntity instanceof SpiderSpawnerEntity spiderSpawner)
            spiderSpawner.onLinkedSpiderHurt();
    }

    private boolean isInsideSolidBlock() {
        final AABB innerBox = getBoundingBox().deflate(0.2D);
        if (innerBox.getXsize() <= 0.0D || innerBox.getYsize() <= 0.0D || innerBox.getZsize() <= 0.0D)
            return false;
        return level().getBlockCollisions(this, innerBox).iterator().hasNext();
    }

    private boolean isTouchingCeiling() {
        final AABB boundingBox = getBoundingBox();
        final double topY = boundingBox.maxY;
        return isCeilingAbovePoint(getX(), topY, getZ())
                || isCeilingAbovePoint(boundingBox.minX + 0.35D, topY, boundingBox.minZ + 0.35D)
                || isCeilingAbovePoint(boundingBox.maxX - 0.35D, topY, boundingBox.minZ + 0.35D)
                || isCeilingAbovePoint(boundingBox.minX + 0.35D, topY, boundingBox.maxZ - 0.35D)
                || isCeilingAbovePoint(boundingBox.maxX - 0.35D, topY, boundingBox.maxZ - 0.35D);
    }

    private boolean isCeilingAbovePoint(double posX, double topY, double posZ) {
        final BlockPos bodyColumnPos = BlockPos.containing(posX, topY - 0.25D, posZ);
        final BlockState bodyColumnState = level().getBlockState(bodyColumnPos);
        if (!bodyColumnState.isAir()
                && !bodyColumnState.is(Blocks.COBWEB)
                && !bodyColumnState.getCollisionShape(level(), bodyColumnPos).isEmpty())
            return false;

        final BlockPos abovePos = BlockPos.containing(posX, topY + 0.12D, posZ);
        final BlockState blockState = level().getBlockState(abovePos);
        if (blockState.isAir() || blockState.is(Blocks.COBWEB))
            return false;
        if (!blockState.isFaceSturdy(level(), abovePos, Direction.DOWN))
            return false;
        return abovePos.getY() >= Mth.floor(topY);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        final boolean attacked = super.doHurtTarget(target);
        if (attacked) {
            final SpiderSpawnerEntity spiderSpawner = findLinkedSpawner();
            if (spiderSpawner != null) {
                final float attackDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
                spiderSpawner.onNestSpiderDealtDamage(attackDamage);
            }
            if (target instanceof LivingEntity livingTarget)
                applyMeleeAttackEffects(livingTarget);
        }
        return attacked;
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= DEATH_ANIMATION_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!isDeadOrDying())
            triggerAnim("main", "death");
        tryRegisterCorpseLoot(damageSource);
        dropCarriedItem();
        super.die(damageSource);
        notifySpawnerOfDeath();
    }

    private void tryRegisterCorpseLoot(DamageSource damageSource) {
        if (level().isClientSide() || !(level() instanceof ServerLevel serverLevel) || spawnerUUID == null)
            return;
        final Entity spawnerEntity = serverLevel.getEntity(spawnerUUID);
        if (!(spawnerEntity instanceof SpiderSpawnerEntity spiderSpawner) || !spiderSpawner.isAlive())
            return;
        if (!SpiderHarvestCorpseGoal.isFarFromNest(this, spiderSpawner))
            return;

        int experienceAmount = getCarriedExperience();
        if (shouldDropExperience())
            experienceAmount += getExperienceReward(serverLevel, damageSource.getEntity());
        if (experienceAmount <= 0)
            return;

        spiderSpawner.registerCorpseLoot(blockPosition(), experienceAmount);
        setCarriedExperience(0);
        suppressDeathExperience = true;
    }

    @Override
    public boolean shouldDropExperience() {
        return !suppressDeathExperience && super.shouldDropExperience();
    }

    @Override
    protected void dropExperience(@Nullable Entity attacker) {
        if (suppressDeathExperience)
            return;
        super.dropExperience(attacker);
    }

    private void dropCarriedItem() {
        if (level().isClientSide())
            return;
        final ItemStack carriedItem = getCarriedItem();
        if (!carriedItem.isEmpty()) {
            final ItemEntity itemEntity = new ItemEntity(level(), getX(), getY() + 0.5D, getZ(), carriedItem.copy());
            itemEntity.setDefaultPickUpDelay();
            level().addFreshEntity(itemEntity);
        }
        final int carriedExperience = getCarriedExperience();
        if (carriedExperience > 0 && level() instanceof ServerLevel serverLevel)
            ExperienceOrb.award(serverLevel, position(), carriedExperience);
        clearCarriedLoot();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy())
            notifySpawnerOfDeath();
        super.remove(reason);
    }

    private void notifySpawnerOfDeath() {
        if (notifiedSpawner || spawnerUUID == null || !(level() instanceof ServerLevel serverLevel))
            return;
        notifiedSpawner = true;
        final Entity spawnerEntity = serverLevel.getEntity(spawnerUUID);
        if (spawnerEntity instanceof SpiderSpawnerEntity spiderSpawner)
            spiderSpawner.onSpawnedSpiderRemoved(getUUID());
        spawnerUUID = null;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (spawnerUUID != null)
            compoundTag.putUUID(SPAWNER_UUID_TAG, spawnerUUID);
        compoundTag.putBoolean(SPAWN_ANIM_PLAYED_TAG, spawnAnimationPlayed);
        if (!getCarriedItem().isEmpty())
            compoundTag.put(CARRIED_ITEM_TAG, getCarriedItem().save(registryAccess()));
        compoundTag.putInt(CARRIED_EXPERIENCE_TAG, getCarriedExperience());
        compoundTag.putInt(SILK_RESERVE_TAG, getSilkReserve());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.hasUUID(SPAWNER_UUID_TAG))
            spawnerUUID = compoundTag.getUUID(SPAWNER_UUID_TAG);
        spawnAnimationPlayed = compoundTag.getBoolean(SPAWN_ANIM_PLAYED_TAG);
        if (compoundTag.contains(CARRIED_ITEM_TAG))
            setCarriedItem(ItemStack.parse(registryAccess(), Objects.requireNonNull(compoundTag.get(CARRIED_ITEM_TAG))).orElse(ItemStack.EMPTY));
        else
            setCarriedItem(ItemStack.EMPTY);
        setCarriedExperience(compoundTag.getInt(CARRIED_EXPERIENCE_TAG));
        if (compoundTag.contains(SILK_RESERVE_TAG))
            setSilkReserve(compoundTag.getInt(SILK_RESERVE_TAG));
        else
            setSilkReserve(getMaxSilkReserve());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleAnimations)
                .triggerableAnim("spawn", SPAWN_ANIMATION)
                .triggerableAnim("atk", ATTACK_ANIMATION)
                .triggerableAnim("pois", POISON_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<RegularSpiderEntity> animationState) {
        if (isDeadOrDying())
            return animationState.setAndContinue(DEATH_ANIMATION);
        if (animationState.isMoving())
            return animationState.setAndContinue(WALK_ANIMATION);
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public String modelName() {
        return "regular_spider";
    }

    @Override
    public String textureName() {
        return "regular_spider";
    }

    @Override
    public String animationName() {
        return "regular_spider";
    }
}
