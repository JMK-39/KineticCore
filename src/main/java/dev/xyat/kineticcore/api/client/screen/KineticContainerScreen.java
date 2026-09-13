package dev.xyat.kineticcore.api.client.screen;

import dev.xyat.kineticcore.api.client.layout.GuiLayout;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class KineticContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private float preferredWidth = KineticScreen.STANDARD_CANVAS_WIDTH;
    private float preferredHeight = KineticScreen.STANDARD_CANVAS_HEIGHT;
    private int safeMargin = KineticScreen.STANDARD_SAFE_MARGIN;
    private float uiScale = 1f;
    private int uiX;
    private int uiY;
    private int uiWidth = 1;
    private int uiHeight = 1;
    private final GuiOverlay overlays = new GuiOverlay();
    private final Map<AbstractWidget, Component> widgetTooltips = new IdentityHashMap<>();
    private GuiSession.DraftSession draftSession = new GuiSession.DraftSession();

    protected KineticContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected final void useStandardContainer() {
        useResponsiveContainer(
                KineticScreen.STANDARD_CANVAS_WIDTH,
                KineticScreen.STANDARD_CANVAS_HEIGHT,
                KineticScreen.STANDARD_SAFE_MARGIN
        );
    }

    protected final void useResponsiveContainer(float preferredWidth, float preferredHeight, int safeMargin) {
        this.preferredWidth = Math.max(1f, preferredWidth);
        this.preferredHeight = Math.max(1f, preferredHeight);
        this.safeMargin = Math.max(0, safeMargin);
    }

    protected final int uiWidth() {
        return uiWidth;
    }

    protected final int uiHeight() {
        return uiHeight;
    }

    protected final float uiScale() {
        return uiScale;
    }

    protected final double toVirtualX(double screenX) {
        return (screenX - uiX) / uiScale;
    }

    protected final double toVirtualY(double screenY) {
        return (screenY - uiY) / uiScale;
    }

    protected final int toScreenX(double virtualX) {
        return uiX + (int) Math.floor(virtualX * uiScale);
    }

    protected final int toScreenY(double virtualY) {
        return uiY + (int) Math.floor(virtualY * uiScale);
    }

    protected final int toScreenRight(double virtualX) {
        return uiX + (int) Math.ceil(virtualX * uiScale);
    }

    protected final int toScreenBottom(double virtualY) {
        return uiY + (int) Math.ceil(virtualY * uiScale);
    }

    protected final void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics instanceof UiCanvasGraphics) {
            graphics.enableScissor(left, top, right, bottom);
            return;
        }
        graphics.enableScissor(toScreenX(left), toScreenY(top), toScreenRight(right), toScreenBottom(bottom));
    }

    protected final void disableUiScissor(GuiGraphics graphics) {
        graphics.disableScissor();
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
            int x, int y, int width, Component message, Supplier<List<String>> dictionarySupplier, Component tooltip
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
        Button button = Button.builder(
                        text == null ? Component.empty() : text,
                        pressed -> { if (action != null) action.onPress(pressed); }
                )
                .bounds(x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT)
                .build();
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
        Button button = Button.builder(
                        text == null ? Component.empty() : text,
                        pressed -> { if (action != null) action.onPress(pressed); }
                )
                .bounds(x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT)
                .build();
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

    public final <W extends AbstractWidget> W addControl(W widget, Component tooltip) {
        if (widget == null) return null;
        addRenderableWidget(widget);
        if (tooltip != null && !tooltip.getString().isBlank()) registerWidgetTooltip(widget, tooltip);
        return widget;
    }

    public final <W extends AbstractWidget> W registerWidgetTooltip(W widget, Component tooltip) {
        if (widget == null) return null;
        if (tooltip == null || tooltip.getString().isBlank()) widgetTooltips.remove(widget);
        else widgetTooltips.put(widget, tooltip);
        return widget;
    }

    private boolean requestWidgetTooltip(double mouseX, double mouseY) {
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

    public final void showTooltip(List<? extends Component> lines) {
        overlays.tooltip(lines);
    }

    public final void showTooltip(Component component, int maxWidth) {
        overlays.tooltip(component, maxWidth);
    }

    public final void showTooltip(List<? extends Component> lines, int maxWidth) {
        overlays.tooltip(lines, maxWidth);
    }

    public final void showItemTooltip(ItemStack stack) {
        overlays.itemTooltip(stack);
    }

    protected final <S> void configureDraft(java.util.function.Supplier<S> capture, java.util.function.Consumer<S> restore) {
        draftSession.configureDraft(this, capture, restore, false);
    }

    protected final <S> void configureStandaloneDraft(java.util.function.Supplier<S> capture, java.util.function.Consumer<S> restore) {
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
        updateMetrics();
        int screenWidth = this.width;
        int screenHeight = this.height;
        this.width = uiWidth;
        this.height = uiHeight;
        try {
            super.init();
            widgetTooltips.clear();
            buildUi();
        } finally {
            this.width = screenWidth;
            this.height = screenHeight;
        }
    }

    protected abstract void buildUi();

    public final void rebuildUi() {
        updateMetrics();
        int screenWidth = this.width;
        int screenHeight = this.height;
        this.width = uiWidth;
        this.height = uiHeight;
        try {
            clearWidgets();
            widgetTooltips.clear();
            super.init();
            buildUi();
        } finally {
            this.width = screenWidth;
            this.height = screenHeight;
        }
    }

    private void updateMetrics() {
        GuiLayout.SafeArea safeArea = GuiLayout.SafeArea.of(width, height, safeMargin);
        GuiLayout.Metrics metrics = GuiLayout.measure(
                safeArea.width(),
                safeArea.height(),
                preferredWidth,
                preferredHeight
        );
        uiScale = Math.max(0.05f, metrics.fitScale());
        uiX = safeArea.left();
        uiY = safeArea.top();
        uiWidth = Math.max(1, (int) Math.floor(safeArea.width() / uiScale));
        uiHeight = Math.max(1, (int) Math.floor(safeArea.height() / uiScale));
    }

    @Override
    public final void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        overlays.beginFrame();
        int virtualMouseX = (int) Math.floor(toVirtualX(mouseX));
        int virtualMouseY = (int) Math.floor(toVirtualY(mouseY));

        GuiGraphics uiGraphics = new ContainerGuiGraphics(graphics);
        uiGraphics.pose().pushPose();
        uiGraphics.pose().translate(uiX, uiY, 0);
        uiGraphics.pose().scale(uiScale, uiScale, 1f);
        try {
            super.render(uiGraphics, virtualMouseX, virtualMouseY, partialTick);
            if (!overlays.blocksInput()) {
                requestContainerTooltips(uiGraphics, virtualMouseX, virtualMouseY, mouseX, mouseY);
            }
            renderUiForeground(uiGraphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            uiGraphics.pose().popPose();
        }

        renderScreenOverlay(graphics, virtualMouseX, virtualMouseY, mouseX, mouseY, partialTick);
        overlays.render(graphics, font, width, height, mouseX, mouseY);
    }

    private interface UiCanvasGraphics {
    }

    private final class ContainerGuiGraphics extends GuiGraphics implements UiCanvasGraphics {
        private ContainerGuiGraphics(GuiGraphics source) {
            super(KineticContainerScreen.this.minecraft, source.bufferSource());
        }

        @Override
        public void enableScissor(int left, int top, int right, int bottom) {
            super.enableScissor(
                    toScreenX(left),
                    toScreenY(top),
                    toScreenRight(right),
                    toScreenBottom(bottom)
            );
        }
    }

    protected void requestContainerTooltips(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY
    ) {
        if (requestWidgetTooltip(virtualMouseX, virtualMouseY)) return;
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            showItemTooltip(hoveredSlot.getItem());
        }
    }

    protected void renderUiForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderScreenOverlay(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY,
            float partialTick
    ) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (overlays.mouseClicked(mouseX, mouseY, button, width, height, font)) {
            return true;
        }
        boolean handled = super.mouseClicked(toVirtualX(mouseX), toVirtualY(mouseY), button);
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (overlays.blocksInput()) {
            return true;
        }
        boolean handled = super.mouseReleased(toVirtualX(mouseX), toVirtualY(mouseY), button);
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overlays.blocksInput()) return true;
        return super.mouseDragged(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button,
                dragX / uiScale,
                dragY / uiScale
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overlays.blocksInput()) return true;
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (GuiSession.routeSelectionListWheel(children(), virtualMouseX, virtualMouseY, delta)) return true;
        return super.mouseScrolled(virtualMouseX, virtualMouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!overlays.blocksInput()) super.mouseMoved(toVirtualX(mouseX), toVirtualY(mouseY));
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
        if (sharedDraft != null && sharedDraft.enabled()) {
            this.draftSession = sharedDraft;
        }
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
