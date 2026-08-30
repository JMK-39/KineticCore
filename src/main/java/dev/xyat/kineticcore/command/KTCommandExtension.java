package dev.xyat.kineticcore.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Extension point used by dependent mods to contribute to the shared
 * {@code /kt} command without making kineticcore depend on those mods.
 */
public interface KTCommandExtension {
    default void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
    }

    default void appendHelpItems(CommandSourceStack source, List<MutableComponent> items) {
    }

    default void reload(CommandSourceStack source) {
    }
}
