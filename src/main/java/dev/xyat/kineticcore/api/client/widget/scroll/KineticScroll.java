package dev.xyat.kineticcore.api.client.widget.scroll;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.util.Mth;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;

public final class KineticScroll {
    private KineticScroll() {}

    private static final class ScrollUtil {
        public static final int SCROLLBAR_VISUAL_WIDTH = 4;
        public static final int DEFAULT_TRACK_COLOR = 0xFF171717;
        public static final int DEFAULT_THUMB_COLOR = 0xFFFF9800;
        public static final int DEFAULT_THUMB_HOVER_COLOR = 0xFFFFD700;

        private static int visualScrollbarWidth(int requestedWidth) {
            return Math.min(SCROLLBAR_VISUAL_WIDTH, Math.max(1, requestedWidth));
        }

        private static int visualScrollbarX(int requestedX, int requestedWidth) {
            return requestedX + Math.max(0, requestedWidth - visualScrollbarWidth(requestedWidth));
        }

        /**
         * 计算滑块高度
         */
        public static int calculateThumbHeight(int trackHeight, int visibleItems, int totalItems, int minHeight) {
            if (totalItems <= 0) return minHeight;
            return Math.max(minHeight, (int) ((float) visibleItems / totalItems * trackHeight));
        }

        /**
         * 根据鼠标位置计算滚动偏移量
         */
        public static int calculateScrollOffset(double mouseY, int trackY, int trackHeight, int thumbHeight, int maxScroll) {
            return (int) Math.round(calculateScrollOffsetPrecise(mouseY, trackY, trackHeight, thumbHeight, maxScroll));
        }

        public static double calculateScrollOffsetPrecise(double mouseY, int trackY, int trackHeight, int thumbHeight, int maxScroll) {
            double relativeY = mouseY - trackY - (thumbHeight / 2.0D);
            double scrollableHeight = trackHeight - thumbHeight;
            if (scrollableHeight <= 0D || maxScroll <= 0) {
                return 0D;
            }
            return Mth.clamp((relativeY / scrollableHeight) * maxScroll, 0D, maxScroll);
        }

        /**
         * Renders the default scrollbar theme.
         */
        public static void renderScrollbar(GuiGraphics g, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, int currentScroll, boolean isDragging) {
            renderScrollbar(g, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, isDragging, DEFAULT_TRACK_COLOR, DEFAULT_THUMB_COLOR, DEFAULT_THUMB_HOVER_COLOR);
        }

        public static void renderScrollbar(GuiGraphics g, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, double currentScroll, boolean isDragging) {
            renderScrollbar(g, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, isDragging, DEFAULT_TRACK_COLOR, DEFAULT_THUMB_COLOR, DEFAULT_THUMB_HOVER_COLOR);
        }

        /**
         * Renders the default scrollbar theme with automatic thumb hover highlighting.
         */
        public static void renderScrollbar(
                GuiGraphics g,
                double mouseX,
                double mouseY,
                int barX,
                int barY,
                int barWidth,
                int trackHeight,
                int thumbHeight,
                int maxScroll,
                int currentScroll,
                boolean isDragging
        ) {
            renderScrollbar(g, mouseX, mouseY, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, (double) currentScroll, isDragging);
        }

        public static void renderScrollbar(
                GuiGraphics g,
                double mouseX,
                double mouseY,
                int barX,
                int barY,
                int barWidth,
                int trackHeight,
                int thumbHeight,
                int maxScroll,
                double currentScroll,
                boolean isDragging
        ) {
            int safeMaxScroll = Math.max(0, maxScroll);
            if (safeMaxScroll <= 0 || trackHeight <= 0 || barWidth <= 0) return;
            int safeThumbHeight = Mth.clamp(thumbHeight, 1, trackHeight);
            double safeCurrentScroll = Math.max(0D, Math.min(currentScroll, safeMaxScroll));
            boolean hovered = isHoveringThumb(
                    mouseX,
                    mouseY,
                    barX,
                    barY,
                    barWidth,
                    trackHeight,
                    safeThumbHeight,
                    safeMaxScroll,
                    safeCurrentScroll
            );
            renderScrollbar(
                    g,
                    barX,
                    barY,
                    barWidth,
                    trackHeight,
                    safeThumbHeight,
                    safeMaxScroll,
                    safeCurrentScroll,
                    isDragging || hovered,
                    DEFAULT_TRACK_COLOR,
                    DEFAULT_THUMB_COLOR,
                    DEFAULT_THUMB_HOVER_COLOR
            );
        }

