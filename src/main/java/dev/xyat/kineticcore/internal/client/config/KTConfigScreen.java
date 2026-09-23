package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.config.client.*;

import dev.xyat.kineticcore.api.client.selector.KineticSelectors;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.GridScrollController;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
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
final class KTConfigScreen extends KineticScreen {
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

    private record LayoutEntry(KTConfigEntry<?> entry, boolean separatorBefore) {
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
    private final Map<LayoutEntry, Integer> visibleRows = new java.util.LinkedHashMap<>();
    private final KineticSearch.Model<KTConfigEntry<?>> entryModel =
            new KineticSearch.Model<>(List.of(), (entry, query) -> KineticSearch.match(buildSearchData(entry), query));
    private final GridScrollController entryScroll = new GridScrollController();
    private List<LayoutEntry> layoutEntries = List.of();
    private Component status;
    private KineticEditBox searchBox;
    private KTConfigEntry<?> hoveredEntry;
    private String searchQuery = "";
    private boolean searchDirty;
    private final boolean showApplyTiming;

    KTConfigScreen(Screen parent, KTConfigPage configPage) {
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
        // Assemble a complete replacement before changing the active draft.
        // A faulty add-on copier must not leave a half-refreshed editor.
        Map<String, Object> refreshedPending = new HashMap<>();
        Map<String, Object> refreshedOriginal = new HashMap<>();
        try {

            for (KTConfigEntry<?> entry : configPage.entries()) {
                if (!entry.isValueEntry()) continue;
                Object value;
                try {
                    value = entry.read();
                } catch (Throwable throwable) {
                    KineticRuntime.logger().error("Failed to read config value {} from page {}",
                            entry.id(), configPage.id(), throwable);
                    value = entry.defaultValue();
                }
                boolean accepted;
                try {
                    accepted = entry.accepts(value);
                } catch (RuntimeException invalidValue) {
                    KineticRuntime.logger().error("Failed to validate config entry {} on page {}",
                            entry.id(), configPage.id(), invalidValue);
                    accepted = false;
                }
                if (!accepted) {
                    KineticRuntime.logger().warn("Invalid config value {} on page {}; using its default",
                            entry.id(), configPage.id());
                    value = entry.defaultValue();
                }
                Object snapshot = entry.snapshot(value);
                refreshedPending.put(entry.id(), snapshot);
                refreshedOriginal.put(entry.id(), entry.snapshot(snapshot));
            }
        } catch (Throwable failure) {
            KineticRuntime.logger().error("Failed to refresh config page {}", configPage.id(), failure);
            return;
        }
        pendingValues.clear();
        pendingValues.putAll(refreshedPending);
        originalValues.clear();
        originalValues.putAll(refreshedOriginal);
        rawTextValues.clear();
        invalidEntries.clear();
        status = null;
    }

    private DraftState captureDraftState() {
        Map<String, Object> values = new HashMap<>();
        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (!entry.isValueEntry()) continue;
            Object value = pendingValues.get(entry.id());
            values.put(entry.id(), value == null ? null : entry.snapshot(value));
        }
        return new DraftState(values, new HashMap<>(rawTextValues), new HashSet<>(invalidEntries));
    }

    private void restoreDraftState(DraftState state) {
        if (state == null) return;
        // Prepare every snapshot before replacing the current draft. An add-on
        // copier can fail midway; existing unsaved edits must remain intact.
        Map<String, Object> restored = new HashMap<>();
        try {
            for (KTConfigEntry<?> entry : configPage.entries()) {
                if (!entry.isValueEntry()) continue;
                Object value = state.values().get(entry.id());
                restored.put(entry.id(), value == null ? null : entry.snapshot(value));
            }
        } catch (Throwable failure) {
            KineticRuntime.logger().error("Failed to restore config draft for page {}", configPage.id(), failure);
            return;
        }
        pendingValues.clear();
        pendingValues.putAll(restored);
        rawTextValues.clear();
        rawTextValues.putAll(state.rawValues());
        invalidEntries.clear();
        invalidEntries.addAll(state.invalid());
        status = null;
        if (KineticClientRuntime.currentScreen() == this) rebuildUi();
    }

