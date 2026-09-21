package dev.xyat.kineticcore.internal.client.search;

public final class SupplementarySearchRegression {
    public static void main(String[] args) {
        String extendedHan = "\uD842\uDFB7"; // CJK extension B U+20BB7
        check(extendedHan.equals(KineticSearchRuntime.normalize(extendedHan)),
                "supplementary CJK ideographs must survive normalization");
        check(KineticSearchRuntime.match("item " + extendedHan, extendedHan),
                "matching an actual supplementary CJK ideograph must succeed");
        check(!KineticSearchRuntime.match("ordinary item", extendedHan),
                "a supplementary CJK query must not behave like an empty wildcard");
        var prepared = KineticSearchRuntime.prepare("ordinary item");
        check(!prepared.matches(extendedHan),
                "prepared search must agree with direct search for supplementary CJK queries");
        check(prepared.matchRank(extendedHan) == -1,
                "unrelated supplementary CJK queries must be ranked as no match");
        System.out.println("PASS: supplementary Unicode search (5 cases)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
