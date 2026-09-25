package dev.xyat.kineticcore.internal.client.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** KineticCore's self-contained, code-point-based Mandarin reading table. */
final class KineticPinyin {
    private static final String DATA_RESOURCE = "/assets/kineticcore/pinyin/han-readings.tsv";
    private static final Map<Integer, String> READINGS = loadReadings();

    private KineticPinyin() {
    }

    static String reading(int codePoint) {
        return READINGS.get(codePoint);
    }

    static int readingCount() {
        return READINGS.size();
    }

    private static Map<Integer, String> loadReadings() {
        Map<Integer, String> readings = new HashMap<>(50_000);
        try (InputStream stream = KineticPinyin.class.getResourceAsStream(DATA_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing built-in Mandarin readings: " + DATA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;

                    int separator = line.indexOf('\t');
                    if (separator <= 0 || separator == line.length() - 1) {
                        throw new IllegalStateException("Malformed Mandarin reading entry: " + line);
                    }
                    int codePoint = Integer.parseInt(line.substring(0, separator), 16);
                    String pinyin = line.substring(separator + 1);
                    if (!Character.isValidCodePoint(codePoint) || !isAsciiSyllable(pinyin)) {
                        throw new IllegalStateException("Invalid Mandarin reading entry: " + line);
                    }
                    if (readings.putIfAbsent(codePoint, pinyin) != null) {
                        throw new IllegalStateException("Duplicate Mandarin reading for U+" + Integer.toHexString(codePoint));
                    }
                }
            }
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        if (readings.isEmpty()) {
            throw new ExceptionInInitializerError("The built-in Mandarin readings table is empty");
        }
        return Map.copyOf(readings);
    }

    private static boolean isAsciiSyllable(String pinyin) {
        for (int index = 0; index < pinyin.length(); index++) {
            char character = pinyin.charAt(index);
            if (character < 'a' || character > 'z') return false;
        }
        return !pinyin.isEmpty();
    }
}
