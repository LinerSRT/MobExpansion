package net.mcskill.mobexpansion.entity;

import net.mcskill.mobexpansion.init.MobExParameters;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpiderSpawnerEntity extends MobExGeckoEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation HIT_ANIMATION = RawAnimation.begin().thenPlay("hit");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("death");

    private static final EntityDataAccessor<Integer> SPIDERS_REMAINING = SynchedEntityData.defineId(SpiderSpawnerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPIDERS_TOTAL = SynchedEntityData.defineId(SpiderSpawnerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ACTIVATED = SynchedEntityData.defineId(SpiderSpawnerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_QUEEN = SynchedEntityData.defineId(SpiderSpawnerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> QUEEN_HEALTH = SynchedEntityData.defineId(SpiderSpawnerEntity.class, EntityDataSerializers.INT);

    public static final int MAX_OUTSIDE_SPIDERS = 2;
    public static final int COLLECTION_RADIUS = RegularSpiderEntity.HOME_RADIUS;
    private static final int DEATH_ANIMATION_TICKS = 10;
    private static final int SPIDER_HURT_COOLDOWN_REDUCTION = 45;
    private static final int PATROL_REFRESH_INTERVAL = 40;
    private static final int NEW_SPAWNER_CLEARANCE = 5;
    private static final int[][] THREAD_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final float EXPERIENCE_HEAL_MULTIPLIER = 1.0F;
    private static final int SPAWN_CLIMB_GRACE_TICKS = 40;
    private static final int COBWEB_ALARM_COOLDOWN_TICKS = 200;
    private static final int QUEEN_BUFF_INTERVAL = 40;
    private static final String STOCK_TAG = "SpidersRemaining";
    private static final String TOTAL_STOCK_TAG = "SpidersTotal";
    private static final String ACTIVATED_TAG = "Activated";
    private static final String LIVING_TAG = "LivingSpiders";
    private static final String SPAWN_COOLDOWN_TAG = "SpawnCooldown";
    private static final String COLLECTED_ITEMS_TAG = "CollectedItems";
    private static final String STOCK_REFILL_TAG = "StockRefillProgress";
    private static final String EXPANSION_DAMAGE_TAG = "ExpansionDamage";
    private static final String HAS_QUEEN_TAG = "HasQueen";
    private static final String QUEEN_HEALTH_TAG = "QueenHealth";
    private static final String HIGH_STOCK_TICKS_TAG = "HighStockTicks";
    private static final String CORPSES_TAG = "NestCorpses";

    private final Set<UUID> livingSpiderIds = new HashSet<>();
    private final Map<Integer, UUID> claimedWeaveThreads = new HashMap<>();
    private final List<NestCorpseLoot> nestCorpses = new ArrayList<>();
    private int spawnCooldown;
    private int cobwebAlarmCooldown;
    private int collectedItemCount;
    private int stockRefillProgress;
    private float expansionDamage;
    private int highStockTicks;

    public SpiderSpawnerEntity(EntityType<? extends SpiderSpawnerEntity> entityType, Level level) {
        super(entityType, level);
        setNoAi(true);
        setPersistenceRequired();
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPIDERS_REMAINING, 0);
        builder.define(SPIDERS_TOTAL, 0);
        builder.define(ACTIVATED, false);
        builder.define(HAS_QUEEN, false);
        builder.define(QUEEN_HEALTH, 0);
    }

    @Override
    protected void registerGoals() {
    }

    @SuppressWarnings("deprecation")
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        final SpawnGroupData spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        rollInitialStockIfNeeded();
        return spawnData;
    }

    public void rollInitialStockIfNeeded() {
        if (level().isClientSide())
            return;
        if (getSpidersTotal() > 0 || getSpidersRemaining() > 0)
            return;
        final int minStock = MobExParameters.getInt(this, "minSpiderStock", 25);
        final int maxStock = MobExParameters.getInt(this, "maxSpiderStock", 50);
        final int stock = Mth.nextInt(random, minStock, Math.max(minStock, maxStock));
        setSpidersRemaining(stock);
        setSpidersTotal(stock);
    }

    public int getMaxOutsideSpiders() {
        return MobExParameters.getInt(this, "maxOutsideSpiders", MAX_OUTSIDE_SPIDERS);
    }

    public int getPatrolHomeRadius() {
        return MobExParameters.getInt(this, "patrolHomeRadius", COLLECTION_RADIUS);
    }

    public int getMaxStockCap() {
        return MobExParameters.getInt(this, "maxStockCap", 80);
    }

    public int getMaxLivingSpiders() {
        return MobExParameters.getInt(this, "maxLivingSpiders", 25);
    }

    public boolean hasRolledStock() {
        return getSpidersTotal() > 0;
    }

    public int getSpidersRemaining() {
        return entityData.get(SPIDERS_REMAINING);
    }

    public int getSpidersTotal() {
        return entityData.get(SPIDERS_TOTAL);
    }

    public boolean hasQueen() {
        return entityData.get(HAS_QUEEN);
    }

    public int getQueenHealth() {
        return entityData.get(QUEEN_HEALTH);
    }

    public String getSpiderCountText() {
        final String stockText = getSpidersRemaining() + "/" + getSpidersTotal();
        if (!hasQueen())
            return stockText;
        return "Q " + stockText + " [" + getQueenHealth() + "]";
    }

    private void setSpidersRemaining(int count) {
        entityData.set(SPIDERS_REMAINING, Math.max(0, count));
    }

    private void setSpidersTotal(int count) {
        entityData.set(SPIDERS_TOTAL, Math.max(0, count));
    }

    private void setHasQueen(boolean hasQueen) {
        entityData.set(HAS_QUEEN, hasQueen);
    }

    private void setQueenHealth(int health) {
        entityData.set(QUEEN_HEALTH, Math.max(0, health));
    }

    private void addSpiderToStock(int amount) {
        if (amount <= 0)
            return;
        final int maxStockCap = getMaxStockCap();
        setSpidersRemaining(Math.min(maxStockCap, getSpidersRemaining() + amount));
        setSpidersTotal(Math.min(maxStockCap, getSpidersTotal() + amount));
    }

    public boolean isActivated() {
        return entityData.get(ACTIVATED);
    }

    private void setActivated(boolean activated) {
        entityData.set(ACTIVATED, activated);
    }

    public AABB getCollectionArea() {
        return getBoundingBox().inflate(getPatrolRadius());
    }

    public int getPatrolRadius() {
        final int homeRadius = getPatrolHomeRadius();
        final int neighborLinkRange = homeRadius * 2;
        int patrolRadius = homeRadius;
        final List<SpiderSpawnerEntity> neighborSpawners = level().getEntitiesOfClass(
                SpiderSpawnerEntity.class,
                getBoundingBox().inflate(neighborLinkRange),
                neighbor -> neighbor != this && neighbor.isAlive()
        );
        for (SpiderSpawnerEntity neighborSpawner : neighborSpawners) {
            final double neighborDistance = distanceTo(neighborSpawner);
            if (neighborDistance > neighborLinkRange)
                continue;
            final int expandedRadius = Mth.ceil(neighborDistance + homeRadius);
            if (expandedRadius > patrolRadius)
                patrolRadius = expandedRadius;
        }
        return patrolRadius;
    }

    public void applyPatrolZone(RegularSpiderEntity spider) {
        spider.restrictToHome(blockPosition(), getPatrolRadius());
    }

    private void refreshLivingSpiderPatrolZones() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        final int patrolRadius = getPatrolRadius();
        final BlockPos homePos = blockPosition();
        for (UUID spiderId : livingSpiderIds) {
            final Entity spiderEntity = serverLevel.getEntity(spiderId);
            if (spiderEntity instanceof RegularSpiderEntity spider && spider.isAlive())
                spider.restrictToHome(homePos, patrolRadius);
        }
    }

    public boolean canBeDestroyed() {
        return isActivated() && getSpidersRemaining() <= 0 && livingSpiderIds.isEmpty();
    }

    public int getLivingSpiderCount() {
        cleanupDeadSpiders();
        return livingSpiderIds.size();
    }

    public boolean isNestPeaceful() {
        if (!(level() instanceof ServerLevel serverLevel))
            return false;
        cleanupDeadSpiders();
        for (UUID spiderId : livingSpiderIds) {
            final Entity spiderEntity = serverLevel.getEntity(spiderId);
            if (spiderEntity instanceof RegularSpiderEntity spider && spider.getTarget() != null && spider.getTarget().isAlive())
                return false;
        }
        final List<LivingEntity> nearbyHostiles = level().getEntitiesOfClass(
                LivingEntity.class,
                getCollectionArea(),
                this::isNestThreat
        );
        return nearbyHostiles.isEmpty();
    }

    private boolean isNestThreat(LivingEntity livingEntity) {
        if (!livingEntity.isAlive() || livingEntity instanceof RegularSpiderEntity || livingEntity instanceof SpiderSpawnerEntity)
            return false;
        if (livingEntity instanceof Player)
            return false;
        return livingEntity instanceof Enemy;
    }

    public boolean shouldReduceOutsideSpiders() {
        return getLivingSpiderCount() > getMaxOutsideSpiders();
    }

    public boolean tryClaimWeaveThread(int directionIndex, UUID spiderId) {
        if (directionIndex < 0 || spiderId == null)
            return false;
        cleanupStaleWeaveClaims();
        final UUID currentClaimant = claimedWeaveThreads.get(directionIndex);
        if (currentClaimant != null && !currentClaimant.equals(spiderId))
            return false;
        claimedWeaveThreads.put(directionIndex, spiderId);
        return true;
    }

    public boolean isWeaveThreadAvailable(int directionIndex, UUID spiderId) {
        if (directionIndex < 0 || spiderId == null)
            return false;
        cleanupStaleWeaveClaims();
        final UUID currentClaimant = claimedWeaveThreads.get(directionIndex);
        return currentClaimant == null || currentClaimant.equals(spiderId);
    }

    public void releaseWeaveThread(int directionIndex, UUID spiderId) {
        if (directionIndex < 0 || spiderId == null)
            return;
        final UUID currentClaimant = claimedWeaveThreads.get(directionIndex);
        if (currentClaimant != null && currentClaimant.equals(spiderId))
            claimedWeaveThreads.remove(directionIndex);
    }

    public void releaseAllWeaveThreads(UUID spiderId) {
        if (spiderId == null)
            return;
        claimedWeaveThreads.entrySet().removeIf(entry -> spiderId.equals(entry.getValue()));
    }

    private void cleanupStaleWeaveClaims() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        final Iterator<Map.Entry<Integer, UUID>> claimIterator = claimedWeaveThreads.entrySet().iterator();
        while (claimIterator.hasNext()) {
            final Map.Entry<Integer, UUID> claimEntry = claimIterator.next();
            final Entity claimantEntity = serverLevel.getEntity(claimEntry.getValue());
            if (!(claimantEntity instanceof RegularSpiderEntity spider) || !spider.isAlive())
                claimIterator.remove();
        }
    }

    public void registerCorpseLoot(BlockPos deathPos, int experienceAmount) {
        if (level().isClientSide() || experienceAmount <= 0)
            return;
        nestCorpses.add(new NestCorpseLoot(deathPos, experienceAmount));
    }

    @Nullable
    public NestCorpseLoot findNearestCorpse(BlockPos fromPos) {
        NestCorpseLoot nearestCorpse = null;
        double nearestDistance = Double.MAX_VALUE;
        for (NestCorpseLoot nestCorpse : nestCorpses) {
            final double distance = nestCorpse.getPosition().distSqr(fromPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestCorpse = nestCorpse;
            }
        }
        return nearestCorpse;
    }

    public boolean hasCorpse(NestCorpseLoot nestCorpse) {
        return nestCorpses.contains(nestCorpse);
    }

    public int harvestCorpse(NestCorpseLoot nestCorpse) {
        if (!nestCorpses.remove(nestCorpse))
            return 0;
        return nestCorpse.getExperienceAmount();
    }

    private void tickNestCorpses() {
        nestCorpses.removeIf(NestCorpseLoot::tickAndExpired);
    }

    private void tickQueenLogic() {
        if (!isActivated() || isDeadOrDying())
            return;

        if (getSpidersRemaining() > MobExParameters.getInt(this, "queenStockThreshold", 40)) {
            if (!hasQueen()) {
                highStockTicks++;
                if (highStockTicks >= MobExParameters.getInt(this, "queenSpawnDelayTicks", 6000))
                    spawnQueen();
            }
        } else {
            highStockTicks = 0;
        }

        if (hasQueen() && tickCount % QUEEN_BUFF_INTERVAL == 0)
            applyQueenBuffToNest();
    }

    private void spawnQueen() {
        setHasQueen(true);
        setQueenHealth(MobExParameters.getInt(this, "queenMaxHealth", 60));
        highStockTicks = 0;
        triggerAnim("main", "spawn");
    }

    private void killQueen() {
        setHasQueen(false);
        setQueenHealth(0);
        triggerAnim("main", "hit");
    }

    private void damageQueen(float amount) {
        if (!hasQueen())
            return;
        setQueenHealth(getQueenHealth() - Mth.ceil(amount));
        if (getQueenHealth() <= 0)
            killQueen();
    }

    private void applyQueenBuffToNest() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        final double buffRadiusSqr = (double) getPatrolRadius() * getPatrolRadius();
        for (UUID spiderId : livingSpiderIds) {
            final Entity spiderEntity = serverLevel.getEntity(spiderId);
            if (!(spiderEntity instanceof RegularSpiderEntity spider) || !spider.isAlive())
                continue;
            if (spider.distanceToSqr(this) > buffRadiusSqr)
                continue;
            spider.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, QUEEN_BUFF_INTERVAL + 20, 0, true, true));
            spider.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, QUEEN_BUFF_INTERVAL + 20, 0, true, true));
            spider.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, QUEEN_BUFF_INTERVAL + 20, 0, true, false));
        }
    }

    public void onSpawnedSpiderRemoved(UUID spiderId) {
        livingSpiderIds.remove(spiderId);
        releaseAllWeaveThreads(spiderId);
    }

    public void absorbSpider(RegularSpiderEntity spider) {
        if (level().isClientSide() || !spider.isAlive())
            return;
        final int carriedExperience = spider.getCarriedExperience();
        if (carriedExperience > 0) {
            depositCollectedExperience(carriedExperience);
            spider.clearCarriedLoot();
        }
        spider.markAbsorbedIntoNest();
        livingSpiderIds.remove(spider.getUUID());
        releaseAllWeaveThreads(spider.getUUID());
        if (getSpidersRemaining() < getMaxStockCap()) {
            setSpidersRemaining(getSpidersRemaining() + 1);
            if (getSpidersRemaining() > getSpidersTotal())
                setSpidersTotal(getSpidersRemaining());
        }
        triggerAnim("main", "spawn");
        spider.discard();
    }

    public void onLinkedSpiderHurt() {
        if (!isActivated() || isDeadOrDying() || getSpidersRemaining() <= 0)
            return;
        spawnCooldown = Math.max(0, spawnCooldown - SPIDER_HURT_COOLDOWN_REDUCTION);
        if (random.nextFloat() < MobExParameters.getFloat(this, "spiderHurtSpawnChance", 0.65F))
            trySpawnSpider(true);
    }

    public void onNestSpiderDealtDamage(float damageAmount) {
        if (level().isClientSide() || !isActivated() || isDeadOrDying() || damageAmount <= 0.0F)
            return;
        expansionDamage += damageAmount;
        final float damageForNewSpawner = MobExParameters.getFloat(this, "damageForNewSpawner", 60.0F);
        while (expansionDamage >= damageForNewSpawner) {
            if (!trySpawnChildSpawner())
                return;
            expansionDamage -= damageForNewSpawner;
            triggerAnim("main", "spawn");
        }
    }

    public void alertAllSpiders(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || !(level() instanceof ServerLevel serverLevel))
            return;
        for (UUID spiderId : livingSpiderIds) {
            final Entity spiderEntity = serverLevel.getEntity(spiderId);
            if (!(spiderEntity instanceof RegularSpiderEntity spider) || !spider.isAlive())
                continue;
            if (spider.hasRestriction() && !spider.isWithinCombatLeash(target.blockPosition()))
                continue;
            if (!spider.isWithinVerticalEngageRange(target))
                continue;
            spider.setTarget(target);
        }
    }

    public void onCobwebDisturbed(LivingEntity disturber) {
        if (level().isClientSide() || !isActivated() || isDeadOrDying())
            return;
        if (cobwebAlarmCooldown > 0)
            return;
        if (disturber instanceof RegularSpiderEntity || disturber instanceof SpiderSpawnerEntity)
            return;
        if (disturber instanceof Player player && player.isSpectator())
            return;
        if (!getCollectionArea().intersects(disturber.getBoundingBox()))
            return;

        cobwebAlarmCooldown = COBWEB_ALARM_COOLDOWN_TICKS;
        cleanupDeadSpiders();
        alertAllSpiders(disturber);

        final int stock = getSpidersRemaining();
        final int maxLivingSpiders = getMaxLivingSpiders();
        final int livingRoom = Math.max(0, maxLivingSpiders - livingSpiderIds.size());
        final float cobwebAlarmSpawnFraction = MobExParameters.getFloat(this, "cobwebAlarmSpawnFraction", 0.7F);
        final int spawnCount = Math.min(livingRoom, Math.min(stock, Mth.ceil(stock * cobwebAlarmSpawnFraction)));
        for (int spawnIndex = 0; spawnIndex < spawnCount; spawnIndex++) {
            if (!trySpawnSpider(true, disturber, maxLivingSpiders))
                break;
        }

        spawnCooldown = Math.max(spawnCooldown, 40);
        triggerAnim("main", "hit");
    }

    public void depositCollectedItem(ItemStack itemStack) {
        if (level().isClientSide() || itemStack.isEmpty())
            return;
        applyDepositProgress(itemStack.getCount(), MobExParameters.getInt(this, "itemsForStockRefill", 5), false, 0.0F);
    }

    public void depositCollectedExperience(int experienceAmount) {
        if (level().isClientSide() || experienceAmount <= 0)
            return;
        applyDepositProgress(
                experienceAmount,
                MobExParameters.getInt(this, "experienceForStockRefill", 3),
                true,
                experienceAmount * EXPERIENCE_HEAL_MULTIPLIER
        );
    }

    private void applyDepositProgress(int progressAmount, int stockRefillCost, boolean healEntireNest, float experienceHealAmount) {
        collectedItemCount += progressAmount;
        stockRefillProgress += progressAmount;
        triggerAnim("main", "hit");

        final int nestHealAmount = MobExParameters.getInt(this, "nestHealAmount", 4);
        if (healEntireNest)
            healAllNestmates(Math.max(nestHealAmount, experienceHealAmount));
        else
            healNearbyNestmates();

        while (stockRefillProgress >= stockRefillCost) {
            stockRefillProgress -= stockRefillCost;
            if (getSpidersRemaining() < getMaxStockCap())
                addSpiderToStock(1);
        }
    }

    private void healAllNestmates(float healAmount) {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        for (UUID spiderId : livingSpiderIds) {
            final Entity spiderEntity = serverLevel.getEntity(spiderId);
            if (spiderEntity instanceof RegularSpiderEntity spider && spider.isAlive())
                spider.heal(healAmount);
        }
    }

    private void healNearbyNestmates() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        for (UUID spiderId : livingSpiderIds) {
            final Entity spiderEntity = serverLevel.getEntity(spiderId);
            if (!(spiderEntity instanceof RegularSpiderEntity spider) || !spider.isAlive())
                continue;
            if (spider.distanceToSqr(this) > 36.0D)
                continue;
            spider.heal(MobExParameters.getInt(this, "nestHealAmount", 4));
        }
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide())
            return;

        rollInitialStockIfNeeded();
        cleanupDeadSpiders();
        tryActivateNearPlayer();

        if (!isActivated() || isDeadOrDying())
            return;

        if (tickCount % PATROL_REFRESH_INTERVAL == 0)
            refreshLivingSpiderPatrolZones();

        if (cobwebAlarmCooldown > 0)
            cobwebAlarmCooldown--;

        tickNestCorpses();
        tickQueenLogic();

        if (spawnCooldown > 0)
            spawnCooldown--;

        if (spawnCooldown <= 0) {
            final boolean nestPeaceful = isNestPeaceful();
            final int livingCount = getLivingSpiderCount();
            final int livingLimit = nestPeaceful ? getMaxOutsideSpiders() : getMaxLivingSpiders();
            if (livingCount < livingLimit)
                trySpawnSpider(false);
        }
    }

    private void tryActivateNearPlayer() {
        if (isActivated())
            return;
        if (level().getNearestPlayer(this, MobExParameters.get(this, "activationRange", 16.0D)) == null)
            return;
        rollInitialStockIfNeeded();
        setActivated(true);
        triggerAnim("main", "spawn");
        spawnCooldown = 20;
    }

    private void cleanupDeadSpiders() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;

        final Iterator<UUID> spiderIterator = livingSpiderIds.iterator();
        while (spiderIterator.hasNext()) {
            final UUID spiderId = spiderIterator.next();
            final Entity spiderEntity = serverLevel.getEntity(spiderId);
            if (!(spiderEntity instanceof RegularSpiderEntity) || !spiderEntity.isAlive())
                spiderIterator.remove();
        }
    }

    private void trySpawnSpider(boolean forcedByHit) {
        trySpawnSpider(forcedByHit, null, -1);
    }

    private boolean trySpawnSpider(boolean forcedByHit, @Nullable LivingEntity attackTarget, int livingLimitOverride) {
        if (!(level() instanceof ServerLevel serverLevel))
            return false;
        if (getSpidersRemaining() <= 0)
            return false;

        final int livingLimit = livingLimitOverride > 0
                ? livingLimitOverride
                : (forcedByHit
                ? MobExParameters.getInt(this, "maxLivingOnHit", 10)
                : (isNestPeaceful() ? getMaxOutsideSpiders() : getMaxLivingSpiders()));
        if (livingSpiderIds.size() >= livingLimit)
            return false;

        final Vec3 spawnPosition = findSafeSpawnPosition(serverLevel);
        if (spawnPosition == null) {
            spawnCooldown = Math.max(spawnCooldown, 30);
            return false;
        }

        final RegularSpiderEntity spider = createNestSpider(serverLevel);
        if (spider == null)
            return false;

        final BlockPos spawnPos = BlockPos.containing(spawnPosition);
        spider.moveTo(spawnPosition.x, spawnPosition.y, spawnPosition.z, random.nextFloat() * 360.0F, 0.0F);
        spider.setSpawnerUUID(getUUID());
        spider.setSpawnClimbGrace(SPAWN_CLIMB_GRACE_TICKS);
        spider.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null);
        applyPatrolZone(spider);
        if (attackTarget != null && attackTarget.isAlive())
            spider.setTarget(attackTarget);
        serverLevel.addFreshEntityWithPassengers(spider);
        spider.playSpawnAnimation();

        livingSpiderIds.add(spider.getUUID());
        setSpidersRemaining(getSpidersRemaining() - 1);
        spawnCooldown = getSpawnIntervalTicks();
        triggerAnim("main", "spawn");
        return true;
    }

    @Nullable
    private RegularSpiderEntity createNestSpider(ServerLevel serverLevel) {
        if (random.nextFloat() < 0.30F)
            return MobExEntities.POISON_SPIDER.get().create(serverLevel);
        return MobExEntities.REGULAR_SPIDER.get().create(serverLevel);
    }

    @Nullable
    private Vec3 findSafeSpawnPosition(ServerLevel serverLevel) {
        final float spiderWidth = MobExEntities.REGULAR_SPIDER.get().getWidth();
        final float spiderHeight = MobExEntities.REGULAR_SPIDER.get().getHeight();

        for (int attempt = 0; attempt < 24; attempt++) {
            final double angle = random.nextDouble() * Math.PI * 2.0D;
            final double distance = 1.8D + random.nextDouble() * 1.5D;
            final double spawnX = getX() + Math.cos(angle) * distance;
            final double spawnZ = getZ() + Math.sin(angle) * distance;
            final double spawnY = getY();
            if (isSafeSpiderSpawn(serverLevel, spawnX, spawnY, spawnZ, spiderWidth, spiderHeight))
                return new Vec3(spawnX, spawnY, spawnZ);
        }

        final double topX = getX();
        final double topY = getY() + getBbHeight() + 0.1D;
        final double topZ = getZ();
        if (isSafeSpiderSpawn(serverLevel, topX, topY, topZ, spiderWidth, spiderHeight))
            return new Vec3(topX, topY, topZ);
        return null;
    }

    private boolean isSafeSpiderSpawn(ServerLevel serverLevel, double spawnX, double spawnY, double spawnZ, float spiderWidth, float spiderHeight) {
        final double halfWidth = spiderWidth / 2.0D;
        final AABB spawnBox = new AABB(
                spawnX - halfWidth,
                spawnY,
                spawnZ - halfWidth,
                spawnX + halfWidth,
                spawnY + spiderHeight,
                spawnZ + halfWidth
        );
        if (!serverLevel.noCollision(spawnBox))
            return false;
        final BlockPos floorPos = BlockPos.containing(spawnX, spawnY - 0.1D, spawnZ);
        final BlockState floorState = serverLevel.getBlockState(floorPos);
        return floorState.isFaceSturdy(serverLevel, floorPos, Direction.UP) || spawnY >= getY() + getBbHeight();
    }

    private int getSpawnIntervalTicks() {
        final int cobwebCount = countNearbyCobwebs();
        return Math.max(25, MobExParameters.getInt(this, "spawnIntervalTicks", 80) - cobwebCount * 3);
    }

    private int countNearbyCobwebs() {
        final BlockPos centerPos = blockPosition();
        int cobwebCount = 0;
        for (BlockPos checkPos : BlockPos.betweenClosed(centerPos.offset(-4, -2, -4), centerPos.offset(4, 3, 4))) {
            if (level().getBlockState(checkPos).is(Blocks.COBWEB))
                cobwebCount++;
        }
        return cobwebCount;
    }

    private boolean trySpawnChildSpawner() {
        if (!(level() instanceof ServerLevel serverLevel))
            return false;

        final List<BlockPos> threadTips = findCobwebThreadTips();
        if (threadTips.isEmpty())
            return false;

        Collections.shuffle(threadTips);
        final int newSpawnerMinDistance = MobExParameters.getInt(this, "newSpawnerMinDistance", 6);
        final double minDistanceSqr = (double) newSpawnerMinDistance * newSpawnerMinDistance;
        for (BlockPos tipPos : threadTips) {
            if (tipPos.distSqr(blockPosition()) < minDistanceSqr)
                continue;
            final BlockPos spawnPos = findSpawnablePositionNearTip(serverLevel, tipPos);
            if (spawnPos == null)
                continue;
            if (isSpawnerTooClose(serverLevel, spawnPos))
                continue;

            final SpiderSpawnerEntity childSpawner = MobExEntities.SPIDER_SPAWN.get().create(serverLevel);
            if (childSpawner == null)
                return false;

            childSpawner.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            childSpawner.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null);
            serverLevel.addFreshEntityWithPassengers(childSpawner);
            childSpawner.triggerAnim("main", "spawn");
            refreshLivingSpiderPatrolZones();
            return true;
        }
        return false;
    }

    private List<BlockPos> findCobwebThreadTips() {
        final List<BlockPos> tipPositions = new ArrayList<>();
        final BlockPos origin = blockPosition();
        for (int[] direction : THREAD_DIRECTIONS) {
            final BlockPos tipPos = walkToCobwebThreadTip(origin, direction);
            if (tipPos == null)
                continue;
            tipPositions.add(tipPos);
        }
        return tipPositions;
    }

    @Nullable
    private BlockPos walkToCobwebThreadTip(BlockPos origin, int[] direction) {
        BlockPos currentPos = findThreadWalkStart(origin);
        BlockPos lastCobwebPos = null;
        final int maxSteps = Math.max(6, getPatrolRadius()) * 2;

        for (int step = 0; step < maxSteps; step++) {
            final BlockPos nextPos = findNextCobwebAlongThread(currentPos, direction);
            if (nextPos == null)
                break;
            if (!level().getBlockState(nextPos).is(Blocks.COBWEB))
                break;
            lastCobwebPos = nextPos;
            currentPos = nextPos;
        }
        return lastCobwebPos;
    }

    private BlockPos findThreadWalkStart(BlockPos origin) {
        final BlockPos aboveOrigin = origin.above();
        if (level().getBlockState(aboveOrigin).is(Blocks.COBWEB))
            return aboveOrigin;
        if (level().getBlockState(origin).is(Blocks.COBWEB))
            return origin;
        for (Direction direction : Direction.values()) {
            final BlockPos neighborPos = origin.relative(direction);
            if (level().getBlockState(neighborPos).is(Blocks.COBWEB))
                return neighborPos;
        }
        return aboveOrigin;
    }

    @Nullable
    private BlockPos findNextCobwebAlongThread(BlockPos fromPos, int[] direction) {
        final BlockPos forwardPos = fromPos.offset(direction[0], 0, direction[1]);
        if (level().getBlockState(forwardPos).is(Blocks.COBWEB))
            return forwardPos;

        final BlockPos forwardUpPos = fromPos.offset(direction[0], 1, direction[1]);
        if (level().getBlockState(forwardUpPos).is(Blocks.COBWEB))
            return forwardUpPos;

        final BlockPos forwardDownPos = fromPos.offset(direction[0], -1, direction[1]);
        if (level().getBlockState(forwardDownPos).is(Blocks.COBWEB))
            return forwardDownPos;

        final BlockPos upPos = fromPos.above();
        if (level().getBlockState(upPos).is(Blocks.COBWEB) && isSolidBlocking(forwardPos))
            return upPos;

        final BlockPos downPos = fromPos.below();
        if (level().getBlockState(downPos).is(Blocks.COBWEB))
            return downPos;

        return null;
    }

    private boolean isSolidBlocking(BlockPos blockPos) {
        final BlockState blockState = level().getBlockState(blockPos);
        if (blockState.is(Blocks.COBWEB))
            return false;
        return !blockState.getCollisionShape(level(), blockPos).isEmpty();
    }

    @Nullable
    private BlockPos findSpawnablePositionNearTip(ServerLevel serverLevel, BlockPos tipPos) {
        final BlockPos[] candidateBases = {
                tipPos,
                tipPos.north(),
                tipPos.south(),
                tipPos.east(),
                tipPos.west(),
                tipPos.above(),
                tipPos.below()
        };
        for (BlockPos candidateBase : candidateBases) {
            final BlockPos spawnPos = findSpawnablePosition(serverLevel, candidateBase);
            if (spawnPos != null)
                return spawnPos;
        }
        return findSpawnablePosition(serverLevel, tipPos);
    }

    private boolean isSpawnerTooClose(ServerLevel serverLevel, BlockPos spawnPos) {
        final List<SpiderSpawnerEntity> nearbySpawners = serverLevel.getEntitiesOfClass(
                SpiderSpawnerEntity.class,
                new AABB(spawnPos).inflate(NEW_SPAWNER_CLEARANCE)
        );
        return !nearbySpawners.isEmpty();
    }

    private BlockPos findSpawnablePosition(ServerLevel serverLevel, BlockPos originPos) {
        BlockPos.MutableBlockPos checkPos = originPos.mutable();
        for (int offsetY = 4; offsetY >= -6; offsetY--) {
            checkPos.set(originPos.getX(), originPos.getY() + offsetY, originPos.getZ());
            final BlockState belowState = serverLevel.getBlockState(checkPos.below());
            final BlockState currentState = serverLevel.getBlockState(checkPos);
            final BlockState aboveState = serverLevel.getBlockState(checkPos.above());
            if (!belowState.isFaceSturdy(serverLevel, checkPos.below(), Direction.UP))
                continue;
            if (!currentState.getCollisionShape(serverLevel, checkPos).isEmpty())
                continue;
            if (!aboveState.getCollisionShape(serverLevel, checkPos.above()).isEmpty())
                continue;
            return checkPos.immutable();
        }
        return null;
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.is(DamageTypes.IN_WALL) || damageSource.is(DamageTypes.CRAMMING))
            return true;
        if (damageSource.is(DamageTypes.GENERIC_KILL) || damageSource.is(DamageTypes.FELL_OUT_OF_WORLD))
            return super.isInvulnerableTo(damageSource);
        if (!canBeDestroyed())
            return true;
        return super.isInvulnerableTo(damageSource);
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (level().isClientSide())
            return false;
        if (damageSource.is(DamageTypes.IN_WALL) || damageSource.is(DamageTypes.CRAMMING))
            return false;
        if (isInvulnerableTo(damageSource)) {
            if (isActivated() && !canBeDestroyed() && damageSource.getEntity() instanceof LivingEntity livingAttacker) {
                triggerAnim("main", "hit");
                if (hasQueen())
                    damageQueen(Math.max(1.0F, amount));
                final int spidersPerHit = MobExParameters.getInt(this, "spidersPerHit", 3);
                for (int spawnIndex = 0; spawnIndex < spidersPerHit; spawnIndex++)
                    trySpawnSpider(true);
                alertAllSpiders(livingAttacker);
            }
            return false;
        }
        final boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt) {
            final Entity attacker = damageSource.getEntity();
            if (attacker instanceof LivingEntity livingAttacker)
                alertAllSpiders(livingAttacker);
        }
        return wasHurt;
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!isDeadOrDying())
            triggerAnim("main", "death");
        super.die(damageSource);
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
    public void knockback(double strength, double x, double z) {
        if (canBeDestroyed())
            super.knockback(strength, x, z);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(@NotNull Entity entity) {
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (isEffectiveAi() || isControlledByLocalInstance()) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt(STOCK_TAG, getSpidersRemaining());
        compoundTag.putInt(TOTAL_STOCK_TAG, getSpidersTotal());
        compoundTag.putBoolean(ACTIVATED_TAG, isActivated());
        compoundTag.putInt(SPAWN_COOLDOWN_TAG, spawnCooldown);
        compoundTag.putInt(COLLECTED_ITEMS_TAG, collectedItemCount);
        compoundTag.putInt(STOCK_REFILL_TAG, stockRefillProgress);
        compoundTag.putFloat(EXPANSION_DAMAGE_TAG, expansionDamage);
        compoundTag.putBoolean(HAS_QUEEN_TAG, hasQueen());
        compoundTag.putInt(QUEEN_HEALTH_TAG, getQueenHealth());
        compoundTag.putInt(HIGH_STOCK_TICKS_TAG, highStockTicks);

        final ListTag livingList = new ListTag();
        for (UUID spiderId : livingSpiderIds)
            livingList.add(NbtUtils.createUUID(spiderId));
        compoundTag.put(LIVING_TAG, livingList);

        final ListTag corpsesList = new ListTag();
        for (NestCorpseLoot nestCorpse : nestCorpses)
            corpsesList.add(nestCorpse.save());
        compoundTag.put(CORPSES_TAG, corpsesList);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        setSpidersRemaining(compoundTag.getInt(STOCK_TAG));
        setSpidersTotal(compoundTag.contains(TOTAL_STOCK_TAG) ? compoundTag.getInt(TOTAL_STOCK_TAG) : getSpidersRemaining());
        setActivated(compoundTag.getBoolean(ACTIVATED_TAG));
        spawnCooldown = compoundTag.getInt(SPAWN_COOLDOWN_TAG);
        collectedItemCount = compoundTag.getInt(COLLECTED_ITEMS_TAG);
        stockRefillProgress = compoundTag.getInt(STOCK_REFILL_TAG);
        expansionDamage = compoundTag.getFloat(EXPANSION_DAMAGE_TAG);
        setHasQueen(compoundTag.getBoolean(HAS_QUEEN_TAG));
        setQueenHealth(compoundTag.getInt(QUEEN_HEALTH_TAG));
        highStockTicks = compoundTag.getInt(HIGH_STOCK_TICKS_TAG);

        livingSpiderIds.clear();
        final ListTag livingList = compoundTag.getList(LIVING_TAG, Tag.TAG_INT_ARRAY);
        for (Tag entry : livingList)
            livingSpiderIds.add(NbtUtils.loadUUID(entry));

        nestCorpses.clear();
        final ListTag corpsesList = compoundTag.getList(CORPSES_TAG, Tag.TAG_COMPOUND);
        for (int corpseIndex = 0; corpseIndex < corpsesList.size(); corpseIndex++)
            nestCorpses.add(NestCorpseLoot.load(corpsesList.getCompound(corpseIndex)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::handleAnimations)
                .triggerableAnim("hit", HIT_ANIMATION)
                .triggerableAnim("spawn", SPAWN_ANIMATION)
                .triggerableAnim("death", DEATH_ANIMATION));
    }

    private PlayState handleAnimations(AnimationState<SpiderSpawnerEntity> animationState) {
        if (isDeadOrDying())
            return animationState.setAndContinue(DEATH_ANIMATION);
        return animationState.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public String modelName() {
        return "spider_spawn";
    }

    @Override
    public String textureName() {
        return "spider_spawn";
    }

    @Override
    public String animationName() {
        return "spider_spawn";
    }
}
