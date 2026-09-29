package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import dev.xyat.kineticcore.internal.runtime.KineticCreativeTabsRuntime;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Registers creative tabs, adds items to existing tabs and reads the tab registry. */
public final class KineticCreativeTabs {
    /** Adds items to creative tabs while their contents are built. */
    @FunctionalInterface
    public interface Handler {
        /**
         * Called once per tab each time tab contents are rebuilt; check {@link Context#tabKey()} to pick the tab.
         */
        void handle(Context context);
    }

    /** The tab being built. */
    public interface Context {
        /** Returns the key of the tab being built. */
        ResourceKey<CreativeModeTab> tabKey();

        /** Appends one item with count 1 to the tab. */
        void accept(ItemLike item);

        /** Appends a copy of the stack to the tab, including its NBT. */
        void accept(ItemStack stack);
    }

    /**
     * One registered creative tab.
     *
     * @param key registry key of the tab
     * @param tab the tab
     */
    public record TabEntry(ResourceKey<CreativeModeTab> key, CreativeModeTab tab) {
        /**
         * @throws NullPointerException if an argument is {@code null}
         */
        public TabEntry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(tab, "tab");
        }
    }

    private KineticCreativeTabs() {
    }

    /**
     * Registers a new creative tab. Call it from the mod constructor.
     *
     * @param id tab id
     * @param factory creates the tab during registration, typically {@code CreativeModeTab.builder()...build()}
     * @return a handle that resolves after registration
     * @throws NullPointerException if an argument is {@code null}
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

    /** Returns every registered tab with its key, in registry order, as an unmodifiable list. */
    public static List<TabEntry> entries() {
        return KineticCreativeTabsRuntime.entries();
    }

    /** Returns every registered tab in registry order as an unmodifiable list. */
    public static List<CreativeModeTab> values() {
        return KineticCreativeTabsRuntime.values();
    }

    /**
     * Looks up a tab by id.
     *
     * @return the tab, or {@code null} when none is registered with the id
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static CreativeModeTab get(ResourceLocation id) {
        return KineticCreativeTabsRuntime.get(Objects.requireNonNull(id, "id"));
    }

    /**
     * Returns the id of a tab.
     *
     * @return the id, or {@code null} when the tab is not registered
     * @throws NullPointerException if {@code tab} is {@code null}
     */
    public static ResourceLocation id(CreativeModeTab tab) {
        return KineticCreativeTabsRuntime.id(Objects.requireNonNull(tab, "tab"));
    }

    /** Returns whether a tab is registered with the id. */
    public static boolean contains(ResourceLocation id) {
        return KineticCreativeTabsRuntime.contains(Objects.requireNonNull(id, "id"));
    }

}
