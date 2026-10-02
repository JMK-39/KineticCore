package dev.xyat.kineticcore.internal.client.gui.widget.tab;

import dev.xyat.kineticcore.api.client.gui.widget.KineticTabStrip;
import dev.xyat.kineticcore.api.client.gui.widget.TabStripItem;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.KineticScroll.GridScrollController;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import java.util.Objects;

/** Variable-width tab strip with pinned leading tabs and horizontal scrolling; created only through {@code KineticWidgets}. */
public final class TabStripWidget extends AbstractWidget implements KineticTabStrip, InternalControl {
    private static final int TAB_HEIGHT = KineticScreen.COMPACT_CONTROL_HEIGHT;
    private static final int TAB_GAP = 4;
    private static final int ARROW_WIDTH = 18;
    private static final int EDGE_PADDING = 2;
    private static final int SCROLLBAR_GAP = 5;
    private static final int SCROLLBAR_HEIGHT = 4;
    private static final int MIN_TAB_WIDTH = 36;
    private static final int MAX_TAB_WIDTH = 126;
    private static final int MIN_THUMB_WIDTH = 24;

    private final FactoryAccess factoryAccess;
    private final Font font;
    private final int pinnedLeadingTabs;
    private final Consumer<Integer> responder;
    private final GridScrollController scroll = new GridScrollController();
    private final StateButton previousButton;
    private final StateButton nextButton;
    private final List<StateButton> tabButtons = new ArrayList<>();
    private final List<Integer> tabWidths = new ArrayList<>();
    private final List<Integer> scrollStarts = new ArrayList<>();
    private List<TabStripItem> tabs = List.of();
    private int selectedIndex = -1;
    private int hoveredTabIndex = -1;
    private int pendingInitialScrollOffset;

    public TabStripWidget(
            FactoryAccess access,
            Font font,
            int x,
            int y,
            int width,
            List<? extends TabStripItem> tabs,
            int pinnedLeadingTabs,
            int selectedIndex,
            int initialScrollOffset,
            Component previousText,
            Component nextText,
            Consumer<Integer> responder
    ) {
        super(x, y, Math.max(1, width), TAB_HEIGHT + SCROLLBAR_GAP + SCROLLBAR_HEIGHT, Component.empty());
        this.factoryAccess = Objects.requireNonNull(access, "access");
        this.font = font;
        this.pinnedLeadingTabs = Math.max(0, pinnedLeadingTabs);
        Component previousLabel = previousText == null ? Component.empty() : previousText;
        Component nextLabel = nextText == null ? Component.empty() : nextText;
        this.responder = responder;
        this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
        this.previousButton = new StateButton(
                factoryAccess, x, y, ARROW_WIDTH, TAB_HEIGHT, previousLabel,
                ignored -> scrollBy(-scrollStep())
        );
        this.nextButton = new StateButton(
                factoryAccess, x, y, ARROW_WIDTH, TAB_HEIGHT, nextLabel,
                ignored -> scrollBy(scrollStep())
        );
        setTabs(tabs);
        setSelectedIndex(selectedIndex);
    }

    @Override
    public void setTabs(List<? extends TabStripItem> nextTabs) {
        tabs = nextTabs == null ? List.of() : List.copyOf(nextTabs);
        tabButtons.clear();
        tabWidths.clear();
        scrollStarts.clear();

        for (int index = 0; index < tabs.size(); index++) {
            TabStripItem tab = tabs.get(index);
            int buttonIndex = index;
            StateButton button = new StateButton(
                    factoryAccess, getX(), getY(), tabWidth(tab), TAB_HEIGHT,
                    displayLabel(tab, index == selectedIndex),
                    ignored -> select(buttonIndex, true)
            );
            tabButtons.add(button);
            tabWidths.add(button.getWidth());
        }

        if (tabs.isEmpty()) selectedIndex = -1;
        else if (selectedIndex < 0 || selectedIndex >= tabs.size()) selectedIndex = 0;

        refreshRange();
        refreshLayout();
        refreshSelection();
    }

    @Override
    public List<TabStripItem> tabs() {
        return List.copyOf(tabs);
    }

    @Override
    public int selectedIndex() {
        return selectedIndex;
    }

    @Override
    public void setSelectedIndex(int index) {
        if (tabs.isEmpty()) {
            selectedIndex = -1;
            refreshSelection();
            return;
        }
        select(Math.max(0, Math.min(tabs.size() - 1, index)), false);
    }

    @Override
    public int scrollOffset() {
        return scroll.offset();
    }

    @Override
    public void setScrollOffset(int offset) {
        scroll.setOffset(offset);
        refreshLayout();
    }

    @Override
    public int maxScrollOffset() {
        return scroll.maxOffset();
    }

