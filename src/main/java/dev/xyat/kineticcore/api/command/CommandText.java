package dev.xyat.kineticcore.api.command;

import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

/**
 * Builds chat lines for command help and feedback: translated headers, clickable command entries with hover
 * descriptions, and clickable text that runs, suggests, copies or opens a link.
 */
public final class CommandText {
    private CommandText() {
    }

    /** Returns a translated header line; formatting comes from the language file. */
    public static MutableComponent header(String translationKey) {
        return KineticI18n.translatable(translationKey);
    }

    /**
     * Returns a chat entry that runs {@code command} when clicked.
     *
     * @param command full command including the leading slash, for example {@code /mymod reload}
     * @param descriptionKey language key of the hover description
     * @return the command prefix symbol followed by the clickable command
     */
    public static MutableComponent executable(String command, String descriptionKey) {
        return KineticI18n.translatable("gui.kineticcore.symbol.command_prefix")
                .append(clickToRun(Component.literal(command), command, KineticI18n.translatable(descriptionKey)));
    }

    /**
     * Returns a chat entry that fills the chat input with {@code commandPrefix} when clicked, so the player can add
     * arguments before sending.
     *
     * @param display text shown in chat
     * @param commandPrefix text inserted into the chat input, for example {@code /mymod set }
     * @param descriptionKey language key of the hover description
     * @return the command prefix symbol followed by the clickable text
     */
    public static MutableComponent suggest(String display, String commandPrefix, String descriptionKey) {
        return suggest(Component.literal(display), commandPrefix, descriptionKey);
    }

    /**
     * Same as {@link #suggest(String, String, String)} but with translated or styled display text, for example
     * {@code KineticI18n.translatable("cmd.mymod.help.set")}.
     *
     * @param display component shown in chat
     * @param commandPrefix text inserted into the chat input, for example {@code /mymod set }
     * @param descriptionKey language key of the hover description
     * @return the command prefix symbol followed by the clickable text
     */
    public static MutableComponent suggest(Component display, String commandPrefix, String descriptionKey) {
        return KineticI18n.translatable("gui.kineticcore.symbol.command_prefix")
                .append(clickToSuggest(display, commandPrefix, KineticI18n.translatable(descriptionKey)));
    }

    /**
     * Returns a copy of {@code text} that runs {@code command} when clicked in chat, without the command prefix symbol.
     *
     * @param text visible text, usually a translated component whose colors come from the language file
     * @param command full command including the leading slash
     * @param hover hover text, or {@code null} for none
     * @return the clickable copy
     */
    public static MutableComponent clickToRun(Component text, String command, @Nullable Component hover) {
        return clickable(text, new ClickEvent(ClickEvent.Action.RUN_COMMAND, command), hover);
    }

    /**
     * Returns a copy of {@code text} that fills the chat input with {@code command} when clicked.
     *
     * @param text visible text
     * @param command text inserted into the chat input, for example {@code /mymod set }
     * @param hover hover text, or {@code null} for none
     * @return the clickable copy
     */
    public static MutableComponent clickToSuggest(Component text, String command, @Nullable Component hover) {
        return clickable(text, new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command), hover);
    }

    /**
     * Returns a copy of {@code text} that copies {@code value} to the clipboard when clicked.
     *
     * @param text visible text
     * @param value text placed on the clipboard; may contain line breaks
     * @param hover hover text, or {@code null} for none
     * @return the clickable copy
     */
    public static MutableComponent clickToCopy(Component text, String value, @Nullable Component hover) {
        return clickable(text, new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value), hover);
    }

    /**
     * Returns a copy of {@code text} that opens {@code url} when clicked; the client still asks the player to confirm.
     *
     * @param text visible text
     * @param url absolute http or https address
     * @param hover hover text, or {@code null} for none
     * @return the clickable copy
     */
    public static MutableComponent clickToOpenUrl(Component text, String url, @Nullable Component hover) {
        return clickable(text, new ClickEvent(ClickEvent.Action.OPEN_URL, url), hover);
    }

    private static MutableComponent clickable(Component text, ClickEvent click, @Nullable Component hover) {
        return text.copy().withStyle(style -> {
            Style clicked = style.withClickEvent(click);
            return hover == null ? clicked : clicked.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
        });
    }
}
