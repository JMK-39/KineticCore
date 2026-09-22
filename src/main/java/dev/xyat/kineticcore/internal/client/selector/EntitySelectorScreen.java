package dev.xyat.kineticcore.internal.client.selector;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.GridScrollController;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.render.KineticEntityPreview.EntityPreviewRenderer;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.NeutralMob;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Reusable visual multi-selector for exact entity IDs. It edits a local draft
 * only: applying invokes the callback, while persistence remains the caller's
 * responsibility.
 */
public final class EntitySelectorScreen extends KineticScreen {
    private static final int COLS = 8;
    private static final int CELL_W = 68;
    private static final int CELL_H = 68;
    private static final int GRID_X = 42;
    private static final int GRID_Y = 67;
    private static final int VISIBLE_ROWS = 3;
    private static final int GRID_W = COLS * CELL_W;
    private static final int GRID_H = VISIBLE_ROWS * CELL_H;
    private static final int SCROLL_X = GRID_X + GRID_W + 6;
    private static final int SCROLL_W = 4;
    private static final int MODS_PER_PAGE = 8;

    private enum CategoryFilter { ALL, FRIENDLY, AQUATIC, NEUTRAL, MONSTER, UNDEAD, MISC }

    /** The two filter groups are independent; an empty group matches all. */
    private final EnumSet<CategoryFilter> selectedCategories = EnumSet.noneOf(CategoryFilter.class);
    private final Set<String> selectedMods = new TreeSet<>();
    private final List<String> availableMods = new ArrayList<>();
    private final Map<String, CategoryFilter> entityCategoryCache = new HashMap<>();
    private final Screen parent;
    private final Consumer<List<String>> onApply;
    private final List<String> allEntityIds = new ArrayList<>();
    private final List<String> filteredEntityIds = new ArrayList<>();
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private final Set<String> originalIds = new LinkedHashSet<>();
    private final Map<String, String> searchData = new HashMap<>();
    private final GridScrollController scroll = new GridScrollController();
    private final EntityPreviewRenderer previewRenderer = KineticWidgets.createEntityPreviewRenderer(
            EntityPreviewRenderer.DEFAULT_CACHE_SIZE,
            EntityPreviewRenderer.DEFAULT_FILL_RATIO,
            EntityPreviewRenderer.DEFAULT_MAX_AUTO_SCALE_FACTOR
    );

    private KineticEditBox searchBox;
    private String searchQuery = "";
    private List<Component> deferredTooltip;

    public EntitySelectorScreen(
            Screen parent, Component title, Collection<String> initialEntityIds,
            Consumer<List<String>> onApply
    ) {
        this(parent, title, initialEntityIds, null, onApply);
    }

    public EntitySelectorScreen(
            Screen parent, Component title, Collection<String> initialEntityIds,
            Collection<String> allowedEntityIds, Consumer<List<String>> onApply
    ) {
        super(title);
        this.parent = parent;
        setParentScreen(parent);
        this.onApply = onApply;
        Set<String> allowed = allowedEntityIds == null ? null : new LinkedHashSet<>(allowedEntityIds);
        KineticRegistries.entityTypes().ids().stream()
                .map(ResourceLocation::toString)
                .filter(id -> allowed == null || allowed.contains(id))
                .sorted(String::compareToIgnoreCase)
                .forEach(allEntityIds::add);
        if (initialEntityIds != null) {
            for (String value : initialEntityIds) {
                if (value == null || value.isBlank()) continue;
                String id = value.trim();
                if (allowed != null && !allowed.contains(id)) continue;
                selectedIds.add(id);
                // Preserve a caller's saved IDs even when the corresponding mod is temporarily absent.
                if (!allEntityIds.contains(id)) allEntityIds.add(id);
            }
        }
        allEntityIds.sort(String::compareToIgnoreCase);
        allEntityIds.stream()
                .map(KineticResourceIds::tryParse)
                .filter(java.util.Objects::nonNull)
                .map(ResourceLocation::getNamespace)
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .forEach(availableMods::add);
        originalIds.addAll(selectedIds);
        buildSearchData();
    }

    @Override
    protected void buildUi() {
        searchBox = addTextField(
                GRID_X, 38, Math.min(GRID_W, 430),
                KineticText.translatable("gui.kineticcore.entity_selector.search_hint"),
                KineticText.translatable("gui.kineticcore.entity_selector.search_hint"),
                null, null
        );
        searchBox.setMaxLength(256);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(query -> {
            searchQuery = query == null ? "" : query;
            updateSearch(searchQuery);
        });

        addCompactButton(GRID_X + 438, 38, 96,
                KineticText.translatable("gui.kineticcore.entity_selector.filter"),
                KineticText.translatable("gui.kineticcore.entity_selector.filter.tooltip"),
                this::showFilterMenu);

        addButton(166, 325, 92, KineticText.translatable("gui.kineticcore.entity_selector.clear"), null, selectedIds::clear);
        addButton(274, 325, 92, KineticText.translatable("gui.kineticcore.config.back"), null, this::onClose);
        addButton(382, 325, 92, KineticText.translatable("gui.kineticcore.entity_selector.apply"), null, this::applyAndReturn);

        updateSearch(searchQuery);
    }

