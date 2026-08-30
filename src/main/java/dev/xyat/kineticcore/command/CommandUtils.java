package dev.xyat.kineticcore.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class CommandUtils {

    public static MutableComponent createHeader(String subModuleKey) {
        return Component.translatable(subModuleKey);
    }

    public static MutableComponent createExecutableCommand(String command, String descKey) {
        return Component.literal("➤ ")
                .append(Component.literal(command)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(descKey).withStyle(ChatFormatting.GOLD)))
                        ));
    }

    public static MutableComponent createSuggestCommand(String displayString, String suggestPrefix, String descKey) {
        return Component.literal("➤ ")
                .append(Component.literal(displayString)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestPrefix))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(descKey).withStyle(ChatFormatting.GOLD)))
                        ));
    }
}