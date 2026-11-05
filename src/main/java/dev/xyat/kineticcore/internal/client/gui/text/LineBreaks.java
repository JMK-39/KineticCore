package dev.xyat.kineticcore.internal.client.gui.text;

import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps text as {@link Font#split} does, with the Chinese and Japanese punctuation rules of {@link LineBreakRules}:
 * no line starts with a comma or a closing bracket and none ends with an opening one. Text without such punctuation,
 * or that fits on one line, keeps vanilla's lines unchanged. Every wrapped text of KineticCore's GUIs goes through here.
 */
public final class LineBreaks {
    private LineBreaks() {
    }

    /** The lines to draw, as {@link Font#split} returns them. */
    public static List<FormattedCharSequence> split(Font font, FormattedText text, int maxWidth) {
        return Language.getInstance().getVisualOrder(splitLines(font, text, maxWidth));
    }

    /** The lines in logical order, as {@code StringSplitter.splitLines} returns them. */
    public static List<FormattedText> splitLines(Font font, FormattedText text, int maxWidth) {
        int width = Math.max(1, maxWidth);
        List<FormattedText> vanilla = font.getSplitter().splitLines(text, width, Style.EMPTY);
        if (vanilla.size() < 2) return vanilla;
        List<Style> styles = new ArrayList<>();
        int[] points = flatten(text, styles);
        if (!LineBreakRules.affected(points)) return vanilla;
        List<LineBreakRules.Range> ranges;
        try {
            ranges = LineBreakRules.apply(points, from -> locate(points, from == 0 ? vanilla
                    : font.getSplitter().splitLines(part(points, styles, from, points.length), width, Style.EMPTY), from));
        } catch (IllegalStateException unmatched) {
            // Vanilla's lines are always pieces of the text; if they ever are not, its own lines are drawn.
            return vanilla;
        }
        List<FormattedText> lines = new ArrayList<>(ranges.size());
        for (LineBreakRules.Range range : ranges) lines.add(part(points, styles, range.start(), range.end()));
        return lines;
    }

    /** The text's characters as drawn (formatting codes applied), with the style of each. */
    private static int[] flatten(FormattedText text, List<Style> styles) {
        List<Integer> points = new ArrayList<>();
        StringDecomposer.iterateFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            points.add(codePoint);
            if (styles != null) styles.add(style);
            return true;
        });
        int[] result = new int[points.size()];
        for (int i = 0; i < result.length; i++) result[i] = points.get(i);
        return result;
    }

    /** Where vanilla's lines of the text from {@code from} lie in it; the spaces and line feeds it dropped between them are skipped. */
    private static List<LineBreakRules.Range> locate(int[] points, List<FormattedText> lines, int from) {
        List<LineBreakRules.Range> ranges = new ArrayList<>(lines.size());
        int position = from;
        for (FormattedText line : lines) {
            int[] content = flatten(line, null);
            int start = position;
            while (!matches(points, start, content)) {
                if (start >= points.length || (points[start] != ' ' && points[start] != '\n')) throw new IllegalStateException("line not in text");
                start++;
            }
            ranges.add(new LineBreakRules.Range(start, start + content.length));
            position = start + content.length;
        }
        return ranges;
    }

    private static boolean matches(int[] points, int start, int[] content) {
        if (start + content.length > points.length) return false;
        for (int i = 0; i < content.length; i++) {
            if (points[start + i] != content[i]) return false;
        }
        return true;
    }

    /** Code points {@code [start, end)} as text, each run of one style a part of its own. */
    private static FormattedText part(int[] points, List<Style> styles, int start, int end) {
        List<FormattedText> parts = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style current = null;
        for (int i = start; i < end; i++) {
            Style style = styles.get(i);
            if (current != null && !style.equals(current)) {
                parts.add(FormattedText.of(run.toString(), current));
                run.setLength(0);
            }
            current = style;
            run.appendCodePoint(points[i]);
        }
        if (current != null) parts.add(FormattedText.of(run.toString(), current));
        return parts.isEmpty() ? FormattedText.EMPTY : FormattedText.composite(parts);
    }
}
