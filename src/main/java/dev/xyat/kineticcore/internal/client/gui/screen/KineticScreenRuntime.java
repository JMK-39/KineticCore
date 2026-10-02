package dev.xyat.kineticcore.internal.client.gui.screen;

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
    private final Supplier<Font> font;
    private final Supplier<Minecraft> minecraft;
    private final KineticCanvasTransform canvas;
    private final GuiOverlayRuntime overlays = new GuiOverlayRuntime();
    private final KineticScrollFrameRuntime.Frame scrollFrame = new KineticScrollFrameRuntime.Frame();
    private final KineticScreenControls controls;
    private final KineticScreenFocus focus;
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
        try {
            focus.synchronizeControlState();
            screen.renderBackground(graphics);
            overlays.beginFrame();
            previewWheelTargets.clear();
            content.render(graphics, mouseX, mouseY, partialTick);
            overlays.render(graphics, font.get(), screen.width, screen.height, mouseX, mouseY);
        } finally {
            KineticScrollFrameRuntime.exit(scrollFrame);
        }
    }

    /**
     * Draws {@code content} inside the scaled canvas, clipped to it. The renderer receives canvas graphics and the
     * mouse position in page coordinates.
     */
    public void renderInCanvas(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, FrameRenderer content) {
        int virtualMouseX = (int) Math.floor(canvas.toVirtualX(mouseX));
        int virtualMouseY = (int) Math.floor(canvas.toVirtualY(mouseY));
        GuiGraphics canvasGraphics = new CanvasGuiGraphics(minecraft.get(), graphics, canvas);
        canvasGraphics.pose().pushPose();
        canvasGraphics.pose().translate(canvas.x(), canvas.y(), 0);
        canvasGraphics.pose().scale(canvas.scale(), canvas.scale(), 1f);
        canvasGraphics.enableScissor(0, 0, canvas.width(), canvas.height());
        try {
            content.render(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            try {
                canvasGraphics.disableScissor();
            } finally {
                canvasGraphics.pose().popPose();
            }
        }
    }

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
        return controls.requestWidgetTooltip(mouseX, mouseY, hitFilter);
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
        if (canvas.isNative() || graphics instanceof CanvasGuiGraphics) {
            list.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        Minecraft client = minecraft.get();
        if (client == null) return;
        GuiGraphics proxy = new CanvasGuiGraphics(client, graphics, canvas);
        proxy.pose().pushPose();
        proxy.pose().translate(canvas.x(), canvas.y(), 0);
        proxy.pose().scale(canvas.scale(), canvas.scale(), 1f);
        try {
            list.render(proxy, mouseX, mouseY, partialTick);
        } finally {
            proxy.pose().popPose();
        }
    }

    /** Enables a scissor rectangle given in page coordinates. */
    public void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics == null) return;
        if (canvas.isNative() || graphics instanceof CanvasGuiGraphics) {
            KineticRenderRuntime.enableScissor(graphics, left, top, right, bottom);
            return;
        }
        KineticRenderRuntime.enableScissor(graphics,
                canvas.toScreenX(left), canvas.toScreenY(top), canvas.toScreenRight(right), canvas.toScreenBottom(bottom));
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers, KeyHandler business, KeyHandler fallback) {
        focus.synchronizeControlState();
        if (overlays.keyPressed(keyCode)) return true;
        if (controls.handleAutoCompleteKey(keyCode, scanCode, modifiers)) return true;
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
        if (overlays.blocksInput()) return true;
        return host.handle(codePoint, modifiers);
    }
}
