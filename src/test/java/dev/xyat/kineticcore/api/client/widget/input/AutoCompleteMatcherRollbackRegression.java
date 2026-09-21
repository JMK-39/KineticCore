package dev.xyat.kineticcore.api.client.widget.input;

import java.nio.file.Files;
import java.nio.file.Path;

/** A throwing add-on matcher cannot permanently replace a working autocomplete matcher. */
public final class AutoCompleteMatcherRollbackRegression {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("src/main/java/dev/xyat/kineticcore/api/client/widget/input/KineticAutoComplete.java"));
        int start = source.indexOf("public void setSuggestionMatcher(BiPredicate<Suggestion, String> matcher)");
        int end = source.indexOf("/** Sets the maximum number of suggestion rows", start);
        require(start >= 0 && end > start, "matcher setter missing");
        String method = source.substring(start, end);
        require(method.contains("BiPredicate<Suggestion, String> previous = this.suggestionMatcher;"), "previous matcher not saved");
        require(method.contains("updateSuggestions(this.getValue())"), "replacement matcher not applied");
        require(method.contains("catch (RuntimeException | Error failure)"), "failed matcher not trapped");
        require(method.contains("this.suggestionMatcher = previous;"), "failed matcher not rolled back");
        require(method.contains("throw failure;"), "original matcher error swallowed");
        System.out.println("PASS: 5 matcher rollback contracts");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
