package dev.xyat.kineticcore.api.client.input;

/** Reusable stateful input gesture helpers for client features and addons. */
public final class KineticInputGestures {
    private KineticInputGestures() {
    }

    /** Creates a rising-edge double-tap detector using a tick-based input window. */
    public static DoubleTap doubleTap(int windowTicks) {
        return new DoubleTap(windowTicks);
    }

    /** Stateful rising-edge double-tap detector. Call {@link #update(boolean)} once per client tick. */
    public static final class DoubleTap {
        private final int windowTicks;
        private boolean wasDown;
        private int remainingTicks;

        private DoubleTap(int windowTicks) {
            this.windowTicks = Math.max(1, windowTicks);
        }

        /** Updates the held state and returns true only on the second press inside the configured window. */
        public boolean update(boolean down) {
            if (this.remainingTicks > 0) {
                this.remainingTicks--;
            }

            boolean pressed = down && !this.wasDown;
            this.wasDown = down;
            if (!pressed) return false;

            if (this.remainingTicks > 0) {
                this.remainingTicks = 0;
                return true;
            }

            this.remainingTicks = this.windowTicks;
            return false;
        }

        /** Clears any armed first press and held-state history. */
        public void reset() {
            this.wasDown = false;
            this.remainingTicks = 0;
        }
    }
}
