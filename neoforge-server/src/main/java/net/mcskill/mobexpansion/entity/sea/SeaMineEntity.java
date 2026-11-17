package net.mcskill.mobexpansion.entity.sea;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Морская мина на цепи. Стоит на месте; если рядом враг (или её ударили) — пять раз мигает раскалённым корпусом
 * и взрывается: урон в радиусе 8 (к краю слабее), всех игроков в радиусе 16 отбрасывает. Блоки не ломает.
 * <p>
 * Цепь до дна — только рисунок: длина считается на сервере при появлении и уходит клиенту, мир не меняется
 * (оригинал ставил блоки цепи).
 */
public class SeaMineEntity extends SeaCreatureEntity {
    public static final int COLD = 0;
    public static final int HOT = 1;
    /** Насколько ниже позиции мины начинается цепь (низ модели). */
    public static final double CHAIN_TOP = 0.6D;
    private static final int MAX_CHAIN = 48;
    private static final double TRIGGER_RADIUS = 6.0D;
    private static final double BLAST_RADIUS = 8.0D;
    private static final double SHOCK_RADIUS = 16.0D;
    private static final EntityDataAccessor<Float> CHAIN = SynchedEntityData.defineId(SeaMineEntity.class, EntityDataSerializers.FLOAT);

    private Ability detonate;
    private boolean exploded;

    public SeaMineEntity(EntityType<? extends SeaMineEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 3;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHAIN, 0.0F);
    }

    /** Длина цепи от низа мины до дна (0 — дна в пределах {@link #MAX_CHAIN} нет). */
    public float chainLength() {
        return entityData.get(CHAIN);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        final Ability.Builder builder = Ability.builder("detonate").animation(null).duration(49).cooldown(0)
                .when((mob, target) -> false).unstoppable().ignoreSight();
        // пять вспышек с щелчком, на 48-м тике — взрыв
        for (int flash : new int[]{0, 14, 23, 33, 43}) {
            builder.at(flash, (mob, target) -> {
                setStance(HOT);
                sound(SoundEvents.CREEPER_DEATH, 1.0F, 1.0F);
            });
            builder.at(flash + 4, (mob, target) -> setStance(COLD));
        }
        builder.at(48, (mob, target) -> {
            explode();
            discard();
        });
        detonate = builder.build();
        abilities.add(detonate);
    }

    @Override
    protected void registerGoals() {
    }

    /** Запустить отсчёт (если ещё не идёт). */
    public void arm() {
        if (level().isClientSide() || exploded || isDeadOrDying() || isUsingAbility("detonate"))
            return;
        abilities();
        forceAbility(detonate, null);
    }

    private void explode() {
        if (exploded)
            return;
        exploded = true;
        setStance(COLD);
        final Vec3 center = position().add(0.0D, 0.5D, 0.0D);
        final float full = damage(1.0D);
        for (LivingEntity victim : enemiesNear(center, BLAST_RADIUS)) {
            final double distance = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D).distanceTo(center);
            final float amount = (float) (full * (1.0D - 0.5D * Math.min(1.0D, distance / BLAST_RADIUS)));
            victim.hurt(damageSources().explosion(this, this), amount);
        }
        for (Player player : level().getEntitiesOfClass(Player.class, new AABB(center, center).inflate(SHOCK_RADIUS), this::isEnemy)) {
            final double distance = player.position().distanceTo(center);
            if (distance <= SHOCK_RADIUS)
                push(player, center, 2.5D * (1.0D - distance / SHOCK_RADIUS), 0.3D);
        }
        particles(ParticleTypes.EXPLOSION_EMITTER, center, 1, 0.0, 0.0);
        particles(ParticleTypes.FLASH, center, 1, 0.0, 0.0);
        particles(isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.POOF, center, 200, 2.0, 1.0);
        sound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.8F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying())
            return;
        if (tickCount == 2 || tickCount % 200 == 0)
            entityData.set(CHAIN, measureChain());
        if (tickCount % 10 == 0 && !isUsingAbility() && !enemiesNear(position().add(0.0D, 0.5D, 0.0D), TRIGGER_RADIUS).isEmpty())
            arm();
    }

    /** Расстояние от низа мины до первой твёрдой поверхности под ней. */
    private float measureChain() {
        final double top = getY() - CHAIN_TOP;
        final BlockPos.MutableBlockPos cursor = BlockPos.containing(getX(), top, getZ()).mutable();
        for (int i = 0; i <= MAX_CHAIN; i++) {
            final BlockState state = level().getBlockState(cursor);
            final VoxelShape shape = state.getCollisionShape(level(), cursor);
            if (!shape.isEmpty()) {
                final double ground = cursor.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                return (float) Math.max(0.0D, top - ground);
            }
            if (cursor.getY() <= level().getMinBuildHeight())
                break;
            cursor.move(0, -1, 0);
        }
        return 0.0F;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        final boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && isEnemy(attacker))
            arm();
        return hurt;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        // подбитая мина взрывается сразу
        if (!dead && !level().isClientSide())
            explode();
        super.die(source);
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean pitchesModel() {
        return false;
    }

    @Override
    protected boolean suffocatesOutOfWater() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    protected String walkAnimation() {
        return "idle";
    }

    @Override
    protected String deathAnimation() {
        return "idle";
    }

    @Override
    protected int deathTicks() {
        return 1;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Exploded", exploded);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        exploded = tag.getBoolean("Exploded");
        setStance(COLD);
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE.value();
    }
}