    @Override
    public void scrollBy(int pixels) {
        int next = Math.max(0, Math.min(scroll.maxOffset(), scroll.offset() + pixels));
        scroll.setOffset(next);
        refreshLayout();
    }

    @Override
    public void ensureSelectedVisible() {
        int pinned = pinnedCount();
        if (selectedIndex < pinned || selectedIndex < 0 || selectedIndex >= tabs.size()) return;
        int local = selectedIndex - pinned;
        if (local < 0 || local >= scrollStarts.size()) return;
        int start = scrollStarts.get(local);
        int end = start + tabWidths.get(selectedIndex);
        int viewportWidth = scrollViewportWidth();
        int offset = scroll.offset();
        if (start < offset) setScrollOffset(start);
        else if (end > offset + viewportWidth) setScrollOffset(end - viewportWidth);
    }

    @Override
    public int tabAt(double mouseX, double mouseY) {
        if (mouseY < getY() || mouseY >= getY() + TAB_HEIGHT) return -1;
        int pinned = pinnedCount();
        for (int index = 0; index < pinned; index++) {
            if (tabButtons.get(index).isMouseOver(mouseX, mouseY)) return index;
        }
        if (mouseX < scrollViewportLeft() || mouseX >= scrollViewportRight()) return -1;
        for (int index = pinned; index < tabButtons.size(); index++) {
            if (tabButtons.get(index).isMouseOver(mouseX, mouseY)) return index;
        }
        return -1;
    }

    @Override
    public Component hoveredTooltip() {
        Component tab = hoveredTabIndex >= 0 && hoveredTabIndex < tabs.size()
                ? tabs.get(hoveredTabIndex).tooltip()
                : null;
        return tab != null ? tab : scroll.hoveredScrollbarTooltip();
    }

    @Override
    protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshLayout();
        int pinned = pinnedCount();
        for (int index = 0; index < tabButtons.size(); index++) {
            tabButtons.get(index).setInvertedFlash(scroll.isSelectionFlashInverted(index));
        }
        for (int index = 0; index < pinned; index++) {
            tabButtons.get(index).render(graphics, mouseX, mouseY, partialTick);
        }

        previousButton.render(graphics, mouseX, mouseY, partialTick);
        nextButton.render(graphics, mouseX, mouseY, partialTick);

        if (scrollViewportWidth() > 0) {
            KineticRenderRuntime.enableScissor(graphics, scrollViewportLeft(), getY(), scrollViewportRight(), getY() + TAB_HEIGHT);
            try {
                for (int index = pinned; index < tabButtons.size(); index++) {
                    StateButton button = tabButtons.get(index);
                    if (button.getX() + button.getWidth() <= scrollViewportLeft() || button.getX() >= scrollViewportRight()) continue;
                    int clippedMouseX = mouseX >= scrollViewportLeft() && mouseX < scrollViewportRight()
                            ? mouseX
                            : Integer.MIN_VALUE;
                    button.render(graphics, clippedMouseX, mouseY, partialTick);
                }
            } finally {
                KineticRenderRuntime.disableScissor(graphics);
            }
        }

        if (scroll.canScroll()) {
            scroll.renderHorizontal(
                    graphics, mouseX, mouseY,
                    getX(), scrollbarY(), getWidth(), SCROLLBAR_HEIGHT, MIN_THUMB_WIDTH
            );
        }

