package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.search.*;
import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Catalog embedded in the root editor; selected entries and edited parameters stay independent. */
public final class ItemCreatorInlineList {
    private final boolean effects;
    private String query = "";
    private int offset;
    private List<KineticSuggestion> visible = List.of();
    private KineticToggleActionList list;
    private ItemCreatorDraft draft;
    public ItemCreatorInlineList(boolean effects) { this.effects = effects; }
    public void build(KineticUi ui, ItemCreatorDraft draft, boolean search, Consumer<String> edit) {
        this.draft = draft;
        if (search) ui.textField(180, 76, 298).value(query).placeholder(ItemCreatorPage.tr("search_hint")).maxLength(256)
                .onChange(text -> { query = text; offset = 0; update(); if (list != null) list.setScrollOffset(0); }).build();
        int y = search ? 100 : 76;
        list = ui.toggleActionList(180, y, 298, 278 - y, List.of()).textRows(true).toggleWidth(22).actionWidth(36).scrollOffset(offset)
                .onToggle((index, on) -> { select(visible.get(index).value(), on); update(); })
                .onSelect(index -> edit.accept(visible.get(index).value()))
                .onAction(index -> edit.accept(visible.get(index).value())).build();
        update(); list.setScrollOffset(offset);
    }
    private boolean selected(String id) { return effects ? draft.isEffectSelected(id) : draft.isAttributeSelected(id); }
    private boolean pinned(String id) { return effects ? draft.isEffectPinned(id) : draft.isAttributePinned(id); }
    private void select(String id, boolean on) {
        try { if (effects) draft.selectEffect(id, on); else draft.selectAttribute(id, on); }
        catch (IllegalStateException limit) { dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.toast(ItemCreatorPage.tr("selection_limit", ItemCreatorValidation.MAX_ENTRIES)); }
    }
    private void update() {
        var source = effects ? KineticSearch.potionDictionary() : KineticSearch.attributeDictionary();
        visible = source.stream().filter(s -> KineticSearch.match(s.value() + " " + s.translation().getString(), query))
                .sorted(Comparator.comparing((KineticSuggestion s) -> !pinned(s.value()))
                        .thenComparing(s -> !s.value().startsWith("minecraft:")).thenComparing(KineticSuggestion::value)).toList();
        if (list == null) return;
        list.setItems(visible.stream().map(s -> new ToggleActionItem(Component.literal(s.value()).withStyle(ChatFormatting.WHITE),
                display(s), summary(s.value()), true, false, selected(s.value()),
                Component.literal("✓"), Component.literal("＋"), ItemCreatorPage.tr("list_hint"), true,
                ItemCreatorPage.tr("edit"), ItemCreatorPage.tr("edit"), true, false,
                effects ? KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(s.value())) : null)).toList());
    }
    public void remember() { if (list != null) offset = list.scrollOffset(); }
    private Component display(KineticSuggestion entry) {
        var text = entry.translation().copy().withStyle(ChatFormatting.GOLD); if (!selected(entry.value())) return text;
        if (effects) { var spec = draft.effect(entry.value()); return text.append(Component.literal(" · " + (spec.amplifier() + 1) + " · ")
                .withStyle(ChatFormatting.WHITE)).append(spec.durationTicks() == -1 ? ItemCreatorPage.tr("permanent") : Component.literal(spec.durationTicks() / 20D + "s")); }
        var spec = draft.attribute(entry.value()); return text.append(Component.literal(" · " + (spec.operation() == AttributeSpec.Operation.MULTIPLY_TOTAL ? spec.amount() + 1 : spec.amount())).withStyle(ChatFormatting.WHITE));
    }
    public Component summary(String id) {
        if (effects) { var spec = draft.effect(id); return Component.literal(id).append("\n").append(spec.durationTicks() == -1
                ? Component.literal((spec.amplifier() + 1) + " · ").append(ItemCreatorPage.tr("permanent"))
                : ItemCreatorPage.tr("effect_summary", spec.amplifier() + 1, spec.durationTicks() / 20D)); }
        var spec = draft.attribute(id); return Component.literal(id).append("\n").append(ItemCreatorPage.tr(spec.operation().name().toLowerCase(Locale.ROOT)))
                .append(" " + (spec.operation() == AttributeSpec.Operation.MULTIPLY_TOTAL ? spec.amount() + 1 : spec.amount()));
    }
}
