package net.mcskill.mobexpansion.entity.effect;

import net.mcskill.mobexpansion.entity.IModel;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Визуальный эффект способности на GeckoLib-модели (чёрная дыра, луч, шипы скалка и т.п.).
 * Проигрывает одну анимацию модели, живёт lifetime тиков. Логика урона — в серверном {@link Ticker}.
 * Не сохраняется в мир.
 */
public class MobExEffectEntity extends Entity implements GeoEntity, IModel {
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(MobExEffectEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(MobExEffectEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(MobExEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(MobExEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> GLOWING = SynchedEntityData.defineId(MobExEffectEntity.class, EntityDataSerializers.BOOLEAN);

    @FunctionalInterface
    public interface Ticker {
        void tick(MobExEffectEntity effect, int age);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private RawAnimation animation;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private Entity follow;
    private Vec3 followOffset = Vec3.ZERO;
    @Nullable
    private Ticker ticker;
    private boolean spawnedThisSession;

    public MobExEffectEntity(EntityType<? extends MobExEffectEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static MobExEffectEntity create(Level level, @Nullable LivingEntity owner, String model, int lifetime) {
        final MobExEffectEntity effect = new MobExEffectEntity(MobExEntities.ABILITY_EFFECT.get(), level);
        effect.owner = owner;
        effect.entityData.set(MODEL, model);
        effect.entityData.set(ANIMATION, model);
        effect.entityData.set(LIFETIME, lifetime);
        effect.spawnedThisSession = true;
        return effect;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(MODEL, "");
        builder.define(ANIMATION, "");
        builder.define(LIFETIME, 40);
        builder.define(SCALE, 1.0F);
        builder.define(GLOWING, true);
    }

    // ------------------------------------------------------------ настройка

    /** Имя анимации в animation.json (по умолчанию совпадает с именем модели). */
    public MobExEffectEntity animation(String name) {
        entityData.set(ANIMATION, name);
        return this;
    }

    public MobExEffectEntity scale(float scale) {
        entityData.set(SCALE, scale);
        return this;
    }

    /** Рисовать эффект «светящимся» (без затемнения). По умолчанию включено. */
    public MobExEffectEntity glowing(boolean glowing) {
        entityData.set(GLOWING, glowing);
        return this;
    }

    public MobExEffectEntity ticker(Ticker ticker) {
        this.ticker = ticker;
        return this;
    }

    public MobExEffectEntity follow(Entity entity, Vec3 offset) {
        this.follow = entity;
        this.followOffset = offset;
        return this;
    }

    @Nullable
    public LivingEntity getOwner() {
        return owner;
    }

    public float getScale() {
        return entityData.get(SCALE);
    }

    public boolean isGlowingEffect() {
        return entityData.get(GLOWING);
    }

    public int lifetime() {
        return entityData.get(LIFETIME);
    }

    // ------------------------------------------------------------ логика

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide())
            return;
        if (!spawnedThisSession || tickCount >= lifetime() || (owner != null && owner.isRemoved())) {
            discard();
            return;
        }
        if (follow != null) {
            if (!follow.isAlive()) {
                follow = null;
            } else {
                final Vec3 pos = follow.position().add(followOffset);
                setPos(pos.x, pos.y, pos.z);
            }
        }
        if (ticker != null)
            ticker.tick(this, tickCount);
    }

    @Override
    @NotNull
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(32.0D * Math.max(1.0F, getScale()));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    // ------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            final String name = entityData.get(ANIMATION);
            if (name.isEmpty())
                return PlayState.STOP;
            if (animation == null)
                animation = RawAnimation.begin().thenPlay(name);
            return state.setAndContinue(animation);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public String modelName() {
        return entityData.get(MODEL);
    }

    @Override
    public String textureName() {
        return entityData.get(MODEL);
    }

    @Override
    public String animationName() {
        return entityData.get(MODEL);
    }
}
