package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/**
 * One trailing button of a {@link KineticMultiActionList} row.
 *
 * @param label button text
 * @param tooltip button tooltip, or {@code null}
 * @param width button width in page pixels, at least 1
 * @param active whether the button can be clicked
 * @param error whether the button is drawn in the error state
 */
public record RowAction(
        Component label,
        Component tooltip,
        int width,
        boolean active,
        boolean error
) {
    /** Creates an active button that is not in the error state. */
    public RowAction(Component label, Component tooltip, int width) {
        this(label, tooltip, width, true, false);
    }
}
