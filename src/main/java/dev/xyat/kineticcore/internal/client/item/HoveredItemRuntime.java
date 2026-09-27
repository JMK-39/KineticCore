package dev.xyat.kineticcore.internal.client.item;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/** Registry behind {@code KineticHoveredItems}. */
public final class HoveredItemRuntime {
    private static final List<Supplier<ItemStack>> PROVIDERS = new CopyOnWriteArrayList<>();

    private HoveredItemRuntime() {
    }

    public static Runnable register(Supplier<ItemStack> provider) {
        Supplier<ItemStack> safe = Objects.requireNonNull(provider, "provider");
        PROVIDERS.add(safe);
        return () -> PROVIDERS.remove(safe);
    }

    public static ItemStack resolve() {
        for (Supplier<ItemStack> provider : PROVIDERS) {
            ItemStack stack;
            try {
                stack = provider.get();
            } catch (RuntimeException ignored) {
                continue;
            }
            if (stack != null && !stack.isEmpty()) return stack.copy();
        }
        return ItemStack.EMPTY;
    }
}
