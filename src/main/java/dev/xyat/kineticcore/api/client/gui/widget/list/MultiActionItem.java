package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

import java.util.List;

/** One row description for a {@link KineticMultiActionList}. */
public record MultiActionItem(
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean marked,
        boolean error,
        List<RowAction> actions
) {
    /**
     * Creates a new multi action item instance.
     */
    public MultiActionItem(Component label, Component tooltip, List<RowAction> actions) {
        this(label, null, tooltip, true, false, false, actions);
    }
}
