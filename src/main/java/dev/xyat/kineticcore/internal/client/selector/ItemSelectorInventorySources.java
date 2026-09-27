package dev.xyat.kineticcore.internal.client.selector;

import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Extra inventory sources shown in the item selector's inventory tab. */
public final class ItemSelectorInventorySources {
    private static final List<KineticSelectors.InventorySource> SOURCES = new CopyOnWriteArrayList<>();

    private ItemSelectorInventorySources() {
    }

    public static Runnable register(KineticSelectors.InventorySource source) {
        KineticSelectors.InventorySource safe = Objects.requireNonNull(source, "source");
        SOURCES.add(safe);
        return () -> SOURCES.remove(safe);
    }

    static void collect(Player player, Consumer<ItemStack> sink) {
        for (KineticSelectors.InventorySource source : SOURCES) {
            try {
                source.collect(player, sink);
            } catch (RuntimeException ignored) {
            }
        }
    }
}
