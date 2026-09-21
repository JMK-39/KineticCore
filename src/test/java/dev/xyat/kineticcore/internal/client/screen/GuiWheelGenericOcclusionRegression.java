package dev.xyat.kineticcore.internal.client.screen;

import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;

/** Regression: a topmost button or input owns its area above a scrollable child. */
public final class GuiWheelGenericOcclusionRegression {
    private static int checks;

    private static final class Widget implements GuiEventListener {
        private final boolean hovered;
        private final boolean consumes;
        private int scrollCalls;
        private boolean focused;

        Widget(boolean hovered, boolean consumes) {
            this.hovered = hovered;
            this.consumes = consumes;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return hovered;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            scrollCalls++;
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

    private static void check(boolean success, String reason) {
        checks++;
        if (!success) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        Widget underlying = new Widget(true, true);
        Widget button = new Widget(true, false);
        check(GuiSessionRuntime.routeSelectionListWheel(List.of(underlying, button), 12, 14, 1),
                "upper non-scrolling button must own wheel region");
        check(button.scrollCalls == 1, "upper button did not receive wheel event");
        check(underlying.scrollCalls == 0, "wheel leaked to underlying widget");
        Widget outside = new Widget(false, false);
        check(GuiSessionRuntime.routeSelectionListWheel(List.of(underlying, outside), 12, 14, 1),
                "lower widget must remain scrollable outside upper widget");
        check(underlying.scrollCalls == 1 && outside.scrollCalls == 0,
                "wheel must reach only the hovered widget");
        check(!GuiSessionRuntime.routeSelectionListWheel(List.of(underlying, button), 12, 14, Double.NaN),
                "invalid wheel must not be dispatched");
        check(button.scrollCalls == 1 && underlying.scrollCalls == 1,
                "invalid wheel mutated widget state");
        System.out.println("PASS: " + checks + " generic GUI wheel occlusion regression cases");
    }
}
