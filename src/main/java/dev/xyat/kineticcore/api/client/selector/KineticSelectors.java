package dev.xyat.kineticcore.api.client.selector;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.client.search.KineticItemSearch;
import dev.xyat.kineticcore.internal.client.selector.ColorPickerScreen;
import dev.xyat.kineticcore.internal.client.selector.EntitySelectorScreen;
import dev.xyat.kineticcore.internal.client.selector.ItemListEditorScreen;
import dev.xyat.kineticcore.internal.client.selector.ItemSelectorScreen;
import dev.xyat.kineticcore.internal.client.selector.NbtEditorScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Single entry facade for item, entity, NBT, color, and palette selectors.
 */
public final class KineticSelectors {
    /** Default maximum number of colors retained by palette selectors. */
    public static final int DEFAULT_MAX_PALETTE_COLORS = 24;

    private KineticSelectors() {
    }

    /** Identifies whether an item-selector result represents an item, item tag, or mod namespace. */
    public enum ItemSelectionType {
        ITEM,
        TAG,
        MOD
    }

    /** Selects the item source shown when the selector first opens. */
    public enum ItemSource {
        ALL,
        INVENTORY
    }

    /** Selects the optional filter applied when the selector first opens. */
    public enum ItemFilter {
        NONE,
        MOD,
        TAG
    }

    /** Controls which rule kinds are available in the item-list editor. */
    public enum ItemListMode {
        ITEMS_ONLY,
        ITEMS_TAGS_MODS
    }

    /**
     * Immutable initial source, filter, category, and search state for an item selector.
     * Nullable enum values fall back to {@link ItemSource#ALL} and {@link ItemFilter#NONE}; nullable text becomes empty.
     */
    public record ItemSelectorPreset(
            ItemSource source,
            ItemFilter filter,
            String filterValue,
            String categoryKey,
            String search
    ) {
        /** Normalizes nullable selector state into the canonical initial preset representation. */
        public ItemSelectorPreset {
            source = source == null ? ItemSource.ALL : source;
            filter = filter == null ? ItemFilter.NONE : filter;
            filterValue = filterValue == null ? "" : filterValue.trim();
            categoryKey = categoryKey == null ? "" : categoryKey.trim();
            search = search == null ? "" : search;
            if (filter == ItemFilter.NONE) filterValue = "";
            if (source == ItemSource.INVENTORY) categoryKey = "";
        }

        /** Returns the neutral selector preset with all registered items and no category or search filter. */
        public static ItemSelectorPreset defaults() {
            return new ItemSelectorPreset(ItemSource.ALL, ItemFilter.NONE, "", "", "");
        }

        /** Creates an all-items preset focused on one mod namespace and its matching {@code @namespace} query. */
        public static ItemSelectorPreset modCategory(String namespace) {
            String value = namespace == null ? "" : namespace.trim();
            return new ItemSelectorPreset(
                    ItemSource.ALL,
                    ItemFilter.NONE,
                    "",
                    value.isEmpty() ? "" : "mod:" + value,
                    value.isEmpty() ? "" : "@" + value
            );
        }
    }

    /**
     * Read-only result returned by the item selector.
     * <p>
     * Item selections retain their own stack copy, and {@link #stack()} returns another copy so consumers cannot
     * mutate selector state through the result object. Tag and mod selections use {@link #value()} instead.
     */
    public static final class ItemSelection {
        private final ItemSelectionType type;
        private final ItemStack stack;
        private final String value;

        private ItemSelection(ItemSelectionType type, ItemStack stack, String value) {
            this.type = Objects.requireNonNull(type, "type");
            this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
            this.value = value == null ? "" : value;
        }

        /** Returns the kind of selector result. */
        public ItemSelectionType type() {
            return type;
        }

        /** Returns a defensive copy of the selected item stack, or an empty stack for non-item results. */
        public ItemStack stack() {
            return stack.copy();
        }

        /** Returns the selected tag/mod identifier, or an empty string for item results. */
        public String value() {
            return value;
        }

        /** Returns whether this selector result represents an item. */
        public boolean isItem() {
            return type == ItemSelectionType.ITEM && !stack.isEmpty();
        }

        /** Returns whether this selector result represents a tag id. */
        public boolean isTag() {
            return type == ItemSelectionType.TAG && !value.isBlank();
        }

        /** Returns whether this selector result represents a mod id. */
        public boolean isMod() {
            return type == ItemSelectionType.MOD && !value.isBlank();
        }
    }

    /** Opens the standard item selector with the neutral default preset. */
    public static void openItemSelector(Screen parent, Consumer<ItemSelection> onSelect) {
        openItemSelector(parent, ItemSelectorPreset.defaults(), onSelect);
    }

    /** Opens the item selector with an explicit optional preset. */
    public static void openItemSelector(
            Screen parent,
            ItemSelectorPreset preset,
            Consumer<ItemSelection> onSelect
    ) {
        Objects.requireNonNull(onSelect, "onSelect");
        // Capture this call's preset in its callback; an overlapping cache preparation must not replace it.
        KineticItemSearch.prepare(() -> KineticClientRuntime.openScreen(new ItemSelectorScreen(parent, selection -> {
            ItemSelectionType type = switch (selection.type()) {
                case ITEM -> ItemSelectionType.ITEM;
                case TAG -> ItemSelectionType.TAG;
                case MOD -> ItemSelectionType.MOD;
            };
            onSelect.accept(new ItemSelection(type, selection.stack(), selection.value()));
        }, preset)));
    }

    /** Opens the standard entity selector and returns the applied entity ids through {@code onApply}. */
    public static void openEntitySelector(
            Screen parent,
            Component title,
            Collection<String> initialEntityIds,
            Consumer<List<String>> onApply
    ) {
        KineticClientRuntime.openScreen(new EntitySelectorScreen(parent, title, initialEntityIds, onApply));
    }

    /** Opens the standard item-rule list editor using the requested rule mode. */
    public static void openItemListEditor(
            Screen parent,
            Component title,
            List<String> initialRules,
            ItemListMode mode,
            Consumer<List<String>> onSave
    ) {
        ItemListEditorScreen.SelectionMode internalMode = mode == ItemListMode.ITEMS_ONLY
                ? ItemListEditorScreen.SelectionMode.ITEMS_ONLY
                : ItemListEditorScreen.SelectionMode.ITEMS_TAGS_MODS;
        KineticClientRuntime.openScreen(new ItemListEditorScreen(parent, title, initialRules, internalMode, onSave));
    }

    /** Opens the standard NBT text editor with the supplied initial serialized value. */
    public static void openNbtEditor(Screen parent, String initialNbt, Consumer<String> onSave) {
        KineticClientRuntime.openScreen(new NbtEditorScreen(initialNbt, onSave, parent));
    }

    /** Opens the standard single-color picker with the supplied initial RGB value. */
    public static void openColorPicker(Screen parent, Component title, int initialRgb, Consumer<Integer> onApply) {
        KineticClientRuntime.openScreen(ColorPickerScreen.single(parent, title, initialRgb, onApply));
    }

    /** Opens the palette editor with an explicit maximum color count. */
    public static void openPalette(
            Screen parent,
            Component title,
            List<Integer> initialColors,
            int maxColors,
            Consumer<List<Integer>> onApply
    ) {
        List<Integer> safeColors = initialColors == null ? List.of() : List.copyOf(initialColors);
        int safeMax = Math.max(1, Math.min(DEFAULT_MAX_PALETTE_COLORS, maxColors));
        KineticClientRuntime.openScreen(ColorPickerScreen.palette(parent, title, safeColors, safeMax, onApply));
    }

}
