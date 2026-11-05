package dev.xyat.kineticcore.internal.client.gui.widget.scroll;

//? if <26.1
import net.minecraft.client.renderer.MultiBufferSource;

/** Tests the production smooth and grid scroll state without running the Minecraft client. */
public final class KineticScrollStateRegression {
    private static int checks;
    private static void check(boolean okay, String message) {
        checks++;
        if (!okay) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        KineticScroll.State smooth = new KineticScroll.State();
        smooth.snap(5, 10);
        double previous = smooth.target();
        check(!Double.isNaN(previous) && previous == 5, "initial target");
        smooth.wheel(5, Double.NaN, 2, 10);
        check(smooth.target() == previous, "invalid wheel must preserve previous target");
        smooth.follow(Double.NaN, 10, false);
        check(Double.isFinite(smooth.current()) && smooth.target() == previous, "invalid follow must not poison target");
        smooth.snap(Double.NaN, 10);
        check(Double.isFinite(smooth.current()) && Double.isFinite(smooth.target()), "invalid snap must not poison state");
        smooth.update(3, Double.NaN, true);
        check(Double.isFinite(smooth.current()) && Double.isFinite(smooth.target()), "invalid maximum must not poison state");

        KineticScroll.GridScrollController grid = new KineticScroll.GridScrollController();
        grid.update(40, 4);
        grid.setOffset(10);
        int lastOffset = grid.offset();
        check(!grid.scroll(Double.NaN, 3), "invalid wheel delta must not be consumed");
        check(grid.offset() == lastOffset && Double.isFinite(grid.smoothOffset()), "invalid wheel cannot alter grid state");
        check(!grid.scroll(1, Double.NaN), "invalid step must not be consumed");
        check(grid.offset() == lastOffset, "invalid step must not move list");
        check(grid.beginDrag(2, 15, 0, 0, 6, 120, 12, 0), "begin valid vertical drag");
        grid.update(2, 4);
        check(!grid.drag(40, 0, 120, 12), "range becoming non-scrollable must cancel previous drag");
        check(!grid.release(0), "cancelled drag must not consume subsequent release");
        check(grid.offset() == 0, "range shrink clamps offset to zero");
        grid.updateContent(7 * 44, 298, 44);
        grid.setOffset(Integer.MAX_VALUE);
        check(Math.abs(grid.smoothOffset() * 44 - 10) < 0.001,
                "partial final row is fully revealed without blank space");
        grid.scroll(-100, 1);
        check(Math.abs(grid.smoothOffset() * 44 - 10) < 0.001, "wheel cannot overscroll the exact bottom");
        check(grid.beginDrag(2, 20, 0, 0, 6, 120, 12, 0), "begin exact-range drag");
        grid.drag(1000, 0, 120, 12);
        check(Math.abs(grid.smoothOffset() * 44 - 10) < 0.001, "thumb reaches same exact bottom");
        grid.release(0);
        grid.updateContent(900, 298, 44);
        grid.setOffset(Integer.MAX_VALUE);
        check(Math.abs(grid.smoothOffset() * 44 - 602) < 0.001, "non-integral row range stays pixel exact");
        grid.updateContent(200, 298, 44);
        check(!grid.canScroll() && grid.smoothOffset() == 0, "short content never scrolls into blank space");
        check(KineticScroll.stateThumbHeight(100, 8, 4, 10) == 100,
                "thumb cannot exceed track when visible count exceeds total");
        check(KineticScroll.stateThumbHeight(0, 3, 5, 10) == 0,
                "zero-height scrollbar has no thumb");
        check(KineticScroll.stateThumbHeight(80, 1, 8, 200) == 80,
                "minimum thumb must be capped by track height");
        KineticScroll.GridScrollController horizontal = new KineticScroll.GridScrollController();
        horizontal.updateRange(5, 2, 6);
        horizontal.renderHorizontal(new ThumbGraphics(), 0, 0, 4, 8, 20, 4, 50);
        System.out.println("PASS: " + checks + " scroll state regression checks");
    }

    /**
     * Checks every fill of the horizontal thumb. A named class: an anonymous one takes the parameter names of the
     * Minecraft constructor, which are obfuscated duplicates in the CI build and do not compile there.
     */
    private static final class ThumbGraphics extends net.minecraft.client.gui.GuiGraphics {
        ThumbGraphics() {
            //? if >=26.1 {
            /*super(null, new net.minecraft.client.renderer.state.gui.GuiRenderState(), 0, 0);
            *///?} else {
            super(null, (MultiBufferSource.BufferSource) null);
            //?}
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
            check(x1 >= 4 && x2 <= 24 && x2 >= x1, "horizontal thumb escaped 20-pixel track");
        }
    }
}