    private void buildSearchData() {
        searchData.clear();
        for (String id : allEntityIds) {
            ResourceLocation location = KineticResourceIds.tryParse(id);
            EntityType<?> type = location == null ? null : KineticRegistries.entityTypes().get(location);
            String name = type == null ? id : type.getDescription().getString();
            String raw = id + " " + name;
            searchData.put(id, raw.toLowerCase(Locale.ROOT));
        }
    }

    /** The one selector filter menu supports category and mod filters together. */
    private void showFilterMenu() {
        List<KineticOverlays.MenuItem> entries = new ArrayList<>();
        entries.add(KineticOverlays.MenuItem.action(
                KineticText.translatable("gui.kineticcore.entity_selector.filter.reset"), () -> {
                    selectedCategories.clear();
                    selectedMods.clear();
                    updateSearch(searchQuery);
                    showFilterMenu();
                }));
        entries.add(KineticOverlays.MenuItem.separator());
        Component allCategories = KineticText.translatable("gui.kineticcore.entity_selector.category.all");
        entries.add(KineticOverlays.MenuItem.toggle(allCategories, allCategories,
                selectedCategories.isEmpty(), () -> {
                    selectedCategories.clear();
                    updateSearch(searchQuery);
                    showFilterMenu();
                }));
        for (CategoryFilter category : CategoryFilter.values()) {
            if (category == CategoryFilter.ALL) continue;
            Component name = KineticText.translatable("gui.kineticcore.entity_selector.category."
                    + category.name().toLowerCase(Locale.ROOT));
            entries.add(KineticOverlays.MenuItem.toggle(name, name, selectedCategories.contains(category), () -> {
                if (!selectedCategories.add(category)) selectedCategories.remove(category);
                updateSearch(searchQuery);
                showFilterMenu();
            }));
        }
        entries.add(KineticOverlays.MenuItem.separator());
        entries.add(KineticOverlays.MenuItem.action(
                KineticText.translatable("gui.kineticcore.entity_selector.filter.mods", selectedMods.size()),
                () -> showModMenu(0)));
        openContextMenu(GRID_X + 438, 60, entries);
    }

    /** Page the namespace menu so large modpacks do not overflow the screen. */
    private void showModMenu(int requestedPage) {
        int lastPage = Math.max(0, (availableMods.size() - 1) / MODS_PER_PAGE);
        int page = Math.max(0, Math.min(requestedPage, lastPage));
        List<KineticOverlays.MenuItem> entries = new ArrayList<>();
        entries.add(KineticOverlays.MenuItem.action(
                KineticText.translatable("gui.kineticcore.entity_selector.filter.back"), this::showFilterMenu));
        Component allMods = KineticText.translatable("gui.kineticcore.entity_selector.filter.mod_all");
        entries.add(KineticOverlays.MenuItem.toggle(allMods, allMods, selectedMods.isEmpty(), () -> {
            selectedMods.clear();
            updateSearch(searchQuery);
            showModMenu(page);
        }));
        entries.add(KineticOverlays.MenuItem.separator());
        int from = page * MODS_PER_PAGE;
        int to = Math.min(availableMods.size(), from + MODS_PER_PAGE);
        for (int i = from; i < to; i++) {
            String mod = availableMods.get(i);
            Component name = Component.literal(mod);
            entries.add(KineticOverlays.MenuItem.toggle(name, name, selectedMods.contains(mod), () -> {
                if (!selectedMods.add(mod)) selectedMods.remove(mod);
                updateSearch(searchQuery);
                showModMenu(page);
            }));
        }
        entries.add(KineticOverlays.MenuItem.separator());
        if (page > 0) {
            entries.add(KineticOverlays.MenuItem.action(
                    KineticText.translatable("gui.kineticcore.entity_selector.filter.previous"),
                    () -> showModMenu(page - 1)));
        }
        if (page < lastPage) {
            entries.add(KineticOverlays.MenuItem.action(
                    KineticText.translatable("gui.kineticcore.entity_selector.filter.next"),
                    () -> showModMenu(page + 1)));
        }
        openContextMenu(GRID_X + 438, 60, entries);
    }

