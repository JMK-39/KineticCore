package dev.xyat.kineticcore.api.client.widget.selection;

import dev.xyat.kineticcore.api.client.widget.KineticControl;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Defines the public state surface for Kinetic tab controls.
 * Tab controls are created through a Kinetic screen or {@code KineticWidgets}; addons only manage tab meaning.
 */
public final class KineticTabs {
    private KineticTabs() {
    }

    /** Standard equal-width tab bar managed by the Kinetic widget API. */
    public interface TabBar {
        /** Returns the immutable list of managed Kinetic tab buttons for API host registration. */
        List<StateButton> buttons();

        /** Returns the tab index currently under the pointer, or {@code -1}. */
        int tabAt(double mouseX, double mouseY);

        /** Returns the currently selected tab index, or {@code -1} when no tabs exist. */
        int selectedIndex();

        /** Synchronizes the selected tab without invoking the selection responder. */
        void setSelectedIndex(int index);

        /** Enables or disables one tab while keeping selection rendering API-managed. */
        void setTabActive(int index, boolean active);
    }

    /** Shared Kinetic state, geometry, and input contract for API-owned composite widgets. */
    public interface WidgetControl extends KineticControl {
    }

    /** One variable-width tab description for a {@link ScrollableTabStrip}. */
    public record ScrollableTab(Component label, Component selectedLabel, Component tooltip) {
        /**
         * Creates a new scrollable tab instance.
         */
        public ScrollableTab(Component label, Component tooltip) {
            this(label, label, tooltip);
        }
    }

    /**
     * Compact variable-width tab strip with optional pinned leading tabs, horizontal scrolling,
     * previous/next controls, clipping, smooth wheel scrolling, and a themed scrollbar.
     */
    public interface ScrollableTabStrip extends WidgetControl {

        /** Replaces the complete tab model while preserving API-managed layout and scrolling. */
        void setTabs(List<? extends ScrollableTab> tabs);

        /** Returns the immutable tab model currently displayed by this strip. */
        List<ScrollableTab> tabs();

        /** Returns the currently selected tab index, or {@code -1} when no tabs exist. */
        int selectedIndex();

        /** Synchronizes the selected tab without invoking the selection responder. */
        void setSelectedIndex(int index);

        /** Returns the nearest current horizontal pixel offset of the scrollable portion. */
        int scrollOffset();

        /** Immediately sets the horizontal pixel offset, clamped to the current content range. */
        void setScrollOffset(int offset);

        /** Returns the maximum horizontal pixel offset currently available. */
        int maxScrollOffset();

        /** Scrolls the variable-width portion by a pixel delta. Positive values move content rightward. */
        void scrollBy(int pixels);

        /** Ensures the selected non-pinned tab is fully visible inside the scrolling viewport. */
        void ensureSelectedVisible();

        /** Returns the tab index currently under the pointer, or {@code -1}. */
        int tabAt(double mouseX, double mouseY);

        /** Returns the tooltip for the tab currently under the pointer, or {@code null}. */
        Component hoveredTooltip();
    }

    /** One row description for a {@link ScrollableSelectionList}. */
    public record SelectionItem(
            Component label,
            Component secondaryLabel,
            Component tooltip,
            boolean active,
            boolean marked,
            boolean error
    ) {
    }

    /** One item-backed row description for a {@link ScrollableItemSelectionList}. */
    public record ItemSelectionItem(
            ItemStack stack,
            Component label,
            Component secondaryLabel,
            Component tooltip,
            boolean active,
            boolean marked,
            boolean error
    ) {
    }

    /** Smooth vertical single-selection list with a standard item slot in every row. */
    public interface ScrollableItemSelectionList extends WidgetControl {
        void setItems(List<? extends ItemSelectionItem> items);

        List<ItemSelectionItem> items();

        int selectedIndex();

        void setSelectedIndex(int index);

        void setBounds(int x, int y, int width, int height);

        int scrollOffset();

        void setScrollOffset(int offset);

        int maxScrollOffset();

        void ensureSelectedVisible();

