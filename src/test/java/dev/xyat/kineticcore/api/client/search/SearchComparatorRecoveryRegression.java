package dev.xyat.kineticcore.api.client.search;

import java.util.Comparator;
import java.util.List;

/** A broken replacement comparator must not poison subsequent search refreshes. */
public final class SearchComparatorRecoveryRegression {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var model = new KineticSearch.Model<>(List.of("z", "a", "m"), (item, query) -> true);
        Comparator<String> good = Comparator.naturalOrder();
        model.setComparator(good);
        model.refresh("");
        check(model.items().equals(List.of("a", "m", "z")), "working comparator sorts correctly");
        model.setComparator((left, right) -> { throw new IllegalArgumentException("bad comparator"); });
        try { model.refresh(""); throw new AssertionError("bad comparator must propagate"); }
        catch (IllegalArgumentException expected) { checks++; }
        check(model.items().equals(List.of("a", "m", "z")), "failed sort preserves previous results");
        model.refresh("");
        check(model.items().equals(List.of("a", "m", "z")), "next refresh must restore the last working comparator");
        model.setComparator(null);
        model.refresh("");
        check(model.items().equals(List.of("z", "a", "m")), "null comparator retains source order");
        System.out.println("PASS: " + checks + " comparator recovery checks");
    }
}
