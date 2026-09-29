package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One row of a {@link KineticItemSelectionList}.
 *
 * @param stack item shown in the row's slot
 * @param label main row text
 * @param secondaryLabel text appended after the label, or {@code null}
 * @param tooltip row tooltip, or {@code null}
 * @param active whether the row can be selected; inactive rows are dimmed
 * @param error whether the row is drawn in the error state
 */
public record ItemSelectionItem(
        ItemStack stack,
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean error
) {
}
