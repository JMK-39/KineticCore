package dev.xyat.kineticcore.internal.client.gui.widget.scroll;

import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollSettings;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;

import javax.annotation.Nonnull;

import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticScrollFrameRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * Shared smooth-scroll state and grid-scroll controller API.
 */
public final class KineticScroll {
    private KineticScroll() {}

    private static final class ScrollUtil {
        private static final int SCROLLBAR_VISUAL_WIDTH = 4;

        private static int visualScrollbarWidth(int requestedWidth) {
            return Math.min(SCROLLBAR_VISUAL_WIDTH, Math.max(1, requestedWidth));
        }

        private static int visualScrollbarX(int requestedX, int requestedWidth) {
            return requestedX + Math.max(0, requestedWidth - visualScrollbarWidth(requestedWidth));
        }

        private static int calculateThumbHeight(int trackHeight, int visibleItems, int totalItems, int minHeight) {
            if (trackHeight <= 0) return 0;
            if (totalItems <= 0) return trackHeight;
            int minimum = Math.min(trackHeight, Math.max(1, minHeight));
            int calculated = (int) ((double) Math.max(0, visibleItems) / totalItems * trackHeight);
            return Math.min(trackHeight, Math.max(minimum, calculated));
        }

        private static int calculateScrollOffset(
                double pointerY,
                int trackY,
                int trackHeight,
                int thumbHeight,
                int maxOffset
        ) {
            return (int) Math.round(calculateScrollOffsetPrecise(
                    pointerY, trackY, trackHeight, thumbHeight, maxOffset
            ));
        }

        private static double calculateScrollOffsetPrecise(
                double pointerY,
                int trackY,
                int trackHeight,
                int thumbHeight,
                int maxOffset
        ) {
            if (!Double.isFinite(pointerY) || trackHeight <= 0 || maxOffset <= 0) return 0D;
            double relativeY = pointerY - trackY - thumbHeight / 2.0D;
            double scrollableHeight = (double) trackHeight - thumbHeight;
            if (scrollableHeight <= 0D) return 0D;
            return Mth.clamp((relativeY / scrollableHeight) * maxOffset, 0D, maxOffset);
        }

        private static double calculateTrackPointerOffsetPrecise(
                double pointerY,
                int trackY,
                int trackHeight,
                int maxOffset
        ) {
            if (!Double.isFinite(pointerY) || trackHeight <= 0 || maxOffset <= 0) return 0D;
            double relativeY = Mth.clamp(pointerY - trackY, 0D, trackHeight);
            return relativeY / trackHeight * maxOffset;
        }

        private static double verticalOffsetFromThumbStart(
                double thumbStartY,
                int trackY,
                int trackHeight,
                int thumbHeight,
                int maxOffset
        ) {
            int safeThumbHeight = Mth.clamp(thumbHeight, 1, Math.max(1, trackHeight));
            int travel = Math.max(0, trackHeight - safeThumbHeight);
            if (!Double.isFinite(thumbStartY) || maxOffset <= 0 || travel == 0) return 0D;
            double local = Mth.clamp(thumbStartY - trackY, 0D, travel);
            return local / travel * maxOffset;
        }

        private static void renderScrollbar(
                GuiGraphics graphics,
                double mouseX,
                double mouseY,
                int x,
                int y,
                int width,
                int height,
                int thumbHeight,
                int maxOffset,
                double offset,
                boolean dragging
        ) {
            int safeMaxOffset = Math.max(0, maxOffset);
            if (safeMaxOffset <= 0 || height <= 0 || width <= 0) return;
            int safeThumbHeight = Mth.clamp(thumbHeight, 1, height);
            double safeOffset = Mth.clamp(offset, 0D, safeMaxOffset);
            boolean hovered = isHoveringThumb(
                    mouseX,
                    mouseY,
                    x,
                    y,
                    width,
                    height,
                    safeThumbHeight,
                    safeMaxOffset,
                    safeOffset
            );
            int thumbY = y + (int) Math.round(
                    safeOffset / safeMaxOffset * (height - safeThumbHeight)
            );
            int visualWidth = visualScrollbarWidth(width);
            int visualX = visualScrollbarX(x, width);
            KineticTheme.Palette theme = GuiTheme.current();
            graphics.fill(visualX, y, visualX + visualWidth, y + height, theme.scrollTrack());
            graphics.fill(
                    visualX,
                    thumbY,
                    visualX + visualWidth,
                    thumbY + safeThumbHeight,
                    dragging || hovered ? theme.scrollThumbHover() : theme.scrollThumb()
            );
        }

        private static void renderHorizontalScrollbar(
                GuiGraphics graphics,
                double mouseX,
                double mouseY,
                int x,
                int y,
                int width,
                int height,
                int thumbWidth,
                int maxOffset,
                double offset,
                boolean dragging
        ) {
            int safeMaxOffset = Math.max(0, maxOffset);
            if (safeMaxOffset <= 0 || height <= 0 || width <= 0) return;
            int safeThumbWidth = Mth.clamp(thumbWidth, 1, width);
            double safeOffset = Mth.clamp(offset, 0D, safeMaxOffset);
            int thumbX = horizontalThumbStart(x, width, safeThumbWidth, safeMaxOffset, safeOffset);
            boolean hovered = mouseX >= thumbX
                    && mouseX <= thumbX + safeThumbWidth
                    && mouseY >= y
                    && mouseY <= y + height;
            KineticTheme.Palette theme = GuiTheme.current();
            graphics.fill(x, y, x + width, y + height, theme.scrollTrack());
            graphics.fill(
                    thumbX,
                    y,
                    thumbX + safeThumbWidth,
                    y + height,
                    dragging || hovered ? theme.scrollThumbHover() : theme.scrollThumb()
            );
        }