    private CategoryFilter categoryOf(String id) {
        CategoryFilter cached = entityCategoryCache.get(id);
        if (cached != null) return cached;
        ResourceLocation key = KineticResourceIds.tryParse(id);
        EntityType<?> type = key == null ? null : KineticRegistries.entityTypes().get(key);
        CategoryFilter category = CategoryFilter.MISC;
        if (type != null) {
            MobCategory mobCategory = type.getCategory();
            boolean aquatic = mobCategory == MobCategory.WATER_CREATURE
                    || mobCategory == MobCategory.WATER_AMBIENT
                    || mobCategory == MobCategory.UNDERGROUND_WATER_CREATURE
                    || mobCategory == MobCategory.AXOLOTLS;
            boolean neutral = false;
            boolean undead = false;
            var level = KineticClientRuntime.currentLevel();
            if (level != null) {
                try {
                    Entity preview = type.create(level);
                    neutral = preview instanceof NeutralMob;
                    undead = preview instanceof LivingEntity living && living.getMobType() == MobType.UNDEAD;
                } catch (Throwable ignored) {
                    // An incompatible mod preview cannot break the shared selector.
                }
            }
            category = aquatic ? CategoryFilter.AQUATIC
                    : undead ? CategoryFilter.UNDEAD
                    : neutral ? CategoryFilter.NEUTRAL
                    : mobCategory == MobCategory.MONSTER ? CategoryFilter.MONSTER
                    : mobCategory == MobCategory.CREATURE ? CategoryFilter.FRIENDLY
                    : CategoryFilter.MISC;
        }
        entityCategoryCache.put(id, category);
        return category;
    }

    private void updateSearch(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Set<String> categoryNames = new LinkedHashSet<>();
        for (CategoryFilter category : selectedCategories) categoryNames.add(category.name());
        filteredEntityIds.clear();
        for (String id : allEntityIds) {
            ResourceLocation location = KineticResourceIds.tryParse(id);
            String namespace = location == null ? "" : location.getNamespace();
            boolean nameMatches = normalized.isEmpty() || KineticSearch.match(
                    searchData.getOrDefault(id, id.toLowerCase(Locale.ROOT)), normalized);
            // Avoid creating entity previews for category detection unless a category was selected.
            String categoryName = selectedCategories.isEmpty() ? "" : categoryOf(id).name();
            if (EntityFilterMatcher.matches(categoryNames, categoryName,
                    selectedMods, namespace, nameMatches)) {
                filteredEntityIds.add(id);
            }
        }
        filteredEntityIds.sort((left, right) -> {
            int selectedCompare = Boolean.compare(selectedIds.contains(right), selectedIds.contains(left));
            return selectedCompare != 0 ? selectedCompare : left.compareToIgnoreCase(right);
        });
        scroll.reset();
        updateScrollRange();
    }

    private void updateScrollRange() {
        int totalRows = (filteredEntityIds.size() + COLS - 1) / COLS;
        scroll.updateRange(Math.max(0, totalRows - VISIBLE_ROWS), totalRows, VISIBLE_ROWS);
    }

