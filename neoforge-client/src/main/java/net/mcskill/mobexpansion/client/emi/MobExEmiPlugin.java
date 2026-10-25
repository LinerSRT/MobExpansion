package net.mcskill.mobexpansion.client.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.mcskill.mobexpansion.Core;
import net.mcskill.mobexpansion.client.info.ClientMobInfo;
import net.mcskill.mobexpansion.info.MobInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

/**
 * EMI: категория «Мобы» — по странице на моба (характеристики, уровень допуска, дроп, спавнеры).
 * Данные приходят с сервера ({@link ClientMobInfo}); при их изменении EMI перезагружается.
 */
@EmiEntrypoint
public class MobExEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory MOB_INFO = new EmiRecipeCategory(
            Core.loc("mob_info"),
            EmiStack.of(Items.ZOMBIE_HEAD),
            EmiStack.of(Items.ZOMBIE_HEAD),
            Comparator.comparingInt(recipe -> recipe instanceof MobInfoEmiRecipe mobRecipe ? mobRecipe.order() : Integer.MAX_VALUE)
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(MOB_INFO);
        final List<MobInfo> mobs = ClientMobInfo.mobs();
        for (int i = 0; i < mobs.size(); i++) {
            final MobInfo info = mobs.get(i);
            if (!info.drops().isEmpty() && BuiltInRegistries.ENTITY_TYPE.containsKey(info.entityId()))
                registry.addRecipe(new MobInfoEmiRecipe(info, i));
        }
    }
}
