package dev.xyat.kineticcore.api.client.gui;

import dev.xyat.kineticcore.api.client.ScrollUtil;
import net.minecraft.client.gui.GuiGraphics;

public final class GridScrollController {
    private int offset;
    private int maxOffset;
    private int totalItems;
    private int visibleItems;
    private boolean dragging;
    private int dragGrabOffset;

    public void update(int totalItems, int visibleItems) {
        this.totalItems = Math.max(0, totalItems);
        this.visibleItems = Math.max(1, visibleItems);
        this.maxOffset = Math.max(0, this.totalItems - this.visibleItems);
        this.offset = clamp(offset);
    }

    public void updateRange(
            int maxOffset,
            int totalItems,
            int visibleItems
    ) {
        this.totalItems = Math.max(0, totalItems);
        this.visibleItems = Math.max(1, visibleItems);
        this.maxOffset = Math.max(0, maxOffset);
        this.offset = clamp(offset);
    }

    public void restoreOffset(int offset) {
        this.offset = Math.max(0, offset);
    }

    public int offset() {
        return offset;
    }

    public int maxOffset() {
        return maxOffset;
    }

    public boolean canScroll() {
        return maxOffset > 0;
    }

    public void setOffset(int offset) {
        this.offset = clamp(offset);
    }

    public void reset() {
        offset = 0;
        dragging = false;
        dragGrabOffset = 0;
    }

    public boolean scroll(double delta) {
        return scroll(delta, 1);
    }

    public boolean scroll(double delta, int step) {
        if (!canScroll() || delta == 0D) return false;

        int safeStep = Math.max(1, step);
        setOffset(
                offset - (int) Math.signum(delta) * safeStep
        );
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
                    thumbHeight, maxOffset, offset, dragging
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
                    thumbHeight, maxOffset, offset
            );

            ScrollUtil.renderScrollbar(
                    graphics, x, y, width, height,
                    thumbHeight, maxOffset, offset,
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

        boolean hovered =
                mouseX >= x
                        && mouseX <= x + width
                        && mouseY >= currentThumbTop
                        && mouseY <= currentThumbTop + currentThumbHeight;

        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                borderColor
        );

        if (width > 2 && height > 2) {
            graphics.fill(
                    x + 1,
                    y + 1,
                    x + width - 1,
                    y + height - 1,
                    trackColor
            );
        }

        graphics.fill(
                x,
                currentThumbTop,
                x + width,
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

        return y + Math.round(
                travel * (offset / (float) maxOffset)
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

        return x + Math.round(
                travel * (offset / (float) maxOffset)
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

        offset = clamp(
                (int) Math.round(
                        ratio * maxOffset
                )
        );
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

        offset = clamp(
                (int) Math.round(
                        ratio * maxOffset
                )
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(value, maxOffset));
    }
}
