package dev.xyat.kineticcore.api.client.search;

import dev.xyat.kineticcore.internal.client.search.ItemSearchIndex;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public final class KineticItemSearch {
    private static volatile List<ItemSearchIndex.CachedItem> cachedSource = List.of();
    private static volatile List<CachedItem> cachedView = List.of();

    private KineticItemSearch() {
    }

    public static final class CachedItem {
        public final ItemStack stack;
        public final String idStr;
        public final String uniqueKey;
        public final String displayName;
        public final String namespace;
        public final List<String> tagIds;
        public final String searchData;

        public CachedItem(ItemStack stack) {
            this(new ItemSearchIndex.CachedItem(stack));
        }

        private CachedItem(ItemSearchIndex.CachedItem item) {
            this.stack = item.stack == null ? ItemStack.EMPTY : item.stack.copy();
            this.idStr = item.idStr == null ? "" : item.idStr;
            this.uniqueKey = item.uniqueKey == null ? "" : item.uniqueKey;
            this.displayName = item.displayName == null ? "" : item.displayName;
            this.namespace = item.namespace == null ? "" : item.namespace;
            this.tagIds = item.tagIds == null ? List.of() : List.copyOf(item.tagIds);
            this.searchData = item.searchData == null ? "" : item.searchData;
        }

        public static CachedItem custom(ItemStack stack, String idStr) {
            return new CachedItem(ItemSearchIndex.CachedItem.custom(stack, idStr));
        }
    }

    public static String getUniqueKey(ItemStack stack) {
        return ItemSearchIndex.getUniqueKey(stack);
    }

    public static Set<String> getRegistryTagIds(ItemStack stack) {
        return ItemSearchIndex.getRegistryTagIds(stack);
    }

    public static List<CachedItem> getItems() {
        List<ItemSearchIndex.CachedItem> source = ItemSearchIndex.getItems();
        if (cachedSource == source) {
            return cachedView;
        }
        synchronized (KineticItemSearch.class) {
            if (cachedSource != source) {
                cachedSource = source;
                cachedView = source.stream().map(CachedItem::new).toList();
            }
            return cachedView;
        }
    }

    public static boolean isReady() {
        return ItemSearchIndex.isReady();
    }

    public static void clear() {
        ItemSearchIndex.clear();
    }

    public static void prepareCache(Runnable onDone) {
        ItemSearchIndex.prepareCache(onDone == null ? () -> { } : onDone);
    }
}
