package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/**
 * One toggle of a {@link KineticMultiToggleList} row.
 *
 * @param onLabel text while on
 * @param offLabel text while off
 * @param tooltip toggle tooltip, or {@code null}
 * @param width toggle width in page pixels, at least 1
 * @param value current state
 * @param active whether the toggle can be clicked
 * @param error whether the toggle is drawn in the error state
 */
public record RowToggle(
        Component onLabel,
        Component offLabel,
        Component tooltip,
        int width,
        boolean value,
        boolean active,
        boolean error
) {
    /** Creates an active toggle that is not in the error state. */
    public RowToggle(Component onLabel, Component offLabel, Component tooltip, int width, boolean value) {
        this(onLabel, offLabel, tooltip, width, value, true, false);
    }
}
