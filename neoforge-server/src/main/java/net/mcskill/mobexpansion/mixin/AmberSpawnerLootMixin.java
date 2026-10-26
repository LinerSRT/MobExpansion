package net.mcskill.mobexpansion.mixin;

import net.mcskill.mobexpansion.spawner.SpawnerLoot;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Amber restores complete entity NBT, including the original UUID. */
@Pseudo
@Mixin(targets = "cy.jdkdigital.productivebees.common.block.entity.AmberBlockEntity", remap = false)
public abstract class AmberSpawnerLootMixin {
    @Inject(method = "createEntity", at = @At("RETURN"), remap = false)
    private static void mobexpansion$clearSpawnerLoot(CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() != null)
            SpawnerLoot.clear(cir.getReturnValue());
    }
}
