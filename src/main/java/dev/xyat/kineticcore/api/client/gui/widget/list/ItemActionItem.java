package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** One item-backed row description for a {@link KineticItemActionList}. */
public record ItemActionItem(
        ItemStack stack,
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean marked,
        boolean error,
        Component actionLabel,
        Component actionTooltip,
        boolean actionActive
) {
    /**
     * Creates a new item action item instance.
     */
    public ItemActionItem(
            ItemStack stack,
            Component label,
            Component tooltip,
            Component actionLabel,
            Component actionTooltip
    ) {
        this(stack, label, null, tooltip, true, false, false, actionLabel, actionTooltip, true);
    }
}
