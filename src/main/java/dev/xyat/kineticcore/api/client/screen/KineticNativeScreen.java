package dev.xyat.kineticcore.api.client.screen;

import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 使用 Minecraft 原生屏幕坐标的 Kinetic 编辑界面基类。
 * 适合不需要虚拟画布，但仍需要统一返回、草稿回滚和保存边界的界面。
 */
public abstract class KineticNativeScreen extends Screen {
    private final GuiOverlay overlays = new GuiOverlay();
    private final Map<AbstractWidget, Component> widgetTooltips = new IdentityHashMap<>();
    private GuiSession.DraftSession draftSession = new GuiSession.DraftSession();

    protected KineticNativeScreen(Component title) {
        super(title);
        dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl.initialize();
    }

    protected final GuiOverlay overlays() {
        return overlays;
    }

    public final double toVirtualX(double screenX) {
        return screenX;
    }

    public final double toVirtualY(double screenY) {
        return screenY;
    }

    public final int toScreenX(double virtualX) {
        return (int) Math.floor(virtualX);
    }

    public final int toScreenY(double virtualY) {
        return (int) Math.floor(virtualY);
    }

    public final int toScreenRight(double virtualX) {
        return (int) Math.ceil(virtualX);
    }

    public final int toScreenBottom(double virtualY) {
        return (int) Math.ceil(virtualY);
    }

