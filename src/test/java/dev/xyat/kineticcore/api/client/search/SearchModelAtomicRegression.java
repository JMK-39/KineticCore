package dev.xyat.kineticcore.api.client.search;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;

/** Ensure search-model failures do not destroy previously usable results or source data. */
public final class SearchModelAtomicRegression {
    public static void main(String[] args) {
        refreshMatcherFailurePreservesPreviousResults();
        refreshComparatorFailurePreservesPreviousResults();
        setSourceFailurePreservesPreviousSource();
        successfulRefreshStillUpdatesLiveView();
        sourceReplacementRequiresExplicitRefresh();
        System.out.println("PASS: 5 atomic search-model regression cases");
    }

    private static void refreshMatcherFailurePreservesPreviousResults() {
        var model = new KineticSearch.Model<>(List.of("one", "two"), (text, query) -> {
            if (query.equals("error") && text.equals("two")) throw new IllegalStateException("matcher failed");
            return true;
        });
        model.refresh("");
        List<String> view = model.items();
        reject(() -> model.refresh("error"));
        check(view.equals(List.of("one", "two")), "matcher error cleared or partially replaced prior view");
    }

    private static void refreshComparatorFailurePreservesPreviousResults() {
        var model = new KineticSearch.Model<>(List.of("b", "a"), (text, query) -> true);
        model.refresh("");
        model.setComparator((left, right) -> {throw new IllegalStateException("sort failed");});
        reject(() -> model.refresh("query"));
        check(model.items().equals(List.of("b", "a")), "comparator failure corrupted visible results");
    }

    private static void setSourceFailurePreservesPreviousSource() {
        var model = new KineticSearch.Model<>(List.of("old"), (text, query) -> true);
        model.refresh("");
        AbstractCollection<String> partial = new AbstractCollection<>() {
            public int size() { return 2; }
            public Iterator<String> iterator() {
                return new Iterator<>() {
                    int count;
                    public boolean hasNext() { if (count == 1) throw new IllegalStateException("failed source"); return count == 0; }
                    public String next() {count++; return "new";}
                };
            }
        };
        reject(() -> model.setSource(partial));
        model.refresh("");
        check(model.items().equals(List.of("old")), "failed source replacement destroyed original source");
    }

    private static void successfulRefreshStillUpdatesLiveView() {
        var model = new KineticSearch.Model<>(List.of("a", "b"), (text, query) -> text.contains(query));
        model.refresh("");
        List<String> view = model.items();
        model.refresh("b");
        check(view.equals(List.of("b")), "successful refresh should update existing live view");
    }

    private static void sourceReplacementRequiresExplicitRefresh() {
        var model = new KineticSearch.Model<>(List.of("old"), (text, query) -> true);
        model.refresh("");
        model.setSource(List.of("new"));
        check(model.items().equals(List.of("old")), "setSource should not silently refresh view");
        model.refresh("");
        check(model.items().equals(List.of("new")), "new source should take effect after refresh");
    }

    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("expected failure"); }
        catch (IllegalStateException expected) { /* deliberate test failure */ }
    }
    private static void check(boolean condition, String message) {if (!condition) throw new AssertionError(message);}
}
