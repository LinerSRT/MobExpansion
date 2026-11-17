package net.mcskill.mobexpansion.entity.sea;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Дополнительный хитбокс огромного моба (голова, хвост): удары по нему уходят хозяину.
 * Положение задаётся смещением относительно корпуса (right, up, front) и обновляется хозяином каждый тик
 * на обеих сторонах — на клиенте хитбокс нужен для прицела, на сервере — для попаданий.
 */
public class SeaCreaturePart extends PartEntity<SeaCreatureEntity> {
    private final String name;
    private final EntityDimensions size;
    private final Vec3 offset;

    public SeaCreaturePart(SeaCreatureEntity parent, String name, float width, float height, Vec3 offset) {
        super(parent);
        this.name = name;
        this.size = EntityDimensions.scalable(width, height);
        this.offset = offset;
        refreshDimensions();
    }

    public String partName() {
        return name;
    }

    /** Поставить хитбокс по текущему положению и повороту хозяина. */
    public void follow() {
        final SeaCreatureEntity parent = getParent();
        final Vec3 pos = parent.localPos(offset.x, offset.y, offset.z);
        setOldPosAndRot();
        setPos(pos.x, pos.y, pos.z);
        if (tickCount == 0) {
            xo = xOld = pos.x;
            yo = yOld = pos.y;
            zo = zOld = pos.z;
        }
        tickCount++;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return true;
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isInvulnerableTo(source) || getParent().isDeadOrDying())
            return false;
        return getParent().hurt(source, amount);
    }

    @Override
    public boolean is(@NotNull Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    @NotNull
    public EntityDimensions getDimensions(@NotNull Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
