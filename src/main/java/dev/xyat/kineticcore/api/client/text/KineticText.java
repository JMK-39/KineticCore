package dev.xyat.kineticcore.api.client.text;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
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


    public static int drawScrollingLeft(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || text == null || maxWidth <= 0) return 0;
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            return graphics.drawString(font, text, x, y, color, shadow);
        }
        int overflow = textWidth - maxWidth;
        int offset = scrollingOffset(overflow);
        graphics.enableScissor(x, y - 1, x + maxWidth, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, text, x - offset, y, color, shadow);
        } finally {
            graphics.disableScissor();
        }
    }

    public static int drawScrollingLeft(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || maxWidth <= 0) return 0;
        String value = text == null ? "" : text;
        int textWidth = font.width(value);
        if (textWidth <= maxWidth) {
            return graphics.drawString(font, value, x, y, color, shadow);
        }
        int offset = scrollingOffset(textWidth - maxWidth);
        graphics.enableScissor(x, y - 1, x + maxWidth, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, value, x - offset, y, color, shadow);
        } finally {
            graphics.disableScissor();
        }
    }

    public static int drawScrollingCentered(
            GuiGraphics graphics,
            Font font,
            Component text,
            int centerX,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || text == null || maxWidth <= 0) return 0;
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            return graphics.drawString(font, text, centerX - textWidth / 2, y, color, shadow);
        }
        int left = centerX - maxWidth / 2;
        int overflow = textWidth - maxWidth;
        int offset = scrollingOffset(overflow);
        graphics.enableScissor(left, y - 1, left + maxWidth, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, text, left - offset, y, color, shadow);
        } finally {
            graphics.disableScissor();
        }
    }

    public static int drawScrollingCentered(
            GuiGraphics graphics,
            Font font,
            String text,
            int centerX,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || maxWidth <= 0) return 0;
        String value = text == null ? "" : text;
        int textWidth = font.width(value);
        if (textWidth <= maxWidth) {
            return graphics.drawString(font, value, centerX - textWidth / 2, y, color, shadow);
        }
        int left = centerX - maxWidth / 2;
        int offset = scrollingOffset(textWidth - maxWidth);
        graphics.enableScissor(left, y - 1, left + maxWidth, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, value, left - offset, y, color, shadow);
        } finally {
            graphics.disableScissor();
        }
    }

    public static int drawScrollingRight(
            GuiGraphics graphics,
            Font font,
            Component text,
            int rightX,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || text == null || maxWidth <= 0) return 0;
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            return graphics.drawString(font, text, rightX - textWidth, y, color, shadow);
        }
        int left = rightX - maxWidth;
        int overflow = textWidth - maxWidth;
        int offset = scrollingOffset(overflow);
        graphics.enableScissor(left, y - 1, rightX, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, text, left - offset, y, color, shadow);
        } finally {
            graphics.disableScissor();
        }
    }

    public static int drawScrollingRight(
            GuiGraphics graphics,
            Font font,
            String text,
            int rightX,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || maxWidth <= 0) return 0;
        String value = text == null ? "" : text;
        int textWidth = font.width(value);
        if (textWidth <= maxWidth) {
            return graphics.drawString(font, value, rightX - textWidth, y, color, shadow);
        }
        int left = rightX - maxWidth;
        int offset = scrollingOffset(textWidth - maxWidth);
        graphics.enableScissor(left, y - 1, rightX, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, value, left - offset, y, color, shadow);
        } finally {
            graphics.disableScissor();
        }
    }

    public static int scrollOffset(int contentWidth, int viewportWidth) {
        return scrollingOffset(Math.max(0, contentWidth - viewportWidth));
    }

    private static int scrollingOffset(int overflow) {
        if (overflow <= 0) return 0;
        double seconds = Util.getMillis() / 1000.0D;
        double duration = Math.max(overflow * 0.5D, 3.0D);
        double phase = Math.sin((Math.PI / 2.0D) * Math.cos((Math.PI * 2.0D) * seconds / duration)) / 2.0D + 0.5D;
        return (int) Math.round(Mth.lerp(phase, 0.0D, (double) overflow));
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
