package dev.xyat.kineticcore.api.client.effect;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

/**
 * Layout preferences for the potion-effect display in inventory screens.
 *
 * <p>The owning feature installs suppliers once with {@link #configure}; renderers then query them every frame, so
 * the answers follow config changes without re-registration. Call on the client thread.
 */
public final class KineticEffectDisplay {
    private static volatile BooleanSupplier leftSide = () -> false;
    private static volatile BooleanSupplier holdTabToExpand = () -> false;
    private static volatile BooleanSupplier potionItemIcon = () -> false;
    private static volatile IntPredicate compactRule = availableSpace -> false;

    private KineticEffectDisplay() {
    }

    /**
     * Installs the preference suppliers. Each {@code null} argument restores its default ({@code false}).
     *
     * @param leftSideSupplier whether effects are drawn on the left of the inventory instead of the right
     * @param holdTabSupplier whether effects stay compact until the player holds Tab
     * @param potionItemIconSupplier whether effects use a potion item icon instead of the effect sprite
     * @param compactPredicate receives the free horizontal space in GUI pixels and returns whether to use the
     *   compact layout
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

    /** Returns whether effects are drawn on the left of the inventory. */
    public static boolean leftSide() {
        return leftSide.getAsBoolean();
    }

    /** Returns whether effects stay compact until the player holds Tab. */
    public static boolean holdTabToExpand() {
        return holdTabToExpand.getAsBoolean();
    }

    /** Returns whether effects use a potion item icon instead of the effect sprite. */
    public static boolean potionItemIcon() {
        return potionItemIcon.getAsBoolean();
    }

    /**
     * Returns whether effects should use the compact layout.
     *
     * @param availableSpace free horizontal space next to the inventory, in GUI pixels
     */
    public static boolean compact(int availableSpace) {
        return compactRule.test(availableSpace);
    }
}
