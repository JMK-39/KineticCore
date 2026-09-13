package dev.xyat.kineticcore.api.client.screen;

import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
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
    }

    protected final GuiOverlay overlays() {
        return overlays;
    }

    public final EditBox addTextField(int x, int y, int width, Component message) {
        return addTextField(x, y, width, message, null);
    }

    public final EditBox addTextField(int x, int y, int width, Component message, Component tooltip) {
        EditBox box = new KineticWidgets.KineticEditBox(font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message);
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Supplier<java.util.List<String>> dictionarySupplier, Component tooltip
    ) {
        KineticWidgets.AutoCompleteBox box = new KineticWidgets.AutoCompleteBox(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message, dictionarySupplier
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addIntegerField(
            int x, int y, int width, Component message, boolean allowNegative, Integer minValue, Integer maxValue, Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = KineticWidgets.NumericEditBox.integer(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message, allowNegative, minValue, maxValue
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addLongField(
            int x, int y, int width, Component message, boolean allowNegative, Long minValue, Long maxValue, Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = KineticWidgets.NumericEditBox.longInteger(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message, allowNegative, minValue, maxValue
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addDecimalField(
            int x, int y, int width, Component message, boolean allowNegative, Double minValue, Double maxValue, Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = KineticWidgets.NumericEditBox.decimal(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message, allowNegative, minValue, maxValue
        );
        addRenderableWidget(box);
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final Button addButton(int x, int y, int width, Component text, Component tooltip, Runnable action) {
        return addButton(x, y, width, text, tooltip, action == null ? null : ignored -> action.run());
    }

    public final Button addButton(int x, int y, int width, Component text, Component tooltip, Button.OnPress action) {
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                pressed -> { if (action != null) action.onPress(pressed); }, null, 0
        );
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
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                pressed -> { if (action != null) action.onPress(pressed); }, null, 0
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    protected final KineticWidgets.ColorSwatchButton addColorSwatchButton(
            int x, int y, int rgb, Component tooltip, Runnable action
    ) {
        KineticWidgets.ColorSwatchButton button = new KineticWidgets.ColorSwatchButton(
                x, y, KineticScreen.COMPACT_CONTROL_HEIGHT, rgb,
                ignored -> { if (action != null) action.run(); }
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.HighZButton addHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                pressed -> { if (action != null) action.onPress(pressed); }, null, zLevel
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

    public final void renderTextFieldPlaceholder(
            GuiGraphics graphics,
            EditBox box,
            Component placeholder
    ) {
        if (graphics == null || box == null || placeholder == null
                || !box.visible || !box.getValue().isEmpty() || box.isFocused()) {
            return;
        }
        String text = font.plainSubstrByWidth(
                placeholder.getString(),
                Math.max(0, box.getWidth() - 10)
        );
        graphics.drawString(
                font,
                text,
                box.getX() + 5,
                box.getY() + (box.getHeight() - font.lineHeight) / 2,
                GuiTheme.current().mutedText(),
                false
        );
    }

    public final void showTooltip(Component component) {
        overlays.tooltip(component);
    }

    public final void showItemTooltip(ItemStack stack) {
        overlays.itemTooltip(stack);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overlays.blocksInput()) return true;
        if (GuiSession.routeSelectionListWheel(children(), mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
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
