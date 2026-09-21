package dev.xyat.kineticcore.internal.client.screen;

import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;

/** A visually upper widget owns its hovered region even when it declines the wheel action. */
public final class GuiWheelOcclusionRegression {
    private static int assertions;

    private static final class TrackingWidget implements GuiEventListener {
        private final boolean consumes;
        private int calls;
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

    private static void check(boolean condition, String reason) {
        assertions++;
        if (!condition) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        TrackingWidget lower = new TrackingWidget(true);
        TrackingWidget upper = new TrackingWidget(false);
        check(GuiSessionRuntime.routeSelectionListWheel(List.of(lower, upper), 4, 5, 1),
                "top hovered widget should own wheel region at its scroll boundary");
        check(upper.calls == 1, "top hovered widget not called exactly once");
        check(lower.calls == 0, "wheel leaked to underlying widget");
        check(!GuiSessionRuntime.routeSelectionListWheel(List.of(lower, upper), Double.NaN, 5, 1),
                "invalid x coordinate must not reach widgets");
        check(!GuiSessionRuntime.routeSelectionListWheel(List.of(lower, upper), 4, Double.POSITIVE_INFINITY, 1),
                "invalid y coordinate must not reach widgets");
        check(upper.calls == 1 && lower.calls == 0, "invalid position dispatched wheel");
        check(!GuiSessionRuntime.routeSelectionListWheel(List.of(lower, upper), 4, 5, 0),
                "zero wheel should not be consumed");
        System.out.println("PASS: " + assertions + " wheel occlusion and coordinates cases");
    }
}
