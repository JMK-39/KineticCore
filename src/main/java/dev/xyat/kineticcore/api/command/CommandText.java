package dev.xyat.kineticcore.api.command;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public final class CommandText {
    private CommandText() {
    }

    public static MutableComponent header(String translationKey) {
        return Component.translatable(translationKey);
    }

    public static MutableComponent executable(String command, String descriptionKey) {
        return Component.translatable("gui.kineticcore.symbol.command_prefix")
                .append(Component.literal(command)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable(descriptionKey)
                                ))
                        ));
    }

    public static MutableComponent suggest(String display, String commandPrefix, String descriptionKey) {
        return Component.translatable("gui.kineticcore.symbol.command_prefix")
                .append(Component.literal(display)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandPrefix))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable(descriptionKey)
                                ))
                        ));
    }
}
