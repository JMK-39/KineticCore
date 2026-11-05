package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.ui.NumberType;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;
import dev.xyat.kineticcore.api.client.gui.widget.KineticDropdown;
import dev.xyat.kineticcore.api.client.gui.widget.KineticEntityPreview;
import dev.xyat.kineticcore.api.client.gui.widget.KineticNumberField;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTextField;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.gui.render.KineticTexture;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.inventory.KineticItemText;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import java.util.*;

/**
 * Potion and spawn-egg editor. Left: the saved records (kept in the client config), newest first; the record being
 * edited has a yellow outline. Right: name and form (potion) or mob (egg), a search box and the catalog of effects or
 * attributes. Ticking an entry puts it on the item and shows its level and duration (or operation and value) right in
 * its row. Save stores the record and moves the ticked entries to the top of the catalog; Generate asks the server for
 * the item. Vanilla entries come before modded ones.
 */
public final class ItemCreatorEditorPage extends ItemCreatorPage {
    private static final int LEFT_X = 4, LEFT_W = 168, RIGHT_X = LEFT_X + LEFT_W + 8, RIGHT = 636, RIGHT_W = RIGHT - RIGHT_X;
    private static final int TOP_Y = 28, SEARCH_Y = 52, HEAD_Y = 80, LIST_Y = 92, LIST_BOTTOM = 356, LIST_H = LIST_BOTTOM - LIST_Y;
    private static final int ROW_H = 24, SCROLLBAR_W = 4, MIN_THUMB = 16, ROW_RIGHT = RIGHT - SCROLLBAR_W - 2;
    // Controls of a ticked row, right-aligned 2 px inside the row.
    // 3 from the row edge leaves 2 px inside its 1 px outline, the same as above and below the row controls.
    private static final int PERMANENT_W = 48, PERMANENT_X = ROW_RIGHT - 3 - PERMANENT_W;
    private static final int SECONDS_W = 56, SECONDS_X = PERMANENT_X - 4 - SECONDS_W;
    private static final int LEVEL_W = 40, LEVEL_X = SECONDS_X - 4 - LEVEL_W;
    private static final int VALUE_W = 64, VALUE_X = ROW_RIGHT - 3 - VALUE_W;
    private static final int OPERATION_W = 96, OPERATION_X = VALUE_X - 4 - OPERATION_W;
    private static final int CHECK_X = RIGHT_X + 5, CHECK_SIZE = 12;
    private static final int KIND_W = 100, ENTITY_W = 140, TAB_W = 80;
    private static final int GENERATE_W = 48, SAVE_W = 40, NEW_W = 40, PREVIEW_H = 120;
    private static final ItemGridDensity EQUIPMENT_DENSITY = ItemGridDensity.COMPACT;
    private static final String MULTIPLIER_TOAST = "kineticcore:item_creator_multiplier";

    private final boolean potion;
    private final ItemCreatorPresets presets;
    private ItemCreatorDraft draft;
    /** The saved record being edited, or null for a new draft. */
    private String recordId;
    /** What the record holds; the draft differs from it when there are unsaved edits. */
    private ItemCreatorDraft.Snapshot savedSnapshot;
    /** The ticked entries at the last open or save: they sort to the top until the next save. */
    private ItemCreatorDraft.Snapshot orderSnapshot;
    private final KineticEntityPreview preview = KineticEntityPreview.create();
    private final KineticScrollController scroll = new KineticScrollController();
    private final Map<String, ItemStack> icons = new HashMap<>();
    /** Row fields holding text that is not a valid value, as "id#field". */
    private final Set<String> invalid = new HashSet<>();
    private boolean showEffects, pending, rebuildPending, refocusSearch;
    private boolean showEquipment;
    private LivingEntity previewEntity;
    private String previewState = "";
    private Component notice = Component.empty();
    private String query = "";
    private List<KineticSuggestion> visible = List.of();
    private KineticTextField searchField;
    private KineticButton saveButton;
    private KineticItemGrid equipmentGrid;

    public ItemCreatorEditorPage(boolean potion) { this(potion, ItemCreatorPresets.local()); }
    public ItemCreatorEditorPage(boolean potion, ItemCreatorPresets presets) { this(potion, null, presets); }
    /** Edits the given draft without saved records (validation fixtures). */
    public ItemCreatorEditorPage(boolean potion, ItemCreatorDraft draft) { this(potion, draft, null); }
    ItemCreatorEditorPage(boolean potion, ItemCreatorDraft draft, ItemCreatorPresets presets) {
        super(potion ? "potion_title" : "spawn_title");
        this.potion = potion; this.presets = presets; showEffects = potion;
        if (draft != null) { load(null, draft); return; }
        var entry = presets == null ? null : presets.entry(presets.activeId(potion));
        load(entry == null ? null : entry.id(), entry == null ? new ItemCreatorDraft() : ItemCreatorDraft.from(entry.draft()));
    }