        /**
         * 渲染可自定义颜色的滚动条。
         */
        public static void renderScrollbar(GuiGraphics g, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, int currentScroll, boolean highlighted, int trackColor, int thumbColor, int highlightColor) {
            renderScrollbar(g, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, (double) currentScroll, highlighted, trackColor, thumbColor, highlightColor);
        }

        public static void renderScrollbar(GuiGraphics g, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, double currentScroll, boolean highlighted, int trackColor, int thumbColor, int highlightColor) {
            if (maxScroll <= 0 || trackHeight <= 0 || barWidth <= 0) return;
            double safeScroll = Math.max(0D, Math.min(currentScroll, maxScroll));
            int safeThumbHeight = Mth.clamp(thumbHeight, 1, trackHeight);
            int thumbY = barY + (int) Math.round(safeScroll / maxScroll * (trackHeight - safeThumbHeight));
            int visualWidth = visualScrollbarWidth(barWidth);
            int visualX = visualScrollbarX(barX, barWidth);
            g.fill(visualX, barY, visualX + visualWidth, barY + trackHeight, trackColor);
            g.fill(visualX, thumbY, visualX + visualWidth, thumbY + safeThumbHeight, highlighted ? highlightColor : thumbColor);
        }

        /**
         * 鼠标是否悬浮在滚动条滑块上。
         */
        public static boolean isHoveringThumb(double mouseX, double mouseY, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, int currentScroll) {
            return isHoveringThumb(mouseX, mouseY, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, (double) currentScroll);
        }

        public static boolean isHoveringThumb(double mouseX, double mouseY, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, double currentScroll) {
            if (maxScroll <= 0 || trackHeight <= 0 || barWidth <= 0) return false;
            double safeScroll = Math.max(0D, Math.min(currentScroll, maxScroll));
            int safeThumbHeight = Mth.clamp(thumbHeight, 1, trackHeight);
            int thumbY = barY + (int) Math.round(safeScroll / maxScroll * (trackHeight - safeThumbHeight));
            int visualWidth = visualScrollbarWidth(barWidth);
            int visualX = visualScrollbarX(barX, barWidth);
            return mouseX >= visualX && mouseX <= visualX + visualWidth && mouseY >= thumbY && mouseY <= thumbY + safeThumbHeight;
        }
    }

    public static class State {
        private double current;
        private double target;
        private double max;
        private long lastAnimationNanos = System.nanoTime();
        private boolean initialized;

        public double update(double target, double max) {
            return update(target, max, false);
        }

