package net.mcskill.mobexpansion.command;

import com.mojang.brigadier.CommandDispatcher;
import net.mcskill.mobexpansion.cosmetics.Cosmetics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /cosmetics} — открыть GUI кастомизации персонажа (доступно всем игрокам).
 */
public final class CosmeticsCommand {
    private CosmeticsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cosmetics")
                .executes(ctx -> {
                    Cosmetics.open(ctx.getSource().getPlayerOrException());
                    return 1;
                }));
    }
}
