package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridDensity;

/** Shared pixel geometry for rendered item slots, their padded clip and pointer selection. */
public record ItemGridLayout(int x, int y, int width, int height, ItemGridDensity density) {
    public int columns() {
        return Math.max(1, (Math.max(0, width - density.padding() * 2) + density.gap()) / density.cellPitch());
    }

    public int visibleRows() {
        return Math.max(1, (Math.max(0, height - density.padding() * 2) + density.gap()) / density.cellPitch());
    }

    public int clipLeft() {
        return x + Math.min(width, density.padding());
    }

    public int clipTop() {
        return y + Math.min(height, density.padding());
    }

    public int clipRight() {
        return Math.max(clipLeft(), x + width - density.padding());
    }

    public int clipBottom() {
        return Math.max(clipTop(), y + height - density.padding());
    }

    public int slotX(int index) {
        return clipLeft() + index % columns() * density.cellPitch();
    }

    public int slotY(int index, int firstRow, int pixelShift) {
        return clipTop() + (index / columns() - firstRow) * density.cellPitch() - pixelShift;
    }

    public boolean rowIntersects(int index, int firstRow, int pixelShift) {
        int slotY = slotY(index, firstRow, pixelShift);
        return slotY < clipBottom() && slotY + density.slotSize() > clipTop();
    }

    public int renderEndIndex(int firstRow, int pixelShift, int itemCount) {
        int pitch = density.cellPitch();
        int visiblePixels = clipBottom() - clipTop() + pixelShift;
        int renderedRows = (visiblePixels + pitch - 1) / pitch;
        return Math.min(itemCount, (firstRow + renderedRows) * columns());
    }

    public boolean insideClip(double mouseX, double mouseY) {
        return mouseX >= clipLeft() && mouseX < clipRight() && mouseY >= clipTop() && mouseY < clipBottom();
    }

    public int itemAt(double mouseX, double mouseY, int firstRow, int pixelShift, int itemCount) {
        if (!insideClip(mouseX, mouseY)) return -1;
        double localX = mouseX - clipLeft();
        double localY = mouseY - clipTop() + pixelShift;
        if (localX < 0 || localY < 0) return -1;
        int pitch = density.cellPitch();
        int column = (int) (localX / pitch);
        int row = (int) (localY / pitch);
        if (column >= columns() || localX - column * pitch >= density.slotSize()
                || localY - row * pitch >= density.slotSize()) return -1;
        int index = (firstRow + row) * columns() + column;
        return index >= 0 && index < itemCount ? index : -1;
    }
}