        hoveredTabIndex = tabAt(mouseX, mouseY);
        KineticControlBridge.setHoverTooltip(this, hoveredTooltip());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (scroll.middleClickThumb(
                mouseX, mouseY, button, true,
                getX(), scrollbarY(), getWidth(), SCROLLBAR_HEIGHT, MIN_THUMB_WIDTH,
                selectedIndex, selectedTabScrollOffset()
        )) {
            refreshLayout();
            return true;
        }
        if (button != 0) return false;
        if (scroll.beginHorizontalDrag(
                mouseX, mouseY, getX(), scrollbarY(), getWidth(), SCROLLBAR_HEIGHT, MIN_THUMB_WIDTH, 2
        )) return true;
        if (previousButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (nextButton.mouseClicked(mouseX, mouseY, button)) return true;
        int tab = tabAt(mouseX, mouseY);
        return tab >= 0 && tabButtons.get(tab).mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scroll.dragHorizontal(mouseX, getX(), getWidth(), MIN_THUMB_WIDTH)) {
            refreshLayout();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return scroll.release(button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
        scroll.scroll(delta, 28D);
        refreshLayout();
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible
                && mouseX >= getX()
                && mouseX < getX() + getWidth()
                && mouseY >= getY()
                && mouseY < getY() + getHeight();
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        if (selectedIndex >= 0 && selectedIndex < tabs.size()) {
            output.add(NarratedElementType.TITLE, displayLabel(tabs.get(selectedIndex), true));
        }
    }

    private void select(int index, boolean notify) {
        if (index < 0 || index >= tabs.size()) return;
        selectedIndex = index;
        refreshSelection();
        ensureSelectedVisible();
        if (notify && responder != null) responder.accept(index);
    }

    private void refreshSelection() {
        for (int index = 0; index < tabButtons.size(); index++) {
            StateButton button = tabButtons.get(index);
            boolean selected = index == selectedIndex;
            button.setSelected(selected);
            button.setMessage(displayLabel(tabs.get(index), selected));
        }
        setMessage(selectedIndex >= 0 && selectedIndex < tabs.size()
                ? displayLabel(tabs.get(selectedIndex), true)
                : Component.empty());
    }

    private void refreshRange() {
        int pinned = pinnedCount();
        scrollStarts.clear();
        int contentWidth = 0;
        for (int index = pinned; index < tabs.size(); index++) {
            scrollStarts.add(contentWidth);
            contentWidth += tabWidths.get(index) + TAB_GAP;
        }
        if (contentWidth > 0) contentWidth -= TAB_GAP;
        int viewportWidth = scrollViewportWidth();
        scroll.updateRange(Math.max(0, contentWidth - viewportWidth), contentWidth, Math.max(1, viewportWidth));
        if (pendingInitialScrollOffset > 0) {
            scroll.setOffset(pendingInitialScrollOffset);
            pendingInitialScrollOffset = 0;
        }
    }

    private void refreshLayout() {
        int pinned = pinnedCount();
        int cursor = getX();
        for (int index = 0; index < pinned; index++) {
            StateButton button = tabButtons.get(index);
            button.setX(cursor);
            button.setY(getY());
            cursor += button.getWidth() + TAB_GAP;
        }

        int previousX = cursor;
        previousButton.setX(previousX);
        previousButton.setY(getY());
        previousButton.active = scroll.offset() > 0;
        previousButton.visible = !tabs.isEmpty();

        nextButton.setX(getX() + getWidth() - ARROW_WIDTH);
        nextButton.setY(getY());
        nextButton.active = scroll.offset() < scroll.maxOffset();
        nextButton.visible = !tabs.isEmpty();

        double offset = scroll.smoothOffset();
        int viewportLeft = scrollViewportLeft();
        int local = 0;
        for (int index = pinned; index < tabButtons.size(); index++) {
            StateButton button = tabButtons.get(index);
            int start = local < scrollStarts.size() ? scrollStarts.get(local) : 0;
            button.setX(viewportLeft + start - (int) Math.round(offset));
            button.setY(getY());
            local++;
        }
    }

    private int pinnedCount() {
        return Math.min(pinnedLeadingTabs, tabs.size());
    }

    private int pinnedWidth() {
        int width = 0;
        int pinned = pinnedCount();
        for (int index = 0; index < pinned; index++) width += tabWidths.get(index) + TAB_GAP;
        return width;
    }

    private int scrollViewportLeft() {
        return getX() + pinnedWidth() + ARROW_WIDTH + TAB_GAP + EDGE_PADDING;
    }

    private int scrollViewportRight() {
        return Math.max(scrollViewportLeft(), getX() + getWidth() - ARROW_WIDTH - TAB_GAP - EDGE_PADDING);
    }

    private int scrollViewportWidth() {
        return Math.max(1, scrollViewportRight() - scrollViewportLeft());
    }

    private int scrollbarY() {
        return getY() + TAB_HEIGHT + SCROLLBAR_GAP;
    }

    private int scrollStep() {
        return Math.max(24, scrollViewportWidth() / 3);
    }

    private int selectedTabScrollOffset() {
        int pinned = pinnedCount();
        if (selectedIndex < pinned || selectedIndex < 0) return scroll.offset();
        int localIndex = selectedIndex - pinned;
        return localIndex < scrollStarts.size() ? scrollStarts.get(localIndex) : scroll.offset();
    }

    private int tabWidth(TabStripItem tab) {
        Component normal = tab == null || tab.label() == null ? Component.empty() : tab.label();
        Component selected = tab == null || tab.selectedLabel() == null ? normal : tab.selectedLabel();
        int textWidth = Math.max(font.width(normal), font.width(selected));
        return Math.max(MIN_TAB_WIDTH, Math.min(MAX_TAB_WIDTH, textWidth + 18));
    }

    private Component displayLabel(TabStripItem tab, boolean selected) {
        if (tab == null) return Component.empty();
        Component normal = tab.label() == null ? Component.empty() : tab.label();
        Component selectedLabel = tab.selectedLabel() == null ? normal : tab.selectedLabel();
        return selected ? selectedLabel : normal;
    }
}
