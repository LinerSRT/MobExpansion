package net.mcskill.mobexpansion.entity.effect;

import net.mcskill.mobexpansion.entity.IModel;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Универсальный снаряд для способностей. Внешний вид задаётся синхронизируемыми полями:
 * GeckoLib-модель (MODEL), предмет (ITEM) или ничего (только частицы).
 * Поведение (урон, взрыв, наведение, эффекты) живёт только на сервере и не сохраняется — снаряды короткоживущие.
 */
public class MobExProjectileEntity extends ThrowableProjectile implements GeoEntity, IModel {
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(MobExProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(MobExProjectileEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(MobExProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(MobExProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SPIN = SynchedEntityData.defineId(MobExProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage = 4.0F;
    private double knockback = 0.3D;
    private double explosionRadius;
    private float explosionDamage;
    private int maxLife = 100;
    private double homing;
    private boolean pierce;
    private final List<MobEffectInstance> hitEffects = new ArrayList<>();
    @Nullable
    private ParticleOptions trail = ParticleTypes.SMOKE;
    private int trailCount = 1;
    @Nullable
    private ParticleOptions impactParticle;
    @Nullable
    private SoundEvent impactSound;
    @Nullable
    private Entity homingTarget;
    @Nullable
    private Consumer<MobExProjectileEntity> onImpact;
    private final List<Entity> pierced = new ArrayList<>();
    private boolean spawnedThisSession;

    public MobExProjectileEntity(EntityType<? extends MobExProjectileEntity> type, Level level) {
        super(type, level);
    }

    public static MobExProjectileEntity create(Level level, LivingEntity owner, String model) {
        final MobExProjectileEntity projectile = new MobExProjectileEntity(MobExEntities.ABILITY_PROJECTILE.get(), level);
        projectile.setOwner(owner);
        projectile.entityData.set(MODEL, model);
        projectile.spawnedThisSession = true;
        return projectile;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(MODEL, "");
        builder.define(ITEM, ItemStack.EMPTY);
        builder.define(SCALE, 1.0F);
        builder.define(GRAVITY, 0.0F);
        builder.define(SPIN, false);
    }

    // ------------------------------------------------------------ настройка (цепочкой)

    public MobExProjectileEntity damage(float damage) {
        this.damage = damage;
        return this;
    }

    public MobExProjectileEntity knockback(double knockback) {
        this.knockback = knockback;
        return this;
    }

    public MobExProjectileEntity explosion(double radius, float damage) {
        this.explosionRadius = radius;
        this.explosionDamage = damage;
        return this;
    }

    public MobExProjectileEntity life(int ticks) {
        this.maxLife = ticks;
        return this;
    }

    public MobExProjectileEntity gravity(double gravity) {
        entityData.set(GRAVITY, (float) gravity);
        return this;
    }

    public double gravity() {
        return entityData.get(GRAVITY);
    }

    public MobExProjectileEntity homing(@Nullable Entity target, double strength) {
        this.homingTarget = target;
        this.homing = strength;
        return this;
    }

    public MobExProjectileEntity pierce() {
        this.pierce = true;
        return this;
    }

    public MobExProjectileEntity effect(Holder<MobEffect> effect, int duration, int amplifier) {
        hitEffects.add(new MobEffectInstance(effect, duration, amplifier));
        return this;
    }

    public MobExProjectileEntity trail(@Nullable ParticleOptions particle, int count) {
        this.trail = particle;
        this.trailCount = count;
        return this;
    }

    public MobExProjectileEntity impact(@Nullable ParticleOptions particle, @Nullable SoundEvent sound) {
        this.impactParticle = particle;
        this.impactSound = sound;
        return this;
    }

    public MobExProjectileEntity onImpact(Consumer<MobExProjectileEntity> callback) {
        this.onImpact = callback;
        return this;
    }

    public MobExProjectileEntity item(ItemStack stack) {
        entityData.set(ITEM, stack);
        return this;
    }

    public MobExProjectileEntity scale(float scale) {
        entityData.set(SCALE, scale);
        return this;
    }

    public MobExProjectileEntity spin() {
        entityData.set(SPIN, true);
        return this;
    }

    public ItemStack getItem() {
        return entityData.get(ITEM);
    }

    public float getScale() {
        return entityData.get(SCALE);
    }

    public boolean isSpinning() {
        return entityData.get(SPIN);
    }

    public boolean hasModel() {
        return !entityData.get(MODEL).isEmpty() && getItem().isEmpty();
    }

    public void faceMotion() {
        final Vec3 motion = getDeltaMovement();
        final double horizontal = motion.horizontalDistance();
        setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    // ------------------------------------------------------------ логика

    @Override
    protected double getDefaultGravity() {
        return entityData.get(GRAVITY);
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && (!spawnedThisSession || tickCount > maxLife)) {
            if (spawnedThisSession)
                explode(position());
            discard();
            return;
        }
        if (!level().isClientSide() && homing > 0.0D && homingTarget != null && homingTarget.isAlive()) {
            final Vec3 wanted = homingTarget.position().add(0.0D, homingTarget.getBbHeight() * 0.5D, 0.0D).subtract(position()).normalize();
            final Vec3 motion = getDeltaMovement();
            final double speed = motion.length();
            final Vec3 steered = motion.normalize().lerp(wanted, homing).normalize().scale(speed);
            setDeltaMovement(steered);
            hurtMarked = true;
        }
        super.tick();
        if (level() instanceof ServerLevel serverLevel && trail != null)
            serverLevel.sendParticles(trail, getX(), getY(), getZ(), trailCount, 0.04D, 0.04D, 0.04D, 0.01D);
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        if (entity instanceof MobExProjectileEntity || entity instanceof MobExEffectEntity)
            return false;
        if (pierced.contains(entity))
            return false;
        final Entity owner = getOwner();
        if (owner instanceof AbilityMobEntity mob)
            return entity instanceof LivingEntity living && mob.isEnemy(living) && super.canHitEntity(entity);
        return entity != owner && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        if (level().isClientSide())
            return;
        final Entity hit = result.getEntity();
        final Entity owner = getOwner();
        final LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        if (damage > 0.0F && hit.hurt(damageSources().mobProjectile(this, livingOwner), damage)) {
            if (hit instanceof LivingEntity living) {
                for (MobEffectInstance effect : hitEffects)
                    living.addEffect(new MobEffectInstance(effect), owner);
                if (knockback > 0.0D)
                    AbilityMobEntity.push(living, position().subtract(getDeltaMovement()), knockback, 0.15D);
            }
        }
        if (pierce)
            pierced.add(hit);
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (level().isClientSide())
            return;
        if (pierce && result.getType() == HitResult.Type.ENTITY)
            return;
        explode(result.getLocation());
        discard();
    }

    private void explode(Vec3 at) {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        if (impactParticle != null)
            serverLevel.sendParticles(impactParticle, at.x, at.y, at.z, explosionRadius > 0 ? 12 : 4, 0.2D, 0.2D, 0.2D, 0.05D);
        if (impactSound != null)
            level().playSound(null, at.x, at.y, at.z, impactSound, getSoundSource(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        if (explosionRadius > 0.0D) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (impactSound == null)
                level().playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), getSoundSource(), 0.8F, 1.1F);
            if (getOwner() instanceof AbilityMobEntity mob) {
                for (LivingEntity living : mob.enemiesNear(at, explosionRadius)) {
                    if (living.hurt(damageSources().explosion(this, mob), explosionDamage)) {
                        AbilityMobEntity.push(living, at, 0.6D, 0.35D);
                        for (MobEffectInstance effect : hitEffects)
                            living.addEffect(new MobEffectInstance(effect), mob);
                    }
                }
            }
        }
        if (onImpact != null) {
            final Consumer<MobExProjectileEntity> callback = onImpact;
            onImpact = null;
            callback.accept(this);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // ------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> PlayState.STOP));
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
