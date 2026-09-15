package dev.xyat.kineticcore.api.client.effect;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

public final class KineticEffectDisplay {
    private static volatile BooleanSupplier leftSide = () -> false;
    private static volatile BooleanSupplier holdTabToExpand = () -> false;
    private static volatile BooleanSupplier potionItemIcon = () -> false;
    private static volatile IntPredicate compactRule = availableSpace -> false;

    private KineticEffectDisplay() {
    }

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

    public static boolean leftSide() {
        return leftSide.getAsBoolean();
    }

    public static boolean holdTabToExpand() {
        return holdTabToExpand.getAsBoolean();
    }

    public static boolean potionItemIcon() {
        return potionItemIcon.getAsBoolean();
    }

    public static boolean compact(int availableSpace) {
        return compactRule.test(availableSpace);
    }
}
