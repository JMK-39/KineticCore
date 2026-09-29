package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * One row of a {@link KineticMultiActionList}.
 *
 * @param label main row text
 * @param secondaryLabel text appended after the label, or {@code null}
 * @param tooltip row tooltip, or {@code null}
 * @param active whether the row can be selected; inactive rows are dimmed
 * @param error whether the row is drawn in the error state
 * @param actions trailing buttons from left to right
 */
public record MultiActionItem(
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean error,
        List<RowAction> actions
) {
    /** Creates an active row without a secondary label. */
    public MultiActionItem(Component label, Component tooltip, List<RowAction> actions) {
        this(label, null, tooltip, true, false, actions);
    }
}
