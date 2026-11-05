package dev.xyat.kineticcore.internal.client.screen;

import java.util.function.BooleanSupplier;

/** An explicitly registered deletion action; keyboard editing never invokes it. */
public final class KineticDeleteAction {
    private BooleanSupplier available = () -> false;
    private Runnable action = () -> {};

    public void bind(BooleanSupplier available, Runnable action) {
        this.available = java.util.Objects.requireNonNull(available);
        this.action = java.util.Objects.requireNonNull(action);
    }

    public boolean handle(int keyCode, boolean editingText) {
        if (keyCode != 261 || editingText || !available.getAsBoolean()) return false;
        action.run();
        return true;
    }
}
