package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticEnchantmentRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for enchantments. */
public final class KineticEnchantments {
    private KineticEnchantments() {
    }

    /**
     * Registers this API capability.
     */
    public static <T extends Enchantment> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        return KineticEnchantmentRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

}
