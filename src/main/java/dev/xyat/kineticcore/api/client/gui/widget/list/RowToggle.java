package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/** One toggle column description for a {@link KineticMultiToggleList} row. */
public record RowToggle(
        Component onLabel,
        Component offLabel,
        Component tooltip,
        int width,
        boolean value,
        boolean active,
        boolean error
) {
    /**
     * Creates a new row toggle instance.
     */
    public RowToggle(Component onLabel, Component offLabel, Component tooltip, int width, boolean value) {
        this(onLabel, offLabel, tooltip, width, value, true, false);
    }
}
