package net.mcskill.mobexpansion.entity.boss;

import net.mcskill.mobexpansion.entity.ability.Ability;
import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Вороний жнец — второй из пака Raven Knights: капюшон, коса-секира, ~3,7 блока.
 * <ul>
 *     <li>взмахи секирой перед собой, тяжёлый удар иссушает и отбрасывает, серия с вороньим туманом вокруг;</li>
 *     <li>«кольцо шипов» — шипы встают вокруг жнеца и через 1,3 с бьют всех в 5 блоках (сильнейший удар);</li>
 *     <li>с дистанции — призыв секир: над каждым врагом в 20 блоках появляется секира и бьёт сверху
 *     или дважды сбоку;</li>
 *     <li>каждые 19 с — вороньи стаи.</li>
 * </ul>
 * Свой — {@link CrowKnightEntity} (одна фракция). Блоки не ломает.
 */
public class CrowReaperEntity extends AbilityMobEntity {
    private static final int SWARM_PERIOD = 380;

    private int swarmTimer = SWARM_PERIOD / 2;

    public CrowReaperEntity(EntityType<? extends CrowReaperEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 700.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public String faction() {
        return CrowKnightEntity.FACTION;
    }

    @Override
    protected void registerAbilities(List<Ability> abilities) {
        abilities.add(Ability.builder("basicattack").seconds(1.75).range(0, 5.0).cooldown(30).weight(3)
                .at(14, (mob, target) -> CrowKnightEntity.slash(mob, 1.0, 180.0F, 0.5, 0.1, 0))
                .build());
        abilities.add(Ability.builder("chargeattack").seconds(2.55).range(0, 5.0).cooldown(80).weight(2)
                .at(20, (mob, target) -> {
                    CrowKnightEntity.slash(mob, 1.0, 180.0F, 0.2, 0.0, 100);
                    for (LivingEntity enemy : mob.enemiesNear(mob.position(), 3.0D))
                        AbilityMobEntity.push(enemy, mob.position(), 1.2D, 0.5D);
                })
                .build());
        abilities.add(Ability.builder("fogcombo").seconds(3.0).range(0, 5.0).cooldown(120).weight(2)
                .at(13, (mob, target) -> {
                    CrowKnightEntity.slash(mob, 1.0, 180.0F, 0.2, 0.0, 0);
                    CrowKnightEntity.fogRing(mob, 4.0D, 8);
                })
                .at(38, (mob, target) -> {
                    CrowKnightEntity.slash(mob, 1.0, 180.0F, 0.2, 0.0, 0);
                    for (LivingEntity enemy : mob.enemiesNear(mob.position(), 5.0D))
                        AbilityMobEntity.push(enemy, mob.position(), 1.2D, 0.6D);
                })
                .build());
        abilities.add(Ability.builder("spikesattack").seconds(2.75).range(0, 6.0).cooldown(200).weight(2).unstoppable()
                .at(0, (mob, target) -> {
                    mob.spawnEffect("crow_reaper_spikes", mob.position(), 60).animation("spawn").glowing(false);
                    mob.particleRing(ParticleTypes.SQUID_INK, mob.position().add(0.0D, 0.1D, 0.0D), 5.0D, 40);
                })
                .at(26, (mob, target) -> {
                    mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 5.0D, mob.damage(2.0), 1.6, 0.8);
                    mob.sound(SoundEvents.POINTED_DRIPSTONE_LAND, 2.0F, 0.4F);
                    mob.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.2F);
                })
                .build());
        abilities.add(Ability.builder("summonaxe").seconds(2.04).range(3.0, 20.0).cooldown(160).weight(2).ignoreSight()
                .at(20, (mob, target) -> mob.hitArea(mob.position().add(0.0D, 1.0D, 0.0D), 3.5D, mob.damage(1.0), 0.6, 0.2))
                .at(22, (mob, target) -> {
                    for (LivingEntity enemy : mob.enemiesNear(mob.position(), 20.0D))
                        axe(mob, enemy.position());
                    mob.sound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.6F, 0.6F);
                })
                .build());
    }

    /** Секира над точкой: сверху (удар через 1 с) или сбоку (два удара — через 1 и 2,9 с), иссушает. */
    private static void axe(AbilityMobEntity mob, Vec3 spot) {
        final boolean overhead = mob.getRandom().nextBoolean();
        final float heavy = mob.damage(2.0);
        final float light = mob.damage(1.25);
        mob.particleRing(ParticleTypes.SQUID_INK, spot.add(0.0D, 0.1D, 0.0D), 2.0D, 16);
        mob.spawnEffect("crow_axe", spot, overhead ? 46 : 70, mob.getRandom().nextFloat() * 360.0F)
                .animation(overhead ? "updown" : "sideattack").glowing(false).ticker((effect, age) -> {
                    if (age == 20 || (!overhead && age == 58)) {
                        for (LivingEntity hit : mob.hitArea(effect.position().add(0.0D, 1.0D, 0.0D), 2.0D, overhead ? heavy : light, 0.4, 0.2))
                            mob.effect(hit, MobEffects.WITHER, 80, 0);
                        mob.sound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.4F);
                    }
                });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying())
            return;
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceTo(target) > 20.0D)
            return;
        if (--swarmTimer <= 0) {
            swarmTimer = SWARM_PERIOD;
            CrowKnightEntity.swarms(this, target);
        }
    }

    // ---------------------------------------------------------------- прочее

    @Override
    protected int deathTicks() {
        return Ability.ticks(9.25);
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
        return "crow_reaper";
    }

    @Override
    public String textureName() {
        return "crow_reaper";
    }

    @Override
    public String animationName() {
        return "crow_reaper";
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.6F, 0.5F);
    }
}
