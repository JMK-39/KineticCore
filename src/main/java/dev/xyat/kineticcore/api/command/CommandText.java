package dev.xyat.kineticcore.api.command;

import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/** Public API type for command text. */
public final class CommandText {
    private CommandText() {
    }

    /**
     * Performs the header API operation.
     */
    public static MutableComponent header(String translationKey) {
        return KineticI18n.translatable(translationKey);
    }

    /**
     * Performs the executable API operation.
     */
    public static MutableComponent executable(String command, String descriptionKey) {
        return KineticI18n.translatable("gui.kineticcore.symbol.command_prefix")
                .append(Component.literal(command)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        KineticI18n.translatable(descriptionKey)
                                ))
                        ));
    }

    /**
     * Creates suggest command.
     */
    public static MutableComponent createSuggestCommand(String display, String commandPrefix, String descriptionKey) {
        return suggest(display, commandPrefix, descriptionKey);
    }

    /**
     * Performs the suggest API operation.
     */
    public static MutableComponent suggest(String display, String commandPrefix, String descriptionKey) {
        return KineticI18n.translatable("gui.kineticcore.symbol.command_prefix")
                .append(Component.literal(display)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandPrefix))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        KineticI18n.translatable(descriptionKey)
                                ))
                        ));
    }
}
