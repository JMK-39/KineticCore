package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** One item-backed row description for a {@link KineticItemSelectionList}. */
public record ItemSelectionItem(
        ItemStack stack,
        Component label,
        Component secondaryLabel,
        Component tooltip,
        boolean active,
        boolean marked,
        boolean error
) {
}
