package dev.xyat.kineticcore.api.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public interface CommandExtension {
    default void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
    }

    default void appendHelpItems(CommandSourceStack source, List<MutableComponent> items) {
    }

    default void reload(CommandSourceStack source) {
    }
}
