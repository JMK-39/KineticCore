package dev.xyat.kineticcore.api.client.widget.state;
import java.util.List;
import java.util.Comparator;

/** Failed refreshes must not lose previously tracked edits or their ordering. */
public final class EditedEntryAtomicRefreshRegression {
    private static int cases;
    private static void check(boolean condition, String message) {
        cases++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        EditedEntryTracker<String> standalone = new EditedEntryTracker<>();
        standalone.update("old", true);
        standalone.update("recent", true);
        try {
            standalone.refresh(List.of("new", "recent", "old"), entry -> {
                if (entry.equals("recent")) throw new IllegalStateException("scan failed");
                return true;
            });
            throw new AssertionError("predicate failure should propagate");
        } catch (IllegalStateException expected) { }
        check(standalone.isEdited("old") && standalone.isEdited("recent") && !standalone.isEdited("new"),
                "standalone failed refresh changed committed membership");
        check(standalone.comparator(Comparator.naturalOrder()).compare("recent", "old") < 0,
                "standalone failed refresh lost recency order");

        KineticUiState.EditedEntries<String> embedded = new KineticUiState.EditedEntries<>();
        embedded.update("old", true);
        embedded.update("recent", true);
        try {
            embedded.refresh(List.of("new", "recent", "old"), entry -> {
                if (entry.equals("recent")) throw new IllegalStateException("scan failed");
                return true;
            });
            throw new AssertionError("predicate failure should propagate");
        } catch (IllegalStateException expected) { }
        check(embedded.isEdited("old") && embedded.isEdited("recent") && !embedded.isEdited("new"),
                "embedded failed refresh changed committed membership");
        check(embedded.comparator(Comparator.naturalOrder()).compare("recent", "old") < 0,
                "embedded failed refresh lost recency order");
        standalone.refresh(List.of("old", "recent", "new"), ignored -> true);
        embedded.refresh(List.of("old", "recent", "new"), ignored -> true);
        check(standalone.isEdited("new") && embedded.isEdited("new"), "successful refresh after failure must work");
        System.out.println("PASS: " + cases + " atomic edited-entry regression cases");
    }
}
