package net.mcskill.mobexpansion.menu;

import net.mcskill.mobexpansion.attributes.EntityAttributeConfig;
import net.mcskill.mobexpansion.attributes.EntityAttributeDefaults;
import net.mcskill.mobexpansion.drops.DropDestination;
import net.mcskill.mobexpansion.drops.EntityLootConfig;
import net.mcskill.mobexpansion.drops.LootConditionEntry;
import net.mcskill.mobexpansion.drops.LootConditionType;
import net.mcskill.mobexpansion.drops.MobDropEntry;
import net.mcskill.mobexpansion.drops.MobExperienceEntry;
import net.mcskill.mobexpansion.init.MobExMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MobDropConfigMenu extends AbstractContainerMenu {
    private final Map<ResourceLocation, EntityLootConfig> configsByEntity = new HashMap<>();
    private final Map<ResourceLocation, EntityAttributeConfig> attributesByEntity = new HashMap<>();
    private ResourceLocation selectedEntityId;
    private int selectedDropIndex;

    public MobDropConfigMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        super(MobExMenus.MOB_DROP_CONFIG.get(), containerId);
        selectedEntityId = readOptionalId(buffer.readUtf());
        final int entityCount = buffer.readVarInt();
        for (int entityIndex = 0; entityIndex < entityCount; entityIndex++) {
            final ResourceLocation entityId = ResourceLocation.tryParse(buffer.readUtf());
            final List<MobDropEntry> dropEntries = readDrops(buffer);
            final List<MobExperienceEntry> experienceEntries = readExperience(buffer);
            final EntityAttributeConfig attributeConfig = readAttributes(buffer);
            final boolean replaceVanilla = buffer.readBoolean();
            if (entityId != null) {
                configsByEntity.put(entityId, new EntityLootConfig(dropEntries, experienceEntries, replaceVanilla));
                attributesByEntity.put(entityId, attributeConfig.sanitized(entityId));
            }
        }
        selectedDropIndex = 0;
    }

    private static List<MobDropEntry> readDrops(FriendlyByteBuf buffer) {
        final int dropCount = buffer.readVarInt();
        final List<MobDropEntry> dropEntries = new ArrayList<>();
        for (int dropIndex = 0; dropIndex < dropCount; dropIndex++) {
            final ResourceLocation itemId = ResourceLocation.tryParse(buffer.readUtf());
            dropEntries.add(new MobDropEntry(itemId, buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), readConditions(buffer)));
        }
        return dropEntries;
    }

    private static List<MobExperienceEntry> readExperience(FriendlyByteBuf buffer) {
        final int experienceCount = buffer.readVarInt();
        final List<MobExperienceEntry> experienceEntries = new ArrayList<>();
        for (int experienceIndex = 0; experienceIndex < experienceCount; experienceIndex++)
            experienceEntries.add(new MobExperienceEntry(buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), readConditions(buffer)));
        return experienceEntries;
    }

    private static EntityAttributeConfig readAttributes(FriendlyByteBuf buffer) {
        final double maxHealth = buffer.readDouble();
        final double armor = buffer.readDouble();
        final double armorToughness = buffer.readDouble();
        final double attackDamage = buffer.readDouble();
        final double movementSpeed = buffer.readDouble();
        final double knockbackResistance = buffer.readDouble();
        final double followRange = buffer.readDouble();
        final int extraCount = buffer.readVarInt();
        final Map<String, Double> extras = new LinkedHashMap<>();
        for (int index = 0; index < extraCount; index++)
            extras.put(buffer.readUtf(), buffer.readDouble());
        return new EntityAttributeConfig(
                maxHealth,
                armor,
                armorToughness,
                attackDamage,
                movementSpeed,
                knockbackResistance,
                followRange,
                extras,
                buffer.readBoolean() ? DropDestination.KILLER_INVENTORY : DropDestination.WORLD
        );
    }

    private static List<LootConditionEntry> readConditions(FriendlyByteBuf buffer) {
        final int conditionCount = buffer.readVarInt();
        final List<LootConditionEntry> conditions = new ArrayList<>();
        for (int conditionIndex = 0; conditionIndex < conditionCount; conditionIndex++) {
            conditions.add(new LootConditionEntry(
                    LootConditionType.fromName(buffer.readUtf()),
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readByte(),
                    buffer.readByte()
            ));
        }
        return conditions;
    }

    private static ResourceLocation readOptionalId(String text) {
        if (text == null || text.isEmpty())
            return null;
        return ResourceLocation.tryParse(text);
    }

    public ResourceLocation getSelectedEntityId() {
        return selectedEntityId;
    }

    public void setSelectedEntityId(ResourceLocation selectedEntityId) {
        this.selectedEntityId = selectedEntityId;
        this.selectedDropIndex = 0;
    }

    public EntityLootConfig getSelectedConfig() {
        if (selectedEntityId == null)
            return EntityLootConfig.EMPTY;
        return configsByEntity.getOrDefault(selectedEntityId, EntityLootConfig.EMPTY);
    }

    public List<MobDropEntry> getSelectedDrops() {
        return getSelectedConfig().drops();
    }

    public void setSelectedConfig(EntityLootConfig entityConfig) {
        if (selectedEntityId == null)
            return;
        configsByEntity.put(selectedEntityId, entityConfig == null ? EntityLootConfig.EMPTY : entityConfig);
        if (selectedDropIndex >= getSelectedDrops().size())
            selectedDropIndex = Math.max(0, getSelectedDrops().size() - 1);
    }

    public Map<ResourceLocation, EntityLootConfig> getConfigsByEntity() {
        return configsByEntity;
    }

    public Map<ResourceLocation, EntityAttributeConfig> getAttributesByEntity() {
        return attributesByEntity;
    }

    public EntityAttributeConfig getSelectedAttributes() {
        if (selectedEntityId == null)
            return EntityAttributeDefaults.get(null);
        return attributesByEntity.getOrDefault(selectedEntityId, EntityAttributeDefaults.get(selectedEntityId));
    }

    public void setSelectedAttributes(EntityAttributeConfig attributeConfig) {
        if (selectedEntityId == null)
            return;
        attributesByEntity.put(selectedEntityId, attributeConfig == null
                ? EntityAttributeDefaults.get(selectedEntityId)
                : attributeConfig.sanitized(selectedEntityId));
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.isAlive();
    }
}
