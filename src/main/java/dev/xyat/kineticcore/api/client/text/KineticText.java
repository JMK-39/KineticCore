package dev.xyat.kineticcore.api.client.text;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public final class KineticText {
    private KineticText() {
    }

    public static MutableComponent translatable(String key, Object... args) {
        if (args == null || args.length == 0) {
            return Component.translatable(key);
        }

        String template = I18n.get(key);
        Object[] styledArgs = args.clone();
        List<ChatFormatting> activeFormats = new ArrayList<>();
        int sequentialArg = 0;

        for (int i = 0; i < template.length(); i++) {
            char current = template.charAt(i);
            if (current == '\u00A7' && i + 1 < template.length()) {
                ChatFormatting formatting = ChatFormatting.getByCode(template.charAt(i + 1));
                if (formatting != null) {
                    applyFormat(activeFormats, formatting);
                }
                i++;
                continue;
            }

            if (current != '%' || i + 1 >= template.length()) {
                continue;
            }

            char next = template.charAt(i + 1);
            if (next == '%') {
                i++;
                continue;
            }

            int argIndex = -1;
            int tokenEnd = i + 1;
            if (next == 's') {
                argIndex = sequentialArg++;
                tokenEnd = i + 1;
            } else if (Character.isDigit(next)) {
                int digitsStart = i + 1;
                int cursor = digitsStart;
                while (cursor < template.length() && Character.isDigit(template.charAt(cursor))) {
                    cursor++;
                }
                if (cursor < template.length() - 1 && template.charAt(cursor) == '$' && template.charAt(cursor + 1) == 's') {
                    try {
                        argIndex = Integer.parseInt(template.substring(digitsStart, cursor)) - 1;
                    } catch (NumberFormatException ignored) {
                        argIndex = -1;
                    }
                    tokenEnd = cursor + 1;
                }
            }

            if (argIndex >= 0 && argIndex < styledArgs.length && !activeFormats.isEmpty()) {
                styledArgs[argIndex] = applyFormats(styledArgs[argIndex], activeFormats);
                i = tokenEnd;
            }
        }

        return Component.translatable(key, styledArgs);
    }

    private static void applyFormat(List<ChatFormatting> activeFormats, ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) {
            activeFormats.clear();
            return;
        }
        if (formatting.getColor() != null) {
            activeFormats.removeIf(existing -> existing.getColor() != null);
        }
        if (!activeFormats.contains(formatting)) {
            activeFormats.add(formatting);
        }
    }

    private static MutableComponent applyFormats(Object value, List<ChatFormatting> formats) {
        MutableComponent component = value instanceof Component existing
                ? existing.copy()
                : Component.literal(String.valueOf(value));
        component.withStyle(formats.toArray(new ChatFormatting[0]));
        return component;
    }
}
