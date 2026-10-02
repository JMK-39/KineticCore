package dev.xyat.kineticcore.api.inventory;

import dev.xyat.kineticcore.internal.inventory.KineticItemTextRuntime;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Item stacks as text, in the syntax the running Minecraft version uses in commands such as {@code /give}:
 * {@code minecraft:diamond_sword{Enchantments:[...]}} on 1.20.1 and
 * {@code minecraft:diamond_sword[enchantments={...}]} from 1.20.5 on. The text never contains the count.
 */
public final class KineticItemText {
    private KineticItemText() {
    }

    /**
     * Parses an item, with its NBT or components, into a stack of one.
     *
     * @return the stack; empty for {@code minecraft:air}
     * @throws IllegalArgumentException if the item is unknown or the NBT or components are malformed
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static ItemStack parse(String text) {
        return KineticItemTextRuntime.parse(Objects.requireNonNull(text, "text"));
    }

    /**
     * Writes an item and its NBT or components so that {@link #parse(String)} reads it back.
     *
     * @return the text; empty for an empty or {@code null} stack
     */
    public static String format(ItemStack stack) {
        return KineticItemTextRuntime.format(stack);
    }
}
