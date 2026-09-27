package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One item description for a {@link KineticItemGrid}. The outline selects a themed business
 * state. Error outlines override hover and selection; hover overrides selection and status;
 * selection overrides status. Items with no outline state use a white outline.
 */
public record ItemGridItem(
        ItemStack stack,
        Component tooltip,
        boolean active,
        boolean selected,
        boolean error,
        ItemGridOutline outline
) {
}
