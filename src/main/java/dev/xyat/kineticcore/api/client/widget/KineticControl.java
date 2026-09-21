package dev.xyat.kineticcore.api.client.widget;

import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import net.minecraft.network.chat.Component;

/**
 * Public state, geometry, and input contract shared by standard Kinetic controls.
 * Addons use this contract instead of inherited Minecraft widget implementation details.
 */
public interface KineticControl {
    /** Returns the current left position. */
    default int getX() { return KineticControlBridge.getX(this); }

    /** Returns the current top position. */
    default int getY() { return KineticControlBridge.getY(this); }

    /** Returns the current width. */
    default int getWidth() { return KineticControlBridge.getWidth(this); }

    /** Returns the current height. */
    default int getHeight() { return KineticControlBridge.getHeight(this); }

    /** Moves the control horizontally. */
    default void setX(int x) { KineticControlBridge.setX(this, x); }

    /** Changes the control width while keeping its current position. */
    default void setWidth(int width) { KineticControlBridge.setWidth(this, width); }

    /** Moves the control vertically. */
    default void setY(int y) { KineticControlBridge.setY(this, y); }

    /** Returns whether the point is inside the control bounds, independent of enabled/visible state. */
    default boolean contains(double mouseX, double mouseY) {
        return KineticControlBridge.contains(this, mouseX, mouseY);
    }

    /** Returns whether the pointer is currently over this control using the underlying widget interaction state. */
    default boolean isMouseOver(double mouseX, double mouseY) {
        return KineticControlBridge.isMouseOver(this, mouseX, mouseY);
    }

    /** Routes a mouse click through this API-owned control. */
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return KineticControlBridge.mouseClicked(this, mouseX, mouseY, button);
    }

    /** Routes a mouse drag through this API-owned control. */
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return KineticControlBridge.mouseDragged(this, mouseX, mouseY, button, dragX, dragY);
    }

    /** Routes a mouse release through this API-owned control. */
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return KineticControlBridge.mouseReleased(this, mouseX, mouseY, button);
    }

    /** Routes a mouse-wheel event through this API-owned control. */
    default boolean scrollControl(double mouseX, double mouseY, double delta) {
        return KineticControlBridge.mouseScrolled(this, mouseX, mouseY, delta);
    }

    /** Enables or disables interaction for this control. */
    default void setEnabled(boolean enabled) { KineticControlBridge.setEnabled(this, enabled); }

    /** Shows or hides this control. */
    default void setVisible(boolean visible) { KineticControlBridge.setVisible(this, visible); }

    /** Updates the standard tooltip for this control; {@code null} or blank clears it. */
    default void setTooltip(Component tooltip) { KineticControlBridge.setTooltip(this, tooltip); }

    /** Updates the visible/narrated label of this control through the stable Kinetic contract. */
    default void setText(Component text) { KineticControlBridge.setText(this, text); }

    /** Returns the current label owned by this control. */
    default Component text() { return KineticControlBridge.text(this); }

    /** Returns whether this control currently accepts interaction. */
    default boolean isEnabled() { return KineticControlBridge.isEnabled(this); }

    /** Returns whether this control is currently visible. */
    default boolean isVisible() { return KineticControlBridge.isVisible(this); }
}
