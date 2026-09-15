package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticEnchantmentRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticEnchantments {
    private KineticEnchantments() {
    }

    public static <T extends Enchantment> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        return KineticEnchantmentRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

    public static <T extends Enchantment> KineticRegistryHandle<T> register(
            String namespace,
            String path,
            Supplier<? extends T> factory
    ) {
        return register(new ResourceLocation(namespace, path), factory);
    }
}
