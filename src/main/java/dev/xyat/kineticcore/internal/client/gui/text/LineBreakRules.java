package dev.xyat.kineticcore.internal.client.gui.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Chinese and Japanese line-break rules on top of vanilla's wrapping: closing punctuation (，。、）」 and the like)
 * never starts a line and opening punctuation (（「“ and the like) never ends one. Vanilla breaks CJK text after any
 * character, so a comma could open the next line; where it does, the break moves back up to {@link #MAX_MOVE}
 * characters and the rest of the text is wrapped again from there. Breaks at spaces (Latin words) and forced breaks
 * are left as vanilla made them. Works on code points, without Minecraft, so the rules can be checked headless.
 */
public final class LineBreakRules {
    /** How many characters a break may move back, so a line never shrinks to almost nothing. */
    public static final int MAX_MOVE = 3;
    private static final String NO_START = "，。、；：？！）］｝〕〉》」』】〙〗〟｠”’»…‥·・—～％‰℃．｡､,.;:?!)]}%";
    private static final String NO_END = "（［｛〔〈《「『【〘〖〝｟“‘«([{";

    /** Code points {@code [start, end)} of the whole text that make one line. */
    public record Range(int start, int end) {
    }

    /** Vanilla's lines of the text from code point {@code from} on, as ranges into the whole text. */
    @FunctionalInterface
    public interface Splitter {
        List<Range> split(int from);
    }

    private LineBreakRules() {
    }

    public static boolean noLineStart(int codePoint) {
        return NO_START.indexOf(codePoint) >= 0;
    }

    public static boolean noLineEnd(int codePoint) {
        return NO_END.indexOf(codePoint) >= 0;
    }

    /** Whether any character of the text is bound by a rule; without one, vanilla's lines stand as they are. */
    public static boolean affected(int[] codePoints) {
        for (int codePoint : codePoints) {
            if (noLineStart(codePoint) || noLineEnd(codePoint)) return true;
        }
        return false;
    }

    /** Whether a break before code point {@code at} puts closing punctuation first or opening punctuation last. */
    public static boolean badBreak(int[] codePoints, int at) {
        return at > 0 && at < codePoints.length && (noLineStart(codePoints[at]) || noLineEnd(codePoints[at - 1]));
    }

    /**
     * The lines of the text with the rules applied. Only breaks between two characters (nothing dropped between the
     * lines) move; when no place within {@link #MAX_MOVE} characters is allowed, vanilla's break stays.
     */
    public static List<Range> apply(int[] codePoints, Splitter splitter) {
        List<Range> result = new ArrayList<>();
        List<Range> lines = splitter.split(0);
        int index = 0;
        while (index < lines.size()) {
            Range line = lines.get(index);
            if (index + 1 < lines.size()) {
                int next = lines.get(index + 1).start();
                if (next == line.end() && badBreak(codePoints, next)) {
                    int at = next, limit = Math.max(line.start() + 1, next - MAX_MOVE);
                    while (at > limit && badBreak(codePoints, at)) at--;
                    if (!badBreak(codePoints, at)) {
                        result.add(new Range(line.start(), at));
                        lines = splitter.split(at);
                        index = 0;
                        continue;
                    }
                }
            }
            result.add(line);
            index++;
        }
        return result;
    }
}
