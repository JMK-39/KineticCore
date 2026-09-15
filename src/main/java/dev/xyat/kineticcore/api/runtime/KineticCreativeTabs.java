package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import dev.xyat.kineticcore.internal.runtime.KineticCreativeTabsRuntime;
import dev.xyat.kineticcore.internal.client.KineticCreativeTabClientRuntime;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class KineticCreativeTabs {
    @FunctionalInterface
    public interface Handler {
        void handle(Context context);
    }

    public interface Context {
        ResourceKey<CreativeModeTab> tabKey();

        void accept(ItemLike item);

        void accept(ItemStack stack);
    }

    public record TabEntry(ResourceKey<CreativeModeTab> key, CreativeModeTab tab) {
        public TabEntry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(tab, "tab");
        }
    }

    private KineticCreativeTabs() {
    }

    public static KineticRegistryHandle<CreativeModeTab> register(
            ResourceLocation id,
            Supplier<? extends CreativeModeTab> factory
    ) {
        return KineticCreativeTabsRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

    public static KineticRegistryHandle<CreativeModeTab> register(
            String namespace,
            String path,
            Supplier<? extends CreativeModeTab> factory
    ) {
        return register(new ResourceLocation(namespace, path), factory);
    }

    public static void onBuildContents(Handler handler) {
        KineticCreativeTabsRuntime.onBuildContents(Objects.requireNonNull(handler, "handler"));
    }

    public static List<TabEntry> entries() {
        return KineticCreativeTabsRuntime.entries();
    }

    public static List<CreativeModeTab> values() {
        return KineticCreativeTabsRuntime.values();
    }

    public static CreativeModeTab get(ResourceLocation id) {
        return KineticCreativeTabsRuntime.get(Objects.requireNonNull(id, "id"));
    }

    public static ResourceLocation id(CreativeModeTab tab) {
        return KineticCreativeTabsRuntime.id(Objects.requireNonNull(tab, "tab"));
    }

    public static boolean contains(ResourceLocation id) {
        return KineticCreativeTabsRuntime.contains(Objects.requireNonNull(id, "id"));
    }

    public static void refreshSearch(Collection<ItemStack> items) {
        KineticCreativeTabClientRuntime.refreshSearch(items == null ? List.of() : List.copyOf(items));
    }
}
