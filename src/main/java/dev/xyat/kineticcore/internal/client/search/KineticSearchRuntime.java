package dev.xyat.kineticcore.internal.client.search;

import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Internal text normalization and pinyin matching implementation used by Kinetic search APIs and indexes. */
public final class KineticSearchRuntime {
    private static final PinyinData EMPTY_PINYIN = new PinyinData("", "", "", new String[0], "");
    private static final ThreadLocal<MatchBuffer> MATCH_BUFFER = ThreadLocal.withInitial(MatchBuffer::new);

    private KineticSearchRuntime() {
    }

    public static boolean match(String text, String query) {
        if (query == null || query.isBlank()) return true;
        if (text == null || text.isBlank()) return false;

        String preparedText = normalize(text);
        String[] tokens = queryTokens(query);
        if (tokens.length == 0) return true;

        PinyinData pinyin = null;
        for (String token : tokens) {
            if (preparedText.contains(token)) continue;
            if (pinyin == null) pinyin = preparePinyin(text);
            if (failsPinyinMatch(pinyin, token)) return false;
        }
        return true;
    }

    public static String normalize(String input) {
        if (input == null || input.isBlank()) return "";
        String lower = input.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lower.length());
        boolean lastSpace = true;

        for (int index = 0; index < lower.length();) {
            int c = lower.codePointAt(index);
            index += Character.charCount(c);
            if (isSearchChar(c)) {
                // Treat supplementary Unicode letters (including CJK Extension B)
                // as a single searchable code point, not two discarded surrogates.
                builder.appendCodePoint(c);
                lastSpace = false;
            } else if (!lastSpace) {
                builder.append(' ');
                lastSpace = true;
            }
        }

        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == ' ') {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    public static PreparedSearch prepare(String text) {
        return new PreparedSearch(text);
    }

    public static String pinyinSearchData(String text) {
        return preparePinyin(text).searchData;
    }

    public static PinyinSnapshot pinyinSnapshot(String text) {
        PinyinData data = preparePinyin(text);
        return new PinyinSnapshot(
                data.rawLower,
                data.full,
                data.initials,
                data.syllables.clone(),
                data.searchData
        );
    }

    public record PinyinSnapshot(
            String rawLower,
            String full,
            String initials,
            String[] syllables,
            String searchData
    ) {
        public PinyinSnapshot {
            syllables = syllables == null ? new String[0] : syllables.clone();
        }

        @Override
        public String[] syllables() {
            return syllables.clone();
        }
    }

    public static final class PreparedSearch {
        private final String rawLower;
        private final String normalizedText;
        private final PinyinData pinyin;

        private PreparedSearch(String text) {
            String raw = text == null ? "" : text;
            this.rawLower = raw.toLowerCase(Locale.ROOT);
            this.normalizedText = normalize(raw);
            this.pinyin = preparePinyin(raw);
        }

        public boolean matches(String query) {
            String[] tokens = queryTokens(query);
            if (tokens.length == 0) return true;
            if (normalizedText.isBlank()) return false;
            for (String token : tokens) {
                if (normalizedText.contains(token)) continue;
                if (failsPinyinMatch(pinyin, token)) return false;
            }
            return true;
        }

        public int matchRank(String query) {
            if (query == null || query.isBlank()) return 0;
            String lowerQuery = query.toLowerCase(Locale.ROOT).trim();
            if (rawLower.equals(lowerQuery)) return 0;
            if (rawLower.startsWith(lowerQuery)) return 1;
            if (!pinyin.full.isEmpty() && pinyin.full.startsWith(lowerQuery)) return 2;
            if (!pinyin.initials.isEmpty() && pinyin.initials.startsWith(lowerQuery)) return 3;
            // The rank contract must agree with tokenized, normalized and pinyin matching.
            return matches(query) ? 4 : -1;
        }
    }

    private static String[] queryTokens(String query) {
        String normalized = normalize(query);
        return normalized.isBlank() ? new String[0] : normalized.split(" +");
    }

    private static PinyinData preparePinyin(String text) {
        if (text == null || text.isEmpty()) return EMPTY_PINYIN;
        String rawLower = text.toLowerCase(Locale.ROOT);
        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        StringBuilder spaced = new StringBuilder();
        List<String> syllables = new ArrayList<>(Math.min(text.length(), 32));

        for (char c : text.toCharArray()) {
            if (!Pinyin.isChinese(c)) continue;
            String pinyin = Pinyin.toPinyin(c);
            if (pinyin == null || pinyin.isEmpty()) continue;
            String lower = pinyin.toLowerCase(Locale.ROOT);
            full.append(lower);
            initials.append(lower.charAt(0));
            if (!spaced.isEmpty()) spaced.append(' ');
            spaced.append(lower);
            syllables.add(lower);
        }

        if (syllables.isEmpty()) return new PinyinData(rawLower, "", "", new String[0], "");
        String fullValue = full.toString();
        String initialsValue = initials.toString();
        return new PinyinData(
                rawLower,
                fullValue,
                initialsValue,
                syllables.toArray(String[]::new),
                fullValue + " " + initialsValue + " " + spaced
        );
    }

    private static boolean isSearchChar(int c) {
        return Character.isLetterOrDigit(c)
                || c == '_'
                || c == '-'
                || c == '.'
                || c == ':'
                || c == '@'
                || c == '#'
                || (c >= '一' && c <= '鿿');
    }

    private static boolean failsPinyinMatch(PinyinData data, String query) {
        if (data == null || query == null || query.isBlank()) return true;
        String lowerQuery = query.toLowerCase(Locale.ROOT).trim();
        if (lowerQuery.isEmpty()) return true;
        if (!data.rawLower.isEmpty() && data.rawLower.contains(lowerQuery)) return false;

        String compact = compactLatin(lowerQuery);
        if (compact.isEmpty() || data.syllables.length == 0) return true;
        if (data.full.contains(compact) || data.initials.contains(compact)) return false;
        return failsHybridMatch(data.syllables, compact);
    }

    private static String compactLatin(String input) {
        StringBuilder out = null;
        for (int index = 0; index < input.length(); index++) {
            char c = input.charAt(index);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                if (out != null) out.append(c);
                continue;
            }
            if (out == null) {
                out = new StringBuilder(input.length());
                out.append(input, 0, index);
            }
        }
        return out == null ? input : out.toString();
    }

    private static boolean failsHybridMatch(String[] syllables, String query) {
        int queryLength = query.length();
        MatchBuffer buffer = MATCH_BUFFER.get();
        buffer.ensureCapacity(queryLength + 1);
        boolean[] current = buffer.current;
        boolean[] next = buffer.next;
        Arrays.fill(current, 0, queryLength + 1, false);
        current[0] = true;

        for (String syllable : syllables) {
            Arrays.fill(next, 0, queryLength + 1, false);
            current[0] = true;
            for (int position = 0; position < queryLength; position++) {
                if (!current[position]) continue;
                int max = Math.min(syllable.length(), queryLength - position);
                for (int length = 1; length <= max; length++) {
                    if (query.charAt(position + length - 1) != syllable.charAt(length - 1)) break;
                    int end = position + length;
                    if (end == queryLength) return false;
                    next[end] = true;
                }
            }
            boolean[] swap = current;
            current = next;
            next = swap;
        }
        return true;
    }

    private record PinyinData(String rawLower, String full, String initials, String[] syllables, String searchData) {
    }

    private static final class MatchBuffer {
        private boolean[] current = new boolean[32];
        private boolean[] next = new boolean[32];

        private void ensureCapacity(int required) {
            if (current.length >= required) return;
            int size = current.length;
            while (size < required) size <<= 1;
            current = new boolean[size];
            next = new boolean[size];
        }
    }
}
