package dev.xyat.kineticcore.api.client.widget.scroll;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

/** Regression coverage for invalid input and range changes in the shared selection-list control. */
public final class SmoothSelectionListLifecycleRegression {
    private static int checks;

    private static final class ListUnderTest extends KineticScroll.SmoothSelectionList<Row> {
        private int range = 100;
        private ListUnderTest() { super(120, 120, 0, 120, 20); }
        @Override public int getMaxScroll() { return range; }
        void range(int newRange) { this.range = newRange; }
    }

    private static final class Row extends KineticScroll.SmoothEntry<Row> {
        @Override
        public void render(
                GuiGraphics graphics,
                int index,
                int top,
                int left,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick
        ) {
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) {
        ListUnderTest list = new ListUnderTest();
        list.snapScrollAmount(30D);
        check(list.targetScrollAmount() == 30D, "valid initial scroll");
        check(!list.mouseScrolled(30D, 30D, Double.NaN), "NaN wheel rejected");
        check(list.targetScrollAmount() == 30D, "NaN wheel preserves target");
        check(!list.mouseScrolled(30D, 30D, Double.POSITIVE_INFINITY), "infinite wheel rejected");
        check(list.targetScrollAmount() == 30D, "infinite wheel preserves target");
        list.setScrollAmount(Double.NaN);
        check(list.targetScrollAmount() == 30D, "invalid programmatic amount ignored");
        list.snapScrollAmount(Double.NaN);
        check(list.targetScrollAmount() == 30D, "invalid snap ignored");
        check(!list.mouseClicked(Double.NaN, 20D, 0), "invalid pointer cannot begin scrollbar drag");
        check(!list.mouseClicked(116D, Double.NaN, 0), "invalid pointer Y cannot begin scrollbar drag");
        check(list.mouseClicked(116D, 30D, 0), "valid thumb drag begins");
        check(!list.mouseDragged(116D, Double.NaN, 0, 0D, 1D), "invalid drag Y rejected");
        check(Double.isFinite(list.targetScrollAmount()), "invalid drag preserves finite target");
        list.range(0);
        list.render(new GuiGraphics(null, (MultiBufferSource.BufferSource) null), 0, 0, 0F);
        check(list.targetScrollAmount() == 0D, "shrink resets target");
        check(!list.mouseDragged(116D, 30D, 0, 0D, 1D), "shrink releases stale drag");
        check(!list.mouseReleased(116D, 30D, 0), "release after shrink not consumed");
        list.range(100);
        list.render(new GuiGraphics(null, (MultiBufferSource.BufferSource) null), 0, 0, 0F);
        check(list.targetScrollAmount() == 0D, "expanding does not revive old target");
        System.out.println("PASS: " + checks + " smooth selection-list lifecycle checks");
    }
}
