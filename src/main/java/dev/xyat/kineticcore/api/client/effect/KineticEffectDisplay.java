package dev.xyat.kineticcore.api.client.effect;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

/** Public Kinetic API facade for effect display. */
public final class KineticEffectDisplay {
    private static volatile BooleanSupplier leftSide = () -> false;
    private static volatile BooleanSupplier holdTabToExpand = () -> false;
    private static volatile BooleanSupplier potionItemIcon = () -> false;
    private static volatile IntPredicate compactRule = availableSpace -> false;

    private KineticEffectDisplay() {
    }

    /**
     * Performs the configure API operation.
     */
    public static void configure(
            BooleanSupplier leftSideSupplier,
            BooleanSupplier holdTabSupplier,
            BooleanSupplier potionItemIconSupplier,
            IntPredicate compactPredicate
    ) {
        dev.xyat.kineticcore.api.runtime.KineticClientRuntime.ensureReady();
        leftSide = leftSideSupplier == null ? () -> false : leftSideSupplier;
        holdTabToExpand = holdTabSupplier == null ? () -> false : holdTabSupplier;
        potionItemIcon = potionItemIconSupplier == null ? () -> false : potionItemIconSupplier;
        compactRule = compactPredicate == null ? availableSpace -> false : compactPredicate;
    }

    /**
     * Performs the left side API operation.
     */
    public static boolean leftSide() {
        return leftSide.getAsBoolean();
    }

    /**
     * Performs the hold tab to expand API operation.
     */
    public static boolean holdTabToExpand() {
        return holdTabToExpand.getAsBoolean();
    }

    /**
     * Performs the potion item icon API operation.
     */
    public static boolean potionItemIcon() {
        return potionItemIcon.getAsBoolean();
    }

    /**
     * Performs the compact API operation.
     */
    public static boolean compact(int availableSpace) {
        return compactRule.test(availableSpace);
    }
}
