package dev.xyat.kineticcore.internal.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.Objects;
import java.util.Optional;

public final class KineticItemFluidRuntime {
    private KineticItemFluidRuntime() {
    }

    public static Optional<Fluid> containedFluid(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return FluidUtil.getFluidContained(stack)
                .filter(fluidStack -> !fluidStack.isEmpty())
                .map(FluidStack::getFluid);
    }
}
