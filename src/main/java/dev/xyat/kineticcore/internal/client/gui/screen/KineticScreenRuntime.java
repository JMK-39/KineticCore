package dev.xyat.kineticcore.internal.client.gui.screen;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;
import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.widget.render.KineticEntityPreview.EntityPreviewRenderer;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.KineticScroll.SmoothSelectionList;
import dev.xyat.kineticcore.internal.client.overlay.GuiOverlayRuntime;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.screen.KineticScreenControls;
import dev.xyat.kineticcore.internal.client.screen.KineticScreenFocus;
import dev.xyat.kineticcore.internal.client.widget.KineticScrollFrameRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 三种 Kinetic 界面宿主共享的唯一运行时：Overlay、控件注册、焦点、滚动帧、坐标换算，以及输入与渲染管线。
 * <p>
 * The single runtime shared by {@link KineticScreen}, {@link KineticContainerScreen} and {@link KineticNativeScreen}.
 * It owns overlays, control registration, focus, the scroll frame and the page coordinate transform, and runs the
 * common input and render pipeline. A host screen only supplies what really differs: its vanilla base class calls
 * ({@code super.mouseClicked}, ...), its business hook names, and how it draws a frame.
 */
public final class KineticScreenRuntime {
    /** Screen-specific tail of a mouse button event, in page coordinates. */
    @FunctionalInterface
    public interface MouseButtonHandler {
        /** Returns whether the event was consumed. */
        boolean handle(double mouseX, double mouseY, int button);
    }

    /** Screen-specific tail of a mouse drag, in page coordinates. */
    @FunctionalInterface
    public interface MouseDragHandler {
        /** Returns whether the event was consumed. */
        boolean handle(double mouseX, double mouseY, int button, double dragX, double dragY);
    }

    /** Screen-specific tail of a wheel event, in page coordinates. */
    @FunctionalInterface
    public interface MouseScrollHandler {
        /** Returns whether the event was consumed. */
        boolean handle(double mouseX, double mouseY, double delta);
    }

    /** Screen-specific tail of a mouse move, in page coordinates. */
    @FunctionalInterface
    public interface MouseMoveHandler {
        /** Handles the move. */
        void handle(double mouseX, double mouseY);
    }

    /** Screen-specific key handler. */
    @FunctionalInterface
    public interface KeyHandler {
        /** Returns whether the event was consumed. */
        boolean handle(int keyCode, int scanCode, int modifiers);
    }

    /** Screen-specific character handler. */
    @FunctionalInterface
    public interface CharHandler {
        /** Returns whether the event was consumed. */
        boolean handle(char codePoint, int modifiers);
    }

    /** Draws one frame or one part of a frame. */
    @FunctionalInterface
    public interface FrameRenderer {
        /** Renders with the supplied graphics and mouse position. */
        void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    }

