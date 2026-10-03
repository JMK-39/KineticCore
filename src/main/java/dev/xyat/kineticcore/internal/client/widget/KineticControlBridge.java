package dev.xyat.kineticcore.internal.client.widget;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import dev.xyat.kineticcore.internal.client.gui.GuiInputCompat;
import dev.xyat.kineticcore.internal.client.screen.KineticScreenControls;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;

/** Internal bridge from the public Kinetic control contract to Minecraft widget ownership. */
public final class KineticControlBridge {
    private KineticControlBridge() {
    }

    public static AbstractWidget widget(KineticControl control) {
        if (control instanceof AbstractWidget widget) return widget;
        throw new IllegalArgumentException("Kinetic control is not backed by a Minecraft widget: " + control);
    }

    public static int getX(KineticControl control) {
        return widget(control).getX();
    }

    public static int getY(KineticControl control) {
        return widget(control).getY();
    }

    public static int getWidth(KineticControl control) {
        return widget(control).getWidth();
    }

    public static int getHeight(KineticControl control) {
        return widget(control).getHeight();
    }

    public static void setX(KineticControl control, int x) {
        widget(control).setX(x);
    }

    public static void setWidth(KineticControl control, int width) {
        widget(control).setWidth(width);
    }

    public static void setY(KineticControl control, int y) {
        widget(control).setY(y);
    }


    public static void render(KineticControl control, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        VanillaGuiDraw.render(widget(control), graphics, mouseX, mouseY, partialTick);
    }

    public static boolean contains(KineticControl control, double mouseX, double mouseY) {
        AbstractWidget widget = widget(control);
        return mouseX >= widget.getX()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY >= widget.getY()
                && mouseY < widget.getY() + widget.getHeight();
    }

    public static boolean isMouseOver(KineticControl control, double mouseX, double mouseY) {
        return widget(control).isMouseOver(mouseX, mouseY);
    }

    public static boolean mouseClicked(KineticControl control, double mouseX, double mouseY, int button) {
        return GuiInputCompat.mouseClicked(widget(control), mouseX, mouseY, button);
    }

    public static boolean mouseDragged(
            KineticControl control,
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        return GuiInputCompat.mouseDragged(widget(control), mouseX, mouseY, button, dragX, dragY);
    }

    public static boolean mouseReleased(KineticControl control, double mouseX, double mouseY, int button) {
        return GuiInputCompat.mouseReleased(widget(control), mouseX, mouseY, button);
    }

    public static boolean mouseScrolled(KineticControl control, double mouseX, double mouseY, double delta) {
        return GuiInputCompat.mouseScrolled(widget(control), mouseX, mouseY, delta);
    }

    public static void setEnabled(KineticControl control, boolean enabled) {
        widget(control).active = enabled;
    }

    public static void setEnabled(AbstractWidget widget, boolean enabled) {
        if (widget != null) widget.active = enabled;
    }

    public static void setVisible(KineticControl control, boolean visible) {
        widget(control).visible = visible;
    }

    public static void setVisible(AbstractWidget widget, boolean visible) {
        if (widget != null) widget.visible = visible;
    }

    public static boolean isEnabled(KineticControl control) {
        return widget(control).active;
    }

    public static boolean isVisible(KineticControl control) {
        return widget(control).visible;
    }

    public static void setText(KineticControl control, Component text) {
        widget(control).setMessage(text == null ? Component.empty() : text);
    }

    public static Component text(KineticControl control) {
        return widget(control).getMessage();
    }

    public static boolean isHovered(KineticControl control) {
        return widget(control).isHovered();
    }

    public static boolean isFocused(KineticControl control) {
        return widget(control).isFocused();
    }

    /**
     * 已注册到 Screen 提示通道的控件 -> 所属 Screen 控件表（弱引用，避免泄漏）。这些控件的提示由 Screen 以 Kinetic
     * 样式显示，原版 Tooltip 被抑制，避免双提示；setTooltip 会改写 Screen 中的注册。
     * Screen-registered widgets mapped (weakly) to the owning screen's control registry. Their tooltips are shown
     * by the screen in Kinetic style; the vanilla tooltip is suppressed and setTooltip updates the registration.
     */
    private static final Map<AbstractWidget, WeakReference<KineticScreenControls>> SCREEN_TOOLTIP_WIDGETS =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void markScreenTooltipWidget(AbstractWidget widget, KineticScreenControls owner) {
        if (widget == null) return;
        SCREEN_TOOLTIP_WIDGETS.put(widget, new WeakReference<>(owner));
        widget.setTooltip(null);
    }

    public static void unmarkScreenTooltipWidget(AbstractWidget widget) {
        if (widget == null) return;
        SCREEN_TOOLTIP_WIDGETS.remove(widget);
    }

    private static KineticScreenControls owner(AbstractWidget widget) {
        WeakReference<KineticScreenControls> reference = SCREEN_TOOLTIP_WIDGETS.get(widget);
        return reference == null ? null : reference.get();
    }

    /**
     * 列表类控件每帧刷新的悬停提示：注册在 Screen 中时交给 Screen 显示，独立使用时走原版 Tooltip。
     * Per-frame hover tooltip for composite widgets: screen-registered widgets defer to the screen,
     * detached widgets keep the vanilla tooltip.
     */
    public static void setHoverTooltip(KineticControl control, Component tooltip) {
        AbstractWidget widget = widget(control);
        if (SCREEN_TOOLTIP_WIDGETS.containsKey(widget)) {
            widget.setTooltip(null);
            return;
        }
        setVanillaTooltip(widget, tooltip);
    }

    public static void setTooltip(KineticControl control, Component tooltip) {
        AbstractWidget widget = widget(control);
        KineticScreenControls owner = owner(widget);
        if (owner != null) {
            owner.registerWidgetTooltip(widget, tooltip);
            return;
        }
        setVanillaTooltip(widget, tooltip);
    }

    public static void setTooltip(KineticControl control, Supplier<Component> tooltip) {
        AbstractWidget widget = widget(control);
        KineticScreenControls owner = owner(widget);
        if (owner != null) {
            owner.registerDynamicWidgetTooltip(widget, tooltip);
            return;
        }
        setVanillaTooltip(widget, tooltip == null ? null : tooltip.get());
    }

    private static void setVanillaTooltip(AbstractWidget widget, Component tooltip) {
        widget.setTooltip(tooltip == null || tooltip.getString().isBlank() ? null : Tooltip.create(tooltip));
    }
}