    @Override
    protected void renderCanvasBackground(
            @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        deferredTooltip = null;
        GuiTheme.panel(graphics, 20, 12, 600, 342);
        graphics.drawCenteredString(font, title, canvasWidth() / 2, 22, 0xFFFFAA00);
        GuiTheme.panelAlt(graphics, GRID_X - 3, GRID_Y - 3, GRID_W + 6, GRID_H + 6);
        renderGrid(graphics, mouseX, mouseY);
        scroll.render(graphics, mouseX, mouseY,
                SCROLL_X, GRID_Y, SCROLL_W, GRID_H, 18
        );
        graphics.drawCenteredString(
                font,
                KineticText.translatable("gui.kineticcore.entity_selector.selected", Component.literal(String.valueOf(selectedIds.size()))),
                canvasWidth() / 2,
                306,
                0xFFAAAAAA
        );
        if (filteredEntityIds.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    KineticText.translatable("gui.kineticcore.entity_selector.empty"),
                    GRID_X + GRID_W / 2,
                    GRID_Y + GRID_H / 2,
                    0xFFAAAAAA
            );
        }
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int smoothRow = scroll.smoothIndexOffset();
        int scrollShift = scroll.visualShift(CELL_H);
        int first = smoothRow * COLS;
        int last = Math.min(
                first + (VISIBLE_ROWS + 1) * COLS,
                filteredEntityIds.size()
        );
        enableUiScissor(
                graphics,
                GRID_X,
                GRID_Y,
                GRID_X + GRID_W,
                GRID_Y + GRID_H
        );
        for (int index = first; index < last; index++) {
            int local = index - first;
            int x = GRID_X + local % COLS * CELL_W;
            int y = GRID_Y + local / COLS * CELL_H - scrollShift;
            String id = filteredEntityIds.get(index);
            boolean selected = selectedIds.contains(id);
            boolean hovered = mouseX >= x && mouseX < x + CELL_W
                    && mouseY >= y && mouseY < y + CELL_H;

            EntityPreviewRenderer.drawCheckerboard(graphics, x + 2, y + 2, CELL_W - 4, CELL_H - 16);
            GuiTheme.stateOutline(graphics, x, y, CELL_W, CELL_H, selected, hovered, false);
            if (selected) {
                GuiTheme.stateOutline(graphics, x + 1, y + 1, CELL_W - 2, CELL_H - 2, true, false, false);
            }

            boolean rendered = previewRenderer.render(
                    graphics, id, "selector:" + id,
                    x + 3, y + 3, CELL_W - 6, CELL_H - 19,
                    canvasScale(), canvasX(), canvasY(), hovered
            );
            if (!rendered) {
                graphics.drawCenteredString(font, "?", x + CELL_W / 2, y + 23, 0xFF777777);
            }
            KineticText.drawScrollingCentered(
                    graphics,
                    font,
                    Component.literal(entityName(id)),
                    x + CELL_W / 2,
                    y + CELL_H - 12,
                    CELL_W - 6,
                    selected ? 0xFF55FF55 : 0xFFE0E0E0,
                    false
            );
            if (hovered) {
                deferredTooltip = List.of(
                        Component.literal(entityName(id)),
                        Component.literal(id),
                        KineticText.translatable(selected
                                ? "gui.kineticcore.entity_selector.remove_hint"
                                : "gui.kineticcore.entity_selector.add_hint")
                );
            }
        }
        disableUiScissor(graphics);
    }

    private String entityName(String id) {
        ResourceLocation location = KineticResourceIds.tryParse(id);
        EntityType<?> type = location == null ? null : KineticRegistries.entityTypes().get(location);
        return type == null ? id : type.getDescription().getString();
    }

    private boolean inGrid(double mouseX, double mouseY) {
        return mouseX >= GRID_X && mouseX < GRID_X + GRID_W
                && mouseY >= GRID_Y && mouseY < GRID_Y + GRID_H;
    }

    private int entityIndex(double mouseX, double mouseY) {
        int column = (int) ((mouseX - GRID_X) / CELL_W);
        int row = (int) Math.floor(
                (mouseY - GRID_Y + scroll.visualShift(CELL_H)) / CELL_H
        );
        return scroll.smoothIndexOffset() * COLS + row * COLS + column;
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        boolean widgetHandled = super.canvasMouseClicked(mouseX, mouseY, button);
        if (button == 0 && scroll.beginDrag(
                mouseX, mouseY, SCROLL_X, GRID_Y, SCROLL_W, GRID_H, 18, 2)) return true;
        if (button == 0 && inGrid(mouseX, mouseY)) {
            int index = entityIndex(mouseX, mouseY);
            if (index >= 0 && index < filteredEntityIds.size()) {
                String id = filteredEntityIds.get(index);
                if (!selectedIds.add(id)) selectedIds.remove(id);
                return true;
            }
        }
        return widgetHandled;
    }

    @Override
    protected boolean canvasMouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        return scroll.drag(mouseY, GRID_Y, GRID_H, 18)
                || super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        return scroll.release(button)
                || super.canvasMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (KineticClientRuntime.controlModifierDown() && inGrid(mouseX, mouseY)) {
            int index = entityIndex(mouseX, mouseY);
            if (index >= 0 && index < filteredEntityIds.size()) {
                String id = filteredEntityIds.get(index);
                previewRenderer.adjustZoom("selector:" + id, delta);
                return true;
            }
        }
        if (inGrid(mouseX, mouseY) && scroll.scroll(delta, 1.0D)) return true;
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderTooltips(
            GuiGraphics graphics, int scaledMouseX, int scaledMouseY, int mouseX, int mouseY) {
        if (deferredTooltip != null) {
            showTooltip(deferredTooltip, null);
        }
    }

    private void applyAndReturn() {
        onApply.accept(new ArrayList<>(selectedIds));
        navigateBack();
    }

    @Override
    protected boolean handleCloseRequest() {
        if (selectedIds.equals(originalIds)) {
            return false;
        }
        openDialog(
                KineticText.translatable("gui.kineticcore.config.unsaved_action.title"),
                KineticText.translatable("gui.kineticcore.entity_selector.unsaved"),
                KineticText.translatable("gui.yes"),
                KineticText.translatable("gui.no"),
                this::applyAndReturn,
                this::navigateBack
        );
        return true;
    }

    @Override
    protected void screenRemoved() {
        entityCategoryCache.clear();
        previewRenderer.clear();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }


}
