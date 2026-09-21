package dev.xyat.kineticcore.api.inventory;

import dev.xyat.kineticcore.internal.inventory.KineticItemFluidRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

/** Provides loader-independent access to fluid content stored by an item stack. */
public final class KineticItemFluids {
    private KineticItemFluids() {
    }

    /** Returns the non-empty fluid contained by the supplied item stack, when one is exposed by the platform. */
    public static Optional<Fluid> containedFluid(ItemStack stack) {
        return KineticItemFluidRuntime.containedFluid(stack);
    }
}
