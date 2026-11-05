package dev.xyat.kineticcore.api.client.gui.widget.list;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import dev.xyat.kineticcore.api.client.gui.render.KineticTexture;

/**
 * One slot of a {@link KineticItemGrid}. Outline priority: error, then hover, then selection, then the business
 * outline; slots without any state use a white outline.
 *
 * @param stack item shown in the slot
 * @param tooltip extra tooltip, or {@code null} to show only the item tooltip
 * @param active whether the slot can be clicked
 * @param selected whether the slot is drawn as selected
 * @param error whether the slot is drawn in the error state
 * @param outline business-state outline
 * @param emptyIcon optional background icon shown only while the slot is empty
 */
public record ItemGridItem(
        ItemStack stack,
        Component tooltip,
        boolean active,
        boolean selected,
        boolean error,
        ItemGridOutline outline,
        KineticTexture emptyIcon
) {
    /** Creates a slot without an empty-slot background icon. */
    public ItemGridItem(ItemStack stack, Component tooltip, boolean active, boolean selected, boolean error, ItemGridOutline outline) {
        this(stack, tooltip, active, selected, error, outline, null);
    }
}
