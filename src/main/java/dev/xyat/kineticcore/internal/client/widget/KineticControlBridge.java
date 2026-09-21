package dev.xyat.kineticcore.internal.client.widget;

import dev.xyat.kineticcore.api.client.widget.KineticControl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

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
        widget(control).render(graphics, mouseX, mouseY, partialTick);
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
        return widget(control).mouseClicked(mouseX, mouseY, button);
    }

    public static boolean mouseDragged(
            KineticControl control,
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        return widget(control).mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static boolean mouseReleased(KineticControl control, double mouseX, double mouseY, int button) {
        return widget(control).mouseReleased(mouseX, mouseY, button);
    }

    public static boolean mouseScrolled(KineticControl control, double mouseX, double mouseY, double delta) {
        return widget(control).mouseScrolled(mouseX, mouseY, delta);
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

    public static void setTooltip(KineticControl control, Component tooltip) {
        widget(control).setTooltip(tooltip == null || tooltip.getString().isBlank() ? null : Tooltip.create(tooltip));
    }
}
