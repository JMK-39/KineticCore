package dev.xyat.kineticcore.api.client.search;

import dev.xyat.kineticcore.internal.client.search.ItemSearchIndex;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

import java.util.EnumSet;
import java.util.List;

/**
 * Read-only item-search API backed by Kinetic's shared client index.
 * <p>
 * Cached entries never expose the mutable {@link ItemStack} held by the shared index; {@link CachedItem#stack()}
 * returns a copy so add-ons cannot corrupt another consumer's cached search data.
 */
public final class KineticItemSearch {
    /** Common item categories for combining a text query with type filtering. General includes every nonempty item. */
    public enum ItemCategory {
        COMBAT,
        TOOL,
        FOOD,
        GENERAL,
        BLOCK
    }

    // Publish source identity and its immutable view together: a concurrent caller must never
    // observe a newly indexed source paired with the previous source's view.
    private record CacheSnapshot(List<ItemSearchIndex.CachedItem> source, List<CachedItem> view) {
    }

    private static volatile CacheSnapshot cached = new CacheSnapshot(List.of(), List.of());

    private KineticItemSearch() {
    }

    /** Immutable search metadata for one indexed item. */
    public static final class CachedItem {
        private final ItemStack stack;
        private final String id;
        private final String displayName;
        private final String namespace;
        private final List<String> tagIds;
        private final String searchText;
        private volatile EnumSet<ItemCategory> categories;

        private CachedItem(ItemSearchIndex.CachedItem item) {
            this.stack = item.stack == null ? ItemStack.EMPTY : item.stack.copy();
            this.id = item.idStr == null ? "" : item.idStr;
            this.displayName = item.displayName == null ? "" : item.displayName;
            this.namespace = item.namespace == null ? "" : item.namespace;
            this.tagIds = item.tagIds == null ? List.of() : List.copyOf(item.tagIds);
            this.searchText = buildSearchText(this.displayName, this.id, this.namespace, this.tagIds);
        }

        /** Returns a defensive copy of the indexed item stack. */
        public ItemStack stack() {
            return stack.copy();
        }

        /** Returns the normalized registry/custom id stored for this search entry. */
        public String id() {
            return id;
        }

        /** Returns the display name captured when the entry was indexed. */
        public String displayName() {
            return displayName;
        }

        /** Returns the item namespace used by {@code @namespace} filtering. */
        public String namespace() {
            return namespace;
        }

        /** Returns an immutable list of registry tag ids captured for this item. */
        public List<String> tagIds() {
            return tagIds;
        }

        /** Returns stable plain search text without exposing Kinetic's internal pinyin/index representation. */
        public String searchText() {
            return searchText;
        }

        /** Returns whether this indexed item matches the query using Kinetic search and pinyin rules. */
        public boolean matches(String query) {
            return KineticSearch.match(searchText, query);
        }

        /** Matches both the normal search query and one common item category. */
        public boolean matches(String query, ItemCategory category) {
            return matches(query) && matchesCategory(category);
        }

        /** Checks the item's category without exposing or copying its cached stack. */
        public boolean matchesCategory(ItemCategory category) {
            if (category == null || stack.isEmpty()) return false;
            if (category == ItemCategory.GENERAL) return true;
            EnumSet<ItemCategory> matches = categories;
            if (matches == null) {
                synchronized (this) {
                    matches = categories;
                    if (matches == null) {
                        matches = classify(stack);
                        categories = matches;
                    }
                }
            }
            return matches.contains(category);
        }

        private static String buildSearchText(String displayName, String id, String namespace, List<String> tagIds) {
            StringBuilder builder = new StringBuilder();
            if (displayName != null && !displayName.isBlank()) builder.append(displayName);
            if (id != null && !id.isBlank()) builder.append(' ').append(id);
            if (namespace != null && !namespace.isBlank()) builder.append(" @").append(namespace);
            if (tagIds != null) {
                for (String tag : tagIds) {
                    if (tag != null && !tag.isBlank()) builder.append(" #").append(tag);
                }
            }
            return builder.toString().trim();
        }
    }

    /** Creates an immutable searchable snapshot for an ad-hoc item stack, such as a player inventory entry. */
    public static CachedItem snapshot(ItemStack stack) {
        return new CachedItem(new ItemSearchIndex.CachedItem(stack));
    }

    /** Creates an immutable searchable snapshot whose identifier is supplied by the caller. */
    public static CachedItem customSnapshot(ItemStack stack, String identifier) {
        return new CachedItem(ItemSearchIndex.customItem(stack, identifier));
    }

    /** Checks the same common categories for an item stack outside the shared search index. */
    public static boolean matchesCategory(ItemStack stack, ItemCategory category) {
        if (stack == null || stack.isEmpty() || category == null) return false;
        return category == ItemCategory.GENERAL || classify(stack).contains(category);
    }

    private static EnumSet<ItemCategory> classify(ItemStack stack) {
        Item item = stack.getItem();
        EnumSet<ItemCategory> result = EnumSet.of(ItemCategory.GENERAL);
        boolean tool = isTool(item);
        boolean block = item instanceof BlockItem;
        boolean food = stack.getFoodProperties(null) != null;
        if (tool) result.add(ItemCategory.TOOL);
        if (block) result.add(ItemCategory.BLOCK);
        if (food) result.add(ItemCategory.FOOD);
        if (item instanceof ArmorItem
                || item instanceof SwordItem
                || item instanceof ProjectileWeaponItem
                || item instanceof TridentItem
                || item instanceof ShieldItem
                || !tool && !block && !food && hasAttackModifiers(stack)) {
            result.add(ItemCategory.COMBAT);
        }
        return result;
    }

    private static boolean hasAttackModifiers(ItemStack stack) {
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        return !modifiers.get(Attributes.ATTACK_DAMAGE).isEmpty()
                || !modifiers.get(Attributes.ATTACK_SPEED).isEmpty();
    }

    private static boolean isTool(Item item) {
        return item instanceof DiggerItem
                || item instanceof TieredItem && !(item instanceof SwordItem)
                || item instanceof ShearsItem
                || item instanceof FishingRodItem
                || item instanceof FlintAndSteelItem;
    }

    /** Returns the current immutable cached item-search view. */
    public static List<CachedItem> items() {
        List<ItemSearchIndex.CachedItem> source = ItemSearchIndex.getItems();
        CacheSnapshot snapshot = cached;
        if (snapshot.source() == source) {
            return snapshot.view();
        }
        synchronized (KineticItemSearch.class) {
            snapshot = cached;
            if (snapshot.source() != source) {
                snapshot = new CacheSnapshot(source, source.stream().map(CachedItem::new).toList());
                cached = snapshot;
            }
            return snapshot.view();
        }
    }

    /** Returns whether the shared item-search cache has finished building and is ready for queries. */
    public static boolean ready() {
        return ItemSearchIndex.isReady();
    }

    /** Clears the shared item-search cache so it can be rebuilt for the current client context. */
    public static void clear() {
        ItemSearchIndex.clear();
        cached = new CacheSnapshot(List.of(), List.of());
    }

    /** Ensures the shared cache is prepared, invoking the optional callback after a successful cache build. */
    public static void prepare(Runnable onDone) {
        ItemSearchIndex.prepareCache(onDone == null ? () -> { } : onDone);
    }
}
