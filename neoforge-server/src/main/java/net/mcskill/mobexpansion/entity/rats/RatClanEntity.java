package net.mcskill.mobexpansion.entity.rats;

import net.mcskill.mobexpansion.entity.ability.AbilityMobEntity;
import net.mcskill.mobexpansion.init.MobExSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Крысиный клан (пак Rats | Boss &amp; Mobs): жнец, лучник и их владыка. Общая фракция, тяжёлые шаги
 * (звук базальта), удары разбивают щит — щит уходит на перезарядку, и удар проходит.
 */
public abstract class RatClanEntity extends AbilityMobEntity {
    public static final String FACTION = "rat_clan";

    @Nullable
    private String id;

    protected RatClanEntity(EntityType<? extends RatClanEntity> entityType, Level level) {
        super(entityType, level);
    }

    public String id() {
        if (id == null)
            id = BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
        return id;
    }

    @Override
    public String faction() {
        return FACTION;
    }

    @Override
    public String modelName() {
        return id();
    }

    @Override
    public String textureName() {
        return id();
    }

    @Override
    public String animationName() {
        return id();
    }

    /** Удар по области: сначала ломает поднятые щиты, потом бьёт. */
    public List<LivingEntity> slash(Vec3 center, double radius, double multiplier, int shieldBreakTicks, double knockback, double lift) {
        return slash(center, radius, multiplier, shieldBreakTicks, knockback, lift, false);
    }

    /** То же; pierceImmunity — удар проходит и сквозь неуязвимость после предыдущего (вторая сабля серии). */
    public List<LivingEntity> slash(Vec3 center, double radius, double multiplier, int shieldBreakTicks, double knockback, double lift, boolean pierceImmunity) {
        for (LivingEntity victim : enemiesNear(center, radius)) {
            breakShield(victim, shieldBreakTicks);
            if (pierceImmunity)
                victim.invulnerableTime = 0;
        }
        return hitArea(center, radius, damage(multiplier), knockback, lift);
    }

    /** Сбить поднятый щит: он уходит на перезарядку на ticks тиков. */
    public static void breakShield(LivingEntity target, int ticks) {
        if (!(target instanceof Player player) || !player.isBlocking())
            return;
        player.getCooldowns().addCooldown(player.getUseItem().getItem(), ticks);
        player.stopUsingItem();
        player.level().broadcastEntityEvent(player, (byte) 30);
    }

    public void woosh(int pitchPercent) {
        sound(MobExSounds.RATS_WOOSH, 1.5F, pitchPercent / 100.0F);
    }

    public void cloth(int pitchPercent) {
        sound(MobExSounds.RATS_CLOTH, 1.5F, pitchPercent / 100.0F);
    }

    public void swordSound(int pitchPercent) {
        sound(MobExSounds.RATS_SWORD, 1.5F, pitchPercent / 100.0F);
    }

    /** Рывок вперёд на время удара («setspeed 2» оригинала). */
    public void step(double speed) {
        lunge(forward(), speed, 0.0D);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // как в оригинале: в стене почти не задыхаются
        if (source.is(DamageTypes.IN_WALL))
            amount *= 0.1F;
        return super.hurt(source, amount);
    }

    protected static boolean isProjectile(DamageSource source) {
        return source.is(DamageTypeTags.IS_PROJECTILE);
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(SoundEvents.BASALT_STEP, 0.6F, 1.2F + random.nextFloat() * 0.4F);
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    @NotNull
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    @NotNull
    protected SoundEvent getDeathSound() {
        return SoundEvents.BASALT_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