        int itemAt(double mouseX, double mouseY);

        ItemStack stackAt(double mouseX, double mouseY);

        ItemStack hoveredStack();

        Component hoveredTooltip();
    }

    /** API-defined density presets for scrollable item grids. */
    public enum ItemGridDensity {
        COMPACT(18, 1, 0, 1.0F, false),
        STANDARD(24, 5, 4, 1.0F, false),
        LARGE(26, 6, 6, 1.0F, false),
        PREVIEW(60, 2, 0, 2.7F, true);

        private final int slotSize;
        private final int gap;
        private final int padding;
        private final float renderScale;
        private final boolean decorations;

        ItemGridDensity(int slotSize, int gap, int padding, float renderScale, boolean decorations) {
            this.slotSize = slotSize;
            this.gap = gap;
            this.padding = padding;
            this.renderScale = renderScale;
            this.decorations = decorations;
        }

        /**
         * Returns the slot size.
         */
        public int slotSize() {
            return slotSize;
        }

        /**
         * Performs the gap API operation.
         */
        public int gap() {
            return gap;
        }

        /**
         * Performs the padding API operation.
         */
        public int padding() {
            return padding;
        }

        /**
         * Renders scale.
         */
        public float renderScale() {
            return renderScale;
        }

        /**
         * Performs the decorations API operation.
         */
        public boolean decorations() {
            return decorations;
        }

        /**
         * Performs the cell pitch API operation.
         */
        public int cellPitch() {
            return slotSize + gap;
        }
    }

    /** Standard API-owned marker tone for item-grid business state. */
    public enum ItemGridMarker {
        NONE,
        SUCCESS,
        WARNING
    }

    /** One item description for a {@link ScrollableItemGrid}. */
    public record ItemGridItem(
            ItemStack stack,
            Component tooltip,
            boolean active,
            boolean selected,
            boolean error,
            boolean marked,
            ItemGridMarker marker
    ) {
    }

    /** Smooth scrollable item-slot grid with API-defined density and caller-owned selection semantics. */
    public interface ScrollableItemGrid extends WidgetControl {
        void setItems(List<? extends ItemGridItem> items);

        List<ItemGridItem> items();

        void setBounds(int x, int y, int width, int height);

        int scrollOffset();

        void setScrollOffset(int offset);

        int maxScrollOffset();

        int columns();

        int visibleRows();

        int itemAt(double mouseX, double mouseY);

        ItemStack stackAt(double mouseX, double mouseY);

        ItemStack hoveredStack();

        Component hoveredTooltip();
    }

    /** One row description for a {@link ScrollableActionList}. */
    public record ActionItem(
            Component label,
            Component secondaryLabel,
            Component tooltip,
            boolean active,
            boolean marked,
            boolean error,
            Component actionLabel,
            Component actionTooltip,
            boolean actionActive,
            boolean actionError
    ) {
    }

    /** One item-backed row description for a {@link ScrollableItemActionList}. */
    public record ItemActionItem(
            ItemStack stack,
            Component label,
            Component secondaryLabel,
            Component tooltip,
            boolean active,
            boolean marked,
            boolean error,
            Component actionLabel,
            Component actionTooltip,
            boolean actionActive
    ) {
        /**
         * Creates a new item action item instance.
         */
        public ItemActionItem(
                ItemStack stack,
                Component label,
                Component tooltip,
                Component actionLabel,
                Component actionTooltip
        ) {
            this(stack, label, null, tooltip, true, false, false, actionLabel, actionTooltip, true);
        }
    }

    /**
     * Smooth vertical item-backed single-selection list with one standard trailing action button per row.
     */
    public interface ScrollableItemActionList extends WidgetControl {
        void setItems(List<? extends ItemActionItem> items);

        List<ItemActionItem> items();

        int selectedIndex();

        void setSelectedIndex(int index);

        void setBounds(int x, int y, int width, int height);

        int scrollOffset();

        void setScrollOffset(int offset);

        int maxScrollOffset();

        void ensureSelectedVisible();

        int itemAt(double mouseX, double mouseY);

