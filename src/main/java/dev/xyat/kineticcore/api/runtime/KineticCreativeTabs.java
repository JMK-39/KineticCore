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

/** Public Kinetic API facade for creative tabs. */
public final class KineticCreativeTabs {
    /** Callback contract used by the enclosing API. */
    @FunctionalInterface
    public interface Handler {
        void handle(Context context);
    }

    /** Context exposed to callbacks registered through the enclosing API. */
    public interface Context {
        ResourceKey<CreativeModeTab> tabKey();

        void accept(ItemLike item);

        void accept(ItemStack stack);
    }

    /** Immutable tab entry data exposed by this API. */
    public record TabEntry(ResourceKey<CreativeModeTab> key, CreativeModeTab tab) {
        /**
         * Validates and normalizes this tab entry value.
         */
        public TabEntry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(tab, "tab");
        }
    }

    private KineticCreativeTabs() {
    }

    /**
     * Registers this API capability.
     */
    public static KineticRegistryHandle<CreativeModeTab> register(
            ResourceLocation id,
            Supplier<? extends CreativeModeTab> factory
    ) {
        return KineticCreativeTabsRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

    /**
     * Registers a build-contents listener. Kinetic installs one Forge bridge and dispatches registered handlers
     * in order; one handler throwing a RuntimeException does not prevent later Kinetic handlers from running.
     */
    public static void onBuildContents(Handler handler) {
        KineticCreativeTabsRuntime.onBuildContents(Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Returns the entries.
     */
    public static List<TabEntry> entries() {
        return KineticCreativeTabsRuntime.entries();
    }

    /**
     * Returns the values.
     */
    public static List<CreativeModeTab> values() {
        return KineticCreativeTabsRuntime.values();
    }

    /**
     * Performs the get API operation.
     */
    public static CreativeModeTab get(ResourceLocation id) {
        return KineticCreativeTabsRuntime.get(Objects.requireNonNull(id, "id"));
    }

    /**
     * Returns the id.
     */
    public static ResourceLocation id(CreativeModeTab tab) {
        return KineticCreativeTabsRuntime.id(Objects.requireNonNull(tab, "tab"));
    }

    /**
     * Returns whether contains.
     */
    public static boolean contains(ResourceLocation id) {
        return KineticCreativeTabsRuntime.contains(Objects.requireNonNull(id, "id"));
    }

    /**
     * Performs the refresh search API operation.
     */
    public static void refreshSearch(Collection<ItemStack> items) {
        KineticCreativeTabClientRuntime.refreshSearch(items == null ? List.of() : List.copyOf(items));
    }
}
