package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/** One trailing action description for a {@link KineticMultiActionList} row. */
public record RowAction(
        Component label,
        Component tooltip,
        int width,
        boolean active,
        boolean error
) {
    /**
     * Creates a new row action instance.
     */
    public RowAction(Component label, Component tooltip, int width) {
        this(label, tooltip, width, true, false);
    }
}
