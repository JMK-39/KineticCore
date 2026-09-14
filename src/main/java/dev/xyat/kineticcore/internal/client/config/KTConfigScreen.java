package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.config.client.*;

import dev.xyat.kineticcore.api.client.selector.KineticSelectors;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.ColorPreviewButton;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.GridScrollController;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.NumericEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Generic editor for one module-owned {@link KTConfigPage}. */
public final class KTConfigScreen extends KineticScreen {
    private enum SaveOutcome {
        FAILED,
        UNCHANGED,
        SAVED
    }

    private record DraftState(
            Map<String, Object> values,
            Map<String, String> rawValues,
            Set<String> invalid
    ) {
    }

    private static final int VISIBLE_ROWS = 8;
    private static final int ROW_TOP = 64;
    private static final int ROW_HEIGHT = 27;
    private static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
    private static final int SCROLL_X = 615;
    private static final int SCROLL_WIDTH = 4;

    private final Screen parent;
    private final KTConfigPage configPage;
    private final Map<String, Object> pendingValues = new HashMap<>();
    private final Map<String, Object> originalValues = new HashMap<>();
    private final Map<String, String> rawTextValues = new HashMap<>();
    private final Set<String> invalidEntries = new HashSet<>();
    private final Map<KTConfigEntry<?>, Integer> visibleRows = new HashMap<>();
    private final KineticSearch.Model<KTConfigEntry<?>> entryModel =
            new KineticSearch.Model<>(List.of(), KTConfigScreen::buildSearchData);
    private final GridScrollController entryScroll = new GridScrollController();
    private Component status;
    private EditBox searchBox;
    private KTConfigEntry<?> hoveredEntry;
    private String searchQuery = "";
    private boolean searchDirty;
    private final boolean showApplyTiming;

    public KTConfigScreen(Screen parent, KTConfigPage configPage) {
        super(configPage.title());
        this.parent = parent;
        this.configPage = configPage;
        this.showApplyTiming = configPage.showsApplyTiming();
        this.entryModel.setSource(configPage.entries());
        refreshFromSource();
        configureDraft(this::captureDraftState, this::restoreDraftState);
        KTServerConfigClient.request(configPage);
    }

