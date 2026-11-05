package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.layout.KineticLayout;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.widget.KineticCustomControl;
import dev.xyat.kineticcore.api.client.gui.widget.KineticEntityPreview;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/** Standard 50 by 50 entity cards, two-pixel gaps and an exact, draggable content scrollbar. */
public final class KineticEntityGrid extends KineticCustomControl {
    /** Green membership/configuration state, independent current editing item and persistent error state. */
    public record Entry(String entityId, Component tooltip, boolean selected, boolean current, boolean error) {
        /** Keeps the existing multi-selection API and its green membership outline. */
        public Entry(String entityId, Component tooltip, boolean selected) {
            this(entityId, tooltip, selected, false, false);
        }
    }

    private static final int SIZE = KineticLayout.ENTITY_CARD_SIZE;
    private static final int GAP = KineticLayout.ENTITY_CARD_GAP;
    private static final int PITCH = SIZE + GAP;
    private final KineticEntityPreview preview = KineticEntityPreview.create();
    private final KineticScrollController scroll = new KineticScrollController();
    private List<Entry> items = List.of();
    private BiConsumer<Integer, MouseInput> onClick;
    private int renderedPixels;
    private int hovered = -1;

    /** Creates a borderless viewport; each individual card uses the core item-slot background. */
    public KineticEntityGrid(int x, int y, int width, int height) {
        super(x, y, width, height);
        setTooltip(() -> hovered >= 0 && hovered < items.size() ? items.get(hovered).tooltip() : null);
    }

    /** Replaces entries without changing the scroll position. */
    public void setItems(Collection<Entry> entries) {
        items = entries == null ? List.of() : List.copyOf(entries);
        updateRange();
    }

    /** Returns the current immutable entries. */
    public List<Entry> items() { return items; }

    /** Installs a click handler; the owner controls single or multiple selection semantics. */
    public void onClick(BiConsumer<Integer, MouseInput> handler) { onClick = handler; }

    /** Number of complete standard-size cards that fit across the viewport. */
    public int columns() { return Math.max(1, (controlWidth() - 6 + GAP) / PITCH); }

    /** Current row offset for restoring the viewport. */
    public int scrollOffset() { return scroll.offset(); }

    /** Restores a row offset, clamped to the exact last content pixel. */
    public void setScrollOffset(int offset) { updateRange(); scroll.setOffset(offset); }

    /** Returns the card under the pointer, excluding gaps and the scrollbar gutter. */
    public int itemAt(double x, double y) {
        if (x < controlX() || x >= controlX() + controlWidth() - 6
                || y < controlY() || y >= controlY() + controlHeight()) return -1;
        double localX = x - controlX();
        double localY = y - controlY() + renderedPixels;
        int col = (int) (localX / PITCH);
        int row = (int) (localY / PITCH);
        if (col >= columns() || localX % PITCH >= SIZE || localY % PITCH >= SIZE) return -1;
        int index = row * columns() + col;
        return index >= 0 && index < items.size() ? index : -1;
    }

    /** The hovered card only: its tooltip may cover the neighbouring cards, never the one it describes. */
    @Override
    protected int[] tooltipBounds(double mouseX, double mouseY) {
        int index = itemAt(mouseX, mouseY);
        if (index < 0) return null;
        int x = controlX() + index % columns() * PITCH;
        int y = controlY() + index / columns() * PITCH - renderedPixels;
        int top = Math.max(controlY(), y), bottom = Math.min(controlY() + controlHeight(), y + SIZE);
        return bottom > top ? new int[]{x, top, SIZE, bottom - top} : null;
    }

    private void updateRange() {
        int rows = (items.size() + columns() - 1) / columns();
        scroll.updateContent(rows == 0 ? 0 : rows * PITCH - GAP, controlHeight(), PITCH);
    }

    @Override
    protected void render(KineticGraphics g, int mouseX, int mouseY, float partialTick) {
        updateRange();
        renderedPixels = (int) Math.round(scroll.smoothOffset() * PITCH);
        hovered = itemAt(mouseX, mouseY);
        int firstRow = renderedPixels / PITCH;
        int lastRow = (renderedPixels + controlHeight() + PITCH - 1) / PITCH;
        g.clipped(controlX(), controlY(), controlX() + controlWidth() - 6, controlY() + controlHeight(), () -> {
            for (int i = firstRow * columns(); i < Math.min(items.size(), lastRow * columns()); i++) {
                int x = controlX() + i % columns() * PITCH;
                int y = controlY() + i / columns() * PITCH - renderedPixels;
                Entry entry = items.get(i);
                KineticTheme.itemSlot(g, x, y, SIZE, false);
                preview.render(g, entry.entityId(), "grid:" + entry.entityId(), x + 1, y + 1, SIZE - 2, SIZE - 2, i == hovered);
                if (entry.error()) KineticTheme.indicatorOutline(g, x, y, SIZE, SIZE, KineticTheme.Indicator.DANGER);
                else if (entry.selected()) KineticTheme.indicatorOutline(g, x, y, SIZE, SIZE, KineticTheme.Indicator.SUCCESS);
                // Keep the semantic red/green outer edge visible during hover and current-item breathing.
                if (entry.current() || i == hovered) KineticTheme.stateOutline(g, x + 1, y + 1, SIZE - 2, SIZE - 2,
                        entry.current(), i == hovered, false);
            }
        });
        scroll.render(g, mouseX, mouseY, controlX() + controlWidth() - 4, controlY(), 4, controlHeight(), 16);
    }

    @Override protected boolean onMouseClick(MouseInput input) {
        if (scroll.beginDrag(input.x(), input.y(), input.button(), controlX() + controlWidth() - 4, controlY(), 4, controlHeight(), 16)) return true;
        int index = itemAt(input.x(), input.y());
        if (index < 0 || onClick == null) return false;
        onClick.accept(index, input);
        return true;
    }
    @Override protected boolean onMouseDrag(MouseDragInput input) { return scroll.drag(input.y(), controlY(), controlHeight(), 16); }
    @Override protected boolean onMouseRelease(MouseInput input) { return scroll.release(input.button()); }
    @Override protected boolean onMouseScroll(ScrollInput input) { return scroll.scroll(input.deltaY()); }
}
