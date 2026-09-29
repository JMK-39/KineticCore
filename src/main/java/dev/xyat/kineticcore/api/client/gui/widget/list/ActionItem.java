package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/**
 * One row of a {@link KineticActionList}.
 *
 * @param label main row text
 * @param secondaryLabel text appended after the label, or {@code null}
 * @param tooltip row tooltip, or {@code null}
 * @param active whether the row can be selected; inactive rows are dimmed
 * @param error whether the row is drawn in the error state
 * @param actionLabel trailing button text
 * @param actionTooltip trailing button tooltip, or {@code null}
 * @param actionActive whether the trailing button can be clicked
 * @param actionError whether the trailing button is drawn in the error state
 */
public record ActionItem(
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean error,
        Component actionLabel,
        Component actionTooltip,
        boolean actionActive,
        boolean actionError
) {
}