    /**
     * Reloads every draft value after a specialized child editor saves the
     * same backing configuration. Child screens should call this before
     * returning so a later page save cannot restore stale values.
     */
    public void refreshFromSource() {
        pendingValues.clear();
        originalValues.clear();
        rawTextValues.clear();
        invalidEntries.clear();
        status = null;
        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (!entry.isValue()) continue;
            Object value;
            try {
                value = entry.readSnapshot();
            } catch (Throwable throwable) {
                KineticRuntime.logger().error("Failed to read config value {} from page {}",
                        entry.id(), configPage.id(), throwable);
                value = entry.defaultSnapshot();
            }
            if (!entry.accepts(value)) {
                KineticRuntime.logger().warn("Invalid config value {} on page {}; using its default",
                        entry.id(), configPage.id());
                value = entry.defaultSnapshot();
            }
            Object snapshot = entry.snapshot(value);
            pendingValues.put(entry.id(), snapshot);
            originalValues.put(entry.id(), entry.snapshot(snapshot));
        }
    }

    private DraftState captureDraftState() {
        Map<String, Object> values = new HashMap<>();
        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (!entry.isValue()) continue;
            Object value = pendingValues.get(entry.id());
            values.put(entry.id(), value == null ? null : entry.snapshot(value));
        }
        return new DraftState(values, new HashMap<>(rawTextValues), new HashSet<>(invalidEntries));
    }

    private void restoreDraftState(DraftState state) {
        if (state == null) return;
        pendingValues.clear();
        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (!entry.isValue()) continue;
            Object value = state.values().get(entry.id());
            pendingValues.put(entry.id(), value == null ? null : entry.snapshot(value));
        }
        rawTextValues.clear();
        rawTextValues.putAll(state.rawValues());
        invalidEntries.clear();
        invalidEntries.addAll(state.invalid());
        status = null;
        if (minecraft != null) rebuildUi();
    }

    @Override
    protected void buildUi() {
        resetScrollableWidgets();
        visibleRows.clear();
        entryModel.refresh(searchQuery);
        entryScroll.update(entryModel.items().size(), VISIBLE_ROWS);

        searchBox = addTextField(
                38, 37, 430,
                KineticText.translatable("gui.kineticcore.config.search_fields"),
                KineticText.translatable("gui.kineticcore.config.search_fields"),
                null
        );
        searchBox.setMaxLength(256);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(query -> {
            searchQuery = query == null ? "" : query;
            entryScroll.reset();
            searchDirty = true;
        });

        List<KTConfigEntry<?>> entries = entryModel.items();
        for (int index = 0; index < entries.size(); index++) {
            KTConfigEntry<?> entry = entries.get(index);
            int y = ROW_TOP + index * ROW_HEIGHT;
            visibleRows.put(entry, y);
            if (entry.isValue()) {
                addValueWidgets(entry, y);
            } else if (entry.type() == KTConfigEntry.Type.ACTION) {
                addActionWidget(entry, y);
            }
        }

        int footerY = 325;
        boolean editable = KTConfigApi.canEdit(configPage);
        Component unavailable = editable ? null : KTConfigApi.unavailableReason(configPage);

        Button resetAllButton = addButton(
                166, footerY, 92,
                KineticText.translatable("gui.kineticcore.config.reset_all"),
                unavailable,
                this::resetAll
        );
        resetAllButton.active = editable;

        addButton(
                274, footerY, 92,
                KineticText.translatable("gui.kineticcore.config.back"),
                null,
                this::onClose
        );

        Button saveButton = addButton(
                382, footerY, 92,
                KineticText.translatable("gui.kineticcore.config.save"),
                unavailable,
                this::saveAndClose
        );
        saveButton.active = editable;
    }

    private double entryPixelOffset() {
        return entryScroll.smoothOffset() * ROW_HEIGHT;
    }

    private <T extends AbstractWidget> T addEntryScrollableWidget(T widget) {
        return attachScrollableWidget(
                widget,
                28,
                ROW_TOP,
                SCROLL_X - 2,
                ROW_TOP + LIST_HEIGHT,
                this::entryPixelOffset
        );
    }

    private void addValueWidgets(KTConfigEntry<?> entry, int y) {
        final int resetX = KTConfigControlMetrics.RESET_X;
        final int resetWidth = KTConfigControlMetrics.RESET_WIDTH;
        final int editorWidth = KTConfigControlMetrics.editorWidth(
                font, entry, pendingValues.get(entry.id())
        );
        final int editorX = KTConfigControlMetrics.editorX(editorWidth);
        final boolean editable = KTConfigApi.canEdit(configPage);
        AbstractWidget editor;

        switch (entry.type()) {
            case BOOLEAN -> {
                boolean value = Boolean.TRUE.equals(pendingValues.get(entry.id()));
                editor = addToggleButton(
                        editorX, y, editorWidth, value,
                        booleanText(true),
                        booleanText(false),
                        null,
                        next -> entry.accepts(next),
                        next -> updateValidation(entry.id(), entry, next)
                );
            }
            case INTEGER -> {
                int min = entry.minimum().intValue();
                int max = entry.maximum().intValue();
                NumericEditBox box = addIntegerField(
                        editorX, y, editorWidth, entry.label(),
                        min < 0, min, max,
                        value -> entry.accepts(value.intValue()), null
                );
                box.setValue(rawTextValues.getOrDefault(entry.id(),
                        Integer.toString(((Number) pendingValues.get(entry.id())).intValue())));
                box.setResponder(raw -> {
                    Integer parsed = box.getIntValue();
                    setParsedValue(entry.id(), entry, parsed, box);
                });
                setParsedValue(entry.id(), entry, box.getIntValue(), box);
                editor = box;
            }
            case LONG -> {
                long min = entry.minimum().longValue();
                long max = entry.maximum().longValue();
                NumericEditBox box = addLongField(
                        editorX, y, editorWidth, entry.label(),
                        min < 0, min, max,
                        value -> entry.accepts(value.longValue()), null
                );
                box.setValue(rawTextValues.getOrDefault(entry.id(),
                        Long.toString(((Number) pendingValues.get(entry.id())).longValue())));
                box.setResponder(raw -> {
                    Long parsed = box.getLongValue();
                    setParsedValue(entry.id(), entry, parsed, box);
                });
                Long parsed = box.getLongValue();
                setParsedValue(entry.id(), entry, parsed, box);
                editor = box;
            }
            case DOUBLE -> {
                double min = entry.minimum().doubleValue();
                double max = entry.maximum().doubleValue();
                NumericEditBox box = addDecimalField(
                        editorX, y, editorWidth, entry.label(),
                        min < 0, min, max,
                        value -> entry.accepts(value.doubleValue()), null
                );
                box.setMaxLength(350);
                box.setValue(rawTextValues.getOrDefault(entry.id(),
                        NumericEditBox.format(((Number) pendingValues.get(entry.id())).doubleValue())));
                box.setResponder(raw -> {
                    Double parsed = box.getDoubleValue();
                    setParsedValue(entry.id(), entry, parsed, box);
                });
                setParsedValue(entry.id(), entry, box.getDoubleValue(), box);
                editor = box;
            }
            case STRING -> {
                KineticWidgets.KineticEditBox box = addTextField(
                        editorX, y, editorWidth, entry.label(), null,
                        value -> entry.accepts(value), null
                );
                box.setMaxLength(32767);
                box.setValue(String.valueOf(pendingValues.get(entry.id())));
                box.setResponder(value -> updateValidation(entry.id(), entry, value));
                updateValidation(entry.id(), entry, box.getValue());
                editor = box;
            }
            case LONG_TEXT -> editor = addButton(
                    editorX, y, editorWidth,
                    KineticText.translatable("gui.kineticcore.config.edit_text"),
                    null,
                    () -> openLongTextEditor(entry)
            );
            case CHOICE -> {
                List<KTConfigEntry.ChoiceOption> choiceOptions = entry.choiceOptions();
                List<String> choices = choiceOptions.stream().map(KTConfigEntry.ChoiceOption::value).toList();
                String current = String.valueOf(pendingValues.get(entry.id()));
                int selectedIndex = Math.max(0, choices.indexOf(current));
                editor = addDropdown(
                        editorX, y, editorWidth,
                        choiceOptions.stream().map(KTConfigEntry.ChoiceOption::label).toList(),
                        choiceOptions.stream().map(KTConfigEntry.ChoiceOption::tooltip).toList(),
                        selectedIndex,
                        null,
                        selected -> selected >= 0 && selected < choices.size()
                                && entry.accepts(choices.get(selected)),
                        selected -> {
                            if (selected >= 0 && selected < choices.size()) {
                                updateValidation(entry.id(), entry, choices.get(selected));
                            }
                        }
                );
            }
            case STRING_LIST, ITEM_LIST, ITEM_RULE_LIST, ENTITY_LIST, INTEGER_LIST -> {
                List<?> values = listValue(entry.id());
                editor = addButton(
                        editorX, y, editorWidth,
                        KineticText.translatable(
                                "gui.kineticcore.config.edit_list",
                                Component.literal(String.valueOf(values.size()))
                        ),
                        null,
                        () -> openListEditor(entry)
                );
            }
            case COLOR -> {
                int currentColor = ((Number) pendingValues.get(entry.id())).intValue() & 0xFFFFFF;
                editor = addColorPreviewButton(
                        editorX, y, editorWidth, currentColor,
                        Component.literal(formatColor(currentColor)),
                        null,
                        () -> KineticSelectors.openColorPicker(
                                this,
                                entry.label(),
                                currentColor,
                                selected -> {
                                    updateValidation(entry.id(), entry, selected & 0xFFFFFF);
                                    rawTextValues.remove(entry.id());
                                    status = null;
                                    rebuildUi();
                                }
                        )
                );
            }
            default -> throw new IllegalStateException("Unsupported value type: " + entry.type());
        }

        Component editorTooltip;
        if (!editable) {
            editorTooltip = KTConfigApi.unavailableReason(configPage);
        } else if (entry.type() == KTConfigEntry.Type.COLOR) {
            editorTooltip = entry.tooltip() != null
                    ? KineticText.translatable("gui.kineticcore.config.color_picker.tooltip")
                    .append(Component.literal(" "))
                    .append(entry.tooltip())
                    : KineticText.translatable("gui.kineticcore.config.color_picker.tooltip");
        } else {
            editorTooltip = entry.tooltip();
        }
        editor.active = editable;
        KineticWidgets.setButtonError(editor instanceof Button button ? button : null, invalidEntries.contains(entry.id()));
        registerWidgetTooltip(editor, editorTooltip);
        addEntryScrollableWidget(editor);

        Button reset = addButton(
                resetX, y, resetWidth,
                KineticText.translatable("gui.kineticcore.config.reset"),
                editable
                        ? KineticText.translatable("gui.kineticcore.config.reset.tooltip")
                        : KTConfigApi.unavailableReason(configPage),
                () -> {
                    pendingValues.put(entry.id(), entry.defaultSnapshot());
                    invalidEntries.remove(entry.id());
                    rawTextValues.remove(entry.id());
                    status = null;
                    rebuildUi();
                }
        );
        reset.active = editable;
        addEntryScrollableWidget(reset);
    }

    private void addActionWidget(KTConfigEntry<?> entry, int y) {
        boolean editable = KTConfigApi.canEdit(configPage);
        Component tooltip = editable ? entry.tooltip() : KTConfigApi.unavailableReason(configPage);
        Button button = addButton(
                KTConfigControlMetrics.actionX(), y, KTConfigControlMetrics.ACTION_WIDTH,
                KineticText.translatable("gui.kineticcore.config.open"),
                tooltip,
                () -> requestAction(entry)
        );
        button.active = editable;
        addEntryScrollableWidget(button);
    }

    private void requestAction(KTConfigEntry<?> entry) {
        if (minecraft == null) return;
        if (!isDirty()) {
            runAction(entry);
            return;
        }

        openDialog(
                KineticText.translatable("gui.kineticcore.config.unsaved_action.title"),
                unsavedMessage(),
                KineticText.translatable("gui.yes"),
                KineticText.translatable("gui.no"),
                () -> {
                    SaveOutcome outcome = persistPendingValues();
                    if (outcome == SaveOutcome.FAILED) return;
                    if (shouldShowImmediateSavedToast(outcome)) showSavedToast();
                    runAction(entry);
                },
                () -> runAction(entry)
        );
    }

    private void runAction(KTConfigEntry<?> entry) {
        if (!ensurePageEditable()) return;
        try {
            entry.runAction();
        } catch (Throwable throwable) {
            KineticRuntime.logger().error("Config action {} on page {} failed",
                    entry.id(), configPage.id(), throwable);
            status = KineticText.translatable(
                    "gui.kineticcore.config.action_failed",
                    Component.literal(throwable.getClass().getSimpleName())
            );
        }
    }

    private boolean isDirty() {
        if (!invalidEntries.isEmpty()) return true;
        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (entry.isValue() && !Objects.equals(
                    pendingValues.get(entry.id()), originalValues.get(entry.id()))) {
                return true;
            }
        }
        return false;
    }

    private void openLongTextEditor(KTConfigEntry<?> entry) {
        if (minecraft == null) return;
        String current = String.valueOf(pendingValues.getOrDefault(entry.id(), ""));
        KineticClientRuntime.openScreen(new KTLongTextEditorScreen(
                this,
                entry.label(),
                current,
                value -> {
                    updateValidation(entry.id(), entry, value);
                    rawTextValues.remove(entry.id());
                    status = null;
                    rebuildUi();
                }
        ));
    }

    private void openListEditor(KTConfigEntry<?> entry) {
        if (minecraft == null) return;
        List<?> values = listValue(entry.id());
        if (entry.type() == KTConfigEntry.Type.ENTITY_LIST) {
            List<String> entityIds = values.stream().map(String::valueOf).toList();
            KineticSelectors.openEntitySelector(
                    this,
                    entry.label(),
                    entityIds,
                    result -> updateValidationAndRebuild(
                            entry.id(), entry, new ArrayList<>(result)
                    )
            );
            return;
        }
        if (entry.type() == KTConfigEntry.Type.ITEM_LIST
                || entry.type() == KTConfigEntry.Type.ITEM_RULE_LIST) {
            List<String> itemRules = values.stream().map(String::valueOf).toList();
            KineticSelectors.ItemListMode mode = entry.type() == KTConfigEntry.Type.ITEM_LIST
                    ? KineticSelectors.ItemListMode.ITEMS_ONLY
                    : KineticSelectors.ItemListMode.ITEMS_TAGS_MODS;
            KineticSelectors.openItemListEditor(
                    this,
                    entry.label(),
                    itemRules,
                    mode,
                    result -> updateValidationAndRebuild(
                            entry.id(), entry, new ArrayList<>(result)
                    )
            );
            return;
        }
        boolean integerList = entry.type() == KTConfigEntry.Type.INTEGER_LIST;
        KineticClientRuntime.openScreen(new KTConfigListScreen(
                this, entry.label(), entry.tooltip(), integerList, values,
                result -> updateValidationAndRebuild(
                        entry.id(), entry, new ArrayList<>(result)
                )
        ));
    }

    private List<?> listValue(String id) {
        Object value = pendingValues.get(id);
        return value instanceof List<?> list ? list : List.of();
    }

    private void updateValidation(String id, KTConfigEntry<?> entry, Object value) {
        pendingValues.put(id, value);
        if (entry.accepts(value)) {
            invalidEntries.remove(id);
        } else {
            invalidEntries.add(id);
        }
    }

    private void updateValidationAndRebuild(String id, KTConfigEntry<?> entry, Object value) {
        updateValidation(id, entry, value);
        status = null;
        rebuildUi();
    }

    private void setParsedValue(String id, KTConfigEntry<?> entry, Object value, KineticWidgets.KineticEditBox box) {
        rawTextValues.put(id, box.getValue());
        boolean valid = value != null && entry.accepts(value);
        box.setValidationError(!valid);
        if (!valid) {
            invalidEntries.add(id);
            return;
        }
        invalidEntries.remove(id);
        pendingValues.put(id, value);
    }

    @Override
    public void tick() {
        super.tick();
        if (!searchDirty) return;

        searchDirty = false;
        entryModel.refresh(searchQuery);
        entryScroll.update(entryModel.items().size(), VISIBLE_ROWS);
        rebuildUi();
        if (searchBox != null) {
            focusControl(searchBox);
            searchBox.setCursorPosition(searchQuery.length());
        }
    }

    private void resetAll() {
        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (entry.isValue()) pendingValues.put(entry.id(), entry.defaultSnapshot());
        }
        invalidEntries.clear();
        rawTextValues.clear();
        status = KineticText.translatable("gui.kineticcore.config.reset_done");
        rebuildUi();
    }

    private void saveAndClose() {
        SaveOutcome outcome = persistPendingValues();
        if (outcome == SaveOutcome.FAILED) return;
        if (shouldShowImmediateSavedToast(outcome)) showSavedToast();
        commitDraft();
        navigateBack();
    }

    private SaveOutcome persistPendingValues() {
        if (!ensurePageEditable()) return SaveOutcome.FAILED;

        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (entry.isValue() && !entry.accepts(pendingValues.get(entry.id()))) {
                invalidEntries.add(entry.id());
            }
        }
        if (!invalidEntries.isEmpty()) {
            status = KineticText.translatable("gui.kineticcore.config.invalid", Component.literal(String.valueOf(invalidEntries.size())));
            return SaveOutcome.FAILED;
        }

        List<KTConfigEntry<?>> changedEntries = configPage.entries().stream()
                .filter(KTConfigEntry::isValue)
                .filter(entry -> !Objects.equals(
                        pendingValues.get(entry.id()), originalValues.get(entry.id())))
                .toList();
        if (changedEntries.isEmpty()) {
            status = null;
            return SaveOutcome.UNCHANGED;
        }

        if (configPage.scope() == KTConfigScope.SERVER_AUTHORITATIVE) {
            Map<String, Object> changedValues = new HashMap<>();
            for (KTConfigEntry<?> entry : changedEntries) {
                changedValues.put(entry.id(), entry.snapshot(pendingValues.get(entry.id())));
            }
            if (!KTServerConfigClient.save(configPage, changedValues)) {
                status = KTConfigApi.unavailableReason(configPage);
                return SaveOutcome.FAILED;
            }
        } else {
            List<KTConfigEntry<?>> appliedEntries = new ArrayList<>();
            try {
                for (KTConfigEntry<?> entry : changedEntries) {
                    entry.writeSnapshot(pendingValues.get(entry.id()));
                    appliedEntries.add(entry);
                }
                configPage.save();
            } catch (Throwable throwable) {
                KineticRuntime.logger().error("Failed to save config page {}", configPage.id(), throwable);
                rollbackOriginalValues(appliedEntries);
                status = KineticText.translatable("gui.kineticcore.config.save_failed", Component.literal(throwable.getClass().getSimpleName()));
                return SaveOutcome.FAILED;
            }
        }

        for (KTConfigEntry<?> entry : changedEntries) {
            originalValues.put(entry.id(), entry.snapshot(pendingValues.get(entry.id())));
        }
        status = null;
        return SaveOutcome.SAVED;
    }

    private boolean ensurePageEditable() {
        if (KTConfigApi.canEdit(configPage)) return true;
        status = KTConfigApi.unavailableReason(configPage);
        GuiOverlay.toast("kineticcore_config_unavailable", status);
        return false;
    }

    private boolean shouldShowImmediateSavedToast(SaveOutcome outcome) {
        return outcome == SaveOutcome.UNCHANGED
                || (outcome == SaveOutcome.SAVED && configPage.scope() != KTConfigScope.SERVER_AUTHORITATIVE);
    }

    private void showSavedToast() {
        try {
            KTConfigApi.notifySaved(configPage);
        } catch (Throwable throwable) {
            KineticRuntime.logger().debug("Could not show config saved toast", throwable);
        }
    }

    private void rollbackOriginalValues(List<KTConfigEntry<?>> appliedEntries) {
        for (KTConfigEntry<?> entry : appliedEntries) {
            try {
                entry.writeSnapshot(originalValues.get(entry.id()));
            } catch (Throwable rollbackFailure) {
                KineticRuntime.logger().error("Failed to roll back config value {} on page {}",
                        entry.id(), configPage.id(), rollbackFailure);
            }
        }
    }


    void serverSnapshotUpdated(String pageId) {
        if (!configPage.id().equals(pageId)) return;
        ServerConfigClientRuntime.applyCached(configPage);
        refreshFromSource();
        commitDraft();
        if (minecraft != null) rebuildUi();
    }

    private static Component booleanText(boolean value) {
        return KineticText.translatable(value
                ? "gui.kineticcore.config.enabled"
                : "gui.kineticcore.config.disabled");
    }

    private static String formatColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.panel(graphics, 18, 12, 604, 342);
        graphics.drawCenteredString(font, title, canvasWidth() / 2, 24, 0xFFFFAA00);

        hoveredEntry = null;
        double pixelOffset = entryPixelOffset();
        enableUiScissor(graphics, 28, ROW_TOP, SCROLL_X - 2, ROW_TOP + LIST_HEIGHT);
        try {
            for (Map.Entry<KTConfigEntry<?>, Integer> row : visibleRows.entrySet()) {
                KTConfigEntry<?> entry = row.getKey();
                int y = row.getValue() - (int) Math.round(pixelOffset);
                if (y + ROW_HEIGHT <= ROW_TOP || y >= ROW_TOP + LIST_HEIGHT) continue;
                int tooltipWidth = entry.type() == KTConfigEntry.Type.DESCRIPTION ? 582 : 292;
                if (mouseY >= ROW_TOP && mouseY < ROW_TOP + LIST_HEIGHT
                        && GuiTheme.hovering(mouseX, mouseY, 30, y - 3, tooltipWidth, 23)) {
                    hoveredEntry = entry;
                }
                if (entry.type() == KTConfigEntry.Type.SECTION) {
                    graphics.fill(30, y - 3, 612, y + 20, 0x55222222);
                    graphics.drawString(font, entry.label(), 38, y + 4, 0xFFFFAA00, false);
                } else if (entry.type() == KTConfigEntry.Type.DESCRIPTION) {
                    KineticText.drawScrollingLeft(graphics, font, entry.label(), 38, y + 5, 562, 0xFFAAAAAA, false);
                } else {
                    int color = invalidEntries.contains(entry.id()) ? 0xFFFF5555 : 0xFFE0E0E0;
                    KineticText.drawScrollingLeft(graphics, font, entry.label(), 38, y + 6, 282, color, false);
                }
            }
        } finally {
            disableUiScissor(graphics);
        }

        if (entryModel.items().isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    KineticText.translatable("gui.kineticcore.config.no_fields"),
                    canvasWidth() / 2,
                    176,
                    0xFFAAAAAA
            );
        }

        GuiTheme.scrollbar(
                entryScroll, graphics, mouseX, mouseY,
                SCROLL_X, ROW_TOP, SCROLL_WIDTH, LIST_HEIGHT, 18
        );

        if (status != null) {
            graphics.drawCenteredString(font, status, canvasWidth() / 2, 309,
                    invalidEntries.isEmpty() ? 0xFFFFFF55 : 0xFFFF5555);
        } else if (showApplyTiming) {
            List<FormattedCharSequence> timingLines = font.split(configPage.applyDetail(), 570);
            int visibleLineCount = Math.min(2, timingLines.size());
            int firstY = visibleLineCount == 1 ? 309 : 298;
            for (int index = 0; index < visibleLineCount; index++) {
                graphics.drawCenteredString(
                        font,
                        timingLines.get(index),
                        canvasWidth() / 2,
                        firstY + index * 11,
                        GuiTheme.current().text()
                );
            }
        }
    }

    @Override
    protected void renderTooltips(
            GuiGraphics graphics,
            int scaledMouseX,
            int scaledMouseY,
            int mouseX,
            int mouseY
    ) {
        if (hoveredEntry == null) return;
        Component tooltip = hoveredEntry.type() == KTConfigEntry.Type.DESCRIPTION
                ? hoveredEntry.label()
                : hoveredEntry.tooltip();
        if (tooltip != null) {
            showTooltip(tooltip, 400);
        }
    }


    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && entryScroll.beginDrag(
                mouseX, mouseY, SCROLL_X, ROW_TOP, SCROLL_WIDTH, LIST_HEIGHT, 18, 2)) {
            return true;
        }
        return super.canvasMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (entryScroll.drag(mouseY, ROW_TOP, LIST_HEIGHT, 18)) {
            return true;
        }
        return super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        return entryScroll.release(button)
                || super.canvasMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= 28 && mouseX <= 620
                && mouseY >= ROW_TOP && mouseY < ROW_TOP + LIST_HEIGHT
                && entryScroll.scroll(delta)) {
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        discardDraft();
        navigateBack();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String buildSearchData(KTConfigEntry<?> entry) {
        StringBuilder data = new StringBuilder(entry.id())
                .append(' ')
                .append(entry.label().getString());
        if (entry.tooltip() != null) data.append(' ').append(entry.tooltip().getString());
        String raw = data.toString();
        return raw + ' ' + KineticSearch.pinyin(raw);
    }

    private Component unsavedMessage() {
        return KineticText.translatable("gui.kineticcore.config.unsaved_action.message")
                .copy()
                .append("\n")
                .append(configPage.applyDetail());
    }

}