    private record PreviewWheelTarget(EntityPreviewRenderer renderer, String key,
                                      int left, int top, int right, int bottom) {
        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    private final Screen screen;
    private final KineticScreenHost hostScreen;
    private final Supplier<Font> font;
    private final Supplier<Minecraft> minecraft;
    private final KineticCanvasTransform canvas;
    private final GuiOverlayRuntime overlays = new GuiOverlayRuntime();
    private final KineticScrollFrameRuntime.Frame scrollFrame = new KineticScrollFrameRuntime.Frame();
    private final KineticScreenControls controls;
    private final KineticScreenFocus focus;
    private final dev.xyat.kineticcore.internal.client.screen.KineticDeleteAction deletion = new dev.xyat.kineticcore.internal.client.screen.KineticDeleteAction();
    private final List<PreviewWheelTarget> previewWheelTargets = new ArrayList<>();

    /**
     * Creates the runtime of one host screen. The widget callbacks are the host's protected vanilla registration
     * methods, passed in because only the screen itself may call them.
     */
    public <S extends Screen & KineticScreenHost> KineticScreenRuntime(
            S screen,
            KineticCanvasTransform canvas,
            Supplier<Font> font,
            Supplier<Minecraft> minecraft,
            Consumer<AbstractWidget> addRenderable,
            Consumer<ObjectSelectionList<?>> addEventList,
            Consumer<AbstractWidget> removeWidget
    ) {
        KineticClientRuntimeImpl.initialize();
        this.screen = screen;
        this.hostScreen = screen;
        this.canvas = canvas;
        this.font = font;
        this.minecraft = minecraft;
        this.controls = new KineticScreenControls(
                font, overlays, addRenderable, addEventList, removeWidget, screen::openContextMenu
        );
        this.focus = new KineticScreenFocus(screen);
    }

    /** Overlay layer: menus, dialogs, tooltips. */
    public GuiOverlayRuntime overlays() {
        return overlays;
    }

    /** Registered controls, tooltips and autocomplete popups. */
    public KineticScreenControls controls() {
        return controls;
    }

    /** Screen and control focus bookkeeping. */
    public KineticScreenFocus focus() {
        return focus;
    }

    /** Page coordinate transform. */
    public KineticCanvasTransform canvas() {
        return canvas;
    }

    /** Registers a preview's page hit area for the current frame; Ctrl + wheel over it goes to the preview first. */
    public void registerPreviewWheelTarget(EntityPreviewRenderer renderer, String key, int x, int y, int width, int height) {
        if (renderer == null || key == null || width <= 0 || height <= 0) return;
        previewWheelTargets.add(new PreviewWheelTarget(renderer, key, x, y, x + width, y + height));
    }

    // ---- frame ---------------------------------------------------------------------------------------------------

    /**
     * Runs one frame: binds the scroll frame, synchronizes focus, draws the vanilla background, lets the host draw
     * its content, then draws the overlay layer on top in screen coordinates.
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, FrameRenderer content) {
        KineticScrollFrameRuntime.enter(scrollFrame);
        KineticScreenRuntime previous = rendering;
        rendering = this;
        try {
            focus.synchronizeControlState();
            if (net.minecraft.client.Minecraft.getInstance().level != null) {
                // In a world the game stays visible and live around the canvas, only dimmed (no blur, the same on
                // every version); the canvas panels themselves are opaque.
                graphics.fillGradient(0, 0, screen.width, screen.height, 0xC0101010, 0xD0101010);
            } else {
                //? if >=1.20.2 {
                /*hostScreen.renderVanillaBackground(graphics, mouseX, mouseY, partialTick);
                *///?} else {
                screen.renderBackground(graphics);
                //?}
            }
            // A page that drew the theme shadow last frame gets it across the whole screen, so the dimmed area is
            // not a canvas-sized box with the brighter margins around it.
            pageShadowOnScreen = pageShadowRequested;
            pageShadowRequested = false;
            if (pageShadowOnScreen) GuiTheme.shadowFill(graphics, screen.width, screen.height);
            overlays.beginFrame();
            previewWheelTargets.clear();
            content.render(graphics, mouseX, mouseY, partialTick);
            overlays.render(graphics, font.get(), screen.width, screen.height, mouseX, mouseY);
        } finally {
            rendering = previous;
            KineticScrollFrameRuntime.exit(scrollFrame);
        }
    }

    // The runtime whose frame is being drawn, and the page shadow state that frame uses.
    private static KineticScreenRuntime rendering;
    private int pageRenderDepth;
    private boolean pageShadowRequested;
    private boolean pageShadowOnScreen;

    /**
     * Called by the theme shadow. Inside a canvas page it records the request and returns whether the full-screen
     * shadow already covers the page this frame, in which case the canvas-sized fill is skipped.
     */
    public static boolean pageShadowCoveredByScreen() {
        KineticScreenRuntime runtime = rendering;
        if (runtime == null || runtime.pageRenderDepth <= 0) return false;
        runtime.pageShadowRequested = true;
        return runtime.pageShadowOnScreen;
    }

    /**
     * Draws {@code content} inside the scaled canvas, clipped to it. The renderer receives canvas graphics and the
     * mouse position in page coordinates.
     */
    // When this screen opened: a page eases in from 96 % of its size during the first 180 ms.
    private final long openedAt = System.nanoTime();

    private void applyOpeningScale(GuiGraphics graphics) {
        double t = Math.min(1D, (System.nanoTime() - openedAt) / 180_000_000D);
        if (t >= 1D) return;
        double inverse = 1D - t;
        float scale = (float) (0.96D + 0.04D * (1D - inverse * inverse * inverse));
        float centreX = canvas.width() / 2F, centreY = canvas.height() / 2F;
        VanillaGuiDraw.translate(graphics, centreX, centreY, 0);
        VanillaGuiDraw.scale(graphics, scale, scale, 1F);
        VanillaGuiDraw.translate(graphics, -centreX, -centreY, 0);
    }

    public void renderInCanvas(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, FrameRenderer content) {
        int virtualMouseX = (int) Math.floor(canvas.toVirtualX(mouseX));
        int virtualMouseY = (int) Math.floor(canvas.toVirtualY(mouseY));
        //? if >=26.1 {
        /*// 26.1 maps scissor rectangles through the pose, so page coordinates work as they are.
        VanillaGuiDraw.push(graphics);
        VanillaGuiDraw.translate(graphics, canvas.x(), canvas.y(), 0);
        VanillaGuiDraw.scale(graphics, canvas.scale(), canvas.scale(), 1f);
        applyOpeningScale(graphics);
        graphics.enableScissor(0, 0, canvas.width(), canvas.height());
        canvasDepth++;
        pageRenderDepth++;
        try {
            content.render(graphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            canvasDepth--;
            pageRenderDepth--;
            try {
                graphics.disableScissor();
            } finally {
                VanillaGuiDraw.pop(graphics);
            }
        }
    }

    // Number of renderInCanvas calls on the stack; drawing inside them is in page coordinates.
    private int canvasDepth;

    private boolean drawsInPageSpace(GuiGraphics graphics) {
        return canvasDepth > 0;
    }
    *///?} else {
        GuiGraphics canvasGraphics = new CanvasGuiGraphics(minecraft.get(), graphics, canvas);
        VanillaGuiDraw.push(canvasGraphics);
        VanillaGuiDraw.translate(canvasGraphics, canvas.x(), canvas.y(), 0);
        VanillaGuiDraw.scale(canvasGraphics, canvas.scale(), canvas.scale(), 1f);
        applyOpeningScale(canvasGraphics);
        canvasGraphics.enableScissor(0, 0, canvas.width(), canvas.height());
        pageRenderDepth++;
        try {
            content.render(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            pageRenderDepth--;
            try {
                canvasGraphics.disableScissor();
            } finally {
                VanillaGuiDraw.pop(canvasGraphics);
            }
        }
    }

    private boolean drawsInPageSpace(GuiGraphics graphics) {
        return graphics instanceof CanvasGuiGraphics;
    }
    //?}

    /**
     * Returns whether business tooltips may be requested this frame. A hovered scrollbar hint wins over everything
     * else and is requested here as a side effect; scrollbars often sit inside custom controls whose empty tooltip
     * must not hide it.
     */
    public boolean businessTooltipsAllowed(boolean mouseInsidePage) {
        return mouseInsidePage
                && !overlays.blocksInput()
                && !controls.hasOpenAutoCompletePopup()
                && !requestScrollbarHint();
    }

    /** Requests the tooltip of the hovered registered control, if any passes {@code hitFilter}. */
    public boolean requestWidgetTooltip(double mouseX, double mouseY, Predicate<AbstractWidget> hitFilter) {
        boolean handled = controls.requestWidgetTooltip(mouseX, mouseY, hitFilter);
        AbstractWidget widget = controls.lastHitWidget();
        if (widget == null || !overlays.hasTooltip()) return handled;
        if (widget instanceof dev.xyat.kineticcore.internal.client.gui.widget.TooltipAnchorSource source) {
            // A list or grid: beside the hovered row or cell (the page may have shown the row's tooltip itself).
            int[] row = source.tooltipAnchor(mouseX, mouseY);
            if (row != null) anchorTooltip(row[0], row[1], row[2], row[3], false);
        } else if (widget.getHeight() <= 24) {
            // A button or field: below or above it.
            anchorTooltip(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), true);
        }
        return handled;
    }

    /**
     * Places this frame's tooltip beside an area given in page coordinates: below or above it for a control, beside
     * it for a row or cell. Call it after the tooltip is requested.
     */
    public void anchorTooltip(int x, int y, int width, int height, boolean control) {
        overlays.tooltipAnchor(canvas.toScreenX(x), canvas.toScreenY(y), canvas.toScreenRight(x + width),
                canvas.toScreenBottom(y + height), control);
    }

    private boolean requestScrollbarHint() {
        Component hint = scrollFrame.hint();
        if (hint == null || hint.getString().isBlank()) return false;
        overlays.tooltip(hint, 320);
        return true;
    }

    /** Renders a smooth selection list with this host's page transform. */
    public void renderSmoothSelectionList(SmoothSelectionList<?> list, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (list == null || graphics == null) return;
        if (canvas.isNative() || drawsInPageSpace(graphics)) {
            VanillaGuiDraw.render(list, graphics, mouseX, mouseY, partialTick);
            return;
        }
        Minecraft client = minecraft.get();
        if (client == null) return;
        //? if >=26.1 {
        /*GuiGraphics proxy = graphics;
        *///?} else {
        GuiGraphics proxy = new CanvasGuiGraphics(client, graphics, canvas);
        //?}
        VanillaGuiDraw.push(proxy);
        VanillaGuiDraw.translate(proxy, canvas.x(), canvas.y(), 0);
        VanillaGuiDraw.scale(proxy, canvas.scale(), canvas.scale(), 1f);
        //? if >=26.1
        /*canvasDepth++;*/
        try {
            VanillaGuiDraw.render(list, proxy, mouseX, mouseY, partialTick);
        } finally {
            //? if >=26.1
            /*canvasDepth--;*/
            VanillaGuiDraw.pop(proxy);
        }
    }

    /** Enables a scissor rectangle given in page coordinates. */
    public void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics == null) return;
        if (canvas.isNative() || drawsInPageSpace(graphics)) {
            KineticRenderRuntime.enableScissor(graphics, left, top, right, bottom);
            return;
        }
        int screenLeft = canvas.toScreenX(left);
        int screenTop = canvas.toScreenY(top);
        int screenRight = canvas.toScreenRight(right);
        int screenBottom = canvas.toScreenBottom(bottom);
        KineticRenderRuntime.enableScissor(graphics, screenLeft, screenTop, screenRight, screenBottom);
    }

    /** Ends a scissor rectangle started with {@link #enableUiScissor}. */
    public void disableUiScissor(GuiGraphics graphics) {
        if (graphics == null) return;
        KineticRenderRuntime.disableScissor(graphics);
    }

    // ---- input ---------------------------------------------------------------------------------------------------

    /** Runs per-tick control work around the host's business tick. */
    public void tick(Runnable businessTick) {
        controls.tickManagedControls();
        businessTick.run();
        focus.synchronizeControlState();
    }

    /**
     * Mouse press: overlays first, then autocomplete popups and scrollbar middle-click, then the host. A press
     * outside the page or one nothing consumed clears control focus.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button, MouseButtonHandler host) {
        if (overlays.mouseClicked(mouseX, mouseY, button, screen.width, screen.height, font.get())) return true;
        double virtualMouseX = canvas.toVirtualX(mouseX);
        double virtualMouseY = canvas.toVirtualY(mouseY);
        controls.clearAutoCompleteFocusOutside(virtualMouseX, virtualMouseY);
        if (!canvas.contains(mouseX, mouseY)) {
            focus.clearControlFocus();
            return false;
        }
        if (controls.handleAutoCompleteClick(virtualMouseX, virtualMouseY, button)) return true;
        if (scrollFrame.middleClick(button)) return true;
        boolean handled = host.handle(virtualMouseX, virtualMouseY, button);
        if (!handled) focus.clearControlFocus();
        return handled;
    }

    /** Mouse release: overlays and autocomplete first, then the host. */
    public boolean mouseReleased(double mouseX, double mouseY, int button, MouseButtonHandler host) {
        overlays.mouseReleased();
        if (overlays.blocksInput()) return true;
        if (controls.handleAutoCompleteReleased(button)) return true;
        return host.handle(canvas.toVirtualX(mouseX), canvas.toVirtualY(mouseY), button);
    }

    /** Mouse drag: overlays and autocomplete first, then the host with the drag delta in page units. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, MouseDragHandler host) {
        if (overlays.mouseDragged(mouseX, mouseY, screen.width, screen.height, font.get())) return true;
        if (overlays.blocksInput()) return true;
        double virtualMouseX = canvas.toVirtualX(mouseX);
        double virtualMouseY = canvas.toVirtualY(mouseY);
        if (controls.handleAutoCompleteDragged(virtualMouseX, virtualMouseY)) return true;
        return host.handle(virtualMouseX, virtualMouseY, button, dragX / canvas.scale(), dragY / canvas.scale());
    }

    /** Wheel: overlays, then Ctrl + wheel previews, then autocomplete popups, then the host. */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta, MouseScrollHandler host) {
        if (overlays.mouseScrolled(mouseX, mouseY, delta, screen.width, screen.height, font.get())) return true;
        if (overlays.blocksInput()) return true;
        double virtualMouseX = canvas.toVirtualX(mouseX);
        double virtualMouseY = canvas.toVirtualY(mouseY);
        if (delta != 0D) {
            for (int index = previewWheelTargets.size() - 1; index >= 0; index--) {
                PreviewWheelTarget target = previewWheelTargets.get(index);
                if (target.contains(virtualMouseX, virtualMouseY)
                        && target.renderer().handleControlWheel(target.key(), true, delta)) {
                    return true;
                }
            }
        }
        if (controls.handleAutoCompleteScroll(virtualMouseX, virtualMouseY, delta)) return true;
        return host.handle(virtualMouseX, virtualMouseY, delta);
    }

    /** Mouse move: ignored while an overlay blocks input. */
    public void mouseMoved(double mouseX, double mouseY, MouseMoveHandler host) {
        if (overlays.blocksInput()) return;
        host.handle(canvas.toVirtualX(mouseX), canvas.toVirtualY(mouseY));
    }

    /**
     * Key press: overlays, autocomplete, then the host's business hook; Escape then closes through the host's close
     * hook before the vanilla fallback runs.
     */
    public void bindDeleteAction(java.util.function.BooleanSupplier available, Runnable action) {
        deletion.bind(available, action);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers, KeyHandler business, KeyHandler fallback) {
        focus.synchronizeControlState();
        if (overlays.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (controls.handleAutoCompleteKey(keyCode, scanCode, modifiers)) return true;
        var focused = screen.getFocused();
        boolean editingText = focused instanceof net.minecraft.client.gui.components.EditBox
                || focused instanceof dev.xyat.kineticcore.api.client.gui.widget.KineticTextField
                || focused instanceof net.minecraft.client.gui.components.MultiLineEditBox;
        if (deletion.handle(keyCode, editingText)) return true;
        // Enter belongs to modal confirmation and text editing/completion, never a focused row or action button.
        if (!editingText && (KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.ENTER, keyCode)
                || KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.KP_ENTER, keyCode))) return true;
        if (business.handle(keyCode, scanCode, modifiers)) return true;
        if (KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.ESCAPE, keyCode)) {
            screen.onClose();
            return true;
        }
        return fallback.handle(keyCode, scanCode, modifiers);
    }

    /** Key release: swallowed while an overlay blocks input. */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers, KeyHandler host) {
        focus.synchronizeControlState();
        if (overlays.blocksInput()) return true;
        return host.handle(keyCode, scanCode, modifiers);
    }

    /** Character input: swallowed while an overlay blocks input. */
    public boolean charTyped(char codePoint, int modifiers, CharHandler host) {
        focus.synchronizeControlState();
        if (overlays.charTyped(codePoint, modifiers)) return true;
        if (overlays.blocksInput()) return true;
        return host.handle(codePoint, modifiers);
    }
}
