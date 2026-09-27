package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/** One row description for a {@link KineticToggleList}. */
public record ToggleItem(Component label, Component tooltip, boolean value, boolean active) {
}
