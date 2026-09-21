package dev.xyat.kineticcore.api.client.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Literal percent characters are valid translated labels; unresolved format placeholders are not. */
public final class SearchPercentTranslationRegression {
    private static int checks;

    public static void main(String[] args) throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/xyat/kineticcore/api/client/search/KineticSearch.java"));
        check(source.contains("Pattern.compile(\"%(?:\\\\d+\\\\$)?[a-zA-Z]\")"),
                "unresolved-format pattern must require a conversion letter after percent");
        check(source.contains("if (KineticClientRuntime.isEnglishLanguage()) return null;"),
                "English locale must still suppress translated search labels");
        check(source.contains("hasResolvedFormat(translated)"),
                "translated label filtering must reject unresolved format placeholders");
        check(!source.contains("translated.contains(\"%\")"),
                "literal percent signs must not be rejected wholesale");
        System.out.println("PASS: " + checks + " search translation regression assertions");
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
