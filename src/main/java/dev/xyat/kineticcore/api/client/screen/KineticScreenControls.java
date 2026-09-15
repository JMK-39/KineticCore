package dev.xyat.kineticcore.api.client.screen;

import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.TabBar;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorPreviewButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorSwatchButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;

/** 三种 Screen 共用的控件创建、注册和 Tooltip 行为；坐标由宿主转换。 */
final class KineticScreenControls {
    @FunctionalInterface
    interface MenuOpener {
        void open(double x, double y, List<GuiOverlay.MenuItem> items);
    }

    private final Supplier<Font> font;
    private final GuiOverlay overlays;
    private final Consumer<AbstractWidget> addRenderable;
    private final Consumer<ObjectSelectionList<?>> addEvent;
    private final Consumer<AbstractWidget> removeWidget;
    private final MenuOpener menuOpener;
    private final Map<AbstractWidget, Component> widgetTooltips = new IdentityHashMap<>();

    KineticScreenControls(Supplier<Font> font, GuiOverlay overlays, Consumer<AbstractWidget> addRenderable,
            Consumer<ObjectSelectionList<?>> addEvent, Consumer<AbstractWidget> removeWidget, MenuOpener menuOpener) {
        this.font = font;
        this.overlays = overlays;
        this.addRenderable = addRenderable;
        this.addEvent = addEvent;
        this.removeWidget = removeWidget;
        this.menuOpener = menuOpener;
    }

