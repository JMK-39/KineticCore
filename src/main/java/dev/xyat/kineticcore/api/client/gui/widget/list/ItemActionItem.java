package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One row of a {@link KineticItemActionList}.
 *
 * @param stack item shown in the row's slot
 * @param label main row text
 * @param secondaryLabel text appended after the label, or {@code null}
 * @param tooltip row tooltip, or {@code null}
 * @param active whether the row can be selected; inactive rows are dimmed
 * @param error whether the row is drawn in the error state
 * @param actionLabel trailing button text
 * @param actionTooltip trailing button tooltip, or {@code null}
 * @param actionActive whether the trailing button can be clicked
 */
public record ItemActionItem(
        ItemStack stack,
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean error,
        Component actionLabel,
        Component actionTooltip,
        boolean actionActive
) {
    /** Creates an active row without a secondary label. */
    public ItemActionItem(
            ItemStack stack,
            Component label,
            Component tooltip,
            Component actionLabel,
            Component actionTooltip
    ) {
        this(stack, label, null, tooltip, true, false, actionLabel, actionTooltip, true);
    }
}
