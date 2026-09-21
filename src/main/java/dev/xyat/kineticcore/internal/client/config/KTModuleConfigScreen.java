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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class KTModuleConfigScreen extends KineticScreen {
    private enum SaveOutcome {
        FAILED,
        UNCHANGED,
        SAVED
    }

    private enum RowKind {
        SCOPE,
        PAGE,
        ENTRY
    }

    private record Row(RowKind kind, KTConfigScope scope, KTConfigPage page, KTConfigEntry<?> entry, Component text) {
        static Row scope(KTConfigScope scope) {
            return new Row(
                    RowKind.SCOPE,
                    scope,
                    null,
                    null,
                    KineticText.translatable(scopeHeaderKey(scope))
            );
        }

        static Row page(KTConfigPage page) {
            return new Row(RowKind.PAGE, page.scope(), page, null, page.title());
        }

        static Row entry(KTConfigPage page, KTConfigEntry<?> entry) {
            return new Row(RowKind.ENTRY, page.scope(), page, entry, entry.label());
        }
    }

    private static final int VISIBLE_ROWS = 9;
    private static final int ROW_TOP = 64;
    private static final int ROW_HEIGHT = 27;
    private static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
    private static final int SCROLL_X = 615;
    private static final int SCROLL_WIDTH = 4;

    private final Screen parent;
    private final List<KTConfigPage> pages;
    private final Map<String, Object> pendingValues = new HashMap<>();
    private final Map<String, Object> originalValues = new HashMap<>();
    private final Map<String, String> rawTextValues = new HashMap<>();
    private final Set<String> invalidEntries = new HashSet<>();
    private final Map<Row, Integer> visibleRows = new LinkedHashMap<>();
    private final GridScrollController rowScroll = new GridScrollController();

    private List<Row> rows = List.of();
    private Component status;
    private KineticEditBox searchBox;
    private Row hoveredRow;
    private String searchQuery = "";
    private boolean searchDirty;
    private boolean lastSaveSentServerRequest;

    KTModuleConfigScreen(
            Screen parent,
            Component moduleTitle,
            List<KTConfigPage> pages
    ) {
        super(Objects.requireNonNull(moduleTitle, "moduleTitle"));
        this.parent = parent;
        this.pages = pages.stream()
                .sorted(Comparator.comparingInt((KTConfigPage page) -> scopeOrder(page.scope()))
                        .thenComparing(KTConfigPage::id))
                .toList();
        refreshFromSource();
        rebuildRows();
        for (KTConfigPage page : this.pages) {
            KTServerConfigClient.request(page);
        }
    }

    public void refreshFromSource() {
        // Assemble a complete replacement before changing the active draft.
        // A faulty add-on copier must not leave a half-refreshed editor.
        Map<String, Object> refreshedPending = new HashMap<>();
        Map<String, Object> refreshedOriginal = new HashMap<>();
        try {


            for (KTConfigPage page : pages) {
                for (KTConfigEntry<?> entry : page.entries()) {
                    if (!entry.isValueEntry()) continue;
                    String key = entryKey(page, entry);
                    Object value;
                    try {
                        value = entry.read();
                    } catch (Throwable throwable) {
                        KineticRuntime.logger().error(
                                "Failed to read config value {} from page {}",
                                entry.id(),
                                page.id(),
                                throwable
                        );
                        value = entry.defaultValue();
                    }
                    boolean accepted;
                    try {
                        accepted = entry.accepts(value);
                    } catch (RuntimeException invalidValue) {
                        KineticRuntime.logger().error("Failed to validate config entry {} on page {}",
                                entry.id(), page.id(), invalidValue);
                        accepted = false;
                    }
                    if (!accepted) {
                        KineticRuntime.logger().warn(
                                "Invalid config value {} on page {}; using its default",
                                entry.id(),
                                page.id()
                        );
                        value = entry.defaultValue();
                    }
                    Object snapshot = entry.snapshot(value);
                    refreshedPending.put(key, snapshot);
                    refreshedOriginal.put(key, entry.snapshot(snapshot));
                }
            }
        } catch (Throwable failure) {
            KineticRuntime.logger().error("Failed to refresh module config", failure);
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

    @Override
    protected void buildUi() {
        resetScrollableWidgets();
        visibleRows.clear();
        rebuildRows();
        rowScroll.update(rows.size(), VISIBLE_ROWS);

        searchBox = addTextField(
                38,
                37,
                430,
                KineticText.translatable("gui.kineticcore.config.search_fields"),
                KineticText.translatable("gui.kineticcore.config.search_fields"),
                null, null
        );
        searchBox.setMaxLength(256);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(query -> {
            searchQuery = query == null ? "" : query;
            rowScroll.reset();
            searchDirty = true;
        });

        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            int y = ROW_TOP + index * ROW_HEIGHT;
            visibleRows.put(row, y);
            if (row.kind() != RowKind.ENTRY || row.entry() == null) continue;
            if (row.entry().isValueEntry()) {
                addValueWidgets(row.page(), row.entry(), y);
            } else if (row.entry().type() == KTConfigEntry.Type.ACTION) {
                addActionWidget(row.page(), row.entry(), y);
            }
        }

        int footerY = 325;
        addButton(166, footerY, 92,
                KineticText.translatable("gui.kineticcore.config.reset_all"),
                null, this::resetAll);
        addButton(274, footerY, 92,
                KineticText.translatable("gui.kineticcore.config.back"),
                null, this::onClose);
        addButton(382, footerY, 92,
                KineticText.translatable("gui.kineticcore.config.save"),
                null, this::saveAndClose);
    }

    private double rowPixelOffset() {
        return rowScroll.smoothOffset() * ROW_HEIGHT;
    }

    private <T extends AbstractWidget> T addRowScrollableWidget(T widget) {
        if (!(widget instanceof dev.xyat.kineticcore.api.client.widget.KineticControl control)) {
            throw new IllegalArgumentException("Scrollable widget must be an API-created control");
        }
        addScrollableWidget(control,
                28,
                ROW_TOP,
                SCROLL_X - 2,
                ROW_TOP + LIST_HEIGHT,
                this::rowPixelOffset
        );
        return widget;
    }

    private void addValueWidgets(KTConfigPage page, KTConfigEntry<?> entry, int y) {
        final int resetX = KTConfigControlMetrics.RESET_X;
        final int resetWidth = KTConfigControlMetrics.RESET_WIDTH;
        final String key = entryKey(page, entry);
        final int editorWidth = KTConfigControlMetrics.editorWidth(
                font, entry, pendingValues.get(key)
        );
        final int editorX = KTConfigControlMetrics.editorX(editorWidth);
        final boolean editable = KTConfigApi.canEdit(page);
        AbstractWidget editor;

        switch (entry.type()) {
            case BOOLEAN -> {
                boolean value = Boolean.TRUE.equals(pendingValues.get(key));
                editor = addToggleButton(
                        editorX, y, editorWidth, value,
                        booleanText(true), booleanText(false), null,
                        next -> entry.accepts(next),
                        next -> updateValidation(key, entry, next)
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
                box.setValue(rawTextValues.getOrDefault(
                        key, Integer.toString(((Number) pendingValues.get(key)).intValue())
                ));
                box.setResponder(raw -> setParsedValue(key, entry, box.getIntValue(), box));
                setParsedValue(key, entry, box.getIntValue(), box);
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
                box.setValue(rawTextValues.getOrDefault(
                        key, Long.toString(((Number) pendingValues.get(key)).longValue())
                ));
                box.setResponder(raw -> {
                    Long parsed = box.getLongValue();
                    setParsedValue(key, entry, parsed, box);
                });
                Long parsed = box.getLongValue();
                setParsedValue(key, entry, parsed, box);
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
                box.setValue(rawTextValues.getOrDefault(
                        key, dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.formatDecimal(((Number) pendingValues.get(key)).doubleValue())
                ));
                box.setResponder(raw -> setParsedValue(key, entry, box.getDoubleValue(), box));
                setParsedValue(key, entry, box.getDoubleValue(), box);
                editor = box;
            }
            case STRING -> {
                KineticEditBox box = addTextField(
                        editorX, y, editorWidth, entry.label(), null,
                        value -> entry.accepts(value), null
                );
                box.setMaxLength(32767);
                box.setValue(String.valueOf(pendingValues.get(key)));
                box.setResponder(value -> updateValidation(key, entry, value));
                updateValidation(key, entry, box.getValue());
                editor = box;
            }
            case LONG_TEXT -> editor = addButton(
                    editorX, y, editorWidth,
                    KineticText.translatable("gui.kineticcore.config.edit_text"),
                    null,
                    () -> openLongTextEditor(page, entry, key)
            );
            case CHOICE -> {
                List<KTConfigEntry.ChoiceOption> choiceOptions = entry.choiceOptions();
                String current = String.valueOf(pendingValues.get(key));
                editor = addDropdown(
                        editorX, y, editorWidth,
                        choiceOptions.stream().map(option -> new dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Option(
                                option.value(), option.translation(), option.tooltip()
                        )).toList(),
                        current, null,
                        entry::accepts,
                        selected -> updateValidation(key, entry, selected)
                );
            }
            case STRING_LIST, ITEM_LIST, ITEM_RULE_LIST, ENTITY_LIST, INTEGER_LIST -> {
                List<?> values = listValue(key);
                editor = addButton(
                        editorX, y, editorWidth,
                        KineticText.translatable(
                                "gui.kineticcore.config.edit_list",
                                Component.literal(String.valueOf(values.size()))
                        ),
                        null,
                        () -> openListEditor(page, entry)
                );
            }
            case COLOR -> {
                int currentColor = ((Number) pendingValues.get(key)).intValue() & 0xFFFFFF;
                editor = addColorPreviewButton(
                        editorX, y, editorWidth, currentColor,
                        Component.literal(formatColor(currentColor)), null,
                        () -> KineticSelectors.openColorPicker(
                                this, entry.label(), currentColor,
                                selected -> {
                                    updateValidation(key, entry, selected & 0xFFFFFF);
                                    rawTextValues.remove(key);
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
            editorTooltip = KTConfigApi.unavailableReason(page);
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
            stateButton.setError(invalidEntries.contains(key));
        }
        if (editor instanceof dev.xyat.kineticcore.api.client.widget.KineticControl control) {
            registerWidgetTooltip(control, editorTooltip);
        }
        addRowScrollableWidget(editor);

        Button reset = addButton(
                resetX, y, resetWidth,
                KineticText.translatable("gui.kineticcore.config.reset"),
                editable
                        ? KineticText.translatable("gui.kineticcore.config.reset.tooltip")
                        : KTConfigApi.unavailableReason(page),
                () -> {
                    pendingValues.put(key, entry.defaultValue());
                    invalidEntries.remove(key);
                    rawTextValues.remove(key);
                    status = null;
                    rebuildUi();
                }
        );
        reset.active = editable;
        addRowScrollableWidget(reset);
    }

    private void addActionWidget(KTConfigPage page, KTConfigEntry<?> entry, int y) {
        boolean editable = KTConfigApi.canEdit(page);
        Component tooltip = editable ? entry.tooltip() : KTConfigApi.unavailableReason(page);
        Button button = addButton(
                KTConfigControlMetrics.actionX(), y, KTConfigControlMetrics.ACTION_WIDTH,
                KineticText.translatable("gui.kineticcore.config.open"),
                tooltip,
                () -> requestAction(page, entry)
        );
        button.active = editable;
        addRowScrollableWidget(button);
    }

    private void requestAction(KTConfigPage page, KTConfigEntry<?> entry) {
        if (KineticClientRuntime.currentScreen() != this || rejectUneditablePage(page)) return;
        if (isClean()) {
            runAction(page, entry);
            return;
        }

        openDialog(
                KineticText.translatable("gui.kineticcore.config.unsaved_action.title"),
                KineticText.translatable("gui.kineticcore.config.unsaved_action.message"),
                KineticText.translatable("gui.yes"),
                KineticText.translatable("gui.no"),
                () -> {
                    SaveOutcome outcome = persistPendingValues();
                    if (outcome == SaveOutcome.FAILED) return;
                    if (shouldShowImmediateSavedToast(outcome)) showSavedToast();
                    runAction(page, entry);
                },
                () -> runAction(page, entry)
        );
    }

    private void runAction(KTConfigPage page, KTConfigEntry<?> entry) {
        if (rejectUneditablePage(page)) return;
        try {
            entry.runAction();
        } catch (Throwable throwable) {
            KineticRuntime.logger().error(
                    "Config action {} on page {} failed",
                    entry.id(),
                    page.id(),
                    throwable
            );
            status = KineticText.translatable(
                    "gui.kineticcore.config.action_failed",
                    Component.literal(throwable.getClass().getSimpleName())
            );
        }
    }

    private boolean isClean() {
        if (!invalidEntries.isEmpty()) return false;
        for (KTConfigPage page : pages) {
            for (KTConfigEntry<?> entry : page.entries()) {
                if (!entry.isValueEntry()) continue;
                String key = entryKey(page, entry);
                if (!Objects.equals(pendingValues.get(key), originalValues.get(key))) return false;
            }
        }
        return true;
    }

    private void openLongTextEditor(KTConfigPage page, KTConfigEntry<?> entry, String key) {
        if (KineticClientRuntime.currentScreen() != this || rejectUneditablePage(page)) return;
        String current = String.valueOf(pendingValues.getOrDefault(key, ""));
        KineticClientRuntime.openScreen(new KTLongTextEditorScreen(
                this,
                entry.label(),
                current,
                value -> {
                    updateValidation(key, entry, value);
                    rawTextValues.remove(key);
                    status = null;
                    rebuildUi();
                }
        ));
    }

    private void openListEditor(KTConfigPage page, KTConfigEntry<?> entry) {
        if (KineticClientRuntime.currentScreen() != this || rejectUneditablePage(page)) return;
        String key = entryKey(page, entry);
        List<?> values = listValue(key);

        if (entry.type() == KTConfigEntry.Type.ENTITY_LIST) {
            List<String> entityIds = values.stream().map(String::valueOf).toList();
            KineticSelectors.openEntitySelector(
                    this,
                    entry.label(),
                    entityIds,
                    result -> updateValidationAndRebuild(
                            key, entry, new ArrayList<>(result)
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
                            key, entry, new ArrayList<>(result)
                    )
            );
            return;
        }

        boolean integerList = entry.type() == KTConfigEntry.Type.INTEGER_LIST;
        KineticClientRuntime.openScreen(new KTConfigListScreen(
                this,
                entry.label(),
                entry.tooltip(),
                integerList,
                values,
                result -> updateValidationAndRebuild(
                        key, entry, new ArrayList<>(result)
                )
        ));
    }

    private List<?> listValue(String key) {
        Object value = pendingValues.get(key);
        return value instanceof List<?> list ? list : List.of();
    }

    private void updateValidation(String key, KTConfigEntry<?> entry, Object value) {
        pendingValues.put(key, value);
        boolean valid;
        try {
            valid = entry.accepts(value);
        } catch (RuntimeException failure) {
            KineticRuntime.logger().error("Failed to validate config entry {}", entry.id(), failure);
            valid = false;
        }
        if (valid) {
            invalidEntries.remove(key);
        } else {
            invalidEntries.add(key);
        }
    }

    private void updateValidationAndRebuild(String key, KTConfigEntry<?> entry, Object value) {
        updateValidation(key, entry, value);
        status = null;
        rebuildUi();
    }

    private void setParsedValue(String key, KTConfigEntry<?> entry, Object value, KineticEditBox box) {
        rawTextValues.put(key, box.getValue());
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
            invalidEntries.add(key);
            return;
        }
        invalidEntries.remove(key);
        pendingValues.put(key, value);
    }

    @Override
    protected void canvasTick() {
        if (!searchDirty) return;

        searchDirty = false;
        rebuildRows();
        rowScroll.update(rows.size(), VISIBLE_ROWS);
        rebuildUi();
        if (searchBox != null) {
            focusControl(searchBox);
            searchBox.setCursorPosition(searchQuery.length());
        }
    }

    private void resetAll() {
        // Prepare defaults across every editable page before changing any draft.
        Map<String, Object> defaults = new HashMap<>();
        try {
            for (KTConfigPage page : pages) {
                if (!KTConfigApi.canEdit(page)) continue;
                for (KTConfigEntry<?> entry : page.entries()) {
                    if (!entry.isValueEntry()) continue;
                    defaults.put(entryKey(page, entry), entry.snapshot(entry.defaultValue()));
                }
            }
        } catch (Throwable failure) {
            KineticRuntime.logger().error("Failed to reset module config", failure);
            status = KineticText.translatable("gui.kineticcore.config.reset_failed",
                    Component.literal(failure.getClass().getSimpleName()));
            return;
        }
        pendingValues.putAll(defaults);
        for (String key : defaults.keySet()) {
            invalidEntries.remove(key);
            rawTextValues.remove(key);
        }
        status = KineticText.translatable("gui.kineticcore.config.reset_done");
        rebuildUi();
    }

    private void saveAndClose() {
        SaveOutcome outcome = persistPendingValues();
        if (outcome == SaveOutcome.FAILED) return;
        if (shouldShowImmediateSavedToast(outcome)) showSavedToast();
        navigateBack();
    }

    private SaveOutcome persistPendingValues() {
        lastSaveSentServerRequest = false;
        for (KTConfigPage page : pages) {
            for (KTConfigEntry<?> entry : page.entries()) {
                if (!entry.isValueEntry()) continue;
                String key = entryKey(page, entry);
                try {
                    if (!entry.accepts(pendingValues.get(key))) invalidEntries.add(key);
                } catch (RuntimeException invalidValue) {
                    // A faulty add-on validator must reject this field, not crash the whole editor.
                    KineticRuntime.logger().error("Failed to validate config entry {} on page {}",
                            entry.id(), page.id(), invalidValue);
                    invalidEntries.add(key);
                }
            }
        }

        if (!invalidEntries.isEmpty()) {
            status = KineticText.translatable(
                    "gui.kineticcore.config.invalid",
                    Component.literal(String.valueOf(invalidEntries.size()))
            );
            return SaveOutcome.FAILED;
        }

        Map<KTConfigPage, List<KTConfigEntry<?>>> changedByPage = new LinkedHashMap<>();
        for (KTConfigPage page : pages) {
            List<KTConfigEntry<?>> changed = page.entries().stream()
                    .filter(KTConfigEntry::isValueEntry)
                    .filter(entry -> {
                        String key = entryKey(page, entry);
                        return !Objects.equals(pendingValues.get(key), originalValues.get(key));
                    })
                    .toList();
            if (!changed.isEmpty()) changedByPage.put(page, changed);
        }

        if (changedByPage.isEmpty()) {
            status = null;
            return SaveOutcome.UNCHANGED;
        }

        for (KTConfigPage page : changedByPage.keySet()) {
            if (rejectUneditablePage(page)) return SaveOutcome.FAILED;
        }

        // Prepare the entire module before committing any individual page. A
        // copier in a later page must not fail after an earlier page was saved.
        Map<String, Object> committedSnapshots = new HashMap<>();
        try {
            for (Map.Entry<KTConfigPage, List<KTConfigEntry<?>>> pageChange : changedByPage.entrySet()) {
                for (KTConfigEntry<?> entry : pageChange.getValue()) {
                    String key = entryKey(pageChange.getKey(), entry);
                    committedSnapshots.put(key, entry.snapshot(pendingValues.get(key)));
                }
            }
        } catch (Throwable failure) {
            KineticRuntime.logger().error("Failed to prepare module config for saving", failure);
            status = KineticText.translatable("gui.kineticcore.config.save_failed",
                    Component.literal(failure.getClass().getSimpleName()));
            return SaveOutcome.FAILED;
        }

        for (Map.Entry<KTConfigPage, List<KTConfigEntry<?>>> pageChange : changedByPage.entrySet()) {
            KTConfigPage page = pageChange.getKey();
            if (page.scope() == KTConfigScope.SERVER_AUTHORITATIVE) {
                Map<String, Object> changedValues = new LinkedHashMap<>();
                for (KTConfigEntry<?> entry : pageChange.getValue()) {
                    String key = entryKey(page, entry);
                    changedValues.put(entry.id(), committedSnapshots.get(key));
                }
                if (!KTServerConfigClient.save(page, changedValues)) {
                    status = KTConfigApi.unavailableReason(page);
                    return SaveOutcome.FAILED;
                }
                lastSaveSentServerRequest = true;
            } else {
                List<KTConfigEntry<?>> applied = new ArrayList<>();
                try {
                    for (KTConfigEntry<?> entry : pageChange.getValue()) {
                        String key = entryKey(page, entry);
                        // Include the in-flight writer: it may mutate the value before throwing.
                        applied.add(entry);
                        entry.writeSnapshot(pendingValues.get(key));
                    }
                    page.save();
                } catch (Throwable throwable) {
                    KineticRuntime.logger().error("Failed to save config page {}", page.id(), throwable);
                    rollbackOriginalValues(page, applied);
                    status = KineticText.translatable(
                            "gui.kineticcore.config.save_failed",
                            Component.literal(throwable.getClass().getSimpleName())
                    );
                    return SaveOutcome.FAILED;
                }
            }

            for (KTConfigEntry<?> entry : pageChange.getValue()) {
                String key = entryKey(page, entry);
                originalValues.put(key, committedSnapshots.get(key));
            }
        }

        status = null;
        return SaveOutcome.SAVED;
    }

    private boolean rejectUneditablePage(KTConfigPage page) {
        if (KTConfigApi.canEdit(page)) return false;
        status = KTConfigApi.unavailableReason(page);
        KineticOverlays.toast("kineticcore_config_unavailable", status, KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        return true;
    }

    private boolean shouldShowImmediateSavedToast(SaveOutcome outcome) {
        return outcome == SaveOutcome.UNCHANGED
                || (outcome == SaveOutcome.SAVED && !lastSaveSentServerRequest);
    }

    private void showSavedToast() {
        try {
            KTConfigApi.notifyModuleSaved(title);
        } catch (Throwable throwable) {
            KineticRuntime.logger().debug("Could not show module config saved toast", throwable);
        }
    }

    private void rollbackOriginalValues(KTConfigPage page, List<KTConfigEntry<?>> appliedEntries) {
        // Reverse write order so dependent fields are restored before their prerequisites.
        for (int index = appliedEntries.size() - 1; index >= 0; index--) {
            KTConfigEntry<?> entry = appliedEntries.get(index);
            try {
                entry.writeSnapshot(originalValues.get(entryKey(page, entry)));
            } catch (Throwable rollbackFailure) {
                KineticRuntime.logger().error(
                        "Failed to roll back config value {} on page {}",
                        entry.id(),
                        page.id(),
                        rollbackFailure
                );
            }
        }
    }

    @Override
    protected void renderCanvasBackground(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        GuiTheme.panel(graphics, 18, 12, 604, 342);
        graphics.drawCenteredString(font, title, canvasWidth() / 2, 24, 0xFFFFAA00);

        hoveredRow = null;
        double pixelOffset = rowPixelOffset();
        enableUiScissor(graphics, 28, ROW_TOP, SCROLL_X - 2, ROW_TOP + LIST_HEIGHT);
        try {
            for (Map.Entry<Row, Integer> visible : visibleRows.entrySet()) {
                Row row = visible.getKey();
                int y = visible.getValue() - (int) Math.round(pixelOffset);
                if (y + ROW_HEIGHT <= ROW_TOP || y >= ROW_TOP + LIST_HEIGHT) continue;
                int tooltipWidth = row.kind() == RowKind.PAGE ? 578 : 292;
                if (mouseY >= ROW_TOP && mouseY < ROW_TOP + LIST_HEIGHT
                        && GuiTheme.hovering(mouseX, mouseY, 30, y - 3, tooltipWidth, 23)) {
                    hoveredRow = row;
                }

                switch (row.kind()) {
                    case SCOPE -> {
                        graphics.fill(30, y - 3, 612, y + 20, 0x66303030);
                        graphics.drawString(font, row.text(), 38, y + 4, GuiTheme.current().text(), false);
                    }
                    case PAGE -> {
                        graphics.fill(34, y - 2, 608, y + 19, 0x44222222);
                        KineticText.drawScrollingLeft(graphics, font, row.text(), 46, y + 4, 540, 0xFFFFAA00, false);
                        if (!KTConfigApi.canEdit(row.page())) {
                            Component locked = KineticText.translatable("gui.kineticcore.config.server_locked");
                            graphics.drawString(
                                    font,
                                    locked,
                                    600 - font.width(locked),
                                    y + 4,
                                    0xFFFF5555,
                                    false
                            );
                        }
                    }
                    case ENTRY -> {
                        KTConfigEntry<?> entry = row.entry();
                        String key = entryKey(row.page(), entry);
                        if (entry.type() == KTConfigEntry.Type.SECTION) {
                            graphics.fill(38, y - 3, 612, y + 20, 0x33222222);
                            graphics.drawString(font, entry.label(), 46, y + 4, 0xFFFFCC55, false);
                        } else if (entry.type() == KTConfigEntry.Type.DESCRIPTION) {
                            KineticText.drawScrollingLeft(graphics, font, entry.label(), 46, y + 5, 554, 0xFFAAAAAA, false);
                        } else {
                            int color;
                            if (!KTConfigApi.canEdit(row.page())) {
                                color = 0xFF999999;
                            } else {
                                color = invalidEntries.contains(key) ? 0xFFFF5555 : 0xFFE0E0E0;
                            }
                            KineticText.drawScrollingLeft(graphics, font, entry.label(), 46, y + 6, 282, color, false);
                        }
                    }
                }
            }
        } finally {
            disableUiScissor(graphics);
        }

        if (rows.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    KineticText.translatable("gui.kineticcore.config.no_fields"),
                    canvasWidth() / 2,
                    176,
                    0xFFAAAAAA
            );
        }

        rowScroll.render(
                graphics,
                mouseX,
                mouseY,
                SCROLL_X,
                ROW_TOP,
                SCROLL_WIDTH,
                LIST_HEIGHT,
                18
        );

        if (status != null) {
            graphics.drawCenteredString(
                    font,
                    status,
                    canvasWidth() / 2,
                    309,
                    invalidEntries.isEmpty() ? 0xFFFFFF55 : 0xFFFF5555
            );
        } else {
            graphics.drawCenteredString(
                    font,
                    KineticText.translatable("gui.kineticcore.config.module_scope_hint"),
                    canvasWidth() / 2,
                    309,
                    0xFFAAAAAA
            );
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
        if (hoveredRow == null) return;

        Component tooltip = switch (hoveredRow.kind()) {
            case SCOPE -> null;
            case PAGE -> {
                if (!KTConfigApi.canEdit(hoveredRow.page())) {
                    yield KTConfigApi.unavailableReason(hoveredRow.page());
                }
                yield hoveredRow.page().description();
            }
            case ENTRY -> hoveredRow.entry().type() == KTConfigEntry.Type.DESCRIPTION
                    ? hoveredRow.entry().label()
                    : hoveredRow.entry().tooltip();
        };

        if (tooltip != null) {
            showTooltip(List.of(tooltip), 400);
        }
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && rowScroll.beginDrag(
                mouseX,
                mouseY,
                SCROLL_X,
                ROW_TOP,
                SCROLL_WIDTH,
                LIST_HEIGHT,
                18,
                2
        )) {
            return true;
        }
        return super.canvasMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (rowScroll.drag(mouseY, ROW_TOP, LIST_HEIGHT, 18)) {
            return true;
        }
        return super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        return rowScroll.release(button) || super.canvasMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= 28 && mouseX <= 620
                && mouseY >= ROW_TOP && mouseY < ROW_TOP + LIST_HEIGHT
                && rowScroll.scroll(delta, 1.0D)) {
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected boolean handleCloseRequest() {
        if (isClean()) {
            return false;
        }

        openDialog(
                KineticText.translatable("gui.kineticcore.config.unsaved_action.title"),
                KineticText.translatable("gui.kineticcore.config.module_unsaved_close"),
                KineticText.translatable("gui.yes"),
                KineticText.translatable("gui.no"),
                () -> {
                    SaveOutcome outcome = persistPendingValues();
                    if (outcome == SaveOutcome.FAILED) return;
                    if (shouldShowImmediateSavedToast(outcome)) showSavedToast();
                    navigateBack();
                },
                () -> navigateBack()
        );
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    void serverSnapshotUpdated(String pageId) {
        KTConfigPage page = pages.stream()
                .filter(candidate -> candidate.id().equals(pageId))
                .findFirst()
                .orElse(null);
        if (page == null) return;

        // Update only the matching page, and publish it as one transaction.
        // If an add-on reader or copier fails, keep every existing baseline and draft.
        Map<String, Object> nextOriginal = new HashMap<>();
        Map<String, Object> nextPending = new HashMap<>();
        Set<String> cleanKeys = new HashSet<>();
        try {
            for (KTConfigEntry<?> entry : page.entries()) {
                if (!entry.isValueEntry()) continue;
                String key = entryKey(page, entry);
                boolean hasLocalDraft = !Objects.equals(pendingValues.get(key), originalValues.get(key))
                        || invalidEntries.contains(key) || rawTextValues.containsKey(key);
                Object value;
                try {
                    value = entry.read();
                } catch (Throwable throwable) {
                    KineticRuntime.logger().error("Failed to refresh server config value {} from page {}",
                            entry.id(), page.id(), throwable);
                    value = entry.defaultValue();
                }
                boolean accepted;
                try {
                    accepted = entry.accepts(value);
                } catch (RuntimeException invalidValue) {
                    KineticRuntime.logger().error("Failed to validate server config entry {} on page {}",
                            entry.id(), page.id(), invalidValue);
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
            KineticRuntime.logger().error("Failed to synchronize module config page {}", page.id(), failure);
            return;
        }
        originalValues.putAll(nextOriginal);
        pendingValues.putAll(nextPending);
        for (String key : cleanKeys) {
            invalidEntries.remove(key);
            rawTextValues.remove(key);
        }
        if (KineticClientRuntime.currentScreen() == this) rebuildUi();
    }

    private void rebuildRows() {
        String normalized = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        List<Row> result = new ArrayList<>();

        appendScopeGroup(result, false, normalized);
        appendScopeGroup(result, true, normalized);
        rows = List.copyOf(result);
    }

    private void appendScopeGroup(List<Row> result, boolean serverGroup, String normalized) {
        List<Row> groupRows = new ArrayList<>();
        for (KTConfigPage page : pages) {
            boolean serverPage = page.scope() == KTConfigScope.SERVER_AUTHORITATIVE;
            if (serverPage != serverGroup) continue;

            List<KTConfigEntry<?>> matched = matchingEntries(page, normalized);
            boolean pageMatched = normalized.isEmpty() || matchesPage(page, normalized);
            if (!pageMatched && matched.isEmpty()) continue;

            groupRows.add(Row.page(page));
            if (normalized.isEmpty() || pageMatched) {
                for (KTConfigEntry<?> entry : page.entries()) groupRows.add(Row.entry(page, entry));
            } else {
                for (KTConfigEntry<?> entry : matched) groupRows.add(Row.entry(page, entry));
            }
        }

        if (!groupRows.isEmpty()) {
            result.add(Row.scope(serverGroup
                    ? KTConfigScope.SERVER_AUTHORITATIVE
                    : KTConfigScope.CLIENT_LOCAL));
            result.addAll(groupRows);
        }
    }

    private List<KTConfigEntry<?>> matchingEntries(KTConfigPage page, String query) {
        if (query.isEmpty()) return page.entries();
        return page.entries().stream()
                .filter(entry -> matchesEntry(entry, query))
                .toList();
    }

    private boolean matchesPage(KTConfigPage page, String query) {
        StringBuilder raw = new StringBuilder(page.id()).append(' ').append(page.title().getString());
        if (page.description() != null) raw.append(' ').append(page.description().getString());
        raw.append(' ').append(KineticText.translatable(page.scope().detailTranslationKey()).getString());
        return matches(raw.toString(), query);
    }

    private boolean matchesEntry(KTConfigEntry<?> entry, String query) {
        StringBuilder raw = new StringBuilder(entry.id()).append(' ').append(entry.label().getString());
        if (entry.tooltip() != null) raw.append(' ').append(entry.tooltip().getString());
        return matches(raw.toString(), query);
    }

    private boolean matches(String raw, String query) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains(query)) return true;
        return KineticSearch.match(raw, query);
    }

    private static int scopeOrder(KTConfigScope scope) {
        return switch (scope) {
            case CLIENT_LOCAL -> 0;
            case LOCAL_INSTALLATION -> 1;
            case SERVER_AUTHORITATIVE -> 2;
        };
    }

    private static String scopeHeaderKey(KTConfigScope scope) {
        return switch (scope) {
            case CLIENT_LOCAL, LOCAL_INSTALLATION -> "gui.kineticcore.config.scope.client.header";
            case SERVER_AUTHORITATIVE -> "gui.kineticcore.config.scope.server.header";
        };
    }

    private static String entryKey(KTConfigPage page, KTConfigEntry<?> entry) {
        return page.id() + "/" + entry.id();
    }

    private static Component booleanText(boolean value) {
        return KineticText.translatable(value
                ? "gui.kineticcore.config.enabled"
                : "gui.kineticcore.config.disabled");
    }

    private static String formatColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

}