        int actionAt(double mouseX, double mouseY);

        ItemStack stackAt(double mouseX, double mouseY);

        ItemStack hoveredStack();

        Component hoveredTooltip();
    }

    /**
     * Smooth vertical single-selection list with one standard trailing action button per row.
     */
    public interface ScrollableActionList extends WidgetControl {
        void setItems(List<? extends ActionItem> items);

        List<ActionItem> items();

        int selectedIndex();

        void setSelectedIndex(int index);

        void setBounds(int x, int y, int width, int height);

        int scrollOffset();

        void setScrollOffset(int offset);

        int maxScrollOffset();

        void ensureSelectedVisible();

        int itemAt(double mouseX, double mouseY);

        int actionAt(double mouseX, double mouseY);

        Component hoveredTooltip();
    }

    /** One trailing action description for a {@link ScrollableMultiActionList} row. */
    public record RowAction(
            Component label,
            Component tooltip,
            int width,
            boolean active,
            boolean error
    ) {
        /**
         * Creates a new row action instance.
         */
        public RowAction(Component label, Component tooltip, int width) {
            this(label, tooltip, width, true, false);
        }
    }

    /** One row description for a {@link ScrollableMultiActionList}. */
    public record MultiActionItem(
            Component label,
            Component secondaryLabel,
            Component tooltip,
            boolean active,
            boolean marked,
            boolean error,
            List<RowAction> actions
    ) {
        /**
         * Creates a new multi action item instance.
         */
        public MultiActionItem(Component label, Component tooltip, List<RowAction> actions) {
            this(label, null, tooltip, true, false, false, actions);
        }
    }

    /** Identifies one trailing action under the pointer. */
    public record ActionHit(int rowIndex, int actionIndex) {
    }

    /** Smooth vertical single-selection list with any number of standard trailing row actions. */
    public interface ScrollableMultiActionList extends WidgetControl {
        void setItems(List<? extends MultiActionItem> items);

        List<MultiActionItem> items();

        int selectedIndex();

        void setSelectedIndex(int index);

        void setBounds(int x, int y, int width, int height);

        int scrollOffset();

        void setScrollOffset(int offset);

        int maxScrollOffset();

        void ensureSelectedVisible();

        int itemAt(double mouseX, double mouseY);

        ActionHit actionAt(double mouseX, double mouseY);

        Component hoveredTooltip();
    }

    /** One row description for a {@link ScrollableToggleActionList}. */
    public record ToggleActionItem(
            Component label,
            Component secondaryLabel,
            Component tooltip,
            boolean active,
            boolean marked,
            boolean error,
            boolean toggleValue,
            Component toggleOnLabel,
            Component toggleOffLabel,
            Component toggleTooltip,
            boolean toggleActive,
            Component actionLabel,
            Component actionTooltip,
            boolean actionActive,
            boolean actionError
    ) {
        /**
         * Creates a new toggle action item instance.
         */
        public ToggleActionItem(
                Component label,
                Component tooltip,
                boolean toggleValue,
                Component toggleOnLabel,
                Component toggleOffLabel,
                Component toggleTooltip,
                Component actionLabel,
                Component actionTooltip
        ) {
            this(label, null, tooltip, true, false, false, toggleValue,
                    toggleOnLabel, toggleOffLabel, toggleTooltip, true,
                    actionLabel, actionTooltip, true, false);
        }
    }

    /** Smooth vertical single-selection list with one real toggle and one trailing action per row. */
    public interface ScrollableToggleActionList extends WidgetControl {
        void setItems(List<? extends ToggleActionItem> items);

        List<ToggleActionItem> items();

        int selectedIndex();

        void setSelectedIndex(int index);

        boolean toggleValue(int index);

        void setToggleValue(int index, boolean value);

        void setBounds(int x, int y, int width, int height);

        int scrollOffset();

        void setScrollOffset(int offset);

        int maxScrollOffset();

        void ensureSelectedVisible();

        int itemAt(double mouseX, double mouseY);

        int toggleAt(double mouseX, double mouseY);