    @Override
    protected void buildUi() {
        resetScrollableWidgets();
        visibleRows.clear();
        entryModel.refresh(searchQuery);
        layoutEntries = compactLayout(entryModel.items());
        entryScroll.update(layoutEntries.size(), VISIBLE_ROWS);

        searchBox = addTextField(
                38, 37, 430,
                KineticText.translatable("gui.kineticcore.config.search_fields"),
                KineticText.translatable("gui.kineticcore.config.search_fields"),
                null, null
        );
        searchBox.setMaxLength(256);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(query -> {
            searchQuery = query == null ? "" : query;
            entryScroll.reset();
            searchDirty = true;
        });

        for (int index = 0; index < layoutEntries.size(); index++) {
            LayoutEntry layout = layoutEntries.get(index);
            KTConfigEntry<?> entry = layout.entry();
            int y = ROW_TOP + index * ROW_HEIGHT;
            visibleRows.put(layout, y);
            if (entry.isValueEntry()) {
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
                this::save
        );
        saveButton.active = editable;
    }

    private static List<LayoutEntry> compactLayout(List<KTConfigEntry<?>> entries) {
        List<LayoutEntry> result = new ArrayList<>();
        boolean separatorPending = false;
        for (KTConfigEntry<?> entry : entries) {
            if (entry.type() == KTConfigEntry.Type.DIVIDER) {
                if (!result.isEmpty()) separatorPending = true;
                continue;
            }
            result.add(new LayoutEntry(entry, separatorPending));
            separatorPending = false;
        }
        return List.copyOf(result);
    }

    private double entryPixelOffset() {
        return entryScroll.smoothOffset() * ROW_HEIGHT;
    }

    private <T extends AbstractWidget> T addEntryScrollableWidget(T widget) {
        if (!(widget instanceof dev.xyat.kineticcore.api.client.widget.KineticControl control)) {
            throw new IllegalArgumentException("Scrollable widget must be an API-created control");
        }
        addScrollableWidget(control,
                28,
                ROW_TOP,
                SCROLL_X - 2,
                ROW_TOP + LIST_HEIGHT,
                this::entryPixelOffset
        );
        return widget;
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
                        dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.formatDecimal(((Number) pendingValues.get(entry.id())).doubleValue())));
                box.setResponder(raw -> {
                    Double parsed = box.getDoubleValue();
                    setParsedValue(entry.id(), entry, parsed, box);
                });
                setParsedValue(entry.id(), entry, box.getDoubleValue(), box);
                editor = box;
            }
            case STRING -> {
                KineticEditBox box = addTextField(
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
                String current = String.valueOf(pendingValues.get(entry.id()));
                editor = addDropdown(
                        editorX, y, editorWidth,
                        choiceOptions.stream().map(option -> new dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Option(
                                option.value(), option.translation(), option.tooltip()
                        )).toList(),
                        current, null,
                        entry::accepts,
                        selected -> updateValidation(entry.id(), entry, selected)
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
        if (editor instanceof dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton stateButton) {
            stateButton.setError(invalidEntries.contains(entry.id()));
        }
        if (editor instanceof dev.xyat.kineticcore.api.client.widget.KineticControl control) {
            registerWidgetTooltip(control, editorTooltip);
        }
        addEntryScrollableWidget(editor);

        Button reset = addButton(
                resetX, y, resetWidth,
                KineticText.translatable("gui.kineticcore.config.reset"),
                editable
                        ? KineticText.translatable("gui.kineticcore.config.reset.tooltip")
                        : KTConfigApi.unavailableReason(configPage),
                () -> {
                    pendingValues.put(entry.id(), entry.defaultValue());
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
        if (KineticClientRuntime.currentScreen() != this) return;
        if (isClean()) {
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
        if (rejectUneditablePage()) return;
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

    private boolean isClean() {
        if (!invalidEntries.isEmpty()) return false;
        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (entry.isValueEntry() && !Objects.equals(
                    pendingValues.get(entry.id()), originalValues.get(entry.id()))) {
                return false;
            }
        }
        return true;
    }

    private void openLongTextEditor(KTConfigEntry<?> entry) {
        if (KineticClientRuntime.currentScreen() != this) return;
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
        if (KineticClientRuntime.currentScreen() != this) return;
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
        boolean valid;
        try {
            valid = entry.accepts(value);
        } catch (RuntimeException failure) {
            KineticRuntime.logger().error("Failed to validate config entry {}", entry.id(), failure);
            valid = false;
        }
        if (valid) {
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

    private void setParsedValue(String id, KTConfigEntry<?> entry, Object value, KineticEditBox box) {
        rawTextValues.put(id, box.getValue());
        boolean valid = false;
        if (value != null) {
            try {
                valid = entry.accepts(value);
            } catch (RuntimeException failure) {
                KineticRuntime.logger().error("Failed to validate config entry {}", entry.id(), failure);
            }
        }
        box.setValidationError(!valid);
        if (!valid) {
            invalidEntries.add(id);
            return;
        }
        invalidEntries.remove(id);
        pendingValues.put(id, value);
    }

    @Override
    protected void canvasTick() {
        if (!searchDirty) return;

        searchDirty = false;
        entryModel.refresh(searchQuery);
        layoutEntries = compactLayout(entryModel.items());
        entryScroll.update(layoutEntries.size(), VISIBLE_ROWS);
        rebuildUi();
        if (searchBox != null) {
            focusControl(searchBox);
            searchBox.setCursorPosition(searchQuery.length());
        }
    }

    private void resetAll() {
        // A failed default supplier or snapshot must not half-reset the editor.
        Map<String, Object> defaults = new HashMap<>();
        try {
            for (KTConfigEntry<?> entry : configPage.entries()) {
                if (entry.isValueEntry()) defaults.put(entry.id(), entry.snapshot(entry.defaultValue()));
            }
        } catch (Throwable failure) {
            KineticRuntime.logger().error("Failed to reset config page {}", configPage.id(), failure);
            status = KineticText.translatable("gui.kineticcore.config.reset_failed",
                    Component.literal(failure.getClass().getSimpleName()));
            return;
        }
        pendingValues.putAll(defaults);
        invalidEntries.clear();
        rawTextValues.clear();
        status = KineticText.translatable("gui.kineticcore.config.reset_done");
        rebuildUi();
    }

    private void save() {
        SaveOutcome outcome = persistPendingValues();
        if (outcome == SaveOutcome.FAILED) return;
        if (shouldShowImmediateSavedToast(outcome)) showSavedToast();
        commitDraft();
    }

    private SaveOutcome persistPendingValues() {
        if (rejectUneditablePage()) return SaveOutcome.FAILED;

        for (KTConfigEntry<?> entry : configPage.entries()) {
            if (!entry.isValueEntry()) continue;
            try {
                if (!entry.accepts(pendingValues.get(entry.id()))) invalidEntries.add(entry.id());
            } catch (RuntimeException invalidValue) {
                // An add-on validator failure is an invalid field, not a fatal screen error.
                KineticRuntime.logger().error("Failed to validate config entry {} on page {}",
                        entry.id(), configPage.id(), invalidValue);
                invalidEntries.add(entry.id());
            }
        }
        if (!invalidEntries.isEmpty()) {
            status = KineticText.translatable("gui.kineticcore.config.invalid", Component.literal(String.valueOf(invalidEntries.size())));
            return SaveOutcome.FAILED;
        }

        List<KTConfigEntry<?>> changedEntries = configPage.entries().stream()
                .filter(KTConfigEntry::isValueEntry)
                .filter(entry -> !Objects.equals(
                        pendingValues.get(entry.id()), originalValues.get(entry.id())))
                .toList();
        if (changedEntries.isEmpty()) {
            status = null;
            return SaveOutcome.UNCHANGED;
        }

        // Snapshot every changed field before the first writer or network request.
        // A copier failure must never leave a partially committed page.
        Map<String, Object> committedSnapshots = new HashMap<>();
        try {
            for (KTConfigEntry<?> entry : changedEntries) {
                committedSnapshots.put(entry.id(), entry.snapshot(pendingValues.get(entry.id())));
            }
        } catch (Throwable failure) {
            KineticRuntime.logger().error("Failed to prepare config page {} for saving", configPage.id(), failure);
            status = KineticText.translatable("gui.kineticcore.config.save_failed",
                    Component.literal(failure.getClass().getSimpleName()));
            return SaveOutcome.FAILED;
        }

        if (configPage.scope() == KTConfigScope.SERVER_AUTHORITATIVE) {
            if (!KTServerConfigClient.save(configPage, committedSnapshots)) {
                status = KTConfigApi.unavailableReason(configPage);
                return SaveOutcome.FAILED;
            }
        } else {
            List<KTConfigEntry<?>> appliedEntries = new ArrayList<>();
            try {
                for (KTConfigEntry<?> entry : changedEntries) {
                    // A writer can mutate its backing value before throwing. Include the
                    // in-flight entry in rollback rather than restoring only older entries.
                    appliedEntries.add(entry);
                    entry.writeSnapshot(pendingValues.get(entry.id()));
                }
                configPage.save();
            } catch (Throwable throwable) {
                KineticRuntime.logger().error("Failed to save config page {}", configPage.id(), throwable);
                rollbackOriginalValues(appliedEntries);
                status = KineticText.translatable("gui.kineticcore.config.save_failed", Component.literal(throwable.getClass().getSimpleName()));
                return SaveOutcome.FAILED;
            }
        }

        originalValues.putAll(committedSnapshots);
        status = null;
        return SaveOutcome.SAVED;
    }

    private boolean rejectUneditablePage() {
        if (KTConfigApi.canEdit(configPage)) return false;
        status = KTConfigApi.unavailableReason(configPage);
        KineticOverlays.toast("kineticcore_config_unavailable", status, KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        return true;
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
        // Reverse write order so dependent fields are restored before their prerequisites.
        for (int index = appliedEntries.size() - 1; index >= 0; index--) {
            KTConfigEntry<?> entry = appliedEntries.get(index);
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
        KTServerConfigClient.applyCached(configPage);

        // Stage every authoritative snapshot first. A faulty add-on copier must
        // never leave half of this page with a newer baseline than the rest.
        Map<String, Object> nextOriginal = new HashMap<>();
        Map<String, Object> nextPending = new HashMap<>();
        Set<String> cleanKeys = new HashSet<>();
        try {
            for (KTConfigEntry<?> entry : configPage.entries()) {
                if (!entry.isValueEntry()) continue;
                String key = entry.id();
                boolean hasLocalDraft = !Objects.equals(pendingValues.get(key), originalValues.get(key))
                        || invalidEntries.contains(key) || rawTextValues.containsKey(key);
                Object value;
                try {
                    value = entry.read();
                } catch (Throwable throwable) {
                    KineticRuntime.logger().error("Failed to refresh server config value {} from page {}",
                            key, configPage.id(), throwable);
                    value = entry.defaultValue();
                }
                boolean accepted;
                try {
                    accepted = entry.accepts(value);
                } catch (RuntimeException invalidValue) {
                    KineticRuntime.logger().error("Failed to validate server config entry {} on page {}",
                            key, configPage.id(), invalidValue);
                    accepted = false;
                }
                if (!accepted) value = entry.defaultValue();
                Object snapshot = entry.snapshot(value);
                nextOriginal.put(key, entry.snapshot(snapshot));
                if (!hasLocalDraft) {
                    nextPending.put(key, snapshot);
                    cleanKeys.add(key);
                }
            }
        } catch (Throwable failure) {
            KineticRuntime.logger().error("Failed to synchronize config page {}", configPage.id(), failure);
            return;
        }
        originalValues.putAll(nextOriginal);
        pendingValues.putAll(nextPending);
        for (String key : cleanKeys) {
            invalidEntries.remove(key);
            rawTextValues.remove(key);
        }

        // Never commit a partially edited draft merely because the server responded.
        boolean dirty = !invalidEntries.isEmpty() || !rawTextValues.isEmpty();
        if (!dirty) {
            for (KTConfigEntry<?> entry : configPage.entries()) {
                if (entry.isValueEntry() && !Objects.equals(
                        pendingValues.get(entry.id()), originalValues.get(entry.id()))) {
                    dirty = true;
                    break;
                }
            }
        }
        if (!dirty) commitDraft();
        if (KineticClientRuntime.currentScreen() == this) rebuildUi();
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
            for (Map.Entry<LayoutEntry, Integer> row : visibleRows.entrySet()) {
                LayoutEntry layout = row.getKey();
                KTConfigEntry<?> entry = layout.entry();
                int y = row.getValue() - (int) Math.round(pixelOffset);
                if (y + ROW_HEIGHT <= ROW_TOP || y >= ROW_TOP + LIST_HEIGHT) continue;
                int tooltipWidth = entry.type() == KTConfigEntry.Type.DESCRIPTION ? 582 : 292;
                if (mouseY >= ROW_TOP && mouseY < ROW_TOP + LIST_HEIGHT
                        && GuiTheme.hovering(mouseX, mouseY, 30, y - 3, tooltipWidth, 23)) {
                    hoveredEntry = entry;
                }
                if (layout.separatorBefore()) {
                    GuiTheme.separator(graphics, 38, y - 5, 562);
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

        if (layoutEntries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    KineticText.translatable("gui.kineticcore.config.no_fields"),
                    canvasWidth() / 2,
                    176,
                    0xFFAAAAAA
            );
        }

        entryScroll.render(
                graphics, mouseX, mouseY,
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
            showTooltip(List.of(tooltip), 400);
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
                && entryScroll.scroll(delta, 1.0D)) {
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected boolean handleCloseRequest() {
        discardDraft();
        return false;
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
        return raw;
    }

    private Component unsavedMessage() {
        return KineticText.translatable("gui.kineticcore.config.unsaved_action.message")
                .copy()
                .append("\n")
                .append(configPage.applyDetail());
    }

}
