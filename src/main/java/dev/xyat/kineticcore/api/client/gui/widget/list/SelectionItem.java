package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/**
 * One row of a {@link KineticSelectionList}.
 *
 * @param label main row text
 * @param secondaryLabel text appended after the label, or {@code null}
 * @param tooltip row tooltip, or {@code null}
 * @param active whether the row can be selected; inactive rows are dimmed
 * @param error whether the row is drawn in the error state
 */
public record SelectionItem(
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean error
) {
}
