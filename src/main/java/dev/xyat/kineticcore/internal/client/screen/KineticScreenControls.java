package dev.xyat.kineticcore.internal.client.screen;

import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.TabBar;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableTab;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableTabStrip;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.SelectionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemSelectionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemGridDensity;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemGridItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemGrid;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemSelectionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.MultiActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableMultiActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableToggleActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.MultiToggleItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleHit;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableMultiToggleList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableSelectionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableToggleList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Option;
import dev.xyat.kineticcore.api.client.widget.slider.KineticSliders.Slider;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.CycleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZToggleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorPreviewButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorSwatchButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ItemButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticMultiLineEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.internal.client.overlay.GuiOverlayRuntime;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Internal control registration and tooltip implementation shared by Kinetic screen facades. */
public final class KineticScreenControls {
    private static final Logger LOGGER = LogUtils.getLogger();
    @FunctionalInterface
    public interface MenuOpener {
        void open(double x, double y, List<KineticOverlays.MenuItem> items);
    }

    private final Supplier<Font> font;
    private final GuiOverlayRuntime overlays;
    private final Consumer<AbstractWidget> addRenderable;
    private final Consumer<ObjectSelectionList<?>> addEvent;
    private final Consumer<AbstractWidget> removeWidget;
    private final MenuOpener menuOpener;
    private final Map<AbstractWidget, Supplier<Component>> widgetTooltips = new IdentityHashMap<>();
    private final Set<AbstractWidget> registeredRenderables = Collections.newSetFromMap(new IdentityHashMap<>());
    // Keep the host's registration/render order for deterministic overlap hit testing.
    private final List<AbstractWidget> tooltipHitOrder = new ArrayList<>();
    private final Set<ObjectSelectionList<?>> registeredEventLists = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<AutoCompleteBox> autoCompleteBoxes = new ArrayList<>();
    private final KineticAutoComplete.AutoCompleteBoxGroup autoCompleteGroup =
            new KineticAutoComplete.AutoCompleteBoxGroup();

    public KineticScreenControls(Supplier<Font> font, GuiOverlayRuntime overlays, Consumer<AbstractWidget> addRenderable,
            Consumer<ObjectSelectionList<?>> addEvent, Consumer<AbstractWidget> removeWidget, MenuOpener menuOpener) {
        this.font = font;
        this.overlays = overlays;
        this.addRenderable = addRenderable;
        this.addEvent = addEvent;
        this.removeWidget = removeWidget;
        this.menuOpener = menuOpener;
    }


    private void registerRenderable(AbstractWidget widget) {
        if (!registeredRenderables.add(widget)) return;
        try {
            addRenderable.accept(widget);
        } catch (RuntimeException | Error failure) {
            // The host refused this widget. Allow a later registration to retry.
            registeredRenderables.remove(widget);
            throw failure;
        }
        tooltipHitOrder.removeIf(tracked -> tracked == widget);
        tooltipHitOrder.add(widget);
        if (widget instanceof AutoCompleteBox box && !autoCompleteBoxes.contains(box)) {
            autoCompleteBoxes.add(box);
            syncAutoCompleteGroup();
        }
    }
    private void registerEvent(ObjectSelectionList<?> widget) {
        if (!registeredEventLists.add(widget)) return;
        try {
            addEvent.accept(widget);
        } catch (RuntimeException | Error failure) {
            registeredEventLists.remove(widget);
            throw failure;
        }
    }
    private void openContextMenu(double x, double y, List<KineticOverlays.MenuItem> items) { menuOpener.open(x, y, items); }
    private void syncAutoCompleteGroup() {
        autoCompleteGroup.setBoxes(autoCompleteBoxes.toArray(AutoCompleteBox[]::new));
    }
    public void clear() {
        widgetTooltips.clear();
        registeredRenderables.clear();
        tooltipHitOrder.clear();
        registeredEventLists.clear();
        autoCompleteBoxes.clear();
        syncAutoCompleteGroup();
    }

    public void tickManagedControls() {
        for (AbstractWidget widget : List.copyOf(registeredRenderables)) {
            if (widget instanceof EditBox editBox) {
                editBox.tick();
            } else if (widget instanceof MultiLineEditBox multiLineEditBox) {
                multiLineEditBox.tick();
            }
        }
    }




