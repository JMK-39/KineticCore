package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.internal.client.gui.GuiInputCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Controls injected into vanilla or third-party screens through {@code ScreenInitContext}. They are rendered after
 * the screen and receive mouse input before it, without joining the screen's listener list, so they never steal
 * keyboard focus from the host screen (for example the chat input).
 */
public final class ScreenOverlayControls {
    private static final Map<Screen, List<AbstractWidget>> CONTROLS = new WeakHashMap<>();
    private static AbstractWidget dragging;

    private ScreenOverlayControls() {
    }

    public static synchronized void add(Screen screen, AbstractWidget widget) {
        if (screen == null || widget == null) return;
        List<AbstractWidget> list = CONTROLS.computeIfAbsent(screen, ignored -> new ArrayList<>());
        if (!list.contains(widget)) list.add(widget);
    }

    public static synchronized void remove(Screen screen, AbstractWidget widget) {
        List<AbstractWidget> list = CONTROLS.get(screen);
        if (list != null) list.remove(widget);
        if (dragging == widget) dragging = null;
    }

    /** Called when a screen (re)initialises: controls are added again by the init handlers. */
    public static synchronized void clear(Screen screen) {
        List<AbstractWidget> list = CONTROLS.remove(screen);
        if (list != null && list.contains(dragging)) dragging = null;
    }

    private static synchronized List<AbstractWidget> snapshot(Screen screen) {
        List<AbstractWidget> list = CONTROLS.get(screen);
        return list == null || list.isEmpty() ? List.of() : List.copyOf(list);
    }

    public static void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (AbstractWidget widget : snapshot(screen)) {
            if (widget.visible) widget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    public static boolean mousePressed(Screen screen, double mouseX, double mouseY, int button) {
        List<AbstractWidget> list = snapshot(screen);
        for (int index = list.size() - 1; index >= 0; index--) {
            AbstractWidget widget = list.get(index);
            if (widget.visible && widget.active && widget.mouseClicked(mouseX, mouseY, button)) {
                dragging = widget;
                return true;
            }
        }
        return false;
    }

    public static boolean mouseReleased(Screen screen, double mouseX, double mouseY, int button) {
        AbstractWidget target = dragging;
        dragging = null;
        if (target == null || !snapshot(screen).contains(target)) return false;
        target.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    public static boolean mouseDragged(Screen screen, double mouseX, double mouseY, int button, double dragX, double dragY) {
        AbstractWidget target = dragging;
        if (target == null || !snapshot(screen).contains(target)) return false;
        return target.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static boolean mouseScrolled(Screen screen, double mouseX, double mouseY, double delta) {
        List<AbstractWidget> list = snapshot(screen);
        for (int index = list.size() - 1; index >= 0; index--) {
            AbstractWidget widget = list.get(index);
            if (widget.visible && widget.active && widget.isMouseOver(mouseX, mouseY)
                    && GuiInputCompat.mouseScrolled(widget, mouseX, mouseY, delta)) {
                return true;
            }
        }
        return false;
    }
}
