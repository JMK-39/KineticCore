package dev.xyat.kineticcore.internal.client.screen;

import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;

/** Most recently registered overlapping widget must receive wheel input first. */
public final class GuiWheelDispatchRegression {
    private static final class TrackingWidget implements GuiEventListener {
        private int calls;
        private final boolean consumes;
        private boolean focused;

        TrackingWidget(boolean consumes) {
            this.consumes = consumes;
        }

        @Override
        public boolean isMouseOver(double x, double y) {
            return true;
        }

        @Override
        public boolean mouseScrolled(double x, double y, double delta) {
            calls++;
            return consumes;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return focused;
        }
    }

    public static void main(String[] args) {
        TrackingWidget lower = new TrackingWidget(true);
        TrackingWidget upper = new TrackingWidget(true);
        check(GuiSessionRuntime.routeSelectionListWheel(List.of(lower, upper), 5, 5, 1), "wheel was not consumed");
        check(upper.calls == 1 && lower.calls == 0, "underlying widget intercepted topmost widget's wheel");
        check(!GuiSessionRuntime.routeSelectionListWheel(List.of(lower, upper), 5, 5, Double.NaN),
                "NaN wheel must be rejected");
        check(!GuiSessionRuntime.routeSelectionListWheel(List.of(lower, upper), 5, 5, Double.POSITIVE_INFINITY),
                "infinite wheel must be rejected");
        check(upper.calls == 1 && lower.calls == 0, "invalid wheel was forwarded to a widget");
        System.out.println("PASS: 5 GUI wheel-routing checks");
    }

    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }
}
