package net.mcskill.mobexpansion.menu;

import net.mcskill.mobexpansion.attributes.EntityAttributeConfig;
import net.mcskill.mobexpansion.attributes.EntityAttributeDefaults;
import net.mcskill.mobexpansion.drops.*;
import net.mcskill.mobexpansion.init.MobExMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    public MobDropConfigMenu(int containerId, Inventory playerInventory) {
        super(MobExMenus.MOB_DROP_CONFIG.get(), containerId);
        selectedEntityId = null;
        selectedDropIndex = 0;
    }

    public static void writeInitialData(FriendlyByteBuf buffer, ResourceLocation selectedEntityId, Map<ResourceLocation, EntityLootConfig> configsByEntity, Map<ResourceLocation, EntityAttributeConfig> attributesByEntity) {
        buffer.writeUtf(selectedEntityId == null ? "" : selectedEntityId.toString());
        buffer.writeVarInt(configsByEntity.size());
        for (Map.Entry<ResourceLocation, EntityLootConfig> entry : configsByEntity.entrySet()) {
            buffer.writeUtf(entry.getKey().toString());
            writeDrops(buffer, entry.getValue().drops());
            writeExperience(buffer, entry.getValue().experience());
            writeAttributes(buffer, attributesByEntity.getOrDefault(entry.getKey(), EntityAttributeDefaults.get(entry.getKey())).sanitized(entry.getKey()));
            buffer.writeBoolean(entry.getValue().replaceVanilla());
        }
    }

    private static void writeDrops(FriendlyByteBuf buffer, List<MobDropEntry> dropEntries) {
        buffer.writeVarInt(dropEntries.size());
        for (MobDropEntry dropEntry : dropEntries) {
            buffer.writeUtf(dropEntry.itemId().toString());
            buffer.writeVarInt(dropEntry.minCount());
            buffer.writeVarInt(dropEntry.maxCount());
            buffer.writeFloat(dropEntry.chance());
            writeConditions(buffer, dropEntry.conditions());
        }
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

    private static void writeExperience(FriendlyByteBuf buffer, List<MobExperienceEntry> experienceEntries) {
        buffer.writeVarInt(experienceEntries.size());
        for (MobExperienceEntry experienceEntry : experienceEntries) {
            buffer.writeVarInt(experienceEntry.minAmount());
            buffer.writeVarInt(experienceEntry.maxAmount());
            buffer.writeFloat(experienceEntry.chance());
            writeConditions(buffer, experienceEntry.conditions());
        }
    }

    private static List<MobExperienceEntry> readExperience(FriendlyByteBuf buffer) {
        final int experienceCount = buffer.readVarInt();
        final List<MobExperienceEntry> experienceEntries = new ArrayList<>();
        for (int experienceIndex = 0; experienceIndex < experienceCount; experienceIndex++)
            experienceEntries.add(new MobExperienceEntry(buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), readConditions(buffer)));
        return experienceEntries;
    }

    private static void writeAttributes(FriendlyByteBuf buffer, EntityAttributeConfig config) {
        buffer.writeDouble(config.maxHealth());
        buffer.writeDouble(config.armor());
        buffer.writeDouble(config.armorToughness());
        buffer.writeDouble(config.attackDamage());
        buffer.writeDouble(config.movementSpeed());
        buffer.writeDouble(config.knockbackResistance());
        buffer.writeDouble(config.followRange());
        buffer.writeVarInt(config.extras().size());
        for (Map.Entry<String, Double> entry : config.extras().entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeDouble(entry.getValue());
        }
        buffer.writeBoolean(config.dropDestination().givesToKiller());
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

    private static void writeConditions(FriendlyByteBuf buffer, List<LootConditionEntry> conditions) {
        buffer.writeVarInt(conditions.size());
        for (LootConditionEntry condition : conditions) {
            buffer.writeUtf(condition.type().name());
            buffer.writeUtf(condition.stringValue());
            buffer.writeVarInt(condition.intMin());
            buffer.writeVarInt(condition.intMax());
            buffer.writeVarInt(condition.intExtra());
            buffer.writeByte(condition.flagA());
            buffer.writeByte(condition.flagB());
        }
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
        selectedDropIndex = 0;
    }

    public int getSelectedDropIndex() {
        return selectedDropIndex;
    }

    public void setSelectedDropIndex(int selectedDropIndex) {
        this.selectedDropIndex = Math.max(0, selectedDropIndex);
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

    public void setSelectedDrops(List<MobDropEntry> dropEntries) {
        setSelectedConfig(getSelectedConfig().withDrops(dropEntries == null ? List.of() : dropEntries));
    }

    public void setSelectedExperience(List<MobExperienceEntry> experienceEntries) {
        setSelectedConfig(getSelectedConfig().withExperience(experienceEntries == null ? List.of() : experienceEntries));
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
        attributesByEntity.put(selectedEntityId, attributeConfig == null ? EntityAttributeDefaults.get(selectedEntityId) : attributeConfig.sanitized(selectedEntityId));
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