        private static int horizontalThumbStart(
                int x,
                int width,
                int thumbWidth,
                int maxOffset,
                double offset
        ) {
            int safeThumbWidth = Mth.clamp(thumbWidth, 1, Math.max(1, width));
            int travel = Math.max(0, width - safeThumbWidth);
            if (maxOffset <= 0 || travel == 0) return x;
            double safeOffset = Mth.clamp(offset, 0D, maxOffset);
            return x + (int) Math.round(travel * (safeOffset / maxOffset));
        }

        private static double horizontalOffsetFromThumbStart(
                double thumbStartX,
                int trackX,
                int trackWidth,
                int thumbWidth,
                int maxOffset
        ) {
            int safeThumbWidth = Mth.clamp(thumbWidth, 1, Math.max(1, trackWidth));
            int travel = Math.max(0, trackWidth - safeThumbWidth);
            if (!Double.isFinite(thumbStartX) || maxOffset <= 0 || travel == 0) return 0D;
            double local = Mth.clamp(thumbStartX - trackX, 0D, travel);
            return local / travel * maxOffset;
        }

        private static boolean isHoveringThumb(
                double mouseX,
                double mouseY,
                int x,
                int y,
                int width,
                int height,
                int thumbHeight,
                int maxOffset,
                double offset
        ) {
            if (maxOffset <= 0 || height <= 0 || width <= 0) return false;
            double safeOffset = Mth.clamp(offset, 0D, maxOffset);
            int safeThumbHeight = Mth.clamp(thumbHeight, 1, height);
            int thumbY = y + (int) Math.round(
                    safeOffset / maxOffset * (height - safeThumbHeight)
            );
            int visualWidth = visualScrollbarWidth(width);
            int visualX = visualScrollbarX(x, width);
            return mouseX >= visualX
                    && mouseX <= visualX + visualWidth
                    && mouseY >= thumbY
                    && mouseY <= thumbY + safeThumbHeight;
        }
    }

    /**
     * Draws a vertical Kinetic scrollbar from explicit raw scroll state using the active theme.
     * The caller supplies only geometry and state; visual colors remain API-owned.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static void renderScrollbarState(
            GuiGraphics graphics,
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height,
            int thumbHeight,
            int maxOffset,
            double offset,
            boolean dragging
    ) {
        ScrollUtil.renderScrollbar(
                graphics,
                mouseX,
                mouseY,
                x,
                y,
                width,
                height,
                thumbHeight,
                maxOffset,
                offset,
                dragging
        );
    }

    /**
     * Calculates the themed scrollbar thumb height for callers that retain explicit raw scroll state.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static int stateThumbHeight(
            int trackHeight,
            int visibleItems,
            int totalItems,
            int minHeight
    ) {
        return ScrollUtil.calculateThumbHeight(trackHeight, visibleItems, totalItems, minHeight);
    }

    /**
     * Converts a pointer Y coordinate into a clamped integer offset for explicit raw scrollbar state.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static int stateOffsetFromPointer(
            double pointerY,
            int trackY,
            int trackHeight,
            int thumbHeight,
            int maxOffset
    ) {
        return ScrollUtil.calculateScrollOffset(pointerY, trackY, trackHeight, thumbHeight, maxOffset);
    }

    /**
     * Converts a pointer Y coordinate into a clamped precise offset for explicit raw scrollbar state.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static double stateOffsetFromPointerPrecise(
            double pointerY,
            int trackY,
            int trackHeight,
            int thumbHeight,
            int maxOffset
    ) {
        return ScrollUtil.calculateScrollOffsetPrecise(pointerY, trackY, trackHeight, thumbHeight, maxOffset);
    }

    /**
     * Converts a pointer Y coordinate across the full track into a clamped precise raw offset.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static double stateOffsetFromTrackPointerPrecise(
            double pointerY,
            int trackY,
            int trackHeight,
            int maxOffset
    ) {
        return ScrollUtil.calculateTrackPointerOffsetPrecise(pointerY, trackY, trackHeight, maxOffset);
    }

    /**
     * Converts a vertical thumb start into a clamped precise raw scrollbar offset.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static double stateOffsetFromThumbStartPrecise(
            double thumbStartY,
            int trackY,
            int trackHeight,
            int thumbHeight,
            int maxOffset
    ) {
        return ScrollUtil.verticalOffsetFromThumbStart(
                thumbStartY,
                trackY,
                trackHeight,
                thumbHeight,
                maxOffset
        );
    }

    /**
     * Draws a horizontal Kinetic scrollbar from explicit raw scroll state using the active theme.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static void renderHorizontalScrollbarState(
            GuiGraphics graphics,
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height,
            int thumbWidth,
            int maxOffset,
            double offset,
            boolean dragging
    ) {
        ScrollUtil.renderHorizontalScrollbar(
                graphics,
                mouseX,
                mouseY,
                x,
                y,
                width,
                height,
                thumbWidth,
                maxOffset,
                offset,
                dragging
        );
    }

    /**
     * Returns the horizontal thumb start for explicit raw scrollbar state.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static int stateHorizontalThumbStart(
            int trackX,
            int trackWidth,
            int thumbWidth,
            int maxOffset,
            double offset
    ) {
        return ScrollUtil.horizontalThumbStart(
                trackX,
                trackWidth,
                thumbWidth,
                maxOffset,
                offset
        );
    }

    /**
     * Converts a horizontal thumb start into a clamped precise raw scrollbar offset.
     * @deprecated 底层裸滚动条状态工具，无法获得悬停提示与中键跳转；业务请改用 {@link GridScrollController}。
     *             Raw scrollbar helper without hover hint / middle-click jump; use {@link GridScrollController}.
     */
    @Deprecated
    public static double stateHorizontalOffsetFromThumbStartPrecise(
            double thumbStartX,
            int trackX,
            int trackWidth,
            int thumbWidth,
            int maxOffset
    ) {
        return ScrollUtil.horizontalOffsetFromThumbStart(
                thumbStartX,
                trackX,
                trackWidth,
                thumbWidth,
                maxOffset
        );
    }

