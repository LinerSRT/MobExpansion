package net.mcskill.mobexpansion.entity.ability;

import net.mcskill.mobexpansion.entity.MobExGeckoEntity;
import net.mcskill.mobexpansion.entity.MobExTargeting;
import net.mcskill.mobexpansion.entity.ai.SurvivalPlayerHurtByTargetGoal;
import net.mcskill.mobexpansion.entity.effect.MobExEffectEntity;
import net.mcskill.mobexpansion.entity.effect.MobExProjectileEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * База для мобов со способностями: каждая атака — {@link Ability} со своей анимацией, таймингами и кулдауном.
 * Бой ведёт {@link AbilityCombatGoal}: выбирает доступную способность по дистанции/условиям, а в паузах двигает
 * моба согласно {@link #combatStyle()}.
 */
public abstract class AbilityMobEntity extends MobExGeckoEntity {
    /** Произвольное «состояние» моба для клиента (развёрнут/свёрнут, фаза босса и т.п.). */
    protected static final EntityDataAccessor<Integer> STANCE = SynchedEntityData.defineId(AbilityMobEntity.class, EntityDataSerializers.INT);

    public enum CombatStyle {
        /** Идёт в упор. */
        MELEE,
        /** Держит дистанцию [preferredMin, preferredMax], отступает, если цель подошла. */
        KITE,
        /** Стоит на месте. */
        STATIONARY,
        /** Летает над целью (только для летающих). */
        HOVER
    }

    private final Map<String, RawAnimation> animationCache = new HashMap<>();
    @Nullable
    private List<Ability> abilities;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    @Nullable
    private Ability active;
    private int activeTick;
    @Nullable
    private LivingEntity activeTarget;
    private int globalCooldown;
    private int hurtSoundCooldown;
    private boolean spawnAbilityPlayed;
    private final Set<Integer> hitThisAbility = new java.util.HashSet<>();

    protected AbilityMobEntity(EntityType<? extends AbilityMobEntity> entityType, Level level) {
        super(entityType, level);
    }

    // ---------------------------------------------------------------- описание моба

    /** Здесь моб регистрирует свои способности. Вызывается лениво один раз. */
    protected abstract void registerAbilities(List<Ability> abilities);

    /** Мобы одной фракции не бьют друг друга и не получают урон от способностей друг друга. */
    public abstract String faction();

    protected CombatStyle combatStyle() {
        return CombatStyle.MELEE;
    }

    /** Дистанция, на которой KITE-моб предпочитает держаться. */
    public double preferredMinRange() {
        return 6.0D;
    }

    public double preferredMaxRange() {
        return 12.0D;
    }

    public double chaseSpeed() {
        return 1.0D;
    }

    protected String idleAnimation() {
        return "idle";
    }

    protected String walkAnimation() {
        return "walk";
    }

    protected String deathAnimation() {
        return "death";
    }

    /** Сколько тиков лежит труп (обычно = длине анимации смерти). */
    protected int deathTicks() {
        return 40;
    }

    /** Способность, которая проигрывается сразу после появления (вылезание из земли и т.п.). */
    @Nullable
    protected Ability spawnAbility() {
        return null;
    }

    /** Дополнительные анимации, которые запускаются вручную через {@link #playAnimation(String)}. */
    protected List<String> extraAnimations() {
        return List.of();
    }

    // ---------------------------------------------------------------- синхронизация / сохранение

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STANCE, 0);
    }

    public int getStance() {
        return entityData.get(STANCE);
    }

    public void setStance(int stance) {
        entityData.set(STANCE, stance);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stance", getStance());
        tag.putBoolean("SpawnPlayed", spawnAbilityPlayed);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStance(tag.getInt("Stance"));
        spawnAbilityPlayed = tag.getBoolean("SpawnPlayed");
    }

    // ---------------------------------------------------------------- цели

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new AbilityCombatGoal(this));
        registerIdleGoals();
        targetSelector.addGoal(1, new SurvivalPlayerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isEnemy));
    }

    protected void registerIdleGoals() {
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public boolean isAlly(@Nullable Entity entity) {
        if (entity == null)
            return false;
        if (entity == this)
            return true;
        if (entity instanceof AbilityMobEntity other)
            return other.faction().equals(faction());
        if (entity instanceof MobExProjectileEntity projectile)
            return isAlly(projectile.getOwner());
        if (entity instanceof MobExEffectEntity effect)
            return isAlly(effect.getOwner());
        return false;
    }

    public boolean isEnemy(@Nullable LivingEntity entity) {
        if (entity == null || entity == this || !entity.isAlive() || isAlly(entity))
            return false;
        return MobExTargeting.isValidHostileTarget(entity) || entity == getTarget();
    }

    // ---------------------------------------------------------------- способности

    public List<Ability> abilities() {
        if (abilities == null) {
            final List<Ability> list = new ArrayList<>();
            registerAbilities(list);
            abilities = List.copyOf(list);
        }
        return abilities;
    }

    public boolean isBusy() {
        return active != null || globalCooldown > 0;
    }

    public boolean isUsingAbility() {
        return active != null;
    }

    public boolean isUsingAbility(String name) {
        return active != null && active.name.equals(name);
    }

    @Nullable
    public Ability activeAbility() {
        return active;
    }

    public int abilityTick() {
        return activeTick;
    }

    public boolean isOnCooldown(String name) {
        return cooldowns.getOrDefault(name, 0) > 0;
    }

    public void setCooldown(String name, int ticks) {
        cooldowns.put(name, ticks);
    }

    /** Выбирает подходящую способность (взвешенный случайный выбор) или null. */
    @Nullable
    public Ability pickAbility(LivingEntity target) {
        final double distance = distanceTo(target);
        final boolean canSee = getSensing().hasLineOfSight(target);
        final List<Ability> usable = new ArrayList<>();
        int totalWeight = 0;
        for (Ability ability : abilities()) {
            if (isOnCooldown(ability.name))
                continue;
            if (distance < ability.minRange || distance > ability.maxRange)
                continue;
            if (ability.needsSight && !canSee)
                continue;
            if (ability.condition != null && !ability.condition.test(this, target))
                continue;
            usable.add(ability);
            totalWeight += ability.weight;
        }
        if (usable.isEmpty())
            return null;
        int roll = random.nextInt(totalWeight);
        for (Ability ability : usable) {
            roll -= ability.weight;
            if (roll < 0)
                return ability;
        }
        return usable.getLast();
    }

    public boolean tryStartAbility(Ability ability, @Nullable LivingEntity target) {
        if (level().isClientSide() || isBusy() || isDeadOrDying())
            return false;
        forceAbility(ability, target);
        return true;
    }

    /** Запуск без проверок кулдауна/дистанции (фазы босса, комбо-продолжения). */
    public void forceAbility(Ability ability, @Nullable LivingEntity target) {
        if (level().isClientSide() || isDeadOrDying())
            return;
        if (active != null)
            finishAbility(true);
        active = ability;
        activeTick = 0;
        activeTarget = target;
        hitThisAbility.clear();
        cooldowns.put(ability.name, ability.cooldown);
        if (ability.stopsMovement)
            getNavigation().stop();
        if (ability.animation != null)
            playAnimation(ability.animation);
        if (ability.onStart != null)
            ability.onStart.run(this, target);
        runTimeline(0);
    }

    public void interruptAbility() {
        if (active != null)
            finishAbility(true);
    }

    private void finishAbility(boolean interrupted) {
        final Ability finished = active;
        final LivingEntity target = activeTarget;
        active = null;
        activeTick = 0;
        activeTarget = null;
        globalCooldown = postAbilityDelay();
        if (interrupted)
            held = null;
        onAbilityFinished(finished, interrupted);
        // onEnd — только при штатном завершении: здесь живут цепочки вроде «захват -> бросок»
        if (!interrupted && finished != null && finished.onEnd != null)
            finished.onEnd.run(this, target);
    }

    /** Уборка после способности (в т.ч. прерванной): вернуть стойку, отпустить захваченную цель и т.п. */
    protected void onAbilityFinished(@Nullable Ability ability, boolean interrupted) {
    }

    /** На сколько тиков за тик уменьшаются кулдауны (вторая фаза босса — быстрее). */
    protected int cooldownSpeed() {
        return 1;
    }

    /** true, если эту сущность ещё не били в текущем применении способности (рывки, перекаты). */
    public boolean firstHit(Entity entity) {
        return hitThisAbility.add(entity.getId());
    }

    /** Как {@link #hitArea}, но каждую цель — не больше одного раза за применение способности. */
    public List<LivingEntity> hitAreaOnce(Vec3 center, double radius, float amount, double knockback, double lift) {
        final List<LivingEntity> hits = new ArrayList<>();
        for (LivingEntity entity : enemiesNear(center, radius)) {
            if (!firstHit(entity))
                continue;
            if (entity.hurt(damageSources().mobAttack(this), amount)) {
                hits.add(entity);
                if (knockback > 0.0D || lift > 0.0D)
                    push(entity, center, knockback, lift);
            }
        }
        return hits;
    }

    /** Пауза между способностями. */
    protected int postAbilityDelay() {
        return 6;
    }

    private void runTimeline(int tick) {
        final Ability ability = active;
        if (ability == null)
            return;
        final List<Ability.Action> actions = ability.timeline.get(tick);
        if (actions != null) {
            for (Ability.Action action : actions) {
                if (active != ability)
                    return;
                action.run(this, currentAbilityTarget());
            }
        }
        for (Ability.Window window : ability.windows) {
            if (active != ability)
                return;
            if (tick >= window.from() && tick <= window.to() && (tick - window.from()) % window.period() == 0)
                window.action().run(this, currentAbilityTarget(), tick);
        }
    }

    @Nullable
    private LivingEntity currentAbilityTarget() {
        final LivingEntity target = getTarget();
        if (target != null && target.isAlive())
            return target;
        return activeTarget != null && activeTarget.isAlive() ? activeTarget : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide())
            return;
        if (hurtSoundCooldown > 0)
            hurtSoundCooldown--;
        if (globalCooldown > 0 && active == null)
            globalCooldown--;
        final int speed = cooldownSpeed();
        cooldowns.replaceAll((name, ticks) -> Math.max(0, ticks - speed));

        if (!spawnAbilityPlayed && tickCount >= 3) {
            spawnAbilityPlayed = true;
            final Ability spawn = spawnAbility();
            if (spawn != null)
                forceAbility(spawn, null);
        }

        tickHeld();
        if (active != null && !isDeadOrDying()) {
            final Ability ability = active;
            if (ability.stopsMovement)
                getNavigation().stop();
            final LivingEntity target = currentAbilityTarget();
            if (ability.tracksTarget && target != null)
                faceTowards(target);
            activeTick++;
            runTimeline(activeTick);
            if (active == ability && activeTick >= ability.duration)
                finishAbility(false);
        }
    }

    // ---------------------------------------------------------------- анимации

    protected RawAnimation loopAnim(String name) {
        return animationCache.computeIfAbsent("loop:" + name, key -> RawAnimation.begin().thenLoop(name));
    }

    protected RawAnimation onceAnim(String name) {
        return animationCache.computeIfAbsent("once:" + name, key -> RawAnimation.begin().thenPlay(name));
    }

    public void playAnimation(String name) {
        triggerAnim("main", name);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        final AnimationController<AbilityMobEntity> controller = new AnimationController<>(this, "main", 4, this::handleAnimation);
        final Set<String> names = new LinkedHashSet<>();
        for (Ability ability : abilities())
            if (ability.animation != null)
                names.add(ability.animation);
        final Ability spawn = spawnAbility();
        if (spawn != null && spawn.animation != null)
            names.add(spawn.animation);
        names.addAll(extraAnimations());
        for (String name : names)
            controller.triggerableAnim(name, onceAnim(name));
        controller.triggerableAnim(deathAnimation(), RawAnimation.begin().thenPlayAndHold(deathAnimation()));
        controllers.add(controller);
    }

    protected PlayState handleAnimation(AnimationState<AbilityMobEntity> state) {
        if (isDeadOrDying())
            return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(deathAnimation()));
        if (state.isMoving() || isVisuallyMoving())
            return state.setAndContinue(loopAnim(walkAnimation()));
        return state.setAndContinue(loopAnim(idleAnimation()));
    }

    /** Для летающих: GeckoLib считает движение только по горизонтали. */
    protected boolean isVisuallyMoving() {
        return false;
    }

    // ---------------------------------------------------------------- урон и смерть

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isAlly(source.getEntity()) || isAlly(source.getDirectEntity()))
            return false;
        final boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && isEnemy(attacker) && getTarget() == null)
            setTarget(attacker);
        return hurt;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (active != null && active.unstoppable)
            return;
        super.knockback(strength, x, z);
    }

    @Override
    public boolean isPushable() {
        return (active == null || !active.unstoppable) && super.isPushable();
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        return false;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (!dead && !level().isClientSide()) {
            interruptAbility();
            playAnimation(deathAnimation());
            if (bossEvent != null)
                bossEvent.setProgress(0.0F);
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= deathTicks() && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void playHurtSound(@NotNull DamageSource source) {
        if (hurtSoundCooldown > 0)
            return;
        hurtSoundCooldown = 30;
        playSound(getHurtSound(source), getSoundVolume(), getVoicePitch());
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    // ---------------------------------------------------------------- помощники для способностей

    /** Урон от атрибута ATTACK_DAMAGE с множителем — так все удары моба масштабируются конфигом. */
    public float damage(double multiplier) {
        return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier);
    }

    public void faceTowards(Entity target) {
        final double dx = target.getX() - getX();
        final double dz = target.getZ() - getZ();
        final float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    /** Горизонтальное направление «вперёд» по корпусу. */
    public Vec3 forward() {
        final float rad = yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(rad), 0.0D, Mth.cos(rad));
    }

    public Vec3 forwardPos(double distance, double height) {
        return position().add(forward().scale(distance)).add(0.0D, height, 0.0D);
    }

    /** Точка относительно корпуса: right — вправо, up — вверх, front — вперёд. */
    public Vec3 localPos(double right, double up, double front) {
        final Vec3 fwd = forward();
        final Vec3 rightDir = new Vec3(-fwd.z, 0.0D, fwd.x);
        return position().add(fwd.scale(front)).add(rightDir.scale(right)).add(0.0D, up, 0.0D);
    }

    public List<LivingEntity> enemiesNear(Vec3 center, double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                entity -> isEnemy(entity) && entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D).distanceToSqr(center) <= (radius + entity.getBbWidth()) * (radius + entity.getBbWidth()));
    }

    public boolean hit(LivingEntity target, float amount, double knockback, double lift) {
        if (!isEnemy(target))
            return false;
        final boolean hurt = target.hurt(damageSources().mobAttack(this), amount);
        if (hurt && (knockback > 0.0D || lift > 0.0D))
            push(target, position(), knockback, lift);
        return hurt;
    }

    /** Удар по всем врагам в радиусе от точки. Возвращает пораженных. */
    public List<LivingEntity> hitArea(Vec3 center, double radius, float amount, double knockback, double lift) {
        final List<LivingEntity> hits = new ArrayList<>();
        for (LivingEntity entity : enemiesNear(center, radius)) {
            if (entity.hurt(damageSources().mobAttack(this), amount)) {
                hits.add(entity);
                if (knockback > 0.0D || lift > 0.0D)
                    push(entity, center, knockback, lift);
            }
        }
        return hits;
    }

    /** Удар конусом перед мобом. */
    public List<LivingEntity> hitCone(double range, float coneDegrees, float amount, double knockback, double lift) {
        final List<LivingEntity> hits = new ArrayList<>();
        final Vec3 fwd = forward();
        final double cos = Math.cos(Math.toRadians(coneDegrees * 0.5D));
        for (LivingEntity entity : enemiesNear(position().add(0.0D, getBbHeight() * 0.5D, 0.0D), range + getBbWidth() * 0.5D)) {
            final Vec3 to = entity.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (to.lengthSqr() > 0.01D && to.normalize().dot(fwd) < cos)
                continue;
            if (entity.hurt(damageSources().mobAttack(this), amount)) {
                hits.add(entity);
                if (knockback > 0.0D || lift > 0.0D)
                    push(entity, position(), knockback, lift);
            }
        }
        return hits;
    }

    /** Удар по линии (луч, рывок). */
    public List<LivingEntity> hitLine(Vec3 from, Vec3 to, double width, float amount, double knockback, double lift) {
        final List<LivingEntity> hits = new ArrayList<>();
        final net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(from, to).inflate(width);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, box, this::isEnemy)) {
            if (entity.getBoundingBox().inflate(width).clip(from, to).isEmpty() && !entity.getBoundingBox().inflate(width).contains(from))
                continue;
            if (entity.hurt(damageSources().mobAttack(this), amount)) {
                hits.add(entity);
                if (knockback > 0.0D || lift > 0.0D)
                    push(entity, from, knockback, lift);
            }
        }
        return hits;
    }

    public static void push(Entity entity, Vec3 from, double strength, double lift) {
        Vec3 direction = entity.position().subtract(from).multiply(1.0D, 0.0D, 1.0D);
        direction = direction.lengthSqr() < 1.0E-4D ? Vec3.ZERO : direction.normalize();
        double resistance = 0.0D;
        if (entity instanceof LivingEntity living)
            resistance = living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        final double scale = Math.max(0.0D, 1.0D - resistance);
        entity.push(direction.x * strength * scale, lift * scale, direction.z * strength * scale);
        entity.hurtMarked = true;
    }

    /** Притянуть сущность к точке. */
    public static void pull(Entity entity, Vec3 to, double strength) {
        final Vec3 direction = to.subtract(entity.position());
        if (direction.lengthSqr() < 0.25D)
            return;
        final Vec3 velocity = direction.normalize().scale(strength);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5D).add(velocity));
        entity.hurtMarked = true;
    }

    public void lunge(Vec3 direction, double speed, double up) {
        final Vec3 horizontal = direction.multiply(1.0D, 0.0D, 1.0D);
        final Vec3 velocity = horizontal.lengthSqr() < 1.0E-4D ? Vec3.ZERO : horizontal.normalize().scale(speed);
        setDeltaMovement(velocity.x, up > 0.0D ? up : getDeltaMovement().y, velocity.z);
        hurtMarked = true;
    }

    /** Прыжок по баллистике в точку (примерно за flightTicks тиков). */
    public void leapTo(Vec3 destination, int flightTicks) {
        final double gravity = 0.08D;
        final Vec3 delta = destination.subtract(position());
        final double vy = (delta.y + 0.5D * gravity * flightTicks * flightTicks) / flightTicks;
        setDeltaMovement(delta.x / flightTicks, Math.min(vy, 2.0D), delta.z / flightTicks);
        hurtMarked = true;
    }

    public void effect(LivingEntity target, Holder<MobEffect> effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, duration, amplifier), this);
    }

    public void particles(ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
        if (level() instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    public void particleRing(ParticleOptions particle, Vec3 center, double radius, int points) {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        for (int i = 0; i < points; i++) {
            final double angle = Math.PI * 2.0D * i / points;
            serverLevel.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    public void particleLine(ParticleOptions particle, Vec3 from, Vec3 to, double step) {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        final Vec3 delta = to.subtract(from);
        final int steps = Math.max(1, (int) (delta.length() / step));
        for (int i = 0; i <= steps; i++) {
            final Vec3 point = from.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(particle, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, getSoundSource(), volume, pitch);
    }

    public void sound(Holder<SoundEvent> sound, float volume, float pitch) {
        sound(sound.value(), volume, pitch);
    }

    public MobExProjectileEntity projectile(String model, Vec3 from) {
        final MobExProjectileEntity projectile = MobExProjectileEntity.create(level(), this, model);
        projectile.setPos(from.x, from.y, from.z);
        return projectile;
    }

    /** Выстрел снарядом в цель с упреждением по её скорости. */
    public MobExProjectileEntity shootAt(MobExProjectileEntity projectile, LivingEntity target, float speed, float inaccuracy) {
        final Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D)
                .add(target.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).scale(projectile.distanceTo(target) / Math.max(0.1F, speed)));
        final Vec3 delta = aim.subtract(projectile.position());
        projectile.shoot(delta.x, delta.y, delta.z, speed, inaccuracy);
        level().addFreshEntity(projectile);
        return projectile;
    }

    /** Навесной выстрел (миномёт): снаряд с гравитацией падает в точку за flightTicks. */
    public MobExProjectileEntity lobAt(MobExProjectileEntity projectile, Vec3 destination, int flightTicks) {
        final double gravity = projectile.gravity();
        final Vec3 delta = destination.subtract(projectile.position());
        final double vy = (delta.y + 0.5D * gravity * flightTicks * flightTicks) / flightTicks;
        projectile.setDeltaMovement(delta.x / flightTicks, vy, delta.z / flightTicks);
        projectile.faceMotion();
        level().addFreshEntity(projectile);
        return projectile;
    }

    public MobExEffectEntity spawnEffect(String model, Vec3 pos, int lifetime) {
        final MobExEffectEntity effect = MobExEffectEntity.create(level(), this, model, lifetime);
        effect.moveTo(pos.x, pos.y, pos.z, yBodyRot, 0.0F);
        level().addFreshEntity(effect);
        return effect;
    }

    @Nullable
    public <T extends Mob> T summon(EntityType<T> type, Vec3 pos) {
        if (!(level() instanceof ServerLevel serverLevel))
            return null;
        final T minion = type.create(serverLevel);
        if (minion == null)
            return null;
        minion.moveTo(pos.x, pos.y, pos.z, random.nextFloat() * 360.0F, 0.0F);
        serverLevel.addFreshEntity(minion);
        if (getTarget() != null)
            minion.setTarget(getTarget());
        return minion;
    }

    /** Найти безопасную точку на земле рядом с позицией (для телепортов и призыва). */
    public Vec3 groundAt(Vec3 pos) {
        final net.minecraft.core.BlockPos.MutableBlockPos cursor = net.minecraft.core.BlockPos.containing(pos.x, pos.y + 3.0D, pos.z).mutable();
        for (int i = 0; i < 10; i++) {
            if (!level().getBlockState(cursor.below()).getCollisionShape(level(), cursor.below()).isEmpty()
                    && level().getBlockState(cursor).getCollisionShape(level(), cursor).isEmpty())
                return new Vec3(pos.x, cursor.getY(), pos.z);
            cursor.move(0, -1, 0);
        }
        return pos;
    }

    public boolean healthBelow(double ratio) {
        return getHealth() <= getMaxHealth() * ratio;
    }

    // ---------------------------------------------------------------- полоска босса

    /** Цвет полоски босса или null, если моб не босс. */
    @Nullable
    protected net.minecraft.world.BossEvent.BossBarColor bossBarColor() {
        return null;
    }

    @Nullable
    private net.minecraft.server.level.ServerBossEvent bossEvent() {
        final net.minecraft.world.BossEvent.BossBarColor color = bossBarColor();
        if (color == null || level().isClientSide())
            return null;
        if (bossEvent == null)
            bossEvent = new net.minecraft.server.level.ServerBossEvent(getDisplayName(), color, net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);
        return bossEvent;
    }

    @Override
    public void startSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        final net.minecraft.server.level.ServerBossEvent event = bossEvent();
        if (event != null)
            event.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        final net.minecraft.server.level.ServerBossEvent event = bossEvent();
        if (event != null)
            event.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        final net.minecraft.server.level.ServerBossEvent event = bossEvent();
        if (event != null)
            event.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void setCustomName(@Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        if (bossEvent != null)
            bossEvent.setName(getDisplayName());
    }

    @Nullable
    private net.minecraft.server.level.ServerBossEvent bossEvent;

    // ---------------------------------------------------------------- захват цели

    /** Схватить цель: каждый тик она удерживается в точке holdOffset (right, up, front) относительно корпуса. */
    public void grab(LivingEntity target, Vec3 holdOffset) {
        held = target;
        heldOffset = holdOffset;
    }

    @Nullable
    public LivingEntity held() {
        return held != null && held.isAlive() ? held : null;
    }

    /** Отпустить и, при желании, отбросить вперёд. */
    public void release(double throwStrength, double lift) {
        final LivingEntity target = held;
        held = null;
        if (target != null && target.isAlive() && (throwStrength > 0.0D || lift > 0.0D)) {
            final Vec3 dir = forward();
            target.setDeltaMovement(dir.x * throwStrength, lift, dir.z * throwStrength);
            target.hurtMarked = true;
        }
    }

    private void tickHeld() {
        if (held == null)
            return;
        if (!held.isAlive() || held.isRemoved() || held.distanceToSqr(this) > 64.0D || isDeadOrDying()) {
            held = null;
            return;
        }
        final Vec3 pos = localPos(heldOffset.x, heldOffset.y, heldOffset.z);
        held.teleportTo(pos.x, pos.y, pos.z);
        held.setDeltaMovement(Vec3.ZERO);
        held.fallDistance = 0.0F;
        held.hurtMarked = true;
    }

    @Nullable
    private LivingEntity held;
    private Vec3 heldOffset = Vec3.ZERO;
}
