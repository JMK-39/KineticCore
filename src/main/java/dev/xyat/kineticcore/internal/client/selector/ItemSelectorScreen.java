package dev.xyat.kineticcore.internal.client.selector;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.search.KineticItemSearch;
import dev.xyat.kineticcore.api.client.selector.KineticSelectors.ItemSelectorPreset;
import dev.xyat.kineticcore.api.client.selector.KineticSelectors.ItemSource;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.internal.compat.curios.KineticCuriosInventoryBridge;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.GridScrollController;

import net.minecraft.client.gui.GuiGraphics;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public class ItemSelectorScreen extends KineticScreen {
    public int getCachedVisibleCols() {
        return cachedVisibleCols;
    }

    public int getCachedVisibleRows() {
        return cachedVisibleRows;
    }

    public enum SelectionType {
        ITEM,
        TAG,
        MOD
    }

    public record Selection(SelectionType type, ItemStack stack, String value) {
        public static Selection item(ItemStack stack) {
            return new Selection(SelectionType.ITEM, stack == null ? ItemStack.EMPTY : stack.copy(), "");
        }

        public static Selection tag(String value) {
            return new Selection(SelectionType.TAG, ItemStack.EMPTY, value == null ? "" : value);
        }

        public static Selection mod(String value) {
            return new Selection(SelectionType.MOD, ItemStack.EMPTY, value == null ? "" : value);
        }

        public boolean isItem() {
            return type == SelectionType.ITEM && stack != null && !stack.isEmpty();
        }

        public boolean isTag() {
            return type == SelectionType.TAG && value != null && !value.isBlank();
        }

        public boolean isMod() {
            return type == SelectionType.MOD && value != null && !value.isBlank();
        }
    }

    private record DisplayCacheKey(int mode, String query, int filterType, String filterValue, String categoryKey) {
    }

    private enum CategoryType {
        MODE,
        VANILLA,
        MOD,
        HEADER
    }

    private record CategoryEntry(CategoryType type, int mode, String key, Component label) {
        boolean selectable() {
            return type != CategoryType.HEADER;
        }
    }

    private record VisibleSlot(int displayIndex, ItemStack stack, int x, int y) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + SLOT_SIZE
                    && mouseY >= y && mouseY < y + SLOT_SIZE;
        }
    }

    private final Consumer<Selection> onSelect;
    private final List<KineticItemSearch.CachedItem> invItems = new ArrayList<>();
    private List<KineticItemSearch.CachedItem> displayList = new ArrayList<>();

    private static String rememberedSearch = "";
    private static int rememberedMode = 0;
    private static String rememberedFilterValue = null;
    private static int rememberedFilterType = 0;
    private static String rememberedCategoryKey = null;

    private String searchText = rememberedSearch;
    private KineticAutoComplete.AutoCompleteBox searchBox;
    private int mode = rememberedMode;
    private final GridScrollController mainScroll =
            new GridScrollController();

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 1;
    private static final int CELL_SIZE = SLOT_SIZE + SLOT_GAP;
    private static final int FIXED_GRID_COLS = 25;
    private static final int FIXED_GRID_ROWS = 16;
    private static final int CATEGORY_WIDTH = 132;
    private static final int CATEGORY_BUTTON_SHIFT_X = -4;
    private static final int CATEGORY_BUTTON_WIDTH = 140;
    private static final int CATEGORY_SCROLL_GAP = 2;
    private static final int CATEGORY_GAP = 5;
    private static final int GRID_SHIFT_LEFT = -5;
    private static final int CATEGORY_SCROLLBAR_SHIFT_X = 1;
    private static final int CATEGORY_SCROLLBAR_WIDTH = 4;
    private static final int MAIN_SCROLL_GAP = 2;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SEARCH_CACHE_LIMIT = 64;
    private int gridX;
    private int gridY;
    private int gridCols;
    private int gridRowsVisible;
    private int categoryX;
    private int categoryY;
    private int topInfoY;

    private List<KineticItemSearch.CachedItem> cachedAllSource = List.of();
    private List<KineticItemSearch.CachedItem> cachedInventorySource = List.of();
    private List<KineticItemSearch.CachedItem> cachedItemSearchIndexIdentity = List.of();
    private Map<String, List<KineticItemSearch.CachedItem>> cachedVanillaCategorySources = Map.of();
    private Map<String, List<KineticItemSearch.CachedItem>> cachedModSources = Map.of();
    private final Map<DisplayCacheKey, List<KineticItemSearch.CachedItem>> displayCache =
            new LinkedHashMap<>(SEARCH_CACHE_LIMIT, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<DisplayCacheKey, List<KineticItemSearch.CachedItem>> eldest) {
                    return size() > SEARCH_CACHE_LIMIT;
                }
            };
    private final List<VisibleSlot> visibleSlotCache = new ArrayList<>();
    private int displayVersion = 0;
    private int cachedVisibleDisplayVersion = -1;
    private int cachedVisibleScroll = -1;
    private int cachedVisibleCols = -1;
    private int cachedVisibleRows = -1;

    private final List<String> allMods = new ArrayList<>();
    private final List<String> allTags = new ArrayList<>();
    private final List<CategoryEntry> categoryEntries = new ArrayList<>();
    private final List<StateButton> categoryButtons = new ArrayList<>();
    private final GridScrollController categoryScroll = new GridScrollController();
    private String categoryKey = rememberedCategoryKey;

    private String activeFilterValue = rememberedFilterValue;
    private int activeFilterType = rememberedFilterType;
    private StateButton applyFilterBtn = null;
    private int btnAreaStartX;

    public ItemSelectorScreen(Screen parent, Consumer<Selection> onSelect) {
        this(parent, onSelect, null);
    }

    /** Applies one request's selection preset without sharing mutable initial state with other pending opens. */
    public ItemSelectorScreen(Screen parent, Consumer<Selection> onSelect, ItemSelectorPreset preset) {
        super(KineticText.translatable("gui.kineticcore.items.item_selector.title"));
        setParentScreen(parent);
        this.onSelect = onSelect;
        if (preset != null) {
            mode = preset.source() == ItemSource.INVENTORY ? 1 : 0;
            activeFilterType = switch (preset.filter()) {
                case NONE -> 0;
                case MOD -> 1;
                case TAG -> 2;
            };
            activeFilterValue = activeFilterType == 0 || preset.filterValue().isBlank()
                    ? null : preset.filterValue();
            categoryKey = mode == 0 && !preset.categoryKey().isBlank() ? preset.categoryKey() : null;
            searchText = preset.search();
            rememberedMode = mode;
            rememberedFilterType = activeFilterType;
            rememberedFilterValue = activeFilterValue;
            rememberedCategoryKey = categoryKey;
            rememberedSearch = searchText;
        }
        loadPlayerStacks();
        loadFilters();
        rebuildCategoryEntries();
        if (mode != 0 && mode != 1) {
            mode = 0;
        }
        if (mode != 0) {
            categoryKey = null;
        }
    }

    private void loadPlayerStacks() {
        Player player = KineticClientRuntime.localPlayer();
        if (player == null) return;

        player.getInventory().items.forEach(this::addInventoryStack);
        player.getArmorSlots().forEach(this::addInventoryStack);
        addInventoryStack(player.getOffhandItem());

        if (KineticPlatform.isModLoaded("curios")) {
            KineticCuriosInventoryBridge.appendPlayerStacks(player, this::addInventoryStack);
        }
    }

    private void addInventoryStack(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            invItems.add(KineticItemSearch.snapshot(stack));
        }
    }

    private void loadFilters() {
        Set<String> modSet = new TreeSet<>();
        KineticRegistries.items().ids().forEach(id -> modSet.add(id.getNamespace()));
        allMods.addAll(modSet);

        Set<String> tagSet = new TreeSet<>();
        KineticRegistries.items().tagIds().forEach(id -> tagSet.add(id.toString()));
        allTags.addAll(tagSet);
    }

    private void rebuildCategoryEntries() {
        categoryEntries.clear();
        categoryEntries.add(new CategoryEntry(CategoryType.MODE, 0, null, KineticText.translatable("gui.kineticcore.items.mode.all")));
        categoryEntries.add(new CategoryEntry(CategoryType.MODE, 1, null, KineticText.translatable("gui.kineticcore.items.mode.inv")));
        categoryEntries.add(new CategoryEntry(CategoryType.VANILLA, 0, "blocks", KineticText.translatable("gui.kineticcore.items.category.blocks")));
        categoryEntries.add(new CategoryEntry(CategoryType.VANILLA, 0, "redstone", KineticText.translatable("gui.kineticcore.items.category.redstone")));
        categoryEntries.add(new CategoryEntry(CategoryType.VANILLA, 0, "tools", KineticText.translatable("gui.kineticcore.items.category.tools")));
        categoryEntries.add(new CategoryEntry(CategoryType.VANILLA, 0, "combat", KineticText.translatable("gui.kineticcore.items.category.combat")));
        categoryEntries.add(new CategoryEntry(CategoryType.VANILLA, 0, "food", KineticText.translatable("gui.kineticcore.items.category.food")));
        categoryEntries.add(new CategoryEntry(CategoryType.VANILLA, 0, "ingredients", KineticText.translatable("gui.kineticcore.items.category.ingredients")));
        categoryEntries.add(new CategoryEntry(CategoryType.VANILLA, 0, "spawn_eggs", KineticText.translatable("gui.kineticcore.items.category.spawn_eggs")));
        categoryEntries.add(new CategoryEntry(CategoryType.HEADER, 0, null, KineticText.translatable("gui.kineticcore.items.category.mods")));
        for (String modId : allMods) {
            categoryEntries.add(new CategoryEntry(CategoryType.MOD, 0, modId, Component.literal("@" + modId)));
        }
        categoryScroll.update(categoryEntries.size(), FIXED_GRID_ROWS);
    }

    @Override
    protected void buildUi() {
        resetScrollableWidgets();
        if (!KineticItemSearch.ready()) {
            KineticItemSearch.prepare(() -> KineticClientRuntime.execute(() -> {
                if (KineticClientRuntime.currentScreen() == this) {
                    rebuildUi();
                }
            }));
            return;
        }

        gridCols = FIXED_GRID_COLS;
        gridRowsVisible = FIXED_GRID_ROWS;
        gridY = 34;
        categoryY = gridY;

        int contentW = gridContentWidth();
        int totalWidth = CATEGORY_WIDTH
                + CATEGORY_SCROLLBAR_WIDTH
                + CATEGORY_GAP
                + contentW
                + SCROLLBAR_WIDTH
                + 4;

        categoryX = Math.max(8, (canvasWidth() - totalWidth) / 2);
        gridX = categoryX + CATEGORY_WIDTH + CATEGORY_SCROLLBAR_WIDTH + CATEGORY_GAP - GRID_SHIFT_LEFT;

        int gap = 4;
        int topY = 5;
        int backBtnW = 40;
        int applyBtnW = 45;
        int rightEdge = gridX + contentW + SCROLLBAR_WIDTH + 4;

        int backBtnX = rightEdge - backBtnW - 2;
        int applyBtnX = backBtnX - gap - applyBtnW;
        btnAreaStartX = applyBtnX;
        topInfoY = topY + 6;

        addButton(
                backBtnX, topY, backBtnW,
                KineticText.translatable("gui.kineticcore.config.back"),
                null,
                this::navigateBack
        );

        applyFilterBtn = addButton(
                applyBtnX, topY, applyBtnW,
                KineticText.translatable("gui.kineticcore.items.filter.apply"),
                null,
                this::applyFilterAsResult
        );
        applyFilterBtn.setVisible(false);
        applyFilterBtn.setEnabled(false);

        int searchX = gridX;
        int maxSearchWidth = Math.max(100, applyBtnX - gap - searchX);
        int searchWidth = Math.min(220, maxSearchWidth);

        searchBox = addAutoCompleteField(
                searchX, topY, searchWidth, Component.empty(),
                KineticText.translatable("gui.kineticcore.items.search.hint"),
                this::autoCompleteSuggestions,
                null
        );

        searchBox.setMaxLength(1024);
        searchBox.setMaxVisibleSuggestions(10);
        searchBox.setResponder(this::onSearchInput);
        searchBox.setSelectionResponder(this::applySelectedSuggestion);
        searchBox.setValue(searchText);

        categoryScroll.update(categoryEntries.size(), FIXED_GRID_ROWS);
        createCategoryButtons();
        refreshDisplay();
    }



    private void onSearchInput(String text) {
        searchText = text == null ? "" : text;
        rememberedSearch = searchText;
        refreshDisplay();
    }

    private List<KineticAutoComplete.Suggestion> autoCompleteSuggestions() {
        String current = searchBox == null ? searchText : searchBox.getValue();
        String trimmed = current == null ? "" : current.trim();
        if (trimmed.startsWith("@")) {
            return allMods.stream()
                    .map(mod -> new KineticAutoComplete.Suggestion("@" + mod, Component.empty()))
                    .toList();
        }
        if (trimmed.startsWith("#")) {
            return allTags.stream()
                    .map(tag -> new KineticAutoComplete.Suggestion("#" + tag, Component.empty()))
                    .toList();
        }
        return List.of();
    }

    private void applySelectedSuggestion(String value) {
        if (value == null || value.length() < 2) {
            return;
        }
        if (value.startsWith("@")) {
            activeFilterType = 1;
            activeFilterValue = value.substring(1);
        } else if (value.startsWith("#")) {
            activeFilterType = 2;
            activeFilterValue = value.substring(1);
        } else {
            return;
        }
        rememberedFilterType = activeFilterType;
        rememberedFilterValue = activeFilterValue;
        refreshDisplay();
    }

    private void refreshDisplay() {
        ensureSourceCache();

        String text = searchBox != null ? searchBox.getValue() : "";
        String query = text.toLowerCase(Locale.ROOT).trim();
        mainScroll.reset();

        DisplayCacheKey cacheKey = new DisplayCacheKey(
                mode,
                query,
                activeFilterType,
                activeFilterValue == null ? "" : activeFilterValue,
                categoryKey == null ? "" : categoryKey
        );
        List<KineticItemSearch.CachedItem> cached = displayCache.get(cacheKey);
        if (cached != null) {
            displayList = cached;
        } else {
            List<KineticItemSearch.CachedItem> source = sourceForMode();
            if (activeFilterType == 0 && query.isEmpty()) {
                displayList = source;
            } else {
                List<KineticItemSearch.CachedItem> scanSource = findCachedSearchBase(cacheKey, source);
                List<KineticItemSearch.CachedItem> filtered = new ArrayList<>();

                String extraQuery = query;
                if (activeFilterType != 0 && (extraQuery.startsWith("@") || extraQuery.startsWith("#"))) {
                    extraQuery = "";
                }

                for (KineticItemSearch.CachedItem item : scanSource) {
                    if (failsActiveFilter(item)) {
                        continue;
                    }
                    if (failsSearch(item, query, extraQuery)) {
                        continue;
                    }
                    filtered.add(item);
                }

                displayList = List.copyOf(filtered);
            }
            displayCache.put(cacheKey, displayList);
        }

        mainScroll.update(
                totalDisplayRows(),
                gridRowsVisible
        );
        markDisplayChanged();
        updateApplyButton();
    }

    private void ensureSourceCache() {
        List<KineticItemSearch.CachedItem> currentItems = KineticItemSearch.items();
        if (currentItems == cachedItemSearchIndexIdentity) {
            return;
        }

        cachedItemSearchIndexIdentity = currentItems;
        cachedAllSource = buildSelectableSource(currentItems);
        cachedInventorySource = buildSelectableSource(invItems);

        Map<String, List<KineticItemSearch.CachedItem>> groupedMods = new LinkedHashMap<>();
        Map<String, List<KineticItemSearch.CachedItem>> groupedVanilla = new LinkedHashMap<>();
        groupedVanilla.put("blocks", new ArrayList<>());
        groupedVanilla.put("redstone", new ArrayList<>());
        groupedVanilla.put("tools", new ArrayList<>());
        groupedVanilla.put("combat", new ArrayList<>());
        groupedVanilla.put("food", new ArrayList<>());
        groupedVanilla.put("ingredients", new ArrayList<>());
        groupedVanilla.put("spawn_eggs", new ArrayList<>());

        for (KineticItemSearch.CachedItem item : cachedAllSource) {
            String namespace = getNamespace(item);
            if (!namespace.isEmpty()) {
                groupedMods.computeIfAbsent(namespace, key -> new ArrayList<>()).add(item);
            }
            for (String category : groupedVanilla.keySet()) {
                if (belongsToVanillaCategory(item, category)) {
                    groupedVanilla.get(category).add(item);
                }
            }
        }

        cachedModSources = freezeGroupedSources(groupedMods);
        cachedVanillaCategorySources = freezeGroupedSources(groupedVanilla);
        displayCache.clear();
    }

    private List<KineticItemSearch.CachedItem> buildSelectableSource(List<KineticItemSearch.CachedItem> source) {
        List<KineticItemSearch.CachedItem> result = new ArrayList<>();
        for (KineticItemSearch.CachedItem item : source) {
            if (isSelectable(item)) {
                result.add(item);
            }
        }
        return List.copyOf(result);
    }

    private boolean isSelectable(KineticItemSearch.CachedItem item) {
        return item != null && item.stack() != null && !item.stack().isEmpty();
    }

    private Map<String, List<KineticItemSearch.CachedItem>> freezeGroupedSources(Map<String, List<KineticItemSearch.CachedItem>> grouped) {
        Map<String, List<KineticItemSearch.CachedItem>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<KineticItemSearch.CachedItem>> entry : grouped.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutable);
    }

    private boolean belongsToVanillaCategory(KineticItemSearch.CachedItem cachedItem, String category) {
        ItemStack stack = cachedItem.stack();
        Item item = stack.getItem();
        String path = registryPath(cachedItem);

        return switch (category) {
            case "blocks" -> item instanceof BlockItem;
            case "redstone" -> isRedstoneItem(item, path);
            case "tools" -> isToolItem(item, path);
            case "combat" -> isCombatItem(item, path);
            case "food" -> stack.isEdible() || item instanceof PotionItem;
            case "ingredients" -> isIngredientItem(cachedItem, path);
            case "spawn_eggs" -> item instanceof SpawnEggItem;
            default -> false;
        };
    }

    private boolean isRedstoneItem(Item item, String path) {
        if (!(item instanceof BlockItem)) {
            return path.equals("redstone") || path.equals("repeater") || path.equals("comparator");
        }
        return containsAny(path,
                "redstone", "repeater", "comparator", "piston", "observer", "lever", "button",
                "pressure_plate", "tripwire", "dispenser", "dropper", "hopper", "target",
                "daylight_detector", "note_block", "jukebox", "tnt", "rail", "sculk_sensor",
                "lightning_rod");
    }

    private boolean isToolItem(Item item, String path) {
        return item instanceof DiggerItem
                || item instanceof ShearsItem
                || item instanceof FishingRodItem
                || item instanceof FlintAndSteelItem
                || item instanceof BrushItem
                || item instanceof BucketItem
                || item instanceof CompassItem
                || containsAny(path, "clock", "spyglass", "lead", "name_tag", "saddle");
    }

    private boolean isCombatItem(Item item, String path) {
        return item instanceof SwordItem
                || item instanceof ArmorItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof ShieldItem
                || item instanceof ArrowItem
                || containsAny(path, "totem_of_undying");
    }

    private boolean isIngredientItem(KineticItemSearch.CachedItem item, String path) {
        if (containsAny(path,
                "ingot", "nugget", "diamond", "emerald", "lapis", "quartz", "amethyst", "coal",
                "charcoal", "scrap", "shard", "crystal", "dust", "powder", "rod", "stick",
                "string", "feather", "leather", "paper", "book", "brick", "flint", "bone",
                "gunpowder", "slime_ball", "magma_cream", "ghast_tear", "ender_pearl", "ender_eye",
                "membrane", "shell", "heart_of_the_sea", "echo_shard", "raw_")) {
            return true;
        }
        String searchData = String.join(" ", item.tagIds());
        return containsAny(searchData,
                "#forge:ingots/", "#forge:nuggets/", "#forge:gems/", "#forge:dusts/",
                "#forge:raw_materials/", "#forge:rods/", "#forge:plates/", "#forge:shards/",
                "#forge:crystals/");
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String registryPath(KineticItemSearch.CachedItem item) {
        if (item == null || item.id() == null) {
            return "";
        }
        int colon = item.id().indexOf(':');
        String path = colon >= 0 && colon + 1 < item.id().length()
                ? item.id().substring(colon + 1)
                : item.id();
        return path.toLowerCase(Locale.ROOT);
    }

    private List<KineticItemSearch.CachedItem> sourceForMode() {
        if (mode == 1) {
            return cachedInventorySource;
        }
        if (categoryKey == null || categoryKey.isBlank()) {
            return cachedAllSource;
        }
        if (categoryKey.startsWith("vanilla:")) {
            return cachedVanillaCategorySources.getOrDefault(categoryKey.substring("vanilla:".length()), List.of());
        }
        if (categoryKey.startsWith("mod:")) {
            return cachedModSources.getOrDefault(categoryKey.substring("mod:".length()), List.of());
        }
        return cachedAllSource;
    }

    private List<KineticItemSearch.CachedItem> rawSourceForMode() {
        return sourceForMode();
    }

    private String getNamespace(KineticItemSearch.CachedItem item) {
        return item == null ? "" : item.namespace().toLowerCase(Locale.ROOT);
    }

    private boolean failsActiveFilter(KineticItemSearch.CachedItem item) {
        if (activeFilterType == 1 && activeFilterValue != null) {
            return !item.namespace().equals(activeFilterValue);
        }
        if (activeFilterType == 2 && activeFilterValue != null) {
            return !item.tagIds().contains(activeFilterValue);
        }
        return false;
    }

    private boolean failsSearch(KineticItemSearch.CachedItem item, String query, String extraQuery) {
        if (activeFilterType != 0) {
            return !extraQuery.isEmpty() && !item.matches(extraQuery);
        }
        if (query.startsWith("@") && query.length() > 1) {
            return !item.namespace().contains(query.substring(1));
        }
        if (query.startsWith("#") && query.length() > 1) {
            String tagQuery = query.substring(1);
            return item.tagIds().stream().noneMatch(tag -> tag.contains(tagQuery));
        }
        return !query.isEmpty() && !item.matches(query);
    }

    private List<KineticItemSearch.CachedItem> findCachedSearchBase(
            DisplayCacheKey requested,
            List<KineticItemSearch.CachedItem> fallback
    ) {
        List<KineticItemSearch.CachedItem> best = fallback;
        int bestLength = -1;

        for (Map.Entry<DisplayCacheKey, List<KineticItemSearch.CachedItem>> entry : displayCache.entrySet()) {
            DisplayCacheKey candidate = entry.getKey();
            if (candidate.mode() != requested.mode()
                    || candidate.filterType() != requested.filterType()
                    || !candidate.filterValue().equals(requested.filterValue())
                    || !candidate.categoryKey().equals(requested.categoryKey())
                    || candidate.query().length() >= requested.query().length()
                    || !requested.query().startsWith(candidate.query())) {
                continue;
            }

            if (candidate.query().length() > bestLength) {
                bestLength = candidate.query().length();
                best = entry.getValue();
            }
        }

        return best;
    }

    private void updateApplyButton() {
        if (applyFilterBtn == null) return;
        boolean canApply = activeFilterType != 0 && activeFilterValue != null && !activeFilterValue.isBlank();
        applyFilterBtn.setVisible(canApply);
        applyFilterBtn.setEnabled(canApply);
    }



    private void clearActiveFilter() {
        activeFilterType = 0;
        activeFilterValue = null;
        rememberedFilterType = 0;
        rememberedFilterValue = null;
        searchText = "";
        rememberedSearch = "";
        searchBox.setValue("");
        refreshDisplay();
    }

    private void applyFilterAsResult() {
        if (onSelect != null) {
            if (activeFilterType == 2 && activeFilterValue != null) {
                onSelect.accept(Selection.tag(activeFilterValue));
            } else if (activeFilterType == 1 && activeFilterValue != null) {
                onSelect.accept(Selection.mod(activeFilterValue));
            }
        }
        this.navigateBack();
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (KineticItemSearch.ready()) {
            syncCategoryButtons();
        }
        graphics.fillGradient(0, 0, this.canvasWidth(), this.canvasHeight(), 0xFF222222, 0xFF111111);
        graphics.fill(0, this.gridY - 4, this.canvasWidth(), this.gridY - 3, 0x40FFFFFF);

    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!KineticItemSearch.ready()) return;
        renderTopInfo(graphics, mouseX, mouseY);
        renderCategories(graphics, mouseX, mouseY);
        renderItems(graphics, mouseX, mouseY);
        renderMainScrollbar(
                graphics,
                mouseX,
                mouseY
        );
    }

    private void renderTopInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        if (searchBox == null) return;
        int infoX = searchBox.getX() + searchBox.getWidth() + 6;

        int infoY =
                topInfoY;

        int maxInfoX =
                btnAreaStartX - 6;
        List<KineticItemSearch.CachedItem> src = rawSourceForMode();
        Component countText = KineticText.translatable(
                "gui.kineticcore.items.count",
                Component.literal(String.format("%,d", displayList.size())),
                Component.literal(String.format("%,d", src.size()))
        );
        if (infoX + font.width(countText) >= maxInfoX) {
            return;
        }

        graphics.drawString(
                font,
                countText,
                infoX,
                infoY,
                0xFFFFFF,
                true
        );

        int nextX =
                infoX
                        + font.width(countText)
                        + 8;

        if (activeFilterType == 0 || activeFilterValue == null || nextX >= maxInfoX) return;
        String prefix = activeFilterType == 1 ? "@" : "#";
        String fullLabel = prefix + activeFilterValue;
        int filterW = Math.max(14, maxInfoX - nextX);
        int availW = Math.max(0, filterW - 14);
        Component filterComp = KineticText.translatable("gui.kineticcore.items.filter.label", Component.literal(fullLabel));
        int badgeY = searchBox.getY() + 2;

        graphics.fill(nextX, badgeY, nextX + filterW, badgeY + 16, 0xCC2A2A2A);
        GuiTheme.stateOutline(graphics, nextX, badgeY, filterW, 16, false, false, false);
        KineticText.drawScrollingLeft(graphics, this.font, filterComp, nextX + 3, badgeY + 4, availW, 0xFFFFFF, false);

        int closeX = nextX + filterW - 11;
        int closeY = badgeY + 4;
        boolean closeHovered = mouseX >= closeX - 1 && mouseX < closeX + 7 && mouseY >= closeY - 1 && mouseY < closeY + 9;
        graphics.drawString(this.font, "✕", closeX, closeY, closeHovered ? 0xFFFFFF : 0xFF5555, false);
    }

    private void renderCategories(GuiGraphics graphics, int mouseX, int mouseY) {
        syncCategoryButtons();
        categoryScroll.render(
                graphics,
                mouseX,
                mouseY,
                categoryScrollbarX(),
                categoryY,
                CATEGORY_SCROLLBAR_WIDTH,
                gridContentHeight(),
                20
        );
    }

    private void createCategoryButtons() {
        categoryButtons.clear();
        for (int index = 0; index < categoryEntries.size(); index++) {
            int categoryIndex = index;
            StateButton button = addCompactScrollableButton(
                    categoryButtonX(),
                    categoryY + index * CELL_SIZE,
                    CATEGORY_BUTTON_WIDTH,
                    Component.empty(),
                    null,
                    () -> selectCategoryIndex(categoryIndex),
                    categoryButtonX(),
                    categoryY,
                    categoryButtonX() + CATEGORY_BUTTON_WIDTH,
                    categoryY + gridContentHeight(),
                    () -> categoryScroll.smoothOffset() * CELL_SIZE
            );
            categoryButtons.add(button);
        }
        syncCategoryButtons();
    }

    private void syncCategoryButtons() {
        categoryScroll.update(categoryEntries.size(), FIXED_GRID_ROWS);
        for (int index = 0; index < categoryButtons.size(); index++) {
            StateButton button = categoryButtons.get(index);
            if (index >= categoryEntries.size()) {
                button.setVisible(false);
                button.setEnabled(false);
                button.setSelected(false);
                continue;
            }

            CategoryEntry entry = categoryEntries.get(index);
            button.setVisible(true);
            button.setText(entry.label());
            button.setEnabled(entry.selectable());
            button.setSelected(entry.selectable() && isCategoryActive(entry));
        }
    }

    private void selectCategoryIndex(int index) {
        if (index < 0 || index >= categoryEntries.size()) {
            return;
        }

        CategoryEntry entry = categoryEntries.get(index);
        if (!entry.selectable()) {
            return;
        }

        mode = entry.mode();
        categoryKey = switch (entry.type()) {
            case VANILLA -> "vanilla:" + entry.key();
            case MOD -> "mod:" + entry.key();
            default -> null;
        };
        rememberedMode = mode;
        rememberedCategoryKey = categoryKey;
        refreshDisplay();
        syncCategoryButtons();
        if (searchBox != null) searchBox.clearSuggestions();
    }

    private boolean isCategoryActive(CategoryEntry entry) {
        return switch (entry.type()) {
            case MODE -> categoryKey == null && mode == entry.mode();
            case VANILLA -> mode == 0 && ("vanilla:" + entry.key()).equals(categoryKey);
            case MOD -> mode == 0 && ("mod:" + entry.key()).equals(categoryKey);
            case HEADER -> false;
        };
    }

    private boolean handleCategoryClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        categoryScroll.update(categoryEntries.size(), FIXED_GRID_ROWS);
        if (categoryScroll.beginDrag(
                mouseX,
                mouseY,
                categoryScrollbarX(),
                categoryY,
                CATEGORY_SCROLLBAR_WIDTH,
                gridContentHeight(),
                20,
                0
        )) {
            return true;
        }
        if (mouseX >= categoryButtonX()
                && mouseX < categoryButtonX() + CATEGORY_BUTTON_WIDTH
                && mouseY >= categoryY
                && mouseY < categoryY + gridContentHeight()) {
            double contentY = mouseY - categoryY + categoryScroll.smoothOffset() * CELL_SIZE;
            int index = (int) Math.floor(contentY / CELL_SIZE);
            int within = (int) Math.floor(contentY - index * CELL_SIZE);
            if (within < SLOT_SIZE) {
                selectCategoryIndex(index);
                return true;
            }
        }
        return false;
    }

    private int categoryButtonX() {
        return categoryX + CATEGORY_BUTTON_SHIFT_X;
    }

    private int categoryScrollbarX() {
        return categoryButtonX() + CATEGORY_BUTTON_WIDTH + CATEGORY_SCROLL_GAP + CATEGORY_SCROLLBAR_SHIFT_X;
    }

    private int mainScrollbarX() {
        return gridX + gridContentWidth() + MAIN_SCROLL_GAP;
    }

    private void renderItems(GuiGraphics graphics, int mouseX, int mouseY) {
        ensureVisibleSlotCache();
        enableUiScissor(
                graphics,
                gridX,
                gridY,
                gridX + gridContentWidth(),
                gridY + gridContentHeight()
        );
        try {
            boolean mouseInGrid = mouseX >= gridX
                    && mouseX < gridX + gridContentWidth()
                    && mouseY >= gridY
                    && mouseY < gridY + gridContentHeight();
            for (VisibleSlot slot : visibleSlotCache) {
                boolean hovered = mouseInGrid && slot.contains(mouseX, mouseY);
                GuiTheme.itemSlot(graphics, slot.x(), slot.y(), SLOT_SIZE, SLOT_SIZE, 4, false, hovered, false);
                graphics.renderItem(slot.stack(), slot.x() + 1, slot.y() + 1);
            }
        } finally {
            disableUiScissor(graphics);
        }
    }



    private void ensureVisibleSlotCache() {
        visibleSlotCache.clear();
        int firstRow = mainScroll.smoothIndexOffset();
        int shiftY = mainScroll.visualShift(CELL_SIZE);
        int startIndex = firstRow * gridCols;
        int endIndex = Math.min(
                startIndex + (gridRowsVisible + 1) * gridCols,
                displayList.size()
        );

        for (int index = startIndex; index < endIndex; index++) {
            int localIndex = index - startIndex;
            int column = localIndex % gridCols;
            int row = localIndex / gridCols;
            int x = gridX + column * CELL_SIZE;
            int y = gridY + row * CELL_SIZE - shiftY;
            if (y + SLOT_SIZE <= gridY || y >= gridY + gridContentHeight()) continue;
            ItemStack stack = displayList.get(index).stack();
            visibleSlotCache.add(new VisibleSlot(index, stack, x, y));
        }

        cachedVisibleDisplayVersion = displayVersion;
        cachedVisibleScroll = mainScroll.offset();
        cachedVisibleCols = gridCols;
        cachedVisibleRows = gridRowsVisible;
    }

    private void invalidateVisibleSlotCache() {
        cachedVisibleDisplayVersion = -1;
        cachedVisibleScroll = -1;
        visibleSlotCache.clear();
    }

    private void markDisplayChanged() {
        displayVersion++;
        invalidateVisibleSlotCache();
    }

    private int gridContentWidth() {
        return gridCols * CELL_SIZE;
    }

    private int gridContentHeight() {
        return gridRowsVisible * CELL_SIZE;
    }

    private int totalDisplayRows() {
        return (int) Math.ceil((double) displayList.size() / Math.max(1, gridCols));
    }

    private VisibleSlot findVisibleSlot(double mouseX, double mouseY) {
        if (mouseX < gridX
                || mouseX >= gridX + gridContentWidth()
                || mouseY < gridY
                || mouseY >= gridY + gridContentHeight()) {
            return null;
        }
        ensureVisibleSlotCache();
        for (VisibleSlot slot : visibleSlotCache) {
            if (slot.contains(mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private void renderMainScrollbar(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        mainScroll.update(
                totalDisplayRows(),
                gridRowsVisible
        );

        mainScroll.render(
                graphics,
                mouseX,
                mouseY,
                mainScrollbarX(),
                gridY,
                SCROLLBAR_WIDTH,
                gridContentHeight(),
                20
        );
    }



    @Override
    protected void renderTooltips(GuiGraphics graphics, int scaledMouseX, int scaledMouseY, int rawMouseX, int rawMouseY) {
        if (!KineticItemSearch.ready() || (searchBox != null && searchBox.isSuggestionPopupOpen())) {
            return;
        }
        VisibleSlot slot = findVisibleSlot(scaledMouseX, scaledMouseY);
        if (slot == null) {
            return;
        }
        showItemTooltip(slot.stack());
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        unfocusSearchIfNeeded(mouseX, mouseY);
        if (handleFilterCloseClick(mouseX, mouseY, button)) return true;
        if (handleCategoryClick(mouseX, mouseY, button)) {
            if (searchBox != null) searchBox.clearSuggestions();
            return true;
        }
        if (super.canvasMouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && KineticItemSearch.ready()) return handleListClick(mouseX, mouseY);
        return false;
    }

    private void unfocusSearchIfNeeded(double mouseX, double mouseY) {
        if (this.searchBox == null || this.searchBox.isMouseOver(mouseX, mouseY)) return;
        blurControl(this.searchBox);
    }

    private boolean handleFilterCloseClick(double mouseX, double mouseY, int button) {
        if (activeFilterType == 0 || activeFilterValue == null || searchBox == null || button != 0) return false;
        int infoX = searchBox.getX() + searchBox.getWidth() + 6;
        List<KineticItemSearch.CachedItem> src = rawSourceForMode();
        Component countText = KineticText.translatable(
                "gui.kineticcore.items.count",
                Component.literal(String.format("%,d", displayList.size())),
                Component.literal(String.format("%,d", src.size()))
        );
        int nextX = infoX + this.font.width(countText) + 8;
        int maxInfoX = btnAreaStartX - 6;
        if (nextX >= maxInfoX) return false;

        String prefix = activeFilterType == 1 ? "@" : "#";
        String fullLabel = prefix + activeFilterValue;
        int filterW = Math.max(14, maxInfoX - nextX);
        int availW = Math.max(0, filterW - 14);
        Component filterComp = KineticText.translatable("gui.kineticcore.items.filter.label", Component.literal(fullLabel));
        int closeX = nextX + filterW - 11;
        int closeY = searchBox.getY() + 6;
        if (mouseX >= closeX - 2 && mouseX < closeX + 8 && mouseY >= closeY - 2 && mouseY < closeY + 10) {
            clearActiveFilter();
            return true;
        }
        return false;
    }



    private boolean handleListClick(double mouseX, double mouseY) {
        int contentWidth = gridContentWidth();
        int contentHeight = gridContentHeight();
        mainScroll.update(
                totalDisplayRows(),
                gridRowsVisible
        );

        if (mainScroll.beginDrag(
                mouseX,
                mouseY,
                gridX
                        + contentWidth
                        + MAIN_SCROLL_GAP,
                gridY,
                SCROLLBAR_WIDTH,
                contentHeight,
                20,
                0
        )) {
            invalidateVisibleSlotCache();
            return true;
        }

        VisibleSlot slot = findVisibleSlot(mouseX, mouseY);
        if (slot == null || slot.displayIndex() < 0 || slot.displayIndex() >= displayList.size()) {
            return false;
        }

        if (onSelect != null) {
            onSelect.accept(Selection.item(displayList.get(slot.displayIndex()).stack()));
        }
        this.navigateBack();
        return true;
    }


    @Override
    protected boolean canvasMouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        boolean handled = mainScroll.release(button);
        handled |= categoryScroll.release(button);
        return handled || super.canvasMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (categoryScroll.drag(mouseY, categoryY, gridContentHeight(), 20)) {
            return true;
        }
        if (mainScroll.drag(mouseY, gridY, gridContentHeight(), 20)) {
            invalidateVisibleSlotCache();
            return true;
        }
        return super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (mouseX >= categoryButtonX()
                && mouseX < categoryScrollbarX() + CATEGORY_SCROLLBAR_WIDTH + 2
                && mouseY >= categoryY
                && mouseY < categoryY + gridContentHeight()) {
            categoryScroll.update(categoryEntries.size(), FIXED_GRID_ROWS);
            if (categoryScroll.scroll(delta, 1.0D)) {
                return true;
            }
        }
        mainScroll.update(totalDisplayRows(), gridRowsVisible);
        if (mainScroll.scroll(delta, 1.0D)) {
            invalidateVisibleSlotCache();
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

}