    /** Animated scroll state shared by Kinetic scrolling controls. */
    public static class State {
        private double current;
        private double target;
        private double max;
        private long lastAnimationNanos = System.nanoTime();
        private boolean initialized;

        /** Updates the animated value with explicit maximum and snap behavior. */
        public double update(double target, double max, boolean immediate) {
            if (!Double.isFinite(target)) return current();
            this.max = validMaximum(max);
            this.target = clamp(target);

            long now = System.nanoTime();
            if (!initialized || immediate) {
                current = this.target;
                initialized = true;
                lastAnimationNanos = now;
                return current;
            }

            advance(now);
            return current;
        }

        /** Follows a logical target with explicit maximum and snap behavior. */
        public double follow(double logicalTarget, double max, boolean immediate) {
            if (!Double.isFinite(logicalTarget)) return current();
            this.max = validMaximum(max);
            target = clamp(target);
            if (!initialized) {
                snap(logicalTarget, this.max);
                return current;
            }

            if (Math.abs(target - logicalTarget) > 1.0E-6D) {
                target = clamp(logicalTarget);
            }

            if (immediate) {
                current = target;
                lastAnimationNanos = System.nanoTime();
                return current;
            }

            advance(System.nanoTime());
            return current;
        }

        /** Follows a logical target smoothly using the supplied maximum. */
        public double follow(double logicalTarget, double max) {
            return follow(logicalTarget, max, false);
        }

        /** Applies one wheel delta to a logical target using an explicit step. */
        public double wheel(
                double logicalTarget,
                double delta,
                double step,
                double max
        ) {
            if (!Double.isFinite(logicalTarget) || !Double.isFinite(delta)
                    || !Double.isFinite(step)) return target;
            this.max = validMaximum(max);
            target = clamp(target);
            if (!initialized) {
                snap(logicalTarget, this.max);
            } else if (Math.abs(target - logicalTarget) > 1.0E-6D) {
                target = clamp(logicalTarget);
            }

            if (delta != 0D) {
                double wheelItems = KineticScrollSettings.wheelItemsPerNotch();
                target = clamp(target - delta * Math.max(0D, step) * wheelItems);
            }

            return target;
        }

        /** Snaps smooth scrolling directly to the supplied clamped value. */
        public void snap(double value, double max) {
            if (!Double.isFinite(value)) return;
            this.max = validMaximum(max);
            this.target = clamp(value);
            this.current = this.target;
            this.initialized = true;
            this.lastAnimationNanos = System.nanoTime();
        }

        /** Returns the current animated scroll value after advancing animation to the current frame time. */
        public double current() {
            if (!initialized) return 0D;
            advance(System.nanoTime());
            return current;
        }

        /** Returns the logical target that the animated scroll value is approaching. */
        public double target() {
            return target;
        }

        private void advance(long now) {
            double deltaSeconds = Math.min(
                    0.05D,
                    Math.max(
                            0D,
                            (now - lastAnimationNanos)
                                    / 1_000_000_000.0D
                    )
            );
            lastAnimationNanos = now;

            target = clamp(target);
            current = clamp(current);

            double difference = target - current;
            if (Math.abs(difference) <= 1.0E-3D) {
                current = target;
                return;
            }

            double factor =
                    1D - Math.exp(-18D * deltaSeconds);

            current = clamp(
                    current + difference * factor
            );
        }

        private double validMaximum(double requested) {
            return Double.isFinite(requested) && requested >= 0D ? requested : max;
        }

        private double clamp(double value) {
            return Math.max(
                    0D,
                    Math.min(value, max)
            );
        }
    }

    /** Kinetic-owned row base for smooth selection lists. Addons extend this instead of vanilla list entries. */
    public abstract static class SmoothEntry<E extends SmoothEntry<E>> extends ObjectSelectionList.Entry<E> {
    }

