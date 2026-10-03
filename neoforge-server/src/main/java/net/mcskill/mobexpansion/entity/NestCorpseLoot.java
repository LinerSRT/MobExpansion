package net.mcskill.mobexpansion.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public final class NestCorpseLoot {
    private static final int DEFAULT_LIFETIME_TICKS = 200;

    private final BlockPos position;
    private final int experienceAmount;
    private int lifeTicks;

    public NestCorpseLoot(BlockPos position, int experienceAmount) {
        this.position = position.immutable();
        this.experienceAmount = Math.max(1, experienceAmount);
        this.lifeTicks = DEFAULT_LIFETIME_TICKS;
    }

    public NestCorpseLoot(BlockPos position, int experienceAmount, int lifeTicks) {
        this.position = position.immutable();
        this.experienceAmount = Math.max(1, experienceAmount);
        this.lifeTicks = lifeTicks;
    }

    public BlockPos getPosition() {
        return position;
    }

    public int getExperienceAmount() {
        return experienceAmount;
    }

    public boolean tickAndExpired() {
        lifeTicks--;
        return lifeTicks <= 0;
    }

    public CompoundTag save() {
        final CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt("X", position.getX());
        compoundTag.putInt("Y", position.getY());
        compoundTag.putInt("Z", position.getZ());
        compoundTag.putInt("Xp", experienceAmount);
        compoundTag.putInt("Life", lifeTicks);
        return compoundTag;
    }

    public static NestCorpseLoot load(CompoundTag compoundTag) {
        return new NestCorpseLoot(
                new BlockPos(compoundTag.getInt("X"), compoundTag.getInt("Y"), compoundTag.getInt("Z")),
                compoundTag.getInt("Xp"),
                compoundTag.getInt("Life")
        );
    }
}
