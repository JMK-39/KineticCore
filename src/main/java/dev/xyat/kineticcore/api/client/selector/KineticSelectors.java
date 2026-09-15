package dev.xyat.kineticcore.api.client.selector;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.search.ItemSearchIndex;
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

public final class KineticSelectors {
    public static final int DEFAULT_MAX_PALETTE_COLORS = 24;

    private KineticSelectors() {
    }

    public enum ItemSelectionType {
        ITEM,
        TAG,
        MOD
    }

    public enum ItemListMode {
        ITEMS_ONLY,
        ITEMS_TAGS_MODS
    }

    public record ItemSelectorPreset(
            int mode,
            int filterType,
            String filterValue,
            String categoryKey,
            String search
    ) {
        public ItemSelectorPreset {
            mode = mode == 1 ? 1 : 0;
            filterType = Math.max(0, filterType);
            filterValue = filterValue == null ? "" : filterValue;
            categoryKey = categoryKey == null ? "" : categoryKey;
            search = search == null ? "" : search;
        }

        public static ItemSelectorPreset defaults() {
            return new ItemSelectorPreset(0, 0, "", "", "");
        }

        public static ItemSelectorPreset modCategory(String namespace) {
            String value = namespace == null ? "" : namespace.trim();
            return new ItemSelectorPreset(0, 0, "", value.isEmpty() ? "" : "mod:" + value, value.isEmpty() ? "" : "@" + value);
        }
    }

    public record ItemSelection(ItemSelectionType type, ItemStack stack, String value) {
        public ItemSelection {
            type = Objects.requireNonNull(type, "type");
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
            value = value == null ? "" : value;
        }

        public boolean isItem() {
            return type == ItemSelectionType.ITEM && !stack.isEmpty();
        }

        public boolean isTag() {
            return type == ItemSelectionType.TAG && !value.isBlank();
        }

        public boolean isMod() {
            return type == ItemSelectionType.MOD && !value.isBlank();
        }
    }

    public static void prepareItems(Runnable ready) {
        ItemSearchIndex.prepareCache(ready == null ? () -> { } : ready);
    }

    public static boolean itemsReady() {
        return ItemSearchIndex.isReady();
    }

    public static void openItemSelector(Screen parent, Consumer<ItemSelection> onSelect) {
        openItemSelector(parent, null, onSelect);
    }

    public static void openItemSelector(
            Screen parent,
            ItemSelectorPreset preset,
            Consumer<ItemSelection> onSelect
    ) {
        Objects.requireNonNull(onSelect, "onSelect");
        if (preset != null) {
            ItemSelectorScreen.configureInitialState(
                    preset.mode(),
                    preset.filterType(),
                    preset.filterValue(),
                    preset.categoryKey(),
                    preset.search()
            );
        }
        prepareItems(() -> KineticClientRuntime.openScreen(new ItemSelectorScreen(parent, selection -> {
            ItemSelectionType type = switch (selection.type()) {
                case ITEM -> ItemSelectionType.ITEM;
                case TAG -> ItemSelectionType.TAG;
                case MOD -> ItemSelectionType.MOD;
            };
            onSelect.accept(new ItemSelection(type, selection.stack(), selection.value()));
        })));
    }

    public static void openEntitySelector(
            Screen parent,
            Component title,
            Collection<String> initialEntityIds,
            Consumer<List<String>> onApply
    ) {
        KineticClientRuntime.openScreen(new EntitySelectorScreen(parent, title, initialEntityIds, onApply));
    }

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

    public static void openNbtEditor(Screen parent, String initialNbt, Consumer<String> onSave) {
        KineticClientRuntime.openScreen(new NbtEditorScreen(initialNbt, onSave, parent));
    }

    public static void openColorPicker(Screen parent, Component title, int initialRgb, Consumer<Integer> onApply) {
        KineticClientRuntime.openScreen(ColorPickerScreen.single(parent, title, initialRgb, onApply));
    }

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

    public static void openPalette(
            Screen parent,
            Component title,
            List<Integer> initialColors,
            Consumer<List<Integer>> onApply
    ) {
        openPalette(parent, title, initialColors, DEFAULT_MAX_PALETTE_COLORS, onApply);
    }
}