    /**
     * Smooth-scrolling selection-list base that keeps vanilla list semantics while applying Kinetic scrolling and scrollbar visuals.
     */
    public abstract static class SmoothSelectionList<E extends SmoothEntry<E>>
            extends ObjectSelectionList<E> {
        private static final int SCROLLBAR_WIDTH = 4;

        private final State smoothScrollState = new State();
        private final int kineticListTop;
        private final int kineticListBottom;
        private final int kineticItemHeight;
        private double targetScrollAmount;
        private boolean smoothScrollInitialized;
        private boolean scrollbarDragging;
        private double scrollbarDragGrabOffset;

        /** Creates a new {@code SmoothSelectionList}. */
        protected SmoothSelectionList(
                int width,
                int height,
                int y0,
                int y1,
                int itemHeight
        ) {
            //? if >=1.20.3 {
            /*// 1.20.3 lists take their viewport height and top edge instead of the top and bottom edges.
            super(KineticClientRuntimeImpl.client(), width, y1 - y0, y0, itemHeight);
            *///?} else {
            super(KineticClientRuntimeImpl.client(), width, height, y0, y1, itemHeight);
            //?}
            this.kineticListTop = y0;
            this.kineticListBottom = y1;
            this.kineticItemHeight = Math.max(1, itemHeight);
        }

        @Override
        public void setScrollAmount(double amount) {
            if (!Double.isFinite(amount)) return;
            double max = Math.max(0D, getMaxScroll());
            targetScrollAmount = Mth.clamp(amount, 0D, max);
            if (!smoothScrollInitialized) {
                smoothScrollState.snap(targetScrollAmount, max);
                super.setScrollAmount(targetScrollAmount);
                smoothScrollInitialized = true;
            }
        }

        /** Snaps this scroll widget directly to the supplied scroll amount. */
        public final void snapScrollAmount(double amount) {
            if (!Double.isFinite(amount)) return;
            double max = Math.max(0D, getMaxScroll());
            targetScrollAmount = Mth.clamp(amount, 0D, max);
            smoothScrollState.snap(targetScrollAmount, max);
            super.setScrollAmount(targetScrollAmount);
            smoothScrollInitialized = true;
        }

        /** Returns the logical pixel scroll amount that this list is animating toward. */
        public final double targetScrollAmount() {
            return targetScrollAmount;
        }

        private int listLeft() {
            //? if >=1.20.3 {
            /*return this.getX();
            *///?} else {
            return this.getLeft();
            //?}
        }

        private int scrollbarX() {
            return listLeft() + this.width - 6;
        }

        @Override
        protected int getScrollbarPosition() {
            return listLeft() + this.width + 2;
        }

        private int scrollbarTrackHeight() {
            return Math.max(1, kineticListBottom - kineticListTop);
        }

        private int scrollbarThumbHeight() {
            int trackHeight = scrollbarTrackHeight();
            int contentHeight = Math.max(trackHeight, getMaxPosition());
            return Mth.clamp(
                    (int) Math.round((double) trackHeight * trackHeight / contentHeight),
                    Math.min(20, trackHeight),
                    trackHeight
            );
        }

        private boolean beginScrollbarDrag(double mouseX, double mouseY, int button) {
            if (button != 0 || !Double.isFinite(mouseX) || !Double.isFinite(mouseY)) return false;
            double max = Math.max(0D, getMaxScroll());
            if (max <= 0D) return false;

            int barX = scrollbarX();
            int trackHeight = scrollbarTrackHeight();
            if (mouseX < barX || mouseX > barX + SCROLLBAR_WIDTH
                    || mouseY < kineticListTop || mouseY > kineticListBottom) {
                return false;
            }

            int thumbHeight = scrollbarThumbHeight();
            double currentScroll = Mth.clamp(super.getScrollAmount(), 0D, max);
            double travel = Math.max(0D, trackHeight - thumbHeight);
            double thumbTop = kineticListTop + (travel <= 0D ? 0D : currentScroll / max * travel);

            scrollbarDragging = true;
            if (mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight) {
                scrollbarDragGrabOffset = mouseY - thumbTop;
            } else {
                scrollbarDragGrabOffset = thumbHeight / 2.0D;
                applyScrollbarDrag(mouseY);
            }
            return true;
        }

        private void applyScrollbarDrag(double mouseY) {
            if (!Double.isFinite(mouseY)) return;
            double max = Math.max(0D, getMaxScroll());
            int trackHeight = scrollbarTrackHeight();
            int thumbHeight = scrollbarThumbHeight();
            double travel = Math.max(0D, trackHeight - thumbHeight);
            if (max <= 0D || travel <= 0D) {
                snapScrollAmount(0D);
                return;
            }

            double thumbTop = Mth.clamp(
                    mouseY - kineticListTop - scrollbarDragGrabOffset,
                    0D,
                    travel
            );
            snapScrollAmount(thumbTop / travel * max);
        }

        private boolean dragScrollbar(double mouseY, int button) {
            if (button != 0 || !scrollbarDragging || !Double.isFinite(mouseY)) return false;
            applyScrollbarDrag(mouseY);
            return true;
        }

        /** Renders the list inside its clipped viewport and draws the API-themed scrollbar when scrolling is available. */
        @Override
        //? if >=1.20.3 {
        /*public void renderWidget(
        *///?} else {
        public void render(
        //?}
                @Nonnull GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            double max = Math.max(0D, getMaxScroll());
            if (max <= 0D) {
                // When the content fits, release the old thumb drag and forget its target.
                scrollbarDragging = false;
                scrollbarDragGrabOffset = 0D;
                snapScrollAmount(0D);
            }
            if (!smoothScrollInitialized) {
                targetScrollAmount = Mth.clamp(
                        super.getScrollAmount(),
                        0D,
                        max
                );
                smoothScrollState.snap(targetScrollAmount, max);
                smoothScrollInitialized = true;
            }

            super.setScrollAmount(
                    smoothScrollState.update(
                            targetScrollAmount,
                            max,
                            scrollbarDragging
                    )
            );
            KineticRenderRuntime.enableScissor(
                    graphics,
                    listLeft(),
                    kineticListTop,
                    listLeft() + this.width,
                    kineticListBottom
            );
            try {
                //? if >=1.20.3 {
                /*super.renderWidget(
                *///?} else {
                super.render(
                //?}
                        graphics,
                        mouseX,
                        mouseY,
                        partialTick
                );
            } finally {
                KineticRenderRuntime.disableScissor(graphics);
            }

            if (max > 0D) {
                int trackHeight = scrollbarTrackHeight();
                int barX = scrollbarX();
                graphics.fill(
                        barX,
                        kineticListTop,
                        barX + SCROLLBAR_WIDTH,
                        kineticListBottom,
                        GuiTheme.current().background()
                );
                ScrollUtil.renderScrollbar(
                        graphics,
                        mouseX,
                        mouseY,
                        barX,
                        kineticListTop,
                        SCROLLBAR_WIDTH,
                        trackHeight,
                        scrollbarThumbHeight(),
                        (int) Math.ceil(max),
                        super.getScrollAmount(),
                        scrollbarDragging
                );
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)) return false;
            if (beginScrollbarDrag(mouseX, mouseY, button)) return true;

            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (handled && button == 0) {
                double max = Math.max(0D, getMaxScroll());
                targetScrollAmount = Mth.clamp(super.getScrollAmount(), 0D, max);
                smoothScrollState.snap(targetScrollAmount, max);
                super.setScrollAmount(targetScrollAmount);
                smoothScrollInitialized = true;
            }
            return handled;
        }