    public final void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics == null) return;
        graphics.enableScissor(left, top, right, bottom);
    }

    public final void disableUiScissor(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.disableScissor();
    }

    public final KineticWidgets.KineticEditBox addTextField(int x, int y, int width, Component message) {
        return addTextField(x, y, width, message, null);
    }

    public final KineticWidgets.KineticEditBox addTextField(int x, int y, int width, Component message, Component tooltip) {
        return addTextField(x, y, width, message, null, tooltip);
    }

    public final KineticWidgets.KineticEditBox addTextField(
            int x, int y, int width, Component message, Component placeholder, Component tooltip
    ) {
        KineticWidgets.KineticEditBox box = KineticWidgets.createTextField(
                font, x, y, width, message, placeholder, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.KineticEditBox addTextField(
            int x, int y, int width, Component message, Component placeholder,
            Predicate<String> validator, Component tooltip
    ) {
        KineticWidgets.KineticEditBox box = KineticWidgets.createTextField(
                font, x, y, width, message, placeholder, validator, null
        );
        addRenderableWidget(box);
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
                font, x, y, width, height, message, placeholder, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Supplier<java.util.List<String>> dictionarySupplier, Component tooltip
    ) {
        return addAutoCompleteField(
                x, y, width, message, null, dictionarySupplier, tooltip
        );
    }

    public final KineticWidgets.AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Component placeholder,
            Supplier<java.util.List<String>> dictionarySupplier, Component tooltip
    ) {
        KineticWidgets.AutoCompleteBox box = KineticWidgets.createAutoCompleteField(
                font, x, y, width, message, placeholder, dictionarySupplier, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox addIntegerAutoCompleteField(
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

    public final KineticWidgets.NumericAutoCompleteBox addIntegerAutoCompleteField(
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
        KineticWidgets.NumericAutoCompleteBox box = KineticWidgets.createIntegerAutoCompleteField(
                font, x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox addLongAutoCompleteField(
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

    public final KineticWidgets.NumericAutoCompleteBox addLongAutoCompleteField(
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
        KineticWidgets.NumericAutoCompleteBox box = KineticWidgets.createLongAutoCompleteField(
                font, x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox addDecimalAutoCompleteField(
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

    public final KineticWidgets.NumericAutoCompleteBox addDecimalAutoCompleteField(
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
        KineticWidgets.NumericAutoCompleteBox box = KineticWidgets.createDecimalAutoCompleteField(
                font, x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addIntegerField(
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

    public final KineticWidgets.NumericEditBox addIntegerField(
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
        KineticWidgets.NumericEditBox box = KineticWidgets.createIntegerField(
                font, x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addLongField(
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

    public final KineticWidgets.NumericEditBox addLongField(
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
        KineticWidgets.NumericEditBox box = KineticWidgets.createLongField(
                font, x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addDecimalField(
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

    public final KineticWidgets.NumericEditBox addDecimalField(
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
        KineticWidgets.NumericEditBox box = KineticWidgets.createDecimalField(
                font, x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return addTabBar(x, y, totalWidth, labels, List.of(), selectedIndex, responder);
    }

    public final KineticWidgets.TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        KineticWidgets.TabBar tabBar = KineticWidgets.createTabBar(
                x, y, totalWidth, labels, selectedIndex, responder
        );
        List<? extends Component> safeTooltips = tooltips == null ? List.of() : tooltips;
        List<Button> buttons = tabBar.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            addRenderableWidget(button);
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
        addRenderableWidget(button);
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
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.ColorSwatchButton addColorSwatchButton(
            int x, int y, int rgb, Component tooltip, Runnable action
    ) {
        KineticWidgets.ColorSwatchButton button = KineticWidgets.createColorSwatchButton(
                x, y, rgb, null, action
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.HighZButton addHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = KineticWidgets.createHighZButton(
                x, y, width, text, null, zLevel, action
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.HighZButton addCompactHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action
    ) {
        return addCompactHighZButton(
                x, y, width, text, tooltip, zLevel,
                action == null ? null : ignored -> action.run()
        );
    }

    public final KineticWidgets.HighZButton addCompactHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = KineticWidgets.createCompactHighZButton(
                x, y, width, text, null, zLevel, action
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.ToggleButton addToggleButton(
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

    public final KineticWidgets.ToggleButton addToggleButton(
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
        KineticWidgets.ToggleButton button = KineticWidgets.createToggleButton(
                x, y, width, value, onText, offText, null, validator, responder
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.ColorPreviewButton addColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        KineticWidgets.ColorPreviewButton button = KineticWidgets.createColorPreviewButton(
                x, y, width, color, text, null, action
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final <T extends AbstractWidget> T addControl(T widget, Component tooltip) {
        if (widget == null) return null;
        addRenderableWidget(widget);
        if (tooltip != null && !tooltip.getString().isBlank()) registerWidgetTooltip(widget, tooltip);
        return widget;
    }

    public final void removeControl(AbstractWidget widget) {
        if (widget == null) return;
        widgetTooltips.remove(widget);
        removeWidget(widget);
    }

    public final <T extends ObjectSelectionList<?>> T addEventListWidget(T list) {
        addWidget(list);
        return list;
    }

    public final <T extends AbstractWidget> T registerWidgetTooltip(T widget, Component tooltip) {
        if (widget == null) return null;
        if (tooltip == null || tooltip.getString().isBlank()) widgetTooltips.remove(widget);
        else widgetTooltips.put(widget, tooltip);
        return widget;
    }

    private void requestWidgetTooltip(double mouseX, double mouseY) {
        for (Map.Entry<AbstractWidget, Component> entry : widgetTooltips.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget == null || !widget.visible) continue;
            if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                    && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) {
                overlays.tooltip(entry.getValue(), 320);
                return;
            }
        }
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

    public final void focusControl(GuiEventListener control) {
        if (control == null) {
            clearControlFocus();
            return;
        }
        GuiEventListener current = getFocused();
        if (current != null && current != control) current.setFocused(false);
        setFocused(control);
        control.setFocused(true);
    }

    public final void blurControl(GuiEventListener control) {
        if (control == null) return;
        control.setFocused(false);
        if (getFocused() == control) setFocused(null);
    }

    public final void clearControlFocus() {
        GuiEventListener current = getFocused();
        if (current != null) current.setFocused(false);
        setFocused(null);
    }

    public final boolean isControlFocused(GuiEventListener control) {
        return control != null && getFocused() == control && control.isFocused();
    }

    public final void openContextMenu(double x, double y, List<GuiOverlay.MenuItem> items) {
        overlays.openMenu((int) Math.round(x), (int) Math.round(y), items);
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

    public final KineticWidgets.Dropdown addDropdown(
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

    public final KineticWidgets.Dropdown addDropdown(
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

    public final KineticWidgets.Dropdown addDropdown(
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

    public final KineticWidgets.Dropdown addDropdown(
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
        KineticWidgets.Dropdown control = KineticWidgets.createDropdown(
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
        addRenderableWidget(control);
        registerWidgetTooltip(control, tooltip);
        return control;
    }

    protected final void reserveStandaloneDraft() {
        draftSession.reserveStandaloneOwner(this);
    }

    protected final <T> void configureDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, false);
    }

    protected final <T> void configureStandaloneDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, true);
    }







    protected final void commitDraft() {
        draftSession.commitBaseline(this);
    }

    protected final void discardDraft() {
        draftSession.discardToBaseline();
    }

    protected final boolean hasUnsavedEdits() {
        return draftSession.isDirty();
    }


    @Override
    protected final void init() {
        super.init();
        rebuildUi();
    }

    public final void rebuildUi() {
        clearWidgets();
        widgetTooltips.clear();
        buildUi();
    }

    protected abstract void buildUi();

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        overlays.beginFrame();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!overlays.blocksInput()) {
            requestWidgetTooltip(mouseX, mouseY);
        }
        renderNativeOverlayRequests(graphics, mouseX, mouseY, partialTick);
        overlays.render(graphics, font, width, height, mouseX, mouseY);
    }

    protected void renderNativeOverlayRequests(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overlays.mouseClicked(mouseX, mouseY, button, width, height, font)) {
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (overlays.blocksInput()) {
            return true;
        }
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overlays.blocksInput()) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overlays.blocksInput()) return true;
        if (GuiSession.routeSelectionListWheel(children(), mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!overlays.blocksInput()) super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (overlays.keyPressed(keyCode)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        return handled;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean handled = super.charTyped(codePoint, modifiers);
        return handled;
    }

    final GuiSession.DraftSession draftSession() {
        return draftSession;
    }

    final void adoptDraftSession(GuiSession.DraftSession sharedDraft) {
        if (sharedDraft != null && sharedDraft.enabled()) draftSession = sharedDraft;
    }

    final void discardPendingEditsForNavigation() {
        draftSession.discardToBaseline();
    }

    public final void navigateBack() {
        GuiSession.back(this);
    }

    @Override
    public void onClose() {
        navigateBack();
    }
}