    private void load(String id, ItemCreatorDraft next) {
        recordId = id; draft = next; savedSnapshot = next.snapshot(); orderSnapshot = savedSnapshot; invalid.clear();
        notice = Component.empty();
    }

    // ---------------------------------------------------------------- layout

    /** The egg editor's attribute and effect lists as views for screenshot tools; the potion editor has one view. */
    @Override public List<ReviewView> reviewViews() {
        return potion ? List.of() : List.of(new ReviewView("attributes", () -> switchTab(false)),
                new ReviewView("effects", () -> switchTab(true)), new ReviewView("equipment", this::switchEquipment));
    }

    @Override protected void build(KineticUi ui) {
        header(ui, "back");
        boolean allowed = Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2);
        int generateX = RIGHT - GENERATE_W, saveX = generateX - 4 - SAVE_W, newX = saveX - 4 - NEW_W;
        saveButton = null;
        equipmentGrid = null;
        if (presets != null) {
            ui.button(newX, 4, NEW_W).text(tr("new_record")).tooltip(tr("new_record.tooltip")).enabled(!pending)
                    .onClick(() -> confirmLeave(() -> { load(null, new ItemCreatorDraft()); presets.setActive(potion, null); scroll.setOffset(0); rebuild(); })).build();
            saveButton = ui.button(saveX, 4, SAVE_W).text(tr("save")).tooltip(tr("save.tooltip")).enabled(!pending && canSave())
                    .onClick(this::save).build();
        }
        ui.button(generateX, 4, GENERATE_W).text(tr(pending ? "pending_short" : "generate")).enabled(allowed && !pending)
                .tooltip(tr(allowed ? "generate.tooltip" : "permission")).onClick(this::generate).build();

        if (presets != null) {
            int listY = potion ? TOP_Y : TOP_Y + PREVIEW_H + 4;
            var records = ui.add(new ItemCreatorRecordList(LEFT_X, listY, LEFT_W, LIST_BOTTOM - listY, () -> recordId,
                    this::icon, this::recordName, this::recordDetails, this::recordClicked));
            records.setItems(presets.entries(potion));
        }

        int sideW = potion ? KIND_W : ENTITY_W, nameW = RIGHT_W - 4 - sideW;
        ui.textField(RIGHT_X, TOP_Y, nameW).value(draft.name).placeholder(tr("name_hint")).maxLength(ItemCreatorValidation.MAX_NAME_LENGTH)
                .tooltip(tr("name")).onChange(text -> { draft.name = text; refreshSave(); }).build();
        if (potion) {
            List<KineticDropdown.Option> kinds = new ArrayList<>();
            for (var kind : KineticItemCreator.PotionKind.values()) {
                kinds.add(new KineticDropdown.Option(kind.name(), tr(kind.name().toLowerCase(Locale.ROOT)), tr("kind.tooltip")));
            }
            ui.dropdown(RIGHT - KIND_W, TOP_Y, KIND_W, kinds).selected(draft.kind.name()).tooltip(tr("kind.tooltip"))
                    .onChange(value -> { draft.kind = KineticItemCreator.PotionKind.valueOf(value); refreshSave(); }).build();
        } else {
            ui.button(RIGHT - ENTITY_W, TOP_Y, ENTITY_W).text(tr("entity_button", entityName(draft.entityId)))
                    .tooltip(tr("choose_entity.tooltip")).onClick(this::chooseEntity).build();
        }

        int searchX = RIGHT_X;
        if (!potion) {
            ui.button(RIGHT_X, SEARCH_Y, TAB_W).text(tr("attributes", draft.attributes().size()))
                    .onClick(() -> switchTab(false)).build().setSelected(!showEquipment && !showEffects);
            ui.button(RIGHT_X + TAB_W + 4, SEARCH_Y, TAB_W).text(tr("effects", draft.effects().size()))
                    .onClick(() -> switchTab(true)).build().setSelected(!showEquipment && showEffects);
            ui.button(RIGHT_X + 2 * (TAB_W + 4), SEARCH_Y, TAB_W).text(tr("equipment", draft.equipment().size()))
                    .onClick(this::switchEquipment).build().setSelected(showEquipment);
            searchX = RIGHT_X + 3 * (TAB_W + 4);
        }
        searchField = null;
        if (showEquipment) { buildEquipment(ui); return; }
        searchField = ui.textField(searchX, SEARCH_Y, RIGHT - searchX).value(query).placeholder(tr("search_hint")).maxLength(256)
                .onChange(text -> { if (text.equals(query)) return; query = text; scroll.setOffset(0); refocusSearch = true; rebuildPending = true; }).build();