        //? if >=1.20.2 {
        /*@Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        *///?} else {
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        //?}
            if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)
                    || !Double.isFinite(delta) || delta == 0D
                    || !this.isMouseOver(mouseX, mouseY)) return false;
            double max = Math.max(0D, getMaxScroll());
            if (max <= 0D) return false;
            if (!smoothScrollInitialized) {
                targetScrollAmount = Mth.clamp(super.getScrollAmount(), 0D, max);
                smoothScrollState.snap(targetScrollAmount, max);
                smoothScrollInitialized = true;
            }
            targetScrollAmount = Mth.clamp(
                    targetScrollAmount - delta * kineticItemHeight * KineticScrollSettings.wheelItemsPerNotch(),
                    0D,
                    max
            );
            return true;
        }

        @Override
        public boolean mouseDragged(
                double mouseX,
                double mouseY,
                int button,
                double dragX,
                double dragY
        ) {
            if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)
                    || !Double.isFinite(dragX) || !Double.isFinite(dragY)) return false;
            if (dragScrollbar(mouseY, button)) return true;

            boolean handled = super.mouseDragged(
                    mouseX,
                    mouseY,
                    button,
                    dragX,
                    dragY
            );
            if (handled && button == 0) {
                double max = Math.max(0D, getMaxScroll());
                targetScrollAmount = Mth.clamp(super.getScrollAmount(), 0D, max);
                smoothScrollState.snap(targetScrollAmount, max);
                super.setScrollAmount(targetScrollAmount);
                smoothScrollInitialized = true;
            }
            return handled;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0 && scrollbarDragging) {
                scrollbarDragging = false;
                scrollbarDragGrabOffset = 0D;
                return true;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }
    }

    /** Controls logical offset, dragging, animation, and themed rendering for grid-style scrolling. */
    public static class GridScrollController {
        private static final long HOVER_HINT_DELAY_NANOS = 500_000_000L;
        private static final long SELECTION_FLASH_DELAY_NANOS = 200_000_000L;
        private static final long SELECTION_FLASH_ON_NANOS = 250_000_000L;
        private static final long SELECTION_FLASH_OFF_NANOS = 250_000_000L;
        private static final int SELECTION_FLASH_COUNT = 2;

        private int offset;
        private double currentOffset;
        private double targetOffset;
        private long lastAnimationNanos = System.nanoTime();
        private int maxOffset;
        private int totalItems;
        private int visibleItems;
        private boolean dragging;
        private int dragGrabOffset;
        private long thumbHoverStartedAtNanos;
        private int flashingSelectionIndex = -1;
        private long selectionFlashStartedAtNanos;
        private IntSupplier boundSelection;
        private IntUnaryOperator boundTargetOffset;
        private boolean ownerHandlesHint;
        private final FrameHook frameHook = new FrameHook();

        /**
         * 绑定“当前选中项”，之后只要在 Kinetic Screen 中调用 {@link #render}/{@link #renderHorizontal}，
         * 悬停 0.5 秒提示与中键跳转回选中项均由 Screen 自动处理；行绘制时调用
         * {@link #renderSelectionFlash} 即可获得两次反色闪烁。目标偏移默认让选中项居中。
         * Binds the selected index so the owning Kinetic screen auto-handles the hover hint
         * and middle-click jump; the default target offset centres the selection.
         */
        public GridScrollController bindSelection(IntSupplier selectedIndex) {
            return bindSelection(selectedIndex, null);
        }

        /**
         * 同上，但由调用方把选中索引换算为滚动偏移（网格用 {@code i -> i / columns - visibleRows / 2}）。
         * Same as above with a caller-supplied index-to-offset mapping (e.g. grids).
         */
        public GridScrollController bindSelection(IntSupplier selectedIndex, IntUnaryOperator targetOffset) {
            this.boundSelection = selectedIndex;
            this.boundTargetOffset = targetOffset;
            return this;
        }

        /** 解除选中项绑定 / Clears the selection binding. */
        public GridScrollController unbindSelection() {
            this.boundSelection = null;
            this.boundTargetOffset = null;
            return this;
        }

        /**
         * 若该索引正处于闪烁的反色阶段，则以主题反色覆盖该行/格。
         * Draws the inverse flash over one row/cell while it is in an inverted phase.
         */
        public boolean renderSelectionFlash(GuiGraphics graphics, int selectionIndex, int x, int y, int width, int height) {
            if (graphics == null || !isSelectionFlashInverted(selectionIndex)) return false;
            GuiTheme.selectionFlash(graphics, x, y, width, height);
            return true;
        }

        /** Updates the logical item count and visible-row count used by this grid controller. */
        public void update(int totalItems, int visibleItems) {
            this.totalItems = Math.max(0, totalItems);
            this.visibleItems = Math.max(1, visibleItems);
            this.maxOffset = Math.max(
                    0,
                    this.totalItems - this.visibleItems
            );
            if (this.maxOffset == 0) {
                dragging = false;
                dragGrabOffset = 0;
            }
            advanceAnimation();
            currentOffset = clamp(currentOffset);
            targetOffset = clamp(targetOffset);
            offset = clamp((int) Math.round(targetOffset));
        }

        /** Updates an explicitly supplied maximum offset while retaining total/visible counts for thumb sizing. */
        public void updateRange(
                int maxOffset,
                int totalItems,
                int visibleItems
        ) {
            this.totalItems = Math.max(0, totalItems);
            this.visibleItems = Math.max(1, visibleItems);
            this.maxOffset = Math.max(0, maxOffset);
            if (this.maxOffset == 0) {
                dragging = false;
                dragGrabOffset = 0;
            }
            advanceAnimation();
            currentOffset = clamp(currentOffset);
            targetOffset = clamp(targetOffset);
            offset = clamp((int) Math.round(targetOffset));
        }

        /** Returns the nearest logical item offset after advancing smooth animation. */
        public int offset() {
            advanceAnimation();
            return offset;
        }

        /** Returns the current fractional logical offset after advancing smooth animation. */
        public double smoothOffset() {
            advanceAnimation();
            return currentOffset;
        }

        /** Returns the first fully addressed item index represented by the current smooth offset. */
        public int smoothIndexOffset() {
            advanceAnimation();
            return clamp((int) Math.floor(currentOffset + 1.0E-6D));
        }

        /** Returns only the fractional part of the current smooth logical offset. */
        public double fractionalOffset() {
            advanceAnimation();
            return currentOffset - Math.floor(currentOffset);
        }

        /** Converts the fractional offset into a pixel shift for one item/row size. */
        public int visualShift(int unitPixels) {
            return (int) Math.round(fractionalOffset() * Math.max(0, unitPixels));
        }

        /** Returns the maximum logical offset currently allowed by this controller. */
        public int maxOffset() {
            return maxOffset;
        }

        /** Returns whether the current content range can scroll beyond its initial position. */
        public boolean canScroll() {
            return maxOffset > 0;
        }

        /** Returns the built-in middle-click hint after the scrollbar thumb has been hovered for half a second. */
        public Component hoveredScrollbarTooltip() {
            // 自绘控件查询过提示 => 由控件自己显示，Screen 不再重复显示。
            // A widget that queries the hint displays it itself; the screen must not duplicate it.
            ownerHandlesHint = true;
            if (boundSelection != null && boundSelection.getAsInt() < 0) return null;
            return currentHint();
        }

        private Component currentHint() {
            if (thumbHoverStartedAtNanos == 0L
                    || System.nanoTime() - thumbHoverStartedAtNanos < HOVER_HINT_DELAY_NANOS) return null;
            return KineticI18n.translatable("gui.kineticcore.scrollbar.middle_click_hint");
        }

        /** Returns whether the supplied row or tab is currently in one of the two inverse-color flashes. */
        public boolean isSelectionFlashInverted(int selectionIndex) {
            if (selectionIndex != flashingSelectionIndex || selectionFlashStartedAtNanos == 0L) return false;
            long elapsed = System.nanoTime() - selectionFlashStartedAtNanos - SELECTION_FLASH_DELAY_NANOS;
            long cycleDuration = SELECTION_FLASH_ON_NANOS + SELECTION_FLASH_OFF_NANOS;
            if (elapsed < 0L || elapsed >= cycleDuration * SELECTION_FLASH_COUNT) return false;
            return elapsed % cycleDuration < SELECTION_FLASH_ON_NANOS;
        }

        /**
         * Handles a middle-click on this controller's thumb, jumps to a selected row/tab offset,
         * and schedules two inverse-color flashes for the selected entry.
         */
        public boolean middleClickThumb(
                double mouseX,
                double mouseY,
                int button,
                boolean horizontal,
                int x,
                int y,
                int width,
                int height,
                int minThumbSize,
                int selectedIndex,
                int targetOffset
        ) {
            if (!KineticMouseButtons.isMiddle(button) || !canScroll()
                    || !isOverThumb(mouseX, mouseY, horizontal, x, y, width, height, minThumbSize)) return false;
            jumpToSelection(selectedIndex, targetOffset);
            return true;
        }

        private void jumpToSelection(int selectedIndex, int targetOffset) {
            if (selectedIndex < 0) return;
            setOffset(targetOffset);
            flashingSelectionIndex = selectedIndex;
            selectionFlashStartedAtNanos = System.nanoTime();
        }

        private final class FrameHook implements KineticScrollFrameRuntime.Participant {
            @Override
            public Component pendingHint() {
                // 没有可跳转的选中项时不显示提示 / No hint when there is no selection to jump to.
                return boundSelection == null || ownerHandlesHint || !canScroll() || boundSelection.getAsInt() < 0
                        ? null : currentHint();
            }

            @Override
            public boolean handleMiddleClick() {
                if (boundSelection == null || !canScroll() || thumbHoverStartedAtNanos == 0L) return false;
                int selected = boundSelection.getAsInt();
                if (selected < 0) return false;
                int target = boundTargetOffset != null
                        ? boundTargetOffset.applyAsInt(selected)
                        : selected - visibleItems / 2;
                jumpToSelection(selected, target);
                return true;
            }
        }

        private void registerFrameHook() {
            if (boundSelection != null) KineticScrollFrameRuntime.register(frameHook);
        }

        /** Immediately sets current and target offsets to the supplied clamped logical position. */
        public void setOffset(int offset) {
            int clamped = clamp(offset);
            this.offset = clamped;
            this.currentOffset = clamped;
            this.targetOffset = clamped;
            this.lastAnimationNanos = System.nanoTime();
        }

        /** Animates toward a logical offset (same easing as wheel scrolling). */
        public void animateTo(int offset) {
            advanceAnimation();
            targetOffset = clamp(offset);
            this.offset = clamp((int) Math.round(targetOffset));
        }

        /** Resets this grid scroll controller to its initial position and drag state. */
        public void reset() {
            offset = 0;
            currentOffset = 0D;
            targetOffset = 0D;
            lastAnimationNanos = System.nanoTime();
            dragging = false;
            dragGrabOffset = 0;
        }


        /** Applies one standard logical scroll step per wheel delta. */
        public boolean scroll(double delta) {
            return scroll(delta, 1.0D);
        }

        /** Applies a scroll delta with an explicit logical step size. */
        public boolean scroll(double delta, double step) {
            if (!canScroll() || delta == 0D || !Double.isFinite(delta)
                    || !Double.isFinite(step)) return false;

            advanceAnimation();
            double safeStep = Math.max(0D, step);
            targetOffset = clamp(
                    targetOffset - delta * safeStep * KineticScrollSettings.wheelItemsPerNotch()
            );
            offset = clamp((int) Math.round(targetOffset));
            return true;
        }

        /** Begins vertical scrollbar dragging when the pointer hits the track or padded thumb hit area. */
        public boolean beginDrag(
                double mouseX,
                double mouseY,
                int x,
                int y,
                int width,
                int height,
                int minThumbHeight,
                int hitPadding
        ) {
            if (!canScroll() || !Double.isFinite(mouseX) || !Double.isFinite(mouseY)) return false;
            if (mouseX < x - hitPadding
                    || mouseX > x + width + hitPadding
                    || mouseY < y
                    || mouseY > y + height) return false;

            int currentThumbHeight = thumbHeight(
                    height,
                    minThumbHeight
            );

            int currentThumbTop = thumbTop(
                    y,
                    height,
                    minThumbHeight
            );

            dragging = true;

            if (mouseY >= currentThumbTop
                    && mouseY <= currentThumbTop + currentThumbHeight) {
                dragGrabOffset = (int) Math.round(
                        mouseY - currentThumbTop
                );
            } else {
                dragGrabOffset = currentThumbHeight / 2;
                updateFromMouse(
                        mouseY,
                        y,
                        height,
                        minThumbHeight
                );
            }

            return true;
        }

        /** Updates an active vertical scrollbar drag from the supplied pointer Y coordinate. */
        public boolean drag(double mouseY, int y, int height, int minThumbHeight) {
            if (!dragging || !Double.isFinite(mouseY)) return false;
            updateFromMouse(mouseY, y, height, minThumbHeight);
            return true;
        }

        /** Begins horizontal scrollbar dragging when the pointer hits the track or padded thumb hit area. */
        public boolean beginHorizontalDrag(
                double mouseX,
                double mouseY,
                int x,
                int y,
                int width,
                int height,
                int minThumbWidth,
                int hitPadding
        ) {
            if (!canScroll() || !Double.isFinite(mouseX) || !Double.isFinite(mouseY)) return false;
            if (mouseX < x
                    || mouseX > x + width
                    || mouseY < y - hitPadding
                    || mouseY > y + height + hitPadding) return false;

            int currentThumbWidth = thumbWidth(
                    width,
                    minThumbWidth
            );

            int currentThumbLeft = thumbLeft(
                    x,
                    width,
                    minThumbWidth
            );

            dragging = true;

            if (mouseX >= currentThumbLeft
                    && mouseX <= currentThumbLeft + currentThumbWidth) {
                dragGrabOffset = (int) Math.round(
                        mouseX - currentThumbLeft
                );
            } else {
                dragGrabOffset = currentThumbWidth / 2;
                updateFromMouseHorizontal(
                        mouseX,
                        x,
                        width,
                        minThumbWidth
                );
            }

            return true;
        }

        /** Updates an active horizontal scrollbar drag from the supplied pointer X coordinate. */
        public boolean dragHorizontal(
                double mouseX,
                int x,
                int width,
                int minThumbWidth
        ) {
            if (!dragging || !Double.isFinite(mouseX)) return false;

            updateFromMouseHorizontal(
                    mouseX,
                    x,
                    width,
                    minThumbWidth
            );
            return true;
        }

        /** Ends an active primary-button scrollbar drag and reports whether a drag was released. */
        public boolean release(int button) {
            if (button != 0 || !dragging) return false;
            dragging = false;
            dragGrabOffset = 0;
            return true;
        }

        /** Renders the standard vertical scrollbar for this controller using the active theme. */
        public void render(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                int x,
                int y,
                int width,
                int height,
                int minThumbHeight
        ) {
            if (!canScroll()) {
                updateThumbHover(false);
                return;
            }
            updateThumbHover(isOverThumb(mouseX, mouseY, false, x, y, width, height, minThumbHeight));
            registerFrameHook();
            ScrollUtil.renderScrollbar(
                    graphics,
                    mouseX,
                    mouseY,
                    x,
                    y,
                    width,
                    height,
                    thumbHeight(height, minThumbHeight),
                    maxOffset,
                    smoothOffset(),
                    dragging
            );
        }

        /** Renders the standard horizontal scrollbar for this controller using the active theme. */
        public void renderHorizontal(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                int x,
                int y,
                int width,
                int height,
                int minThumbWidth
        ) {
            if (!canScroll()) {
                updateThumbHover(false);
                return;
            }

            int currentThumbWidth = thumbWidth(width, minThumbWidth);
            int currentThumbLeft = thumbLeft(x, width, minThumbWidth);
            boolean hovered = mouseX >= currentThumbLeft
                    && mouseX <= currentThumbLeft + currentThumbWidth
                    && mouseY >= y
                    && mouseY <= y + height;
            updateThumbHover(hovered);
            registerFrameHook();
            KineticTheme.Palette theme = GuiTheme.current();
            graphics.fill(x, y, x + width, y + height, theme.scrollTrack());
            graphics.fill(
                    currentThumbLeft,
                    y,
                    currentThumbLeft + currentThumbWidth,
                    y + height,
                    dragging || hovered ? theme.scrollThumbHover() : theme.scrollThumb()
            );
        }

        private boolean isOverThumb(
                double mouseX,
                double mouseY,
                boolean horizontal,
                int x,
                int y,
                int width,
                int height,
                int minThumbSize
        ) {
            if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)) return false;
            if (horizontal) {
                int thumbWidth = thumbWidth(width, minThumbSize);
                int thumbLeft = thumbLeft(x, width, minThumbSize);
                return mouseX >= thumbLeft && mouseX <= thumbLeft + thumbWidth
                        && mouseY >= y && mouseY <= y + height;
            }
            int thumbHeight = thumbHeight(height, minThumbSize);
            int thumbTop = thumbTop(y, height, minThumbSize);
            return mouseX >= x && mouseX <= x + width
                    && mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight;
        }

        private void updateThumbHover(boolean hovered) {
            if (!hovered || dragging) {
                thumbHoverStartedAtNanos = 0L;
            } else if (thumbHoverStartedAtNanos == 0L) {
                thumbHoverStartedAtNanos = System.nanoTime();
            }
        }

        private int thumbTop(
                int y,
                int height,
                int minThumbHeight
        ) {
            int currentThumbHeight = thumbHeight(
                    height,
                    minThumbHeight
            );

            int travel = Math.max(
                    0,
                    height - currentThumbHeight
            );

            if (maxOffset <= 0 || travel == 0) {
                return y;
            }

            advanceAnimation();
            return y + (int) Math.round(
                    travel * (currentOffset / maxOffset)
            );
        }

        private int thumbLeft(
                int x,
                int width,
                int minThumbWidth
        ) {
            int currentThumbWidth = thumbWidth(
                    width,
                    minThumbWidth
            );

            int travel = Math.max(
                    0,
                    width - currentThumbWidth
            );

            if (maxOffset <= 0 || travel == 0) {
                return x;
            }

            advanceAnimation();
            return x + (int) Math.round(
                    travel * (currentOffset / maxOffset)
            );
        }

        private int thumbHeight(int height, int minThumbHeight) {
            return ScrollUtil.calculateThumbHeight(
                    height, visibleItems, totalItems, minThumbHeight
            );
        }

        private int thumbWidth(
                int width,
                int minThumbWidth
        ) {
            if (totalItems <= 0) {
                return width;
            }

            int calculated = Math.round(
                    width
                            * Math.min(
                            1f,
                            visibleItems / (float) totalItems
                    )
            );

            return Math.min(
                    Math.max(0, width),
                    Math.max(Math.max(0, minThumbWidth), calculated)
            );
        }

        private void updateFromMouseHorizontal(
                double mouseX,
                int x,
                int width,
                int minThumbWidth
        ) {
            int currentThumbWidth = thumbWidth(
                    width,
                    minThumbWidth
            );

            int travel = Math.max(
                    1,
                    width - currentThumbWidth
            );

            double relative =
                    mouseX - x - dragGrabOffset;

            double ratio =
                    relative / travel;

            ratio = Math.max(
                    0D,
                    Math.min(1D, ratio)
            );

            double next = clamp(ratio * maxOffset);
            currentOffset = next;
            targetOffset = next;
            offset = clamp((int) Math.round(next));
            lastAnimationNanos = System.nanoTime();
        }

        private void updateFromMouse(
                double mouseY,
                int y,
                int height,
                int minThumbHeight
        ) {
            int currentThumbHeight = thumbHeight(
                    height,
                    minThumbHeight
            );

            int travel = Math.max(
                    1,
                    height - currentThumbHeight
            );

            double relative =
                    mouseY - y - dragGrabOffset;

            double ratio =
                    relative / travel;

            ratio = Math.max(
                    0D,
                    Math.min(1D, ratio)
            );

            double next = clamp(ratio * maxOffset);
            currentOffset = next;
            targetOffset = next;
            offset = clamp((int) Math.round(next));
            lastAnimationNanos = System.nanoTime();
        }

        private void advanceAnimation() {
            long now = System.nanoTime();
            double deltaSeconds = Math.min(0.05D, Math.max(0D, (now - lastAnimationNanos) / 1_000_000_000.0D));
            lastAnimationNanos = now;

            targetOffset = clamp(targetOffset);
            if (dragging) {
                currentOffset = targetOffset;
            } else {
                double difference = targetOffset - currentOffset;
                if (Math.abs(difference) <= 1.0E-3D) {
                    currentOffset = targetOffset;
                } else {
                    double factor = 1D - Math.exp(-18D * deltaSeconds);
                    currentOffset += difference * factor;
                }
            }
            currentOffset = clamp(currentOffset);
            offset = clamp((int) Math.round(targetOffset));
        }

        private double clamp(double value) {
            return Math.max(0D, Math.min(value, maxOffset));
        }

        private int clamp(int value) {
            return Math.max(0, Math.min(value, maxOffset));
        }
    }
}
