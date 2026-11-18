package net.mcskill.mobexpansion.entity.boss;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.init.MobExEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Рыцарь-демон — огненный рыцарь ростом ~3,6 блока (модель из пака Demon Knight).
 * <ul>
 *     <li>две серии ударов с рывками к цели; удары поджигают и отбрасывают, последний удар первой серии подбрасывает;</li>
 *     <li>издалека — «магия»: метки на земле у цели, через секунду в них бьют молнии, из части меток вылетают
 *     {@link DemonEyeEntity} (не больше {@link #MAX_EYES} рядом);</li>
 *     <li>захват: цель в руке, потом бросок, а ослабленную цель (≤ 20% здоровья) — казнь, сильный удар оземь.</li>
 * </ul>
 * Блоки не ломает, огня не боится.
 */
public class DemonKnightEntity extends AbilityMobEntity {
    public static final String FACTION = "demon";
    /** Где держит схваченную цель: правее, на высоте плеча, перед собой. */
    private static final Vec3 HOLD = new Vec3(0.9D, 2.6D, 1.4D);
    private static final int MAX_EYES = 4;

    private Ability throwAbility;
    private Ability executionAbility;
    /** Точки, куда ударят молнии текущей «магии». */
    private final List<Vec3> marks = new ArrayList<>();

    public DemonKnightEntity(EntityType<? extends DemonKnightEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 900.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public String faction() {
        return FACTION;
    }

    // ---------------------------------------------------------------- способности

    /** Рывок к цели перед ударом серии (если цель не в упор). */
    private static void charge(AbilityMobEntity mob, @Nullable LivingEntity target, double speed) {
        if (target == null || mob.distanceTo(target) < 3.0D)
            return;
        mob.lunge(target.position().subtract(mob.position()), speed, 0.0D);
    }

    /** Огненный взмах: конус перед рыцарем, цели загораются. */
    private static void slash(AbilityMobEntity mob, double multiplier, double range, double knockback, double lift) {
        for (LivingEntity hit : mob.hitCone(range, 150.0F, mob.damage(multiplier), knockback, lift))
            hit.igniteForSeconds(3.0F);
        final Vec3 front = mob.forwardPos(2.5D, 1.4D);
        mob.particles(ParticleTypes.SWEEP_ATTACK, front, 3, 0.8, 0.0);
        mob.particles(ParticleTypes.FLAME, front, 24, 1.0, 0.05);
        mob.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.7F);
        mob.sound(SoundEvents.TRIDENT_THROW.value(), 1.4F, 0.7F);
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        // серия из трёх взмахов: два с отбрасыванием, третий подбрасывает
        abilities.add(Ability.builder("combo_1").seconds(8.0).range(0, 9.0).cooldown(240).weight(3).unstoppable()
                .at(23, (mob, target) -> charge(mob, target, 0.9D))
                .at(27, (mob, target) -> slash(mob, 1.0, 5.0, 1.2, 0.25))
                .at(44, (mob, target) -> charge(mob, target, 0.9D))
                .at(48, (mob, target) -> slash(mob, 1.0, 5.0, 1.2, 0.25))
                .at(101, (mob, target) -> charge(mob, target, 1.0D))
                .at(107, (mob, target) -> {
                    for (LivingEntity hit : mob.hitArea(mob.forwardPos(2.5D, 1.0D), 4.5D, mob.damage(1.6), 2.2, 1.2))
                        hit.igniteForSeconds(4.0F);
                    mob.particles(ParticleTypes.EXPLOSION, mob.forwardPos(2.5D, 0.5D), 2, 0.6, 0.0);
                    mob.particleRing(ParticleTypes.FLAME, mob.forwardPos(2.5D, 0.2D), 3.0D, 36);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 1.6F);
                })
                .build());
        // короткая серия: второй удар отбрасывает далеко
        abilities.add(Ability.builder("combo_2").seconds(4.5).range(0, 9.0).cooldown(240).weight(3).unstoppable()
                .at(14, (mob, target) -> charge(mob, target, 0.9D))
                .at(16, (mob, target) -> slash(mob, 1.0, 5.0, 1.0, 0.2))
                .at(32, (mob, target) -> charge(mob, target, 1.1D))
                .at(37, (mob, target) -> slash(mob, 1.3, 5.0, 3.0, 0.35))
                .build());
        // молнии по меткам; часть меток выпускает глаза
        abilities.add(Ability.builder("magic").seconds(4.5).range(10.0, 30.0).cooldown(600).weight(2)
                .at(20, (mob, target) -> {
                    marks.clear();
                    if (target == null)
                        return;
                    marks.add(mob.groundAt(target.position()));
                    for (int i = 0; i < 3; i++) {
                        final double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
                        final double distance = 2.5D + mob.getRandom().nextDouble() * 3.5D;
                        marks.add(mob.groundAt(target.position().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance)));
                    }
                    mob.sound(SoundEvents.EVOKER_PREPARE_ATTACK, 2.0F, 0.6F);
                })
                .every(20, 54, 5, (mob, target, tick) -> {
                    for (Vec3 mark : marks)
                        mob.particleRing(ParticleTypes.FLAME, mark.add(0.0D, 0.1D, 0.0D), 2.5D, 20);
                })
                .at(55, (mob, target) -> {
                    for (Vec3 mark : marks) {
                        mob.spawnEffect("demon_lightning", mark, 57).animation("spawn");
                        for (LivingEntity hit : mob.hitArea(mark.add(0.0D, 0.5D, 0.0D), 3.0D, mob.damage(1.2), 0.4, 0.6))
                            hit.igniteForSeconds(3.0F);
                        mob.particles(ParticleTypes.FLASH, mark.add(0.0D, 1.0D, 0.0D), 1, 0.0, 0.0);
                    }
                    mob.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 1.1F);
                })
                .at(65, (mob, target) -> {
                    if (!(mob instanceof DemonKnightEntity knight))
                        return;
                    for (Vec3 mark : marks)
                        if (knight.eyesNearby() < MAX_EYES && mob.getRandom().nextFloat() < 0.35F)
                            mob.summon(MobExEntities.DEMON_EYE.get(), mark.add(0.0D, 1.0D, 0.0D));
                })
                .build());

        throwAbility = Ability.builder("throw").seconds(4.0).cooldown(0).when((mob, target) -> false)
                .at(29, (mob, target) -> {
                    final LivingEntity victim = mob.held();
                    mob.release(2.6D, 0.9D);
                    if (victim != null) {
                        victim.invulnerableTime = 0;
                        victim.hurt(mob.damageSources().mobAttack(mob), mob.damage(2.0));
                        victim.igniteForSeconds(3.0F);
                    }
                    mob.sound(SoundEvents.TRIDENT_THROW.value(), 1.6F, 0.6F);
                })
                .build();
        abilities.add(throwAbility);
        executionAbility = Ability.builder("execution").seconds(4.35).cooldown(0).when((mob, target) -> false)
                .at(39, (mob, target) -> {
                    final LivingEntity victim = mob.held();
                    mob.release(0.2D, 0.0D);
                    if (victim != null) {
                        victim.invulnerableTime = 0;
                        victim.hurt(mob.damageSources().mobAttack(mob), mob.damage(3.0));
                        mob.particles(ParticleTypes.LAVA, victim.position(), 16, 0.6, 0.1);
                    }
                    mob.sound(SoundEvents.ANVIL_LAND, 1.6F, 0.5F);
                })
                .build();
        abilities.add(executionAbility);
        abilities.add(Ability.builder("grab").duration(41).range(0, 4.0).cooldown(600).weight(2)
                .at(30, (mob, target) -> charge(mob, target, 0.7D))
                .at(34, (mob, target) -> {
                    if (target != null && mob.distanceTo(target) < 4.0D && mob.isEnemy(target)) {
                        mob.grab(target, HOLD);
                        mob.sound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 2.0F, 0.5F);
                    }
                })
                .onEnd((mob, target) -> {
                    final LivingEntity victim = mob.held();
                    if (victim == null)
                        return;
                    mob.forceAbility(victim.getHealth() <= victim.getMaxHealth() * 0.2F ? executionAbility : throwAbility, victim);
                })
                .build());
    }

    private int eyesNearby() {
        return level().getEntitiesOfClass(DemonEyeEntity.class, getBoundingBox().inflate(32.0D)).size();
    }

    @Override
    protected void onAbilityFinished(@Nullable Ability ability, boolean interrupted) {
        if (ability != null && "magic".equals(ability.name()))
            marks.clear();
    }

    // ---------------------------------------------------------------- прочее

    @Override
    protected int deathTicks() {
        return Ability.ticks(2.5) + 10;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public String modelName() {
        return "demon_knight";
    }

    @Override
    public String textureName() {
        return "demon_knight";
    }

    @Override
    public String animationName() {
        return "demon_knight";
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 0.7F, 0.5F);
    }
}
