package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;

/**
 * One row of a {@link KineticToggleActionList}.
 *
 * @param label main row text
 * @param secondaryLabel text appended after the label, or {@code null}
 * @param tooltip row tooltip, or {@code null}
 * @param active whether the row can be selected; inactive rows are dimmed
 * @param error whether the row is drawn in the error state
 * @param toggleValue current toggle state
 * @param toggleOnLabel toggle text while on
 * @param toggleOffLabel toggle text while off
 * @param toggleTooltip toggle tooltip, or {@code null}
 * @param toggleActive whether the toggle can be clicked
 * @param actionLabel trailing button text
 * @param actionTooltip trailing button tooltip, or {@code null}
 * @param actionActive whether the trailing button can be clicked
 * @param actionError whether the trailing button is drawn in the error state
 * @param effectIcon optional registered potion-effect icon drawn before the row text
 */
public record ToggleActionItem(
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean error,
        boolean toggleValue,
        Component toggleOnLabel,
        Component toggleOffLabel,
        Component toggleTooltip,
        boolean toggleActive,
        Component actionLabel,
        Component actionTooltip,
        boolean actionActive,
        boolean actionError,
        net.minecraft.world.effect.MobEffect effectIcon
) {
    /** Creates a row without a potion-effect icon, preserving the standard text-list layout. */
    public ToggleActionItem(Component label, Component secondaryLabel, Component tooltip, boolean active, boolean error, boolean toggleValue,
                            Component toggleOnLabel, Component toggleOffLabel, Component toggleTooltip, boolean toggleActive,
                            Component actionLabel, Component actionTooltip, boolean actionActive, boolean actionError) {
        this(label, secondaryLabel, tooltip, active, error, toggleValue, toggleOnLabel, toggleOffLabel, toggleTooltip, toggleActive,
                actionLabel, actionTooltip, actionActive, actionError, null);
    }
    /** Creates an active row without a secondary label, with an active toggle and trailing button. */
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
        this(label, null, tooltip, true, false, toggleValue,
                toggleOnLabel, toggleOffLabel, toggleTooltip, true,
                actionLabel, actionTooltip, true, false);
    }
}
