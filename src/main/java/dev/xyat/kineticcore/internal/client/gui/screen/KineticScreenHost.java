package dev.xyat.kineticcore.internal.client.gui.screen;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.widget.KineticDropdown.Option;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTabStrip;
import dev.xyat.kineticcore.api.client.gui.widget.TabStripItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.*;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticTextFields.KineticMultiLineEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.internal.client.gui.widget.selection.TabBarButtons;
import dev.xyat.kineticcore.internal.client.gui.widget.slider.KineticSliders.Slider;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Shared control/overlay/focus/draft surface implemented identically by KineticScreen, KineticContainerScreen and
 * KineticNativeScreen. Page hosts and the page UI builder program against this interface.
 */
public interface KineticScreenHost {
    /** Returns the vanilla screen implementing this host. */
    Screen screen();

    /** Width of the host's page coordinate space. */
    int pageWidth();

    /** Height of the host's page coordinate space. */
    int pageHeight();

    AutoCompleteBox addAutoCompleteField(int x, int y, int width, Component message, Component placeholder, Supplier<List<KineticSuggestion>> dictionarySupplier, Component tooltip);
    StateButton addButton(int x, int y, int width, Component text, Component tooltip, Runnable action);
    StateButton addButtonWithHandler(int x, int y, int width, Component text, Component tooltip, Consumer<StateButton> action);
    StateButton addCardButton(int x, int y, int width, Component narration, Component tooltip, Runnable action);
    ColorPreviewButton addColorPreviewButton(int x, int y, int width, int color, Component text, Component tooltip, Runnable action);
    ColorSwatchButton addColorSwatchButton(int x, int y, int rgb, Component tooltip, Runnable action);
    StateButton addCompactButton(int x, int y, int width, Component text, Component tooltip, Runnable action);
    HighZButton addCompactHighZButton(int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action);
    ToggleButton addCompactToggleButton(int x, int y, int width, boolean value, Component onText, Component offText, Component tooltip, Consumer<Boolean> responder);
    ToggleButton addCompactToggleButton(int x, int y, int width, boolean value, Component onText, Component offText, Component tooltip, Predicate<Boolean> validator, Consumer<Boolean> responder);
    <T extends InternalControl> T addControl(T control, Component tooltip);
    CycleButton addCycleButton(int x, int y, int width, int index, List<Component> options, Component tooltip, Predicate<Integer> validator, Consumer<Integer> responder);
    NumericAutoCompleteBox addDecimalAutoCompleteField(int x, int y, int width, Component message, Supplier<List<KineticSuggestion>> dictionarySupplier, boolean allowNegative, Double minValue, Double maxValue, Predicate<Number> validator, Component tooltip);
    NumericEditBox addDecimalField(int x, int y, int width, Component message, boolean allowNegative, Double minValue, Double maxValue, Predicate<Number> validator);
    NumericEditBox addDecimalField(int x, int y, int width, Component message, boolean allowNegative, Double minValue, Double maxValue, Predicate<Number> validator, Component tooltip);
    Dropdown addDropdown(int x, int y, int width, List<? extends Option> options, String selectedValue, Component tooltip, Predicate<String> validator, Consumer<String> responder);
    HighZButton addHighZButton(int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action);
    KineticActionList addHighZScrollableActionList(int x, int y, int width, int height, List<? extends ActionItem> items, int selectedIndex, int initialScrollOffset, int actionWidth, Consumer<Integer> responder, Consumer<Integer> actionResponder, int zLevel);
    KineticItemActionList addHighZScrollableItemActionList(int x, int y, int width, int height, List<? extends ItemActionItem> items, int selectedIndex, int initialScrollOffset, int actionWidth, Consumer<Integer> responder, Consumer<Integer> actionResponder, int zLevel);
    KineticItemGrid addHighZScrollableItemGrid(int x, int y, int width, int height, ItemGridDensity density, List<? extends ItemGridItem> items, int initialScrollOffset, Consumer<Integer> responder, int zLevel);
    KineticItemSelectionList addHighZScrollableItemSelectionList(int x, int y, int width, int height, List<? extends ItemSelectionItem> items, int selectedIndex, int initialScrollOffset, Consumer<Integer> responder, int zLevel);
    KineticMultiActionList addHighZScrollableMultiActionList(int x, int y, int width, int height, List<? extends MultiActionItem> items, int selectedIndex, int initialScrollOffset, Consumer<Integer> responder, BiConsumer<Integer, Integer> actionResponder, int zLevel);
    KineticMultiToggleList addHighZScrollableMultiToggleList(int x, int y, int width, int height, List<? extends MultiToggleItem> items, int selectedIndex, int initialScrollOffset, Consumer<Integer> responder, BiConsumer<ToggleHit, Boolean> toggleResponder, int zLevel);
    KineticSelectionList addHighZScrollableSelectionList(int x, int y, int width, int height, List<? extends SelectionItem> items, int selectedIndex, int initialScrollOffset, Consumer<Integer> responder, int zLevel);
    KineticToggleActionList addHighZScrollableToggleActionList(int x, int y, int width, int height, List<? extends ToggleActionItem> items, int selectedIndex, int initialScrollOffset, int toggleWidth, int actionWidth, Consumer<Integer> responder, BiConsumer<Integer, Boolean> toggleResponder, Consumer<Integer> actionResponder, int zLevel);
    KineticToggleList addHighZScrollableToggleList(int x, int y, int width, int height, List<? extends ToggleItem> items, int initialScrollOffset, BiConsumer<Integer, Boolean> responder, int zLevel);
    TabBarButtons addHighZTabBar(int x, int y, int totalWidth, List<? extends Component> labels, List<? extends Component> tooltips, int selectedIndex, Consumer<Integer> responder, int zLevel);
    HighZToggleButton addHighZToggleButton(int x, int y, int width, boolean value, Component onText, Component offText, Component tooltip, Predicate<Boolean> validator, Consumer<Boolean> responder, int zLevel);
    NumericAutoCompleteBox addIntegerAutoCompleteField(int x, int y, int width, Component message, Supplier<List<KineticSuggestion>> dictionarySupplier, boolean allowNegative, Integer minValue, Integer maxValue, Predicate<Number> validator, Component tooltip);
    NumericEditBox addIntegerField(int x, int y, int width, Component message, boolean allowNegative, Integer minValue, Integer maxValue, Predicate<Number> validator);
    NumericEditBox addIntegerField(int x, int y, int width, Component message, boolean allowNegative, Integer minValue, Integer maxValue, Predicate<Number> validator, Component tooltip);
    ItemButton addItemButton(int x, int y, int width, ItemStack icon, Component text, Component tooltip, Runnable action);
    NumericAutoCompleteBox addLongAutoCompleteField(int x, int y, int width, Component message, Supplier<List<KineticSuggestion>> dictionarySupplier, boolean allowNegative, Long minValue, Long maxValue, Predicate<Number> validator, Component tooltip);
    NumericEditBox addLongField(int x, int y, int width, Component message, boolean allowNegative, Long minValue, Long maxValue, Predicate<Number> validator);
    NumericEditBox addLongField(int x, int y, int width, Component message, boolean allowNegative, Long minValue, Long maxValue, Predicate<Number> validator, Component tooltip);
    KineticMultiLineEditBox addMultiLineTextField(int x, int y, int width, int height, Component message, Component placeholder, Component tooltip);
    KineticActionList addScrollableActionList(int x, int y, int width, int height, List<? extends ActionItem> items, int selectedIndex, int initialScrollOffset, int actionWidth, Consumer<Integer> responder, Consumer<Integer> actionResponder);
    KineticItemActionList addScrollableItemActionList(int x, int y, int width, int height, List<? extends ItemActionItem> items, int selectedIndex, int initialScrollOffset, int actionWidth, Consumer<Integer> responder, Consumer<Integer> actionResponder);
    KineticItemGrid addScrollableItemGrid(int x, int y, int width, int height, ItemGridDensity density, List<? extends ItemGridItem> items, int initialScrollOffset, Consumer<Integer> responder);
    KineticItemSelectionList addScrollableItemSelectionList(int x, int y, int width, int height, List<? extends ItemSelectionItem> items, int selectedIndex, int initialScrollOffset, Consumer<Integer> responder);
    KineticMultiActionList addScrollableMultiActionList(int x, int y, int width, int height, List<? extends MultiActionItem> items, int selectedIndex, int initialScrollOffset, Consumer<Integer> responder, BiConsumer<Integer, Integer> actionResponder);
    KineticMultiToggleList addScrollableMultiToggleList(int x, int y, int width, int height, List<? extends MultiToggleItem> items, int selectedIndex, int initialScrollOffset, Consumer<Integer> responder, BiConsumer<ToggleHit, Boolean> toggleResponder);
    KineticSelectionList addScrollableSelectionList(int x, int y, int width, int height, List<? extends SelectionItem> items, int selectedIndex, int initialScrollOffset, Consumer<Integer> responder);
    KineticTabStrip addScrollableTabStrip(int x, int y, int width, List<? extends TabStripItem> tabs, int pinnedLeadingTabs, int selectedIndex, int initialScrollOffset, Component previousText, Component nextText, Consumer<Integer> responder);
    KineticToggleActionList addScrollableToggleActionList(int x, int y, int width, int height, List<? extends ToggleActionItem> items, int selectedIndex, int initialScrollOffset, int toggleWidth, int actionWidth, Consumer<Integer> responder, BiConsumer<Integer, Boolean> toggleResponder, Consumer<Integer> actionResponder);
    KineticToggleList addScrollableToggleList(int x, int y, int width, int height, List<? extends ToggleItem> items, int initialScrollOffset, BiConsumer<Integer, Boolean> responder);
    Slider addSlider(int x, int y, int width, Component message, double minValue, double maxValue, double step, double value, Predicate<Double> validator, DoubleConsumer responder, Component tooltip);
    TabBarButtons addTabBar(int x, int y, int totalWidth, List<? extends Component> labels, List<? extends Component> tooltips, int selectedIndex, Consumer<Integer> responder);
    KineticEditBox addTextField(int x, int y, int width, Component message);
    KineticEditBox addTextField(int x, int y, int width, Component message, Component placeholder, Predicate<String> validator, Component tooltip);
    ToggleButton addToggleButton(int x, int y, int width, boolean value, Component onText, Component offText, Component tooltip, Consumer<Boolean> responder);
    ToggleButton addToggleButton(int x, int y, int width, boolean value, Component onText, Component offText, Component tooltip, Predicate<Boolean> validator, Consumer<Boolean> responder);
    TabBarButtons addVerticalHighZTabBar(int x, int y, int width, List<? extends Component> labels, List<? extends Component> tooltips, int selectedIndex, Consumer<Integer> responder, int zLevel);
    void blurControl(InternalControl control);
    void clearControlFocus();
    void closeContextMenu();
    void commitDraft();
    <T> void configureDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore);
    <T> void configureStandaloneDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore);
    void discardDraft();
    void focusControl(InternalControl control);
    InternalControl focusedControl();
    boolean hasUnsavedEdits();
    boolean isControlFocused(InternalControl control);
    void navigateBack();
    void onClose();
    void openContextMenu(double virtualX, double virtualY, List<KineticOverlays.MenuItem> items);
    void openContextMenu(double virtualX, double virtualY, List<KineticOverlays.MenuItem> items, int virtualWidth);
    void openDialog(Component title, Component message, Component confirmText, Component cancelText, Runnable onConfirm, Runnable onCancel);
    boolean overlayBlocksInput();
    void rebuildUi();
    <T extends InternalControl> T registerDynamicWidgetTooltip(T control, Supplier<Component> tooltipSupplier);
    <T extends InternalControl> T registerWidgetTooltip(T control, Component tooltip);
    void removeKineticControl(InternalControl control);
    void reserveStandaloneDraft();
    void setParentScreen(Screen parent);
    void showFormattedTooltip(List<FormattedCharSequence> lines);
    void showItemTooltip(ItemStack stack);
    void showTooltip(List<? extends Component> lines, Integer maxWidth);
    void showTooltipLine(Component component);
    int toScreenBottom(double virtualY);
    int toScreenRight(double virtualX);
    int toScreenX(double virtualX);
    int toScreenY(double virtualY);
    double toVirtualX(double screenX);
    double toVirtualY(double screenY);
}
