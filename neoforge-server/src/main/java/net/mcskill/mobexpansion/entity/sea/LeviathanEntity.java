package net.mcskill.mobexpansion.entity.sea;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Левиафан — исполинская акула (около 25 блоков), босс глубин. Корпус — основной хитбокс, голова и хвост —
 * отдельные хитбоксы ({@link SeaCreaturePart}).
 * <ul>
 *     <li>bite — втягивает всех перед пастью и смыкает челюсти, затем бьёт второй раз;</li>
 *     <li>rip — хватает цель пастью и треплет (4 укуса), потом выплёвывает;</li>
 *     <li>bite2 — быстрый укус вплотную;</li>
 *     <li>summon — раненый зовёт акул.</li>
 * </ul>
 */
public class LeviathanEntity extends SeaCreatureEntity {
    /** Точка в пасти относительно корпуса (right, up, front) — там держится схваченная цель. */
    private static final Vec3 JAW = new Vec3(0.0D, 2.2D, 9.5D);
    private static final BlockParticleOption GORE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());
    private static final int MAX_SUMMONED = 3;

    private final SeaCreaturePart[] parts;

    public LeviathanEntity(EntityType<? extends LeviathanEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 250;
        this.parts = new SeaCreaturePart[]{
                new SeaCreaturePart(this, "head", 6.0F, 6.0F, new Vec3(0.0D, -0.8D, 8.7D)),
                new SeaCreaturePart(this, "tail", 5.0F, 8.0F, new Vec3(0.0D, 0.8D, -12.9D))
        };
        // как у дракона: id частей идут сразу за id хозяина
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // втягивание и укус: пасть смыкается на 16-м тике и ещё раз — на 50-м
        abilities.add(Ability.builder("bite").seconds(3.5).range(0, 15.0).cooldown(60).weight(4)
                .at(4, (mob, target) -> sound(SoundEvents.ELDER_GUARDIAN_CURSE, 2.0F, 0.5F))
                .every(6, 15, 1, (mob, target, tick) -> {
                    final Vec3 mouth = mouth();
                    for (LivingEntity victim : enemiesNear(mouth, 10.0D))
                        pull(victim, mouth, 0.6D);
                    particles(ParticleTypes.BUBBLE_COLUMN_UP, mouth, 10, 1.5, 0.1);
                })
                .at(16, (mob, target) -> chomp(mouth(), 8.0D, 1.0D, 1.6D, -0.3D))
                .at(50, (mob, target) -> chomp(mouth(), 6.0D, 0.75D, 1.2D, 0.2D))
                .build());

        // хватка: на 16-м тике берёт ближайшую к пасти цель, треплет её и выплёвывает
        abilities.add(Ability.builder("rip").seconds(3.5).range(0, 12.0).cooldown(200).weight(2).unstoppable()
                .at(1, (mob, target) -> halt())
                .at(16, (mob, target) -> {
                    final LivingEntity victim = nearestTo(mouth(), 8.0D);
                    if (victim != null) {
                        grab(victim, JAW);
                        sound(SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.5F);
                    }
                })
                .every(2, 60, 1, (mob, target, tick) -> halt())
                .every(16, 31, 5, (mob, target, tick) -> {
                    final LivingEntity victim = held();
                    if (victim == null)
                        return;
                    if (hit(victim, damage(0.67D), 0.0D, 0.0D))
                        particles(GORE, victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D), 15, 0.3, 0.0);
                })
                .at(52, (mob, target) -> release(1.5D, 0.4D))
                .build());

        // быстрый укус вплотную
        abilities.add(Ability.builder("bite2").seconds(1.5).range(0, 10.0).cooldown(80).weight(3)
                .at(9, (mob, target) -> chomp(mouth(), 5.0D, 0.6D, 1.0D, 0.1D))
                .build());

        // зов стаи: когда ранен и рядом мало своих
        abilities.add(Ability.builder("summon").seconds(1.875).range(0, 32.0).cooldown(600).weight(2).ignoreSight()
                .when((mob, target) -> mob.healthBelow(0.7D) && summonedNear() < MAX_SUMMONED)
                .at(8, (mob, target) -> sound(SoundEvents.ELDER_GUARDIAN_AMBIENT, 3.0F, 0.5F))
                .at(15, (mob, target) -> {
                    final int count = Math.min(2, MAX_SUMMONED - summonedNear());
                    for (int i = 0; i < count; i++) {
                        final EntityType<SharkEntity> type = random.nextBoolean() ? MobExEntities.SHARK.get() : MobExEntities.HAMMERHEAD_SHARK.get();
                        final Vec3 at = localPos((i == 0 ? -6.0D : 6.0D), 1.0D, 4.0D);
                        if (summon(type, at) != null)
                            particles(ParticleTypes.BUBBLE, at, 40, 1.0, 0.1);
                    }
                })
                .build());
    }

    private Vec3 mouth() {
        return localPos(JAW.x, JAW.y, JAW.z - 2.0D);
    }

    /** Смыкание челюстей: урон всем у пасти, кровь, отброс. */
    private void chomp(Vec3 center, double radius, double multiplier, double knockback, double lift) {
        sound(SoundEvents.EVOKER_FANGS_ATTACK, 2.5F, 0.4F);
        particles(ParticleTypes.BUBBLE, center, 40, 2.0, 0.2);
        for (LivingEntity victim : hitArea(center, radius, damage(multiplier), knockback, lift))
            particles(GORE, victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D), 15, 0.3, 0.0);
    }

    @Nullable
    private LivingEntity nearestTo(Vec3 point, double radius) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : enemiesNear(point, radius)) {
            final double distance = entity.position().distanceToSqr(point);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private int summonedNear() {
        return level().getEntitiesOfClass(SharkEntity.class, getBoundingBox().inflate(32.0D), LivingEntity::isAlive).size();
    }

    // ---------------------------------------------------------------- хитбоксы частей

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null)
            for (int i = 0; i < parts.length; i++)
                parts[i].setId(id + i + 1);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public void tick() {
        super.tick();
        for (SeaCreaturePart part : parts)
            part.follow();
    }

    // ---------------------------------------------------------------- прочее

    @Override
    @Nullable
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.BLUE;
    }

    @Override
    protected int maxPitch() {
        return 25;
    }

    @Override
    protected int maxTurn() {
        return 4;
    }

    @Override
    protected boolean suffocatesOutOfWater() {
        return false;
    }

    @Override
    protected int deathTicks() {
        return Ability.ticks(5.0);
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }
}
