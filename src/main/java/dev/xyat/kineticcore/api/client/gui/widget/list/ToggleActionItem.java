package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/** One row description for a {@link KineticToggleActionList}. */
public record ToggleActionItem(
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean marked,
        boolean error,
        boolean toggleValue,
        Component toggleOnLabel,
        Component toggleOffLabel,
        Component toggleTooltip,
        boolean toggleActive,
        Component actionLabel,
        Component actionTooltip,
        boolean actionActive,
        boolean actionError
) {
    /**
     * Creates a new toggle action item instance.
     */
    public ToggleActionItem(
            Component label,
            Component tooltip,
            boolean toggleValue,
            Component toggleOnLabel,
            Component toggleOffLabel,
            Component toggleTooltip,
            Component actionLabel,
            Component actionTooltip
    ) {
        this(label, null, tooltip, true, false, false, toggleValue,
                toggleOnLabel, toggleOffLabel, toggleTooltip, true,
                actionLabel, actionTooltip, true, false);
    }
}