        int actionAt(double mouseX, double mouseY);

        Component hoveredTooltip();
    }

    /** One toggle column description for a {@link ScrollableMultiToggleList} row. */
    public record RowToggle(
            Component onLabel,
            Component offLabel,
            Component tooltip,
            int width,
            boolean value,
            boolean active,
            boolean error
    ) {
        /**
         * Creates a new row toggle instance.
         */
        public RowToggle(Component onLabel, Component offLabel, Component tooltip, int width, boolean value) {
            this(onLabel, offLabel, tooltip, width, value, true, false);
        }
    }

    /** One row description for a {@link ScrollableMultiToggleList}. */
    public record MultiToggleItem(
            Component label,
            Component secondaryLabel,
            Component tooltip,
            boolean active,
            boolean marked,
            boolean error,
            List<RowToggle> toggles
    ) {
        /**
         * Creates a new multi toggle item instance.
         */
        public MultiToggleItem(Component label, Component tooltip, List<RowToggle> toggles) {
            this(label, null, tooltip, true, false, false, toggles);
        }
    }

    /** Identifies one toggle under the pointer. */
    public record ToggleHit(int rowIndex, int toggleIndex) {
    }

    /** Smooth vertical single-selection list with any number of real toggles per row. */
    public interface ScrollableMultiToggleList extends WidgetControl {
        void setItems(List<? extends MultiToggleItem> items);

        List<MultiToggleItem> items();

        int selectedIndex();

        void setSelectedIndex(int index);

        boolean toggleValue(int rowIndex, int toggleIndex);

        void setToggleValue(int rowIndex, int toggleIndex, boolean value);

        void setBounds(int x, int y, int width, int height);

        int scrollOffset();

        void setScrollOffset(int offset);

        int maxScrollOffset();

        void ensureSelectedVisible();

        int itemAt(double mouseX, double mouseY);

        ToggleHit toggleAt(double mouseX, double mouseY);

        Component hoveredTooltip();
    }

    /** One row description for a {@link ScrollableToggleList}. */
    public record ToggleItem(Component label, Component tooltip, boolean value, boolean active) {
    }

    /**
     * Smooth vertical multi-toggle list using standard Kinetic state buttons, clipping, and scrollbar behavior.
     */
    public interface ScrollableToggleList extends WidgetControl {
        void setItems(List<? extends ToggleItem> items);

        List<ToggleItem> items();

        boolean value(int index);

        void setValue(int index, boolean value);

        void setValues(List<Boolean> values);

        void setBounds(int x, int y, int width, int height);

        int scrollOffset();

        void setScrollOffset(int offset);

        int maxScrollOffset();

        int itemAt(double mouseX, double mouseY);

        Component hoveredTooltip();
    }

    /**
     * Smooth vertical single-selection list using standard Kinetic state buttons, clipping, and scrollbar behavior.
     */
    public interface ScrollableSelectionList extends WidgetControl {

        /** Replaces the complete row model while preserving API-managed scrolling and selection rendering. */
        void setItems(List<? extends SelectionItem> items);

        /** Returns the immutable row model currently displayed by this list. */
        List<SelectionItem> items();

        /** Returns the selected row index, or {@code -1} when no row is selected. */
        int selectedIndex();

        /** Synchronizes the selected row without invoking the selection responder. */
        void setSelectedIndex(int index);

        /** Repositions and resizes the complete list viewport. */
        void setBounds(int x, int y, int width, int height);

        /** Returns the nearest current logical row offset. */
        int scrollOffset();

        /** Immediately sets the logical row offset, clamped to the current content range. */
        void setScrollOffset(int offset);

        /** Returns the maximum logical row offset currently available. */
        int maxScrollOffset();

        /** Ensures the selected row is visible inside the current viewport. */
        void ensureSelectedVisible();

        /** Returns the row index currently under the pointer, or {@code -1}. */
        int itemAt(double mouseX, double mouseY);

        /** Returns the tooltip for the row currently under the pointer, or {@code null}. */
        Component hoveredTooltip();
    }

}