        refreshVisible();
        scroll.updateContent(visible.size() * ROW_H, LIST_H, ROW_H);
        KineticUi rows = ui.scrollViewport(RIGHT_X, LIST_Y, RIGHT, LIST_BOTTOM, this::pixelOffset);
        for (int index = 0; index < visible.size(); index++) {
            String id = visible.get(index).value();
            if (!included(id)) continue;
            int y = LIST_Y + index * ROW_H + (ROW_H - KineticScreen.STANDARD_CONTROL_HEIGHT) / 2;
            if (showEffects) buildEffectRow(rows, id, y); else buildAttributeRow(rows, id, y);
        }
        if (refocusSearch) { refocusSearch = false; focus(searchField); }
    }

    private void buildEffectRow(KineticUi rows, String id, int y) {
        var spec = draft.effect(id);
        var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(id));
        boolean instant = effect != null && effect.isInstantenous();
        KineticNumberField level = rows.numberField(LEVEL_X, y, LEVEL_W, NumberType.INT).range(1, 256)
                .tooltip(tr("level")).onChange(text -> editLevel(id, text)).build();
        level.setTextValue(Integer.toString(spec.amplifier() + 1));
        boolean permanent = spec.durationTicks() == -1;
        KineticNumberField seconds = rows.numberField(SECONDS_X, y, SECONDS_W, NumberType.DECIMAL).allowNegative(false)
                .enabled(!permanent).tooltip(tr("duration")).onChange(text -> editSeconds(id, text)).build();
        seconds.setTextValue(permanent ? "" : number(spec.durationTicks() / 20D));
        rows.toggle(PERMANENT_X, y, PERMANENT_W).value(permanent).labels(tr("permanent_short"), tr("timed_short"))
                .enabled(!instant).tooltip(tr(instant ? "instant_permanent" : "permanent.tooltip"))
                .onChange(on -> setPermanent(id, on, seconds)).build();
    }

    private void buildAttributeRow(KineticUi rows, String id, int y) {
        var spec = draft.attribute(id);
        rows.cycleButton(OPERATION_X, y, OPERATION_W, List.of(tr("addition"), tr("multiply_total"), tr("set")))
                .index(spec.operation().ordinal()).tooltip(tr("operation.tooltip"))
                .onChange(index -> setOperation(id, AttributeSpec.Operation.values()[index])).build();
        KineticNumberField value = rows.numberField(VALUE_X, y, VALUE_W, NumberType.DECIMAL).allowNegative(true)
                .tooltip(tr(spec.operation() == AttributeSpec.Operation.MULTIPLY_TOTAL ? "multiplier" : "value"))
                .onChange(text -> editValue(id, text)).build();
        value.setTextValue(number(shown(spec)));
    }

    // ---------------------------------------------------------------- catalog

    private void switchTab(boolean effects) {
        if (!showEquipment && showEffects == effects) return;
        showEquipment = false; showEffects = effects; query = ""; scroll.setOffset(0); rebuild();
    }

    private void switchEquipment() {
        if (showEquipment) return;
        showEquipment = true; query = ""; scroll.setOffset(0); rebuild();
    }

    private void buildEquipment(KineticUi ui) {
        scroll.updateContent(0, LIST_H, ROW_H);
        List<ItemGridItem> items = new ArrayList<>();
        for (var slot : EquipmentSpec.Slot.values()) {
            var spec = equipment(slot);
            var stack = equipmentStack(slot);
            var tooltip = tr("equipment." + slot.name().toLowerCase(Locale.ROOT)).copy().append("\n").append(tr("equipment.slot.tooltip"));
            items.add(new ItemGridItem(stack, tooltip, true, false, spec != null && stack.isEmpty(), ItemGridOutline.NONE, equipmentIcon(slot)));
        }
        int width = EQUIPMENT_DENSITY.padding() * 2 + 6 * EQUIPMENT_DENSITY.cellPitch() - EQUIPMENT_DENSITY.gap() + 6;
        int height = EQUIPMENT_DENSITY.padding() * 2 + EQUIPMENT_DENSITY.slotSize();
        equipmentGrid = ui.itemGrid(RIGHT_X, LIST_Y, width, height, EQUIPMENT_DENSITY, items).decorations(true)
                .onClick(index -> chooseEquipment(EquipmentSpec.Slot.values()[index])).build();
    }

    private static KineticTexture equipmentIcon(EquipmentSpec.Slot slot) {
        String icon = switch (slot) { case MAINHAND -> "sword"; case OFFHAND -> "shield"; case HEAD -> "helmet";
            case CHEST -> "chestplate"; case LEGS -> "leggings"; case FEET -> "boots"; };
        //? if >=26.1 {
        /*String path = "textures/gui/sprites/container/slot/" + icon + ".png";
        *///?} else {
        String path = "textures/item/" + (slot == EquipmentSpec.Slot.MAINHAND ? "empty_slot_" : "empty_armor_slot_") + icon + ".png";
        //?}
        return KineticTexture.of("minecraft", path, 16, 16);
    }

    private void equipmentMenu(EquipmentSpec.Slot slot, double x, double y) {
        List<KineticOverlays.MenuItem> actions = new ArrayList<>();
        actions.add(KineticOverlays.MenuItem.action(tr("equipment.choose"), tr("equipment.choose.tooltip"), () -> chooseEquipment(slot)));
        if (equipment(slot) != null) {
            actions.add(KineticOverlays.MenuItem.action(tr("equipment.data"), tr("equipment.data.tooltip"), () -> editEquipment(slot)));
            actions.add(KineticOverlays.MenuItem.action(tr("equipment.clear"), tr("equipment.hint"), () -> {
                draft.setEquipment(slot, null); refreshSave(); rebuild();
            }));
        }
        openContextMenu(x, y, actions);
    }

    private EquipmentSpec equipment(EquipmentSpec.Slot slot) {
        return draft.equipment().stream().filter(spec -> spec.slot() == slot).findFirst().orElse(null);
    }

    private ItemStack equipmentStack(EquipmentSpec.Slot slot) {
        var spec = equipment(slot);
        if (spec == null) return ItemStack.EMPTY;
        try { return KineticItemText.parse(spec.item()); } catch (RuntimeException error) { return ItemStack.EMPTY; }
    }

    private void chooseEquipment(EquipmentSpec.Slot slot) {
        var current = equipmentStack(slot);
        var options = KineticSelectors.ItemSelectorOptions.itemsOnly(KineticSelectors.ItemSelectorPreset.defaults(),
                current.isEmpty() ? List.of() : List.of(current), stack -> true);
        KineticSelectors.openItemSelectorWithOptions(options, selection -> {
            if (selection == null || !selection.isItem() || selection.stack().isEmpty()) return;
            try { draft.setEquipment(slot, KineticItemText.format(selection.stack())); }
            catch (IllegalArgumentException error) { KineticOverlays.toast(tr("invalid")); }
            refreshSave();
        });
    }

    private void editEquipment(EquipmentSpec.Slot slot) {
        var spec = equipment(slot); if (spec == null) return;
        String item = spec.item();
        int dataStart = item.length();
        for (char marker : new char[] {'{', '['}) { int at = item.indexOf(marker); if (at >= 0) dataStart = Math.min(dataStart, at); }
        String id = item.substring(0, dataStart);
        KineticSelectors.openNbtEditor(item.substring(dataStart), data -> {
            try {
                var stack = KineticItemText.parse(id + data);
                if (stack.isEmpty()) return tr("invalid").getString();
                new EquipmentSpec(slot, KineticItemText.format(stack));
                return null;
            }
            catch (RuntimeException error) { return tr("invalid").getString(); }
        }, data -> { draft.setEquipment(slot, KineticItemText.format(KineticItemText.parse(id + data))); refreshSave(); });
    }

    private void refreshVisible() {
        var source = showEffects ? KineticSearch.potionDictionary() : KineticSearch.attributeDictionary();
        // Ticked at the last save first, then vanilla before mods, then by ID.
        visible = source.stream().filter(s -> KineticSearch.match(s.value() + " " + s.translation().getString(), query))
                .sorted(Comparator.comparing((KineticSuggestion s) -> !pinned(s.value()))
                        .thenComparing(s -> !s.value().startsWith("minecraft:")).thenComparing(KineticSuggestion::value)).toList();
    }

    private boolean included(String id) { return showEffects ? draft.isEffectSelected(id) : draft.isAttributeSelected(id); }

    private boolean pinned(String id) {
        return showEffects ? orderSnapshot.selectedEffects().contains(id) && orderSnapshot.pinnedEffects().contains(id)
                : orderSnapshot.selectedAttributes().contains(id) && orderSnapshot.pinnedAttributes().contains(id);
    }

    private double pixelOffset() {
        return Math.min(scroll.smoothOffset() * ROW_H, Math.max(0, visible.size() * ROW_H - LIST_H));
    }

    private int rowAt(double x, double y) {
        if (showEquipment) return -1;
        if (x < RIGHT_X || x >= ROW_RIGHT || y < LIST_Y || y >= LIST_BOTTOM) return -1;
        int index = (int) Math.floor((y - LIST_Y + pixelOffset()) / ROW_H);
        return index >= 0 && index < visible.size() ? index : -1;
    }

    private void toggle(String id) {
        boolean on = !included(id);
        try {
            if (showEffects) draft.selectEffect(id, on); else draft.selectAttribute(id, on);
        } catch (IllegalStateException limit) {
            KineticOverlays.toast(tr("selection_limit", ItemCreatorValidation.MAX_ENTRIES));
            return;
        }
        if (!on) invalid.removeIf(key -> key.startsWith(id + "#"));
        rebuild();
    }

    // ---------------------------------------------------------------- row edits

    private void editLevel(String id, String text) {
        var spec = draft.effect(id);
        Integer level = parseInt(text);
        if (level == null || level < 1 || level > 256) { invalid.add(id + "#level"); refreshSave(); return; }
        invalid.remove(id + "#level");
        draft.putEffect(new EffectSpec(id, level - 1, spec.durationTicks(), spec.particles(), spec.icon()));
        refreshSave();
    }

    private void editSeconds(String id, String text) {
        var spec = draft.effect(id);
        if (spec.durationTicks() == -1) return;
        Double seconds = parseDouble(text);
        int ticks;
        try { ticks = seconds == null ? -2 : ItemCreatorValidation.secondsToTicks(seconds); }
        catch (IllegalArgumentException error) { ticks = -2; }
        if (ticks <= 0) { invalid.add(id + "#seconds"); refreshSave(); return; }
        invalid.remove(id + "#seconds");
        draft.putEffect(new EffectSpec(id, spec.amplifier(), ticks, spec.particles(), spec.icon()));
        refreshSave();
    }

    private void setPermanent(String id, boolean permanent, KineticNumberField seconds) {
        var spec = draft.effect(id);
        int ticks = permanent ? -1 : 600;
        if (!permanent) {
            Double typed = parseDouble(seconds.textValue());
            if (typed != null && typed > 0) {
                try { ticks = ItemCreatorValidation.secondsToTicks(typed); } catch (IllegalArgumentException ignored) { }
            }
        }
        draft.putEffect(new EffectSpec(id, spec.amplifier(), ticks, spec.particles(), spec.icon()));
        invalid.remove(id + "#seconds");
        seconds.setEnabled(!permanent);
        seconds.setTextValue(permanent ? "" : number(ticks / 20D));
        refreshSave();
    }

    private void setOperation(String id, AttributeSpec.Operation operation) {
        double value = shown(draft.attribute(id));
        // A factor must be above 0. A newly ticked row starts at +0, which would be "times 0": use the neutral 1
        // and say so.
        if (operation == AttributeSpec.Operation.MULTIPLY_TOTAL && value <= 0) {
            value = 1;
            KineticOverlays.toast(MULTIPLIER_TOAST, tr("multiplier_reset"));
        }
        // The rebuilt row shows the stored value, so text that was not a number is gone.
        invalid.remove(id + "#value");
        draft.putAttribute(new AttributeSpec(id, stored(value, operation), operation));
        rebuild();
    }

    private void editValue(String id, String text) {
        var spec = draft.attribute(id);
        Double value = parseDouble(text);
        boolean factorTooSmall = spec.operation() == AttributeSpec.Operation.MULTIPLY_TOTAL && value != null && value <= 0;
        // Typing 0 as a factor is a mistake worth naming; the same toast is replaced while the player keeps typing.
        if (factorTooSmall) KineticOverlays.toast(MULTIPLIER_TOAST, tr("multiplier_zero"));
        if (value == null || !Double.isFinite(value) || factorTooSmall) {
            invalid.add(id + "#value"); refreshSave(); return;
        }
        invalid.remove(id + "#value");
        draft.putAttribute(new AttributeSpec(id, stored(value, spec.operation()), spec.operation()));
        refreshSave();
    }

    /** A multiplier is shown as the factor (2 = twice) and stored as the change (1). */
    private static double shown(AttributeSpec spec) {
        return spec.operation() == AttributeSpec.Operation.MULTIPLY_TOTAL ? spec.amount() + 1 : spec.amount();
    }

    private static double stored(double shown, AttributeSpec.Operation operation) {
        return operation == AttributeSpec.Operation.MULTIPLY_TOTAL ? shown - 1 : shown;
    }

    private void effectMenu(String id, double x, double y) {
        var spec = draft.effect(id);
        openContextMenu(x, y, List.of(
                KineticOverlays.MenuItem.choice(tr("particles"), tr("particles.tooltip"), spec.particles(), () -> {
                    var now = draft.effect(id);
                    draft.putEffect(new EffectSpec(id, now.amplifier(), now.durationTicks(), !now.particles(), now.icon()));
                    refreshSave();
                }),
                KineticOverlays.MenuItem.choice(tr("icon"), tr("icon.tooltip"), spec.icon(), () -> {
                    var now = draft.effect(id);
                    draft.putEffect(new EffectSpec(id, now.amplifier(), now.durationTicks(), now.particles(), !now.icon()));
                    refreshSave();
                })));
    }

    // ---------------------------------------------------------------- records

    private boolean dirty() { return !draft.snapshot().equals(savedSnapshot); }

    private boolean canSave() { return presets != null && (dirty() || recordId == null && !draft.isBlank()); }

    private void refreshSave() {
        if (saveButton != null) saveButton.setEnabled(!pending && canSave());
    }

    private void save() {
        if (presets == null) return;
        if (!invalid.isEmpty()) { KineticOverlays.toast(tr("invalid_number")); return; }
        draft.pinSelected();
        recordId = presets.save(potion, recordId, draft);
        savedSnapshot = draft.snapshot(); orderSnapshot = savedSnapshot;
        icons.remove(recordId);
        KineticOverlays.toast(tr(presets.saveFailed() ? "preset_save_failed" : "saved"));
        scroll.setOffset(0);
        rebuild();
    }

    /** Runs the action now, or after the player agrees to drop the unsaved edits. */
    private void confirmLeave(Runnable action) {
        if (presets == null || !dirty()) { action.run(); return; }
        openDialog(tr("discard.title"), tr("discard.message"), tr("discard.confirm"), tr("cancel"), action, () -> { });
    }

    private void recordClicked(ItemCreatorPresets.Entry entry, MouseInput input) {
        if (input.isRight()) {
            openContextMenu(input.x(), input.y(), List.of(
                    KineticOverlays.MenuItem.action(tr("record.open"), tr("record.open.tooltip"), () -> openRecord(entry)),
                    KineticOverlays.MenuItem.action(tr("record.copy"), tr("record.copy.tooltip"), () -> {
                        presets.duplicate(entry.id());
                        if (recordId != null) presets.setActive(potion, recordId);
                        rebuild();
                    }),
                    KineticOverlays.MenuItem.separator(),
                    KineticOverlays.MenuItem.danger(tr("record.delete"), tr("record.delete.tooltip"), () ->
                            openDialog(tr("record.delete.title"), recordName(entry), tr("record.delete"), tr("cancel"), () -> {
                                presets.delete(entry.id());
                                icons.remove(entry.id());
                                // The open draft stays on screen as a new, unsaved one.
                                if (entry.id().equals(recordId)) { recordId = null; savedSnapshot = new ItemCreatorDraft().snapshot(); }
                                rebuild();
                            }, () -> { }))));
            return;
        }
        if (!entry.id().equals(recordId)) openRecord(entry);
    }

    private void openRecord(ItemCreatorPresets.Entry entry) {
        confirmLeave(() -> {
            load(entry.id(), ItemCreatorDraft.from(entry.draft()));
            presets.setActive(potion, entry.id());
            scroll.setOffset(0);
            rebuild();
        });
    }

    private ItemStack icon(ItemCreatorPresets.Entry entry) {
        return icons.computeIfAbsent(entry.id(), ignored -> {
            var record = ItemCreatorDraft.from(entry.draft());
            try {
                if (potion) return KineticItemCreator.buildPotion(record.kind, record.effects(), record.name);
                var level = Minecraft.getInstance().level;
                if (level != null) return KineticItemCreator.buildEgg(level, record.template());
            } catch (RuntimeException ignoredError) {
                // An entry from a removed mod: fall back to the plain item.
            }
            return new ItemStack(potion ? Items.POTION : Items.EGG);
        });
    }

    private Component recordName(ItemCreatorPresets.Entry entry) {
        var snapshot = entry.draft();
        if (!snapshot.name().isEmpty()) return Component.literal(snapshot.name());
        if (!potion) return entityName(snapshot.entityId());
        List<String> ids = snapshot.selectedEffects();
        if (ids.isEmpty()) return tr("unnamed");
        var text = ItemCreatorEntryText.name(true, ids.get(0)).copy();
        if (ids.size() > 1) text.append(tr("and_more", ids.size() - 1));
        return text;
    }

    private Component recordDetails(ItemCreatorPresets.Entry entry) {
        var snapshot = entry.draft();
        var text = recordName(entry).copy().withStyle(ChatFormatting.GOLD);
        text.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY));
        text.append(potion ? tr(snapshot.kind().name().toLowerCase(Locale.ROOT)) : entityName(snapshot.entityId()));
        text.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY));
        text.append(potion ? tr("effects", snapshot.selectedEffects().size())
                : tr("attributes", snapshot.selectedAttributes().size()).copy().append(" ").append(tr("effects", snapshot.selectedEffects().size())));
        return text;
    }

    // ---------------------------------------------------------------- mob, generation, closing

    private static Component entityName(String id) {
        var type = KineticRegistries.entityTypes().get(KineticResourceIds.tryParse(id));
        return type == null ? Component.literal(id) : type.getDescription();
    }

    private void chooseEntity() {
        var level = Minecraft.getInstance().level; if (level == null) return;
        List<String> allowed = new ArrayList<>();
        for (var entry : KineticRegistries.entityTypes().entries().entrySet()) {
            var type = entry.getValue();
            //? if >=26.1 {
            /*boolean hasEgg = SpawnEggItem.byId(type).isPresent();
            *///?} else {
            boolean hasEgg = SpawnEggItem.byId(type) != null;
            //?}
            if (!hasEgg) continue;
            try { if (type.create(level) instanceof LivingEntity) allowed.add(entry.getKey().toString()); } catch (RuntimeException ignored) { }
        }
        KineticSelectors.openSingleEntitySelector(tr("choose_entity"), draft.entityId, allowed, id -> { draft.entityId = id; refreshSave(); });
    }

    private void generate() {
        if (pending) return;
        if (!invalid.isEmpty()) { KineticOverlays.toast(tr("invalid_number")); return; }
        pending = true; notice = tr("pending");
        try {
            if (potion) KineticItemCreator.requestPotion(draft.kind, draft.effects(), draft.name, this::received);
            else KineticItemCreator.requestEgg(draft.template(), this::received);
            rebuild();
        } catch (IllegalArgumentException | IllegalStateException error) { pending = false; notice = tr("invalid"); KineticOverlays.toast(notice); rebuild(); }
    }

    private void received(KineticItemCreator.Result result) {
        pending = false; notice = dev.xyat.kineticcore.api.text.KineticI18n.translatable(result.translationKey()); KineticOverlays.toast(notice);
        if (KineticGui.currentPage() == this) rebuild();
    }

    @Override protected boolean onCloseRequested() {
        if (presets == null || !dirty()) return false;
        openDialog(tr("discard.title"), tr("discard.message"), tr("discard.confirm"), tr("cancel"), this::navigateBack, () -> { });
        return true;
    }

    @Override protected void onTick() {
        if (rebuildPending) { rebuildPending = false; rebuild(); }
    }

    // ---------------------------------------------------------------- input

    @Override protected boolean onMouseClick(MouseInput input) {
        if (showEquipment && equipmentGrid != null && input.isRight()) {
            int index = equipmentGrid.itemAt(input.x(), input.y());
            if (index >= 0) { equipmentMenu(EquipmentSpec.Slot.values()[index], input.x(), input.y()); return true; }
        }
        if (scroll.canScroll() && scroll.beginDrag(input.x(), input.y(), input.button(), RIGHT - SCROLLBAR_W, LIST_Y,
                SCROLLBAR_W, LIST_H, MIN_THUMB)) return true;
        int index = rowAt(input.x(), input.y());
        if (index < 0) return false;
        String id = visible.get(index).value();
        if (input.isLeft()) { toggle(id); return true; }
        if (input.isRight() && showEffects && included(id)) { effectMenu(id, input.x(), input.y()); return true; }
        return false;
    }

    @Override protected boolean onMouseDrag(MouseDragInput input) { return scroll.drag(input.y(), LIST_Y, LIST_H, MIN_THUMB); }

    @Override protected boolean onMouseRelease(MouseInput input) { return scroll.release(input.button()); }

    @Override protected boolean onMouseScroll(ScrollInput input) {
        if (input.x() >= RIGHT_X && input.x() < RIGHT && input.y() >= LIST_Y && input.y() < LIST_BOTTOM) return scroll.scroll(input.deltaY());
        return false;
    }

    // ---------------------------------------------------------------- drawing

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        var theme = KineticTheme.current();
        KineticTheme.verticalSeparator(g, RIGHT_X - 4, TOP_Y, LIST_BOTTOM - TOP_Y);
        if (showEquipment) { renderEquipment(g); return; }
        // Column titles over the controls of ticked rows.
        g.scrollingText(tr(showEffects ? "column.effect" : "column.attribute"), RIGHT_X + 4, HEAD_Y, 120, theme.mutedText(), false);
        if (showEffects) {
            g.scrollingTextCentered(tr("level_label"), LEVEL_X + LEVEL_W / 2, HEAD_Y, LEVEL_W, theme.mutedText(), false);
            g.scrollingTextCentered(tr("seconds_label"), SECONDS_X + SECONDS_W / 2, HEAD_Y, SECONDS_W, theme.mutedText(), false);
            g.scrollingTextCentered(tr("permanent_short"), PERMANENT_X + PERMANENT_W / 2, HEAD_Y, PERMANENT_W, theme.mutedText(), false);
        } else {
            g.scrollingTextCentered(tr("operation_label"), OPERATION_X + OPERATION_W / 2, HEAD_Y, OPERATION_W, theme.mutedText(), false);
            g.scrollingTextCentered(tr("value"), VALUE_X + VALUE_W / 2, HEAD_Y, VALUE_W, theme.mutedText(), false);
        }
        if (visible.isEmpty()) {
            g.scrollingTextCentered(tr("no_match"), RIGHT_X + RIGHT_W / 2, LIST_Y + LIST_H / 2 - 4, RIGHT_W - 12, theme.mutedText(), false);
            return;
        }
        int offset = (int) Math.round(pixelOffset());
        int hovered = rowAt(mx, my);
        g.clipped(RIGHT_X, LIST_Y, RIGHT, LIST_BOTTOM, () -> {
            int first = Math.max(0, offset / ROW_H), last = Math.min(visible.size(), (offset + LIST_H) / ROW_H + 2);
            for (int index = first; index < last; index++) renderRow(g, index, LIST_Y + index * ROW_H - offset, index == hovered);
        });
        if (scroll.canScroll()) scroll.render(g, mx, my, RIGHT - SCROLLBAR_W, LIST_Y, SCROLLBAR_W, LIST_H, MIN_THUMB);
    }

    private void renderRow(KineticGraphics g, int index, int y, boolean hovered) {
        String id = visible.get(index).value();
        boolean ticked = included(id);
        int width = ROW_RIGHT - RIGHT_X;
        KineticTheme.surface(g, RIGHT_X, y + 1, width, ROW_H - 2, index % 2 == 0 ? KineticTheme.Surface.PANEL_ALT : KineticTheme.Surface.PANEL);
        if (ticked) KineticTheme.indicatorOutline(g, RIGHT_X, y + 1, width, ROW_H - 2, KineticTheme.Indicator.SUCCESS);
        else KineticTheme.stateOutline(g, RIGHT_X, y + 1, width, ROW_H - 2, false, hovered, false);
        // Tick box.
        int boxY = y + (ROW_H - CHECK_SIZE) / 2;
        g.fill(CHECK_X, boxY, CHECK_X + CHECK_SIZE, boxY + CHECK_SIZE, KineticTheme.current().field());
        KineticTheme.stateOutline(g, CHECK_X, boxY, CHECK_SIZE, CHECK_SIZE, false, hovered, false);
        if (ticked) KineticTheme.indicatorFill(g, CHECK_X + 3, boxY + 3, CHECK_SIZE - 6, CHECK_SIZE - 6, KineticTheme.Indicator.SUCCESS);
        int textX = CHECK_X + CHECK_SIZE + 5;
        if (showEffects) {
            var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(id));
            if (effect != null) g.effectIcon(effect, textX, y + 4, 16);
            textX += 20;
        }
        int textRight = ticked ? (showEffects ? LEVEL_X : OPERATION_X) - 4 : ROW_RIGHT - 6;
        g.scrollingText(ItemCreatorEntryText.label(showEffects, id), textX, y + 8, Math.max(0, textRight - textX),
                KineticTheme.current().text(), false);
    }

    private void renderEquipment(KineticGraphics g) {
        var theme = KineticTheme.current();
        g.scrollingText(tr("equipment.title"), RIGHT_X + 4, HEAD_Y, RIGHT_W - 8, theme.mutedText(), false);
    }

    @Override protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        if (potion) return;
        // The mob above the records, in a square-cornered frame so the checkerboard fills it.
        int px = LEFT_X, py = TOP_Y, size = PREVIEW_H;
        KineticTheme.itemSlot(g, px, py, LEFT_W, size, 2, false, false, false);
        String state = draft.entityId + draft.equipment();
        var level = Minecraft.getInstance().level;
        if (!state.equals(previewState) || previewEntity == null || previewEntity.level() != level) {
            previewEntity = null; previewState = state;
            var type = KineticRegistries.entityTypes().get(KineticResourceIds.tryParse(draft.entityId));
            if (type != null && level != null) {
                try {
                    if (type.create(level) instanceof LivingEntity living) {
                        for (var spec : draft.equipment()) living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.valueOf(spec.slot().name()), KineticItemText.parse(spec.item()));
                        previewEntity = living;
                    }
                } catch (RuntimeException ignored) { }
            }
        }
        preview.render(g, previewEntity, draft.entityId, px + 3, py + 3, LEFT_W - 6, size - 6,
                mx >= px && mx < px + LEFT_W && my >= py && my < py + size);
        registerPreviewZoomArea(preview, draft.entityId, px, py, LEFT_W, size);
    }

    @Override protected void renderTooltips(int mx, int my) {
        int index = rowAt(mx, my);
        if (index < 0) return;
        String id = visible.get(index).value();
        List<Component> lines = new ArrayList<>();
        var name = ItemCreatorEntryText.name(showEffects, id);
        lines.add(name.copy().withStyle(ChatFormatting.GOLD));
        if (!name.getString().equals(id)) lines.add(Component.literal(id).withStyle(ChatFormatting.GRAY));
        if (!included(id)) lines.add(tr("tick.tooltip"));
        else if (showEffects) {
            var spec = draft.effect(id);
            lines.add(tr("switch_state", tr("particles"), tr(spec.particles() ? "on" : "off")));
            lines.add(tr("switch_state", tr("icon"), tr(spec.icon() ? "on" : "off")));
            lines.add(tr("effect_menu.tooltip"));
        }
        showTooltip(lines);
    }

    // ---------------------------------------------------------------- helpers

    private static Integer parseInt(String text) {
        try { return Integer.parseInt(text.trim()); } catch (RuntimeException error) { return null; }
    }

    private static Double parseDouble(String text) {
        try { return Double.parseDouble(text.trim()); } catch (RuntimeException error) { return null; }
    }

    private static String number(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e9 ? Long.toString((long) value) : Double.toString(value);
    }
}
