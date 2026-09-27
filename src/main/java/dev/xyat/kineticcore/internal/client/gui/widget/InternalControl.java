package dev.xyat.kineticcore.internal.client.gui.widget;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * Internal bridge contract implemented by every widget-backed Kinetic control. It implements the public
 * {@link KineticControl} surface on top of the underlying Minecraft widget and keeps the raw input-routing
 * helpers that only KineticCore's own screens use.
 */
public interface InternalControl extends KineticControl {
    @Override
    default int getX() { return KineticControlBridge.getX(this); }

    @Override
    default int getY() { return KineticControlBridge.getY(this); }

    @Override
    default int getWidth() { return KineticControlBridge.getWidth(this); }

    @Override
    default int getHeight() { return KineticControlBridge.getHeight(this); }

    @Override
    default void setX(int x) { KineticControlBridge.setX(this, x); }

    @Override
    default void setWidth(int width) { KineticControlBridge.setWidth(this, width); }

    @Override
    default void setY(int y) { KineticControlBridge.setY(this, y); }

    @Override
    default boolean contains(double mouseX, double mouseY) {
        return KineticControlBridge.contains(this, mouseX, mouseY);
    }

    @Override
    default boolean isHovered() { return KineticControlBridge.isHovered(this); }

    @Override
    default boolean isFocused() { return KineticControlBridge.isFocused(this); }

    /** Returns whether the pointer is currently over this control using the underlying widget interaction state. */
    default boolean isMouseOver(double mouseX, double mouseY) {
        return KineticControlBridge.isMouseOver(this, mouseX, mouseY);
    }

    /** Routes a mouse click through this control. */
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return KineticControlBridge.mouseClicked(this, mouseX, mouseY, button);
    }

    /** Routes a mouse drag through this control. */
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return KineticControlBridge.mouseDragged(this, mouseX, mouseY, button, dragX, dragY);
    }

    /** Routes a mouse release through this control. */
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return KineticControlBridge.mouseReleased(this, mouseX, mouseY, button);
    }

    /** Routes a mouse-wheel event through this control. */
    default boolean scrollControl(double mouseX, double mouseY, double delta) {
        return KineticControlBridge.mouseScrolled(this, mouseX, mouseY, delta);
    }

    @Override
    default void setEnabled(boolean enabled) { KineticControlBridge.setEnabled(this, enabled); }

    @Override
    default void setVisible(boolean visible) { KineticControlBridge.setVisible(this, visible); }

    @Override
    default void setTooltip(Component tooltip) { KineticControlBridge.setTooltip(this, tooltip); }

    @Override
    default void setTooltip(Supplier<Component> tooltip) { KineticControlBridge.setTooltip(this, tooltip); }

    /** Updates the visible/narrated label of this control. */
    default void setText(Component text) { KineticControlBridge.setText(this, text); }

    /** Returns the current label owned by this control. */
    default Component text() { return KineticControlBridge.text(this); }

    @Override
    default boolean isEnabled() { return KineticControlBridge.isEnabled(this); }

    @Override
    default boolean isVisible() { return KineticControlBridge.isVisible(this); }
}
