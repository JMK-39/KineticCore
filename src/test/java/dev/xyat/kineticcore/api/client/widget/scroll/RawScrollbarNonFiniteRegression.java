package dev.xyat.kineticcore.api.client.widget.scroll;

/** Low-level scrollbar conversion must never propagate non-finite pointer coordinates to add-ons. */
public final class RawScrollbarNonFiniteRegression {
    private static int checks;
    public static void main(String[] args) {
        finiteZero(KineticScroll.stateOffsetFromPointerPrecise(Double.NaN, 0, 100, 20, 80), "vertical pointer NaN");
        finiteZero(KineticScroll.stateOffsetFromPointerPrecise(Double.POSITIVE_INFINITY, 0, 100, 20, 80), "vertical pointer infinite");
        finiteZero(KineticScroll.stateOffsetFromTrackPointerPrecise(Double.NaN, 0, 100, 80), "track NaN");
        finiteZero(KineticScroll.stateOffsetFromTrackPointerPrecise(Double.NEGATIVE_INFINITY, 0, 100, 80), "track infinite");
        finiteZero(KineticScroll.stateOffsetFromThumbStartPrecise(Double.NaN, 0, 100, 20, 80), "vertical thumb NaN");
        finiteZero(KineticScroll.stateOffsetFromThumbStartPrecise(Double.POSITIVE_INFINITY, 0, 100, 20, 80), "vertical thumb infinite");
        finiteZero(KineticScroll.stateHorizontalOffsetFromThumbStartPrecise(Double.NaN, 0, 100, 20, 80), "horizontal thumb NaN");
        finiteZero(KineticScroll.stateHorizontalOffsetFromThumbStartPrecise(Double.NEGATIVE_INFINITY, 0, 100, 20, 80), "horizontal thumb infinite");
        finiteZero(KineticScroll.stateOffsetFromPointerPrecise(20, 0, -5, 2, 80), "invalid negative track");
        check(KineticScroll.stateOffsetFromPointerPrecise(50, 0, 100, 20, 80) == 40.0, "valid precise offset unchanged");
        check(KineticScroll.stateOffsetFromTrackPointerPrecise(50, 0, 100, 80) == 40.0, "valid track offset unchanged");
        check(KineticScroll.stateOffsetFromThumbStartPrecise(40, 0, 100, 20, 80) == 40.0, "valid thumb offset unchanged");
        check(KineticScroll.stateHorizontalOffsetFromThumbStartPrecise(40, 0, 100, 20, 80) == 40.0, "valid horizontal offset unchanged");
        System.out.println("PASS: " + checks + " raw scrollbar coordinate checks");
    }
    private static void finiteZero(double value, String message) {
        check(Double.isFinite(value) && value == 0.0D, message);
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message + " failed");
        checks++;
    }
}
