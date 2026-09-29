package dev.xyat.kineticcore.api.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Adds sub-commands, help entries and reload behavior to KineticCore's {@code /kt} command. Register with
 * {@link KineticCommands#registerExtension(String, CommandExtension)}.
 */
public interface CommandExtension {
    /**
     * Adds sub-commands to the {@code /kt} root, for example {@code root.then(Commands.literal("mymod")...)}.
     * Called whenever commands are registered.
     */
    default void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
    }

    /**
     * Adds entries to the {@code /kt} help list shown to {@code source}; check permissions before adding privileged
     * commands.
     */
    default void appendHelpItems(CommandSourceStack source, List<MutableComponent> items) {
    }

    /** Reloads this add-on's data when an operator runs {@code /kt reload}. */
    default void reload(CommandSourceStack source) {
    }
}