    /** Adds text field. */
    public final KineticEditBox addTextField(
            int x, int y, int width, Component message, Component placeholder,
            Predicate<String> validator, Component tooltip
    ) {
        KineticEditBox box = KineticWidgets.createTextField(
                font.get(), x, y, width, message, placeholder, validator, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    /** Adds multi line text field. */
    public final KineticMultiLineEditBox addMultiLineTextField(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Component placeholder,
            Component tooltip
    ) {
        KineticMultiLineEditBox box = KineticWidgets.createMultiLineTextField(
                font.get(), x, y, width, height, message, placeholder, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }


    /** Adds auto complete field. */
    public final AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Component placeholder,
            Supplier<java.util.List<KineticAutoComplete.Suggestion>> dictionarySupplier, Component tooltip
    ) {
        AutoCompleteBox box = KineticWidgets.createAutoCompleteField(
                font.get(), x, y, width, message, placeholder, dictionarySupplier, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }


    /** Adds integer auto complete field. */
    public final NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = KineticWidgets.createIntegerAutoCompleteField(
                font.get(), x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }


    /** Adds long auto complete field. */
    public final NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = KineticWidgets.createLongAutoCompleteField(
                font.get(), x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }


    /** Adds decimal auto complete field. */
    public final NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = KineticWidgets.createDecimalAutoCompleteField(
                font.get(), x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }


    /** Adds integer field. */
    public final NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = KineticWidgets.createIntegerField(
                font.get(), x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }


    /** Adds long field. */
    public final NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = KineticWidgets.createLongField(
                font.get(), x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }


    /** Adds decimal field. */
    public final NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = KineticWidgets.createDecimalField(
                font.get(), x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }




    /** Adds a smooth vertical single-selection list using standard Kinetic row controls. */
    public final ScrollableSelectionList addScrollableSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends SelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder
    ) {
        ScrollableSelectionList list = KineticWidgets.createScrollableSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical single-selection list rendered at the supplied Z depth. */
    public final ScrollableSelectionList addHighZScrollableSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends SelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        ScrollableSelectionList list = KineticWidgets.createHighZScrollableSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical item-backed single-selection list. */
    public final ScrollableItemSelectionList addScrollableItemSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemSelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder
    ) {
        ScrollableItemSelectionList list = KineticWidgets.createScrollableItemSelectionList(
                font.get(), x, y, width, height, items, selectedIndex, initialScrollOffset, responder
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical item-backed single-selection list rendered at the supplied Z depth. */
    public final ScrollableItemSelectionList addHighZScrollableItemSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemSelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        ScrollableItemSelectionList list = KineticWidgets.createHighZScrollableItemSelectionList(
                font.get(), x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth scrollable item-slot grid. */
    public final ScrollableItemGrid addScrollableItemGrid(
            int x, int y, int width, int height,
            ItemGridDensity density, List<? extends ItemGridItem> items,
            int initialScrollOffset, Consumer<Integer> responder
    ) {
        ScrollableItemGrid grid = KineticWidgets.createScrollableItemGrid(
                font.get(), x, y, width, height, density, items, initialScrollOffset, responder
        );
        registerRenderable(KineticControlBridge.widget(grid));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(grid), grid::hoveredTooltip);
        return grid;
    }

    /** Adds a smooth scrollable item-slot grid rendered at the supplied Z depth. */
    public final ScrollableItemGrid addHighZScrollableItemGrid(
            int x, int y, int width, int height,
            ItemGridDensity density, List<? extends ItemGridItem> items,
            int initialScrollOffset, Consumer<Integer> responder, int zLevel
    ) {
        ScrollableItemGrid grid = KineticWidgets.createHighZScrollableItemGrid(
                font.get(), x, y, width, height, density, items, initialScrollOffset, responder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(grid));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(grid), grid::hoveredTooltip);
        return grid;
    }

    /** Adds a smooth vertical single-selection list with one trailing row action. */
    public final ScrollableActionList addScrollableActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder
    ) {
        ScrollableActionList list = KineticWidgets.createScrollableActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical single-selection list with one trailing row action at the supplied Z depth. */
    public final ScrollableActionList addHighZScrollableActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        ScrollableActionList list = KineticWidgets.createHighZScrollableActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical single-selection list with multiple trailing row actions. */
    public final ScrollableMultiActionList addScrollableMultiActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<Integer, Integer> actionResponder
    ) {
        ScrollableMultiActionList list = KineticWidgets.createScrollableMultiActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, actionResponder
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical single-selection list with multiple trailing row actions at the supplied Z depth. */
    public final ScrollableMultiActionList addHighZScrollableMultiActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<Integer, Integer> actionResponder,
            int zLevel
    ) {
        ScrollableMultiActionList list = KineticWidgets.createHighZScrollableMultiActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, actionResponder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical single-selection list with one real toggle and one trailing row action. */
    public final ScrollableToggleActionList addScrollableToggleActionList(
            int x, int y, int width, int height,
            List<? extends ToggleActionItem> items,
            int selectedIndex, int initialScrollOffset,
            int toggleWidth, int actionWidth,
            Consumer<Integer> responder,
            BiConsumer<Integer, Boolean> toggleResponder,
            Consumer<Integer> actionResponder
    ) {
        ScrollableToggleActionList list = KineticWidgets.createScrollableToggleActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical single-selection list with one real toggle and one trailing row action at the supplied Z depth. */
    public final ScrollableToggleActionList addHighZScrollableToggleActionList(
            int x, int y, int width, int height,
            List<? extends ToggleActionItem> items,
            int selectedIndex, int initialScrollOffset,
            int toggleWidth, int actionWidth,
            Consumer<Integer> responder,
            BiConsumer<Integer, Boolean> toggleResponder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        ScrollableToggleActionList list = KineticWidgets.createHighZScrollableToggleActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical item-backed single-selection list with one trailing row action. */
    public final ScrollableItemActionList addScrollableItemActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder
    ) {
        ScrollableItemActionList list = KineticWidgets.createScrollableItemActionList(
                font.get(), x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical item-backed single-selection list with one trailing row action at the supplied Z depth. */
    public final ScrollableItemActionList addHighZScrollableItemActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        ScrollableItemActionList list = KineticWidgets.createHighZScrollableItemActionList(
                font.get(), x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical single-selection list with any number of real toggles per row. */
    public final ScrollableMultiToggleList addScrollableMultiToggleList(
            int x, int y, int width, int height,
            List<? extends MultiToggleItem> items,
            int selectedIndex, int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<ToggleHit, Boolean> toggleResponder
    ) {
        ScrollableMultiToggleList list = KineticWidgets.createScrollableMultiToggleList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, toggleResponder
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical single-selection list with any number of real toggles per row at the supplied Z depth. */
    public final ScrollableMultiToggleList addHighZScrollableMultiToggleList(
            int x, int y, int width, int height,
            List<? extends MultiToggleItem> items,
            int selectedIndex, int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<ToggleHit, Boolean> toggleResponder,
            int zLevel
    ) {
        ScrollableMultiToggleList list = KineticWidgets.createHighZScrollableMultiToggleList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, toggleResponder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical multi-toggle list using standard Kinetic row controls. */
    public final ScrollableToggleList addScrollableToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder
    ) {
        ScrollableToggleList list = KineticWidgets.createScrollableToggleList(
                x, y, width, height, items, initialScrollOffset, responder
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a smooth vertical multi-toggle list rendered at the supplied Z depth. */
    public final ScrollableToggleList addHighZScrollableToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder,
            int zLevel
    ) {
        ScrollableToggleList list = KineticWidgets.createHighZScrollableToggleList(
                x, y, width, height, items, initialScrollOffset, responder, zLevel
        );
        registerRenderable(KineticControlBridge.widget(list));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(list), list::hoveredTooltip);
        return list;
    }

    /** Adds a compact variable-width tab strip with API-managed horizontal scrolling. */
    public final ScrollableTabStrip addScrollableTabStrip(
            int x,
            int y,
            int width,
            List<? extends ScrollableTab> tabs,
            int pinnedLeadingTabs,
            int selectedIndex,
            int initialScrollOffset,
            Component previousText,
            Component nextText,
            Consumer<Integer> responder
    ) {
        ScrollableTabStrip strip = KineticWidgets.createScrollableTabStrip(
                font.get(), x, y, width, tabs, pinnedLeadingTabs, selectedIndex, initialScrollOffset,
                previousText, nextText, responder
        );
        registerRenderable(KineticControlBridge.widget(strip));
        registerDynamicWidgetTooltip(KineticControlBridge.widget(strip), strip::hoveredTooltip);
        return strip;
    }


    /** Adds tab bar. */
    public final TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        TabBar tabBar = KineticWidgets.createTabBar(
                x, y, totalWidth, labels, List.of(), selectedIndex, responder
        );
        List<? extends Component> safeTooltips = tooltips == null ? List.of() : tooltips;
        List<StateButton> buttons = tabBar.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            StateButton button = buttons.get(index);
            registerRenderable(button);
            Component tooltip = index < safeTooltips.size() ? safeTooltips.get(index) : null;
            registerWidgetTooltip(button, tooltip);
        }
        return tabBar;
    }


    /** Adds a tab bar rendered at an elevated Z depth. */
    public final TabBar addHighZTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder,
            int zLevel
    ) {
        TabBar tabBar = KineticWidgets.createHighZTabBar(
                x, y, totalWidth, labels, List.of(), selectedIndex, responder, zLevel
        );
        List<? extends Component> safeTooltips = tooltips == null ? List.of() : tooltips;
        List<StateButton> buttons = tabBar.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            StateButton button = buttons.get(index);
            registerRenderable(button);
            Component tooltip = index < safeTooltips.size() ? safeTooltips.get(index) : null;
            registerWidgetTooltip(button, tooltip);
        }
        return tabBar;
    }

    /** Adds a vertical tab bar rendered at an elevated Z depth. */
    public final TabBar addVerticalHighZTabBar(
            int x,
            int y,
            int width,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder,
            int zLevel
    ) {
        TabBar tabBar = KineticWidgets.createVerticalHighZTabBar(
                x, y, width, labels, List.of(), selectedIndex, responder, zLevel
        );
        List<? extends Component> safeTooltips = tooltips == null ? List.of() : tooltips;
        List<StateButton> buttons = tabBar.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            StateButton button = buttons.get(index);
            registerRenderable(button);
            Component tooltip = index < safeTooltips.size() ? safeTooltips.get(index) : null;
            registerWidgetTooltip(button, tooltip);
        }
        return tabBar;
    }



    /** Adds button. */
    public final StateButton addButton(int x, int y, int width, Component text, Component tooltip, Runnable action) {
        StateButton button = KineticWidgets.createButton(x, y, width, text, null, action);
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }


    /** Adds an API-managed content card button with standard card height and tooltip routing. */
    public final StateButton addCardButton(
            int x, int y, int width, Component narration, Component tooltip, Runnable action
    ) {
        StateButton button = KineticWidgets.createCardButton(x, y, width, narration, null, action);
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Adds an API-managed item button with the standard item-card height and tooltip routing. */
    public final ItemButton addItemButton(
            int x, int y, int width, ItemStack icon, Component text, Component tooltip, Runnable action
    ) {
        ItemButton button = KineticWidgets.createItemButton(x, y, width, icon, text, null, action);
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Adds a state button whose callback needs the managed button instance. */
    public final StateButton addButtonWithHandler(
            int x, int y, int width, Component text, Component tooltip, Consumer<StateButton> action
    ) {
        StateButton button = KineticWidgets.createButtonWithHandler(x, y, width, text, null, action);
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }


    /** Adds a standard slider whose range, step, validation, and responder remain caller-defined. */
    public final Slider addSlider(
            int x,
            int y,
            int width,
            Component message,
            double minValue,
            double maxValue,
            double step,
            double value,
            Predicate<Double> validator,
            DoubleConsumer responder,
            Component tooltip
    ) {
        Slider slider = KineticWidgets.createSlider(
                x, y, width, message, minValue, maxValue, step, value, validator, responder, null
        );
        registerRenderable(slider);
        registerWidgetTooltip(slider, tooltip);
        return slider;
    }

    /** Adds compact button. */
    public final StateButton addCompactButton(
            int x, int y, int width, Component text, Component tooltip, Runnable action
    ) {
        StateButton button = KineticWidgets.createCompactButton(x, y, width, text, null, action);
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Adds color swatch button. */
    public final ColorSwatchButton addColorSwatchButton(
            int x, int y, int rgb, Component tooltip, Runnable action
    ) {
        ColorSwatchButton button = KineticWidgets.createColorSwatchButton(
                x, y, rgb, null, action
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Adds high z button. */
    public final HighZButton addHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action
    ) {
        HighZButton button = KineticWidgets.createHighZButton(
                x, y, width, text, null, zLevel, action
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }


    /** Adds compact high z button. */
    public final HighZButton addCompactHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action
    ) {
        HighZButton button = KineticWidgets.createCompactHighZButton(
                x, y, width, text, null, zLevel, action
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }


    /** Adds toggle button. */
    public final ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder
    ) {
        ToggleButton button = KineticWidgets.createToggleButton(
                x, y, width, value, onText, offText, null, validator, responder
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Adds a compact-height toggle button. */
    public final ToggleButton addCompactToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder
    ) {
        ToggleButton button = KineticWidgets.createCompactToggleButton(
                x, y, width, value, onText, offText, null, validator, responder
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Adds a multi-state cycle button. */
    public final CycleButton addCycleButton(
            int x,
            int y,
            int width,
            int index,
            List<Component> options,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        CycleButton button = KineticWidgets.createCycleButton(
                x, y, width, index, options, null, validator, responder
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Adds high-z toggle button. */
    public final HighZToggleButton addHighZToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder,
            int zLevel
    ) {
        HighZToggleButton button = KineticWidgets.createHighZToggleButton(
                x, y, width, value, onText, offText, null, validator, responder, zLevel
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Adds color preview button. */
    public final ColorPreviewButton addColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        ColorPreviewButton button = KineticWidgets.createColorPreviewButton(
                x, y, width, color, text, null, action
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Registers one raw host widget for rendering and input. */
    public final <T extends AbstractWidget> T registerWidget(T widget, Component tooltip) {
        if (widget == null) return null;
        registerRenderable(widget);
        // Passing null means no tooltip, including when an existing widget is registered again.
        registerWidgetTooltip(widget, tooltip);
        return widget;
    }

    /** Unregisters one raw host widget from rendering and input. */
    public final void unregisterWidget(AbstractWidget widget) {
        if (widget == null) return;
        // The host must actually release the widget before we forget its input,
        // tooltip and autocomplete registrations. A failed host removal can retry.
        removeWidget.accept(widget);
        widgetTooltips.remove(widget);
        registeredRenderables.remove(widget);
        tooltipHitOrder.removeIf(registered -> registered == widget);
        if (widget instanceof AutoCompleteBox box && autoCompleteBoxes.remove(box)) {
            syncAutoCompleteGroup();
        }
    }

    /** Registers one Kinetic smooth selection list for input events. */
    public final <T extends ObjectSelectionList<?>> T addSmoothSelectionList(T list) {
        registerEvent(list);
        return list;
    }

    /** Registers widget tooltip. */
    public final <T extends AbstractWidget> T registerWidgetTooltip(T widget, Component tooltip) {
        if (widget == null) return null;
        trackTooltipWidget(widget);
        if (tooltip == null || tooltip.getString().isBlank()) widgetTooltips.remove(widget);
        else widgetTooltips.put(widget, () -> tooltip);
        return widget;
    }

    /** Registers a tooltip supplier evaluated only when the widget is actually hovered. */
    public final <T extends AbstractWidget> T registerDynamicWidgetTooltip(T widget, Supplier<Component> tooltipSupplier) {
        if (widget == null) return null;
        trackTooltipWidget(widget);
        if (tooltipSupplier == null) widgetTooltips.remove(widget);
        else widgetTooltips.put(widget, tooltipSupplier);
        return widget;
    }

    // Explicitly registered tooltips may belong to widgets already rendered by a host Screen.
    private void trackTooltipWidget(AbstractWidget widget) {
        for (AbstractWidget tracked : tooltipHitOrder) {
            if (tracked == widget) return;
        }
        tooltipHitOrder.add(widget);
    }

    /** Shows unwrapped lines when maxWidth is null, otherwise uses the requested wrapping width. */
    public final void showTooltip(List<? extends Component> lines, Integer maxWidth) {
        if (maxWidth == null) {
            overlays.tooltip(lines);
        } else {
            overlays.tooltip(lines, maxWidth);
        }
    }

    /** Provides the show formatted tooltip operation exposed by this API. */
    public final void showFormattedTooltip(List<FormattedCharSequence> lines) {
        overlays.formattedTooltip(lines);
    }

    /** Provides the show item tooltip operation exposed by this API. */
    public final void showItemTooltip(ItemStack stack) {
        overlays.itemTooltip(stack);
    }

    /** Closes context menu. */
    public final void closeContextMenu() {
        overlays.closeMenu();
    }

    /** Opens dialog. */
    public final void openDialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        overlays.openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
    }




    /** Adds one dropdown whose callbacks receive only raw option values. */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Option> options,
            String selectedValue,
            Component tooltip,
            Predicate<String> validator,
            Consumer<String> responder
    ) {
        List<Option> normalizedOptions = options == null ? List.of() : List.copyOf(options);
        Dropdown control = KineticWidgets.createDropdown(
                x, y, width, normalizedOptions, selectedValue, null, validator, responder, dropdown -> {
                    boolean showTranslations = showLocalizedDetails();
                    List<KineticOverlays.MenuItem> entries = new ArrayList<>();
                    List<Option> values = dropdown.options();
                    for (int index = 0; index < values.size(); index++) {
                        int optionIndex = index;
                        Option option = values.get(index);
                        Component label = Component.literal(option.value());
                        Component detail = showTranslations ? option.translation() : Component.empty();
                        Component optionTooltip = option.tooltip().getString().isBlank() ? label : option.tooltip();
                        entries.add(KineticOverlays.MenuItem.create(
                                label,
                                detail,
                                optionTooltip,
                                optionIndex == dropdown.selectedIndex(),
                                () -> dropdown.choose(optionIndex),
                                true,
                                KineticOverlays.MenuItemStyle.NORMAL
                        ));
                    }
                    openContextMenu(dropdown.getX(), dropdown.getY() + dropdown.getHeight(), entries);
                }
        );
        registerRenderable(control);
        registerWidgetTooltip(control, tooltip);
        return control;
    }

    private static boolean showLocalizedDetails() {
        return !KineticClientRuntime.isEnglishLanguage();
    }


    public boolean hasOpenAutoCompletePopup() {
        return autoCompleteGroup.hasOpenPopup();
    }

    public void renderAutoCompleteSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        autoCompleteGroup.renderSuggestions(graphics, mouseX, mouseY);
    }

    public void clearAutoCompleteFocusOutside(double mouseX, double mouseY) {
        autoCompleteGroup.clearFocusOutside(mouseX, mouseY);
    }

    public boolean handleAutoCompleteClick(double mouseX, double mouseY, int button) {
        // Non-primary clicks may dismiss/close the popup elsewhere, but must never
        // reach an obscured button or context menu while inside its bounds.
        return button == 0
                ? autoCompleteGroup.handleSuggestionClick(mouseX, mouseY)
                : autoCompleteGroup.isAnySuggestionPopupHovered(mouseX, mouseY);
    }

    public boolean handleAutoCompleteDragged(double mouseX, double mouseY) {
        return autoCompleteGroup.handleMouseDragged(mouseX, mouseY);
    }

    public boolean handleAutoCompleteReleased(int button) {
        return autoCompleteGroup.handleMouseReleased(button);
    }

    public boolean handleAutoCompleteScroll(double mouseX, double mouseY, double delta) {
        return autoCompleteGroup.handleHoveredMouseScrolled(mouseX, mouseY, delta);
    }

    public boolean handleAutoCompleteKey(int keyCode, int scanCode, int modifiers) {
        return autoCompleteGroup.handleKeyPressed(keyCode, scanCode, modifiers);
    }

    public boolean requestWidgetTooltip(double mouseX, double mouseY, Predicate<AbstractWidget> hitFilter) {
        for (int index = tooltipHitOrder.size() - 1; index >= 0; index--) {
            AbstractWidget widget = tooltipHitOrder.get(index);
            if (!widget.visible || !hitFilter.test(widget)) continue;
            if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                    && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) {
                Supplier<Component> supplier = widgetTooltips.get(widget);
                try {
                    Component tooltip = supplier == null ? null : supplier.get();
                    if (tooltip != null && !tooltip.getString().isBlank()) overlays.tooltip(tooltip, 320);
                } catch (RuntimeException | Error failure) {
                    // A business-supplied dynamic tooltip must not crash the screen or
                    // expose tooltips belonging to widgets behind this foreground one.
                    LOGGER.error("Kinetic widget tooltip callback failed", failure);
                }
                // A foreground widget with no tooltip must still hide tooltips of covered controls.
                return true;
            }
        }
        return false;
    }
}
