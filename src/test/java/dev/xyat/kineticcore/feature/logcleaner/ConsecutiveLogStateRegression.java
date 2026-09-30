package dev.xyat.kineticcore.feature.logcleaner;

public final class ConsecutiveLogStateRegression {
    public static void main(String[] args) {
        ConsecutiveLogState<String> state = new ConsecutiveLogState<>();
        check(state.accept("first") == 0, "first passes");
        check(state.accept("first") == -1, "duplicate blocked");
        check(state.accept("first") == -1, "next duplicate blocked");
        check(state.accept("different") == 2, "summary before different");
        check(state.accept("different") == -1, "new run duplicate blocked");
        check(state.drain() == 1, "trailing summary");
        check(state.accept("different") == 0, "drain resets run");
    }

    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
}