    public HighZButton addHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action
    ) {
        return addHighZButton(x, y, width, text, tooltip, zLevel, action == null ? null : ignored -> action.run());
    }

    private void registerRenderable(AbstractWidget widget) { addRenderable.accept(widget); }
    private void registerEvent(ObjectSelectionList<?> widget) { addEvent.accept(widget); }
    private void openContextMenu(double x, double y, List<GuiOverlay.MenuItem> items) { menuOpener.open(x, y, items); }
    void clear() { widgetTooltips.clear(); }

    public final KineticEditBox addTextField(int x, int y, int width, Component message) {
        return addTextField(x, y, width, message, null);
    }

    public final KineticEditBox addTextField(int x, int y, int width, Component message, Component tooltip) {
        return addTextField(x, y, width, message, null, tooltip);
    }

    public final KineticEditBox addTextField(
            int x, int y, int width, Component message, Component placeholder, Component tooltip
    ) {
        KineticEditBox box = KineticWidgets.createTextField(
                font.get(), x, y, width, message, placeholder, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

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

    public final MultiLineEditBox addMultiLineTextField(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Component placeholder,
            Component tooltip
    ) {
        MultiLineEditBox box = KineticWidgets.createMultiLineTextField(
                font.get(), x, y, width, height, message, placeholder, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Supplier<java.util.List<String>> dictionarySupplier, Component tooltip
    ) {
        return addAutoCompleteField(
                x, y, width, message, null, dictionarySupplier, tooltip
        );
    }

    public final AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Component placeholder,
            Supplier<java.util.List<String>> dictionarySupplier, Component tooltip
    ) {
        AutoCompleteBox box = KineticWidgets.createAutoCompleteField(
                font.get(), x, y, width, message, placeholder, dictionarySupplier, null
        );
        registerRenderable(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return addIntegerAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    public final NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
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

    public final NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return addLongAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    public final NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
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

    public final NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return addDecimalAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    public final NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
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

    public final NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return addIntegerField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

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

    public final NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return addLongField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

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

    public final NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return addDecimalField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

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

    public final TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return addTabBar(x, y, totalWidth, labels, List.of(), selectedIndex, responder);
    }

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
                x, y, totalWidth, labels, selectedIndex, responder
        );
        List<? extends Component> safeTooltips = tooltips == null ? List.of() : tooltips;
        List<Button> buttons = tabBar.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            registerRenderable(button);
            Component tooltip = index < safeTooltips.size() ? safeTooltips.get(index) : null;
            registerWidgetTooltip(button, tooltip);
        }
        return tabBar;
    }

    public final Button addButton(int x, int y, int width, Component text, Component tooltip, Runnable action) {
        return addButton(x, y, width, text, tooltip, action == null ? null : ignored -> action.run());
    }

    public final Button addButton(int x, int y, int width, Component text, Component tooltip, Button.OnPress action) {
        Button button = KineticWidgets.createButton(x, y, width, text, null, action);
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final Button addCompactButton(
            int x, int y, int width, Component text, Component tooltip, Runnable action
    ) {
        return addCompactButton(x, y, width, text, tooltip, action == null ? null : ignored -> action.run());
    }

    public final Button addCompactButton(
            int x, int y, int width, Component text, Component tooltip, Button.OnPress action
    ) {
        Button button = KineticWidgets.createCompactButton(x, y, width, text, null, action);
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

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

    public final HighZButton addHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action
    ) {
        HighZButton button = KineticWidgets.createHighZButton(
                x, y, width, text, null, zLevel, action
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final HighZButton addCompactHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action
    ) {
        return addCompactHighZButton(
                x, y, width, text, tooltip, zLevel,
                action == null ? null : ignored -> action.run()
        );
    }

    public final HighZButton addCompactHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action
    ) {
        HighZButton button = KineticWidgets.createCompactHighZButton(
                x, y, width, text, null, zLevel, action
        );
        registerRenderable(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return addToggleButton(x, y, width, value, onText, offText, tooltip, ignored -> true, responder);
    }

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

    public final <T extends AbstractWidget> T addControl(T widget, Component tooltip) {
        if (widget == null) return null;
        registerRenderable(widget);
        if (tooltip != null && !tooltip.getString().isBlank()) registerWidgetTooltip(widget, tooltip);
        return widget;
    }

    public final void removeControl(AbstractWidget widget) {
        if (widget == null) return;
        widgetTooltips.remove(widget);
        removeWidget.accept(widget);
    }

    public final <T extends ObjectSelectionList<?>> T addEventListWidget(T list) {
        registerEvent(list);
        return list;
    }

    public final <T extends AbstractWidget> T registerWidgetTooltip(T widget, Component tooltip) {
        if (widget == null) return null;
        if (tooltip == null || tooltip.getString().isBlank()) widgetTooltips.remove(widget);
        else widgetTooltips.put(widget, tooltip);
        return widget;
    }

    public final void showTooltip(Component component) {
        overlays.tooltip(component);
    }

    public final void showTooltip(List<? extends Component> lines) {
        overlays.tooltip(lines);
    }

    public final void showTooltip(Component component, int maxWidth) {
        overlays.tooltip(component, maxWidth);
    }

    public final void showTooltip(List<? extends Component> lines, int maxWidth) {
        overlays.tooltip(lines, maxWidth);
    }

    public final void showFormattedTooltip(List<FormattedCharSequence> lines) {
        overlays.formattedTooltip(lines);
    }

    public final void showItemTooltip(ItemStack stack) {
        overlays.itemTooltip(stack);
    }

    public final void closeContextMenu() {
        overlays.closeMenu();
    }

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

    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return addDropdown(x, y, width, options, List.of(), selectedIndex, tooltip, ignored -> true, responder);
    }

    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        return addDropdown(x, y, width, options, List.of(), selectedIndex, tooltip, validator, responder);
    }

    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return addDropdown(x, y, width, options, optionTooltips, selectedIndex, tooltip, ignored -> true, responder);
    }

    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        List<Component> normalizedOptions = options == null ? new ArrayList<>() : new ArrayList<>(options);
        List<Component> normalizedTooltips = optionTooltips == null ? new ArrayList<>() : new ArrayList<>(optionTooltips);
        Dropdown control = KineticWidgets.createDropdown(
                x, y, width, normalizedOptions, selectedIndex, null, validator, responder, dropdown -> {
                    List<GuiOverlay.MenuItem> entries = new ArrayList<>();
                    List<Component> values = dropdown.options();
                    for (int index = 0; index < values.size(); index++) {
                        int optionIndex = index;
                        Component optionTooltip = index < normalizedTooltips.size()
                                && normalizedTooltips.get(index) != null
                                && !normalizedTooltips.get(index).getString().isBlank()
                                ? normalizedTooltips.get(index)
                                : values.get(index);
                        entries.add(GuiOverlay.MenuItem.toggle(
                                values.get(index),
                                optionTooltip,
                                optionIndex == dropdown.selectedIndex(),
                                () -> dropdown.choose(optionIndex)
                        ));
                    }
                    openContextMenu(x, y + KineticScreen.STANDARD_CONTROL_HEIGHT, entries);
                }
        );
        registerRenderable(control);
        registerWidgetTooltip(control, tooltip);
        return control;
    }

    boolean requestWidgetTooltip(double mouseX, double mouseY) {
        for (Map.Entry<AbstractWidget, Component> entry : widgetTooltips.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget == null || !widget.visible) continue;
            if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                    && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) {
                overlays.tooltip(entry.getValue(), 320);
                return true;
            }
        }
        return false;
    }
}
