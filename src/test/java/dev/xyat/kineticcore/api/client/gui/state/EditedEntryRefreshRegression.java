package dev.xyat.kineticcore.api.client.gui.state;

import java.util.List;
import java.util.Comparator;

/** Headless regression: refreshing the same edited entries must not erase their recency. */
public final class EditedEntryRefreshRegression {
    private static final Comparator<String> FALLBACK = Comparator.comparingInt(List.of("old", "new", "latest")::indexOf);
    public static void main(String[] args) {
        EditedEntryTracker<String> standalone = new EditedEntryTracker<>();
        standalone.update("old", true);
        standalone.update("new", true);
        standalone.refresh(List.of("old", "new"), ignored -> true);
        check(standalone.comparator(FALLBACK).compare("new", "old") < 0,
                "tracker loses most-recent-first order after refresh");
        standalone.refresh(List.of("old", "new", "latest"), ignored -> true);
        check(standalone.comparator(FALLBACK).compare("latest", "new") < 0,
                "new edited entries found during refresh must appear first");

        // Invalid refreshes must leave the previous ranking intact.
        try {
            standalone.refresh(null, ignored -> true);
            throw new AssertionError("null source must fail");
        } catch (NullPointerException expected) { }
        check(standalone.isEdited("new"), "invalid refresh must preserve existing state");
        try {
            standalone.refresh(List.of("new"), null);
            throw new AssertionError("null predicate must fail");
        } catch (NullPointerException expected) { }
        check(standalone.isEdited("old"), "invalid predicate must preserve previous edits");
        try {
            standalone.comparator(null);
            throw new AssertionError("null comparator must fail immediately");
        } catch (NullPointerException expected) { }
        System.out.println("PASS: 5 edited-entry refresh regression cases");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