        public double update(double target, double max, boolean immediate) {
            this.max = Math.max(0D, max);
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

        public double follow(int logicalTarget, double max) {
            return follow(logicalTarget, max, false);
        }

        public double follow(double logicalTarget, double max) {
            return follow(logicalTarget, max, false);
        }

        public double follow(double logicalTarget, double max, boolean immediate) {
            this.max = Math.max(0D, max);
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

        public double follow(int logicalTarget, double max, boolean immediate) {
            this.max = Math.max(0D, max);
            target = clamp(target);
            if (!initialized) {
                snap(logicalTarget, this.max);
                return current;
            }

            if ((int) Math.round(target) != logicalTarget) {
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

        public double wheel(
                double logicalTarget,
                double delta,
                double step,
                double max
        ) {
            this.max = Math.max(0D, max);
            target = clamp(target);
            if (!initialized) {
                snap(logicalTarget, this.max);
            } else if (Math.abs(target - logicalTarget) > 1.0E-6D) {
                target = clamp(logicalTarget);
            }

            if (delta != 0D) {
                target = clamp(target - delta * Math.max(0D, step));
            }

            return target;
        }

        public int wheel(
                int logicalTarget,
                double delta,
                double step,
                double max
        ) {
            this.max = Math.max(0D, max);
            target = clamp(target);
            if (!initialized) {
                snap(logicalTarget, this.max);
            } else if ((int) Math.round(target) != logicalTarget) {
                target = clamp(logicalTarget);
            }

            if (delta != 0D) {
                target = clamp(target - delta * Math.max(0D, step));
            }

            return targetInt();
        }

        public void snap(double value, double max) {
            this.max = Math.max(0D, max);
            this.target = clamp(value);
            this.current = this.target;
            this.initialized = true;
            this.lastAnimationNanos = System.nanoTime();
        }

        public double current() {
            if (!initialized) return 0D;
            advance(System.nanoTime());
            return current;
        }

        public double target() {
            return target;
        }

        public int targetInt() {
            return (int) Math.round(target);
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

        private double clamp(double value) {
            return Math.max(
                    0D,
                    Math.min(value, max)
            );
        }
    }

    public static int calculateThumbHeight(int trackHeight, int visibleItems, int totalItems, int minHeight) {
        return ScrollUtil.calculateThumbHeight(trackHeight, visibleItems, totalItems, minHeight);
    }

    public static int calculateScrollOffset(double mouseY, int trackY, int trackHeight, int thumbHeight, int maxScroll) {
        return ScrollUtil.calculateScrollOffset(mouseY, trackY, trackHeight, thumbHeight, maxScroll);
    }

    public static double calculateScrollOffsetPrecise(double mouseY, int trackY, int trackHeight, int thumbHeight, int maxScroll) {
        return ScrollUtil.calculateScrollOffsetPrecise(mouseY, trackY, trackHeight, thumbHeight, maxScroll);
    }

    public static void renderScrollbar(GuiGraphics g, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, int currentScroll, boolean isDragging) {
        ScrollUtil.renderScrollbar(g, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, isDragging);
    }

    public static void renderScrollbar(GuiGraphics g, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, double currentScroll, boolean isDragging) {
        ScrollUtil.renderScrollbar(g, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, isDragging);
    }

    public static void renderScrollbar(
            GuiGraphics g,
            double mouseX,
            double mouseY,
            int barX,
            int barY,
            int barWidth,
            int trackHeight,
            int thumbHeight,
            int maxScroll,
            int currentScroll,
            boolean isDragging
    ) {
        ScrollUtil.renderScrollbar(g, mouseX, mouseY, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, isDragging);
    }

    public static void renderScrollbar(
            GuiGraphics g,
            double mouseX,
            double mouseY,
            int barX,
            int barY,
            int barWidth,
            int trackHeight,
            int thumbHeight,
            int maxScroll,
            double currentScroll,
            boolean isDragging
    ) {
        ScrollUtil.renderScrollbar(g, mouseX, mouseY, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, isDragging);
    }

    public static void renderScrollbar(
            GuiGraphics g,
            int barX,
            int barY,
            int barWidth,
            int trackHeight,
            int thumbHeight,
            int maxScroll,
            int currentScroll,
            boolean highlighted,
            int trackColor,
            int thumbColor,
            int highlightColor
    ) {
        ScrollUtil.renderScrollbar(g, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, highlighted, trackColor, thumbColor, highlightColor);
    }

    public static void renderScrollbar(
            GuiGraphics g,
            int barX,
            int barY,
            int barWidth,
            int trackHeight,
            int thumbHeight,
            int maxScroll,
            double currentScroll,
            boolean highlighted,
            int trackColor,
            int thumbColor,
            int highlightColor
    ) {
        ScrollUtil.renderScrollbar(g, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, highlighted, trackColor, thumbColor, highlightColor);
    }

    public static boolean isHoveringThumb(double mouseX, double mouseY, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, int currentScroll) {
        return ScrollUtil.isHoveringThumb(mouseX, mouseY, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll);
    }

    public abstract static class SmoothSelectionList<E extends ObjectSelectionList.Entry<E>>
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

        protected SmoothSelectionList(
                int width,
                int height,
                int y0,
                int y1,
                int itemHeight
        ) {
            this(Minecraft.getInstance(), width, height, y0, y1, itemHeight);
        }

        protected SmoothSelectionList(
                Minecraft minecraft,
                int width,
                int height,
                int y0,
                int y1,
                int itemHeight
        ) {
            super(minecraft, width, height, y0, y1, itemHeight);
            this.kineticListTop = y0;
            this.kineticListBottom = y1;
            this.kineticItemHeight = Math.max(1, itemHeight);
        }

        @Override
        public void setScrollAmount(double amount) {
            double max = Math.max(0D, getMaxScroll());
            targetScrollAmount = Mth.clamp(amount, 0D, max);
            if (!smoothScrollInitialized) {
                smoothScrollState.snap(targetScrollAmount, max);
                super.setScrollAmount(targetScrollAmount);
                smoothScrollInitialized = true;
            }
        }

        public final void snapScrollAmount(double amount) {
            double max = Math.max(0D, getMaxScroll());
            targetScrollAmount = Mth.clamp(amount, 0D, max);
            smoothScrollState.snap(targetScrollAmount, max);
            super.setScrollAmount(targetScrollAmount);
            smoothScrollInitialized = true;
        }

        public final double targetScrollAmount() {
            return targetScrollAmount;
        }

        private int scrollbarX() {
            return this.getLeft() + this.width - 6;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getLeft() + this.width + 2;
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
            if (button != 0) return false;
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
            if (button != 0 || !scrollbarDragging) return false;
            applyScrollbarDrag(mouseY);
            return true;
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            double max = Math.max(0D, getMaxScroll());
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
            graphics.enableScissor(
                    this.getLeft(),
                    kineticListTop,
                    this.getLeft() + this.width,
                    kineticListBottom
            );
            try {
                super.render(
                        graphics,
                        mouseX,
                        mouseY,
                        partialTick
                );
            } finally {
                graphics.disableScissor();
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

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (delta == 0D || !this.isMouseOver(mouseX, mouseY)) return false;
            double max = Math.max(0D, getMaxScroll());
            if (max <= 0D) return false;
            if (!smoothScrollInitialized) {
                targetScrollAmount = Mth.clamp(super.getScrollAmount(), 0D, max);
                smoothScrollState.snap(targetScrollAmount, max);
                smoothScrollInitialized = true;
            }
            targetScrollAmount = Mth.clamp(
                    targetScrollAmount - delta * (kineticItemHeight / 3.0D),
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

    public static class GridScrollController {
        private int offset;
        private double currentOffset;
        private double targetOffset;
        private long lastAnimationNanos = System.nanoTime();
        private int maxOffset;
        private int totalItems;
        private int visibleItems;
        private boolean dragging;
        private int dragGrabOffset;

        public void update(int totalItems, int visibleItems) {
            this.totalItems = Math.max(0, totalItems);
            this.visibleItems = Math.max(1, visibleItems);
            this.maxOffset = Math.max(
                    0,
                    this.totalItems - this.visibleItems
            );
            advanceAnimation();
            currentOffset = clamp(currentOffset);
            targetOffset = clamp(targetOffset);
            offset = clamp((int) Math.round(targetOffset));
        }

        public void updateRange(
                int maxOffset,
                int totalItems,
                int visibleItems
        ) {
            this.totalItems = Math.max(0, totalItems);
            this.visibleItems = Math.max(1, visibleItems);
            this.maxOffset = Math.max(0, maxOffset);
            advanceAnimation();
            currentOffset = clamp(currentOffset);
            targetOffset = clamp(targetOffset);
            offset = clamp((int) Math.round(targetOffset));
        }

        public void restoreOffset(int offset) {
            setOffset(offset);
        }

        public int offset() {
            advanceAnimation();
            return offset;
        }

        public double smoothOffset() {
            advanceAnimation();
            return currentOffset;
        }

        public int indexOffset() {
            return offset();
        }

        public int smoothIndexOffset() {
            advanceAnimation();
            return clamp((int) Math.floor(currentOffset + 1.0E-6D));
        }

        public double fractionalOffset() {
            advanceAnimation();
            return currentOffset - Math.floor(currentOffset);
        }

        public int visualShift(int unitPixels) {
            return (int) Math.round(fractionalOffset() * Math.max(0, unitPixels));
        }

        public int maxOffset() {
            return maxOffset;
        }

        public boolean canScroll() {
            return maxOffset > 0;
        }

        public void setOffset(int offset) {
            int clamped = clamp(offset);
            this.offset = clamped;
            this.currentOffset = clamped;
            this.targetOffset = clamped;
            this.lastAnimationNanos = System.nanoTime();
        }

        public void reset() {
            offset = 0;
            currentOffset = 0D;
            targetOffset = 0D;
            lastAnimationNanos = System.nanoTime();
            dragging = false;
            dragGrabOffset = 0;
        }

        public boolean scroll(double delta) {
            return scroll(delta, 1.0D / 3.0D);
        }

        public boolean scroll(double delta, int step) {
            return scroll(delta, (double) Math.max(1, step));
        }

        public boolean scroll(double delta, double step) {
            if (!canScroll() || delta == 0D) return false;

            advanceAnimation();
            double safeStep = Math.max(0.05D, step);
            targetOffset = clamp(targetOffset - delta * safeStep);
            offset = clamp((int) Math.round(targetOffset));
            return true;
        }

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
            if (!canScroll()) return false;
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

        public boolean drag(double mouseY, int y, int height, int minThumbHeight) {
            if (!dragging) return false;
            updateFromMouse(mouseY, y, height, minThumbHeight);
            return true;
        }

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
            if (!canScroll()) return false;
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

        public boolean dragHorizontal(
                double mouseX,
                int x,
                int width,
                int minThumbWidth
        ) {
            if (!dragging) return false;

            updateFromMouseHorizontal(
                    mouseX,
                    x,
                    width,
                    minThumbWidth
            );
            return true;
        }

        public boolean release(int button) {
            if (button != 0 || !dragging) return false;
            dragging = false;
            dragGrabOffset = 0;
            return true;
        }

        public void render(
                GuiGraphics graphics,
                int x,
                int y,
                int width,
                int height,
                int minThumbHeight
        ) {
            if (canScroll()) {
                int thumbHeight = thumbHeight(height, minThumbHeight);
                ScrollUtil.renderScrollbar(
                        graphics, x, y, width, height,
                        thumbHeight, maxOffset, smoothOffset(), dragging
                );
            }
        }

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
            render(
                    graphics,
                    mouseX,
                    mouseY,
                    x,
                    y,
                    width,
                    height,
                    minThumbHeight,
                    ScrollUtil.DEFAULT_TRACK_COLOR,
                    ScrollUtil.DEFAULT_THUMB_COLOR,
                    ScrollUtil.DEFAULT_THUMB_HOVER_COLOR
            );
        }

        public void render(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                int x,
                int y,
                int width,
                int height,
                int minThumbHeight,
                int trackColor,
                int thumbColor,
                int hoverColor
        ) {
            if (canScroll()) {
                int thumbHeight = thumbHeight(height, minThumbHeight);
                boolean hover = ScrollUtil.isHoveringThumb(
                        mouseX, mouseY, x, y, width, height,
                        thumbHeight, maxOffset, smoothOffset()
                );

                ScrollUtil.renderScrollbar(
                        graphics, x, y, width, height,
                        thumbHeight, maxOffset, smoothOffset(),
                        dragging || hover,
                        trackColor, thumbColor, hoverColor
                );
            }
        }

        public void renderFramed(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                int x,
                int y,
                int width,
                int height,
                int minThumbHeight,
                int borderColor,
                int trackColor,
                int thumbColor,
                int hoverColor
        ) {
            if (!canScroll()) return;

            int currentThumbHeight = thumbHeight(
                    height,
                    minThumbHeight
            );

            int currentThumbTop = thumbTop(
                    y,
                    height,
                    minThumbHeight
            );

            int visualWidth = ScrollUtil.visualScrollbarWidth(width);
            int visualX = ScrollUtil.visualScrollbarX(x, width);
            boolean hovered =
                    mouseX >= visualX
                            && mouseX <= visualX + visualWidth
                            && mouseY >= currentThumbTop
                            && mouseY <= currentThumbTop + currentThumbHeight;

            graphics.fill(
                    visualX,
                    y,
                    visualX + visualWidth,
                    y + height,
                    trackColor
            );

            graphics.fill(
                    visualX,
                    currentThumbTop,
                    visualX + visualWidth,
                    currentThumbTop + currentThumbHeight,
                    dragging || hovered
                            ? hoverColor
                            : thumbColor
            );
        }

        public void renderHorizontal(
                GuiGraphics graphics,
                int x,
                int y,
                int width,
                int height,
                int minThumbWidth
        ) {
            renderHorizontal(
                    graphics,
                    Integer.MIN_VALUE,
                    Integer.MIN_VALUE,
                    x,
                    y,
                    width,
                    height,
                    minThumbWidth
            );
        }

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
            renderHorizontal(
                    graphics,
                    mouseX,
                    mouseY,
                    x,
                    y,
                    width,
                    height,
                    minThumbWidth,
                    ScrollUtil.DEFAULT_TRACK_COLOR,
                    ScrollUtil.DEFAULT_THUMB_COLOR,
                    ScrollUtil.DEFAULT_THUMB_HOVER_COLOR
            );
        }

        public void renderHorizontal(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                int x,
                int y,
                int width,
                int height,
                int minThumbWidth,
                int trackColor,
                int thumbColor,
                int hoverColor
        ) {
            if (!canScroll()) return;

            int currentThumbWidth = thumbWidth(
                    width,
                    minThumbWidth
            );

            int currentThumbLeft = thumbLeft(
                    x,
                    width,
                    minThumbWidth
            );

            boolean hovered =
                    mouseX >= currentThumbLeft
                            && mouseX <= currentThumbLeft + currentThumbWidth
                            && mouseY >= y
                            && mouseY <= y + height;

            graphics.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    trackColor
            );

            graphics.fill(
                    currentThumbLeft,
                    y,
                    currentThumbLeft + currentThumbWidth,
                    y + height,
                    dragging || hovered
                            ? hoverColor
                            : thumbColor
            );
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

            return Math.max(
                    minThumbWidth,
                    Math.min(width, calculated)
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
