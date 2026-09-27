package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/** One row description for a {@link KineticActionList}. */
public record ActionItem(
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean marked,
        boolean error,
        Component actionLabel,
        Component actionTooltip,
        boolean actionActive,
        boolean actionError
) {
}
