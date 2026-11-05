package dev.xyat.kineticcore.internal.client.gui.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless regression for the CJK punctuation line-break rules, with a stand-in for vanilla's wrapping: lines of at
 * most a fixed number of characters, broken at the last space in reach (the space dropped) or else after any character.
 */
public final class LineBreakRulesRegression {
    public static void main(String[] args) {
        check(wrap("一二三四，五六", 4), "一二三|四，五六", "a comma never starts a line");
        check(wrap("一二三「四五」", 4), "一二三|「四五」", "an opening bracket never ends a line");
        check(wrap("一二三」。五六", 3), "一二|三」。|五六", "two closing marks move back together with a character");
        check(wrap("一。。。。二", 2), "一。|。。|。二", "no allowed break within reach keeps vanilla's lines");
        check(wrap("ab cd, ef", 4), "ab|cd,|ef", "breaks at spaces stay as they are");
        check(wrap("abcd ,ef", 4), "abcd|,ef", "a mark after a dropped space is not moved");
        check(wrap("一二三四五六", 4), "一二三四|五六", "text without punctuation keeps vanilla's lines");
        check(!LineBreakRules.affected(points("plain words")), "text without bound punctuation is left to vanilla");
        System.out.println("PASS: 8 CJK line-break regression cases");
    }

    private static int[] points(String text) {
        return text.codePoints().toArray();
    }

    private static String wrap(String text, int width) {
        int[] points = points(text);
        List<LineBreakRules.Range> lines = LineBreakRules.apply(points, from -> split(points, from, width));
        StringBuilder result = new StringBuilder();
        for (LineBreakRules.Range line : lines) {
            if (result.length() > 0) result.append('|');
            result.append(new String(points, line.start(), line.end() - line.start()));
        }
        return result.toString();
    }

    private static List<LineBreakRules.Range> split(int[] points, int from, int width) {
        List<LineBreakRules.Range> lines = new ArrayList<>();
        int position = from;
        while (position < points.length) {
            int end = Math.min(points.length, position + width);
            int space = -1;
            if (end < points.length) {
                for (int i = end; i > position; i--) {
                    if (points[i] == ' ') {
                        space = i;
                        break;
                    }
                }
            }
            if (space > position) {
                lines.add(new LineBreakRules.Range(position, space));
                position = space + 1;
            } else {
                lines.add(new LineBreakRules.Range(position, end));
                position = end;
            }
        }
        return lines;
    }

    private static void check(String actual, String expected, String message) {
        if (!actual.equals(expected)) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
