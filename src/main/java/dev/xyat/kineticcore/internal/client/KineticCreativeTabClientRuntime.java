package dev.xyat.kineticcore.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

public final class KineticCreativeTabClientRuntime {
    private KineticCreativeTabClientRuntime() {
    }

    public static void refreshSearch(Collection<ItemStack> items) {
        Minecraft minecraft = Minecraft.getInstance();
        List<ItemStack> snapshot = items == null ? List.of() : List.copyOf(items);
        minecraft.populateSearchTree(SearchRegistry.CREATIVE_NAMES, snapshot);
        minecraft.populateSearchTree(SearchRegistry.CREATIVE_TAGS, snapshot);
    }
}
