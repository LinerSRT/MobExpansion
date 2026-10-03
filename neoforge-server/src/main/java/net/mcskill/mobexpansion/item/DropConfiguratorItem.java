package net.mcskill.mobexpansion.item;

import net.mcskill.mobexpansion.attributes.EntityAttributeConfig;
import net.mcskill.mobexpansion.attributes.EntityAttributeDefaults;
import net.mcskill.mobexpansion.attributes.EntityAttributesConfigIO;
import net.mcskill.mobexpansion.drops.EntityLootConfig;
import net.mcskill.mobexpansion.drops.EntityLootConfigIO;
import net.mcskill.mobexpansion.menu.MobDropConfigMenu;
import net.mcskill.mobexpansion.spawner.SpawnerMobCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DropConfiguratorItem extends Item {
    public DropConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        final ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide())
            return InteractionResultHolder.success(itemStack);
        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResultHolder.consume(itemStack);
        if (!serverPlayer.hasPermissions(4))
            return InteractionResultHolder.fail(itemStack);
        final Map<ResourceLocation, EntityLootConfig> scriptConfigs = EntityLootConfigIO.getCached();
        final Map<ResourceLocation, EntityLootConfig> configsByEntity = new LinkedHashMap<>(scriptConfigs);
        for (ResourceLocation entityId : EntityAttributeDefaults.all().keySet())
            configsByEntity.putIfAbsent(entityId, scriptConfigs.getOrDefault(entityId, EntityLootConfig.EMPTY));
        final Map<ResourceLocation, EntityAttributeConfig> attributesByEntity = new HashMap<>(EntityAttributesConfigIO.loadMergedWithDefaults());
        final ResourceLocation initialEntityId = SpawnerMobCatalog.getSpawnableIds().isEmpty() ? null : SpawnerMobCatalog.getSpawnableIds().getFirst();
        serverPlayer.openMenu(new MenuProvider() {
            @Override
            @NotNull
            public Component getDisplayName() {
                return Component.translatable("item.mobexpansion.drop_configurator");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player menuPlayer) {
                return new MobDropConfigMenu(containerId, playerInventory);
            }
        }, buffer -> MobDropConfigMenu.writeInitialData(buffer, initialEntityId, configsByEntity, attributesByEntity));

        return InteractionResultHolder.consume(itemStack);
    }
}
