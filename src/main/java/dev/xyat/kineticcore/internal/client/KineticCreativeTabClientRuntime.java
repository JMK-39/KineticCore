package dev.xyat.kineticcore.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
/*import net.minecraft.client.multiplayer.ClientPacketListener;
*///?} else {
import net.minecraft.client.searchtree.SearchRegistry;
//?}

import java.util.Collection;
import java.util.List;

public final class KineticCreativeTabClientRuntime {
    private KineticCreativeTabClientRuntime() {
    }

    public static void refreshSearch(Collection<ItemStack> items) {
        Minecraft minecraft = Minecraft.getInstance();
        List<ItemStack> snapshot = items == null ? List.of() : List.copyOf(items);
        //? if >=1.20.5 {
        /*// The creative search trees belong to the connection since 1.20.5.
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) return;
        connection.searchTrees().updateCreativeTooltips(connection.registryAccess(), snapshot);
        connection.searchTrees().updateCreativeTags(snapshot);
        *///?} else {
        minecraft.populateSearchTree(SearchRegistry.CREATIVE_NAMES, snapshot);
        minecraft.populateSearchTree(SearchRegistry.CREATIVE_TAGS, snapshot);
        //?}
    }
}
