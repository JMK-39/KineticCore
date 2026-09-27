package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

import java.util.List;

/** One row description for a {@link KineticMultiToggleList}. */
public record MultiToggleItem(
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean marked,
        boolean error,
        List<RowToggle> toggles
) {
    /**
     * Creates a new multi toggle item instance.
     */
    public MultiToggleItem(Component label, Component tooltip, List<RowToggle> toggles) {
        this(label, null, tooltip, true, false, false, toggles);
    }
}
