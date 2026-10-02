package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
//? if <1.21
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;

public final class KineticEnchantmentRegistryRuntime {
    private KineticEnchantmentRegistryRuntime() {
    }

    public static <T extends Enchantment> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        //? if >=1.21 {
        /*throw new UnsupportedOperationException("Enchantments are data-driven since Minecraft 1.21 and cannot be "
                + "registered from code; ship " + id + " as data/" + id.getNamespace() + "/enchantment/" + id.getPath() + ".json");
        *///?} else {
        return KineticDeferredRegistryRuntime.register(Registries.ENCHANTMENT, id, factory);
        //?}
    }
}
