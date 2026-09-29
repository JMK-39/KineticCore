package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/**
 * One row of a {@link KineticToggleList}.
 *
 * @param label toggle text
 * @param tooltip row tooltip, or {@code null}
 * @param value current state
 * @param active whether the toggle can be clicked
 */
public record ToggleItem(Component label, Component tooltip, boolean value, boolean active) {
}
