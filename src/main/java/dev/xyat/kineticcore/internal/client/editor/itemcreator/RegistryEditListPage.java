package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.network.chat.Component;
import java.util.List;

public abstract class RegistryEditListPage extends ItemCreatorPage {
    protected final ItemCreatorDraft draft;
    private final boolean effects;
    private String query = "";
    private boolean searchExpanded;
    private int scrollOffset;
    private List<KineticSuggestion> visible = List.of();
    private KineticToggleActionList list;
    protected RegistryEditListPage(ItemCreatorDraft draft, boolean effects) {
        super(effects ? "effect_title" : "attribute_title"); this.draft = draft; this.effects = effects;
    }
    @Override protected void build(KineticUi ui) {
        header(ui, "back");
        ui.button(410, 12, 76).text(tr("search")).onClick(() -> { rememberScroll(); searchExpanded = !searchExpanded; rebuild(); }).build();
        if (searchExpanded) ui.textField(22, 52, 352).value(query).placeholder(tr("search_hint")).maxLength(256)
                .onChange(text -> { query = text; scrollOffset = 0; updateRows(); list.setScrollOffset(0); }).build();
        int y = searchExpanded ? 86 : 54;
        list = ui.toggleActionList(22, y, 352, 226 - (searchExpanded ? 32 : 0), List.of())
                .toggleWidth(26).actionWidth(42).scrollOffset(scrollOffset)
                .onToggle((index, on) -> { String id = visible.get(index).value(); if (effects) draft.selectEffect(id, on); else draft.selectAttribute(id, on); })
                .onSelect(this::edit).onAction(this::edit).build();
        updateRows(); list.setScrollOffset(scrollOffset);
    }
    private void updateRows() {
        var source = effects ? KineticSearch.potionDictionary() : KineticSearch.attributeDictionary();
        visible = source.stream().filter(s -> KineticSearch.match(s.value() + " " + s.translation().getString(), query)).toList();
        list.setItems(visible.stream().map(s -> new ToggleActionItem(Component.literal(s.value()), s.translation(), summary(s.value()), true, false,
                effects ? draft.isEffectSelected(s.value()) : draft.isAttributeSelected(s.value()), Component.literal("✓"), Component.literal("＋"), tr("list_hint"), true,
                tr("edit"), summary(s.value()), true, false)).toList());
    }
    private Component summary(String id) {
        if (effects) {
            var spec = draft.effect(id);
            return spec.durationTicks() == -1 ? Component.literal(id).append("\n").append(tr("permanent"))
                    : Component.literal(id).append("\n").append(tr("effect_summary", spec.amplifier() + 1, spec.durationTicks() / 20D));
        }
        var spec = draft.attribute(id);
        return Component.literal(id).append("\n").append(tr(spec.operation().name().toLowerCase(java.util.Locale.ROOT))).append(": " + (spec.operation() == dev.xyat.kineticcore.api.itemcreator.AttributeSpec.Operation.MULTIPLY_TOTAL ? spec.amount() + 1 : spec.amount()));
    }
    private void rememberScroll() { if (list != null) scrollOffset = list.scrollOffset(); }
    private void edit(int index) {
        rememberScroll(); String id = visible.get(index).value();
        openChild(effects ? new EffectEditPage(draft, id) : new AttributeEditPage(draft, id));
    }
    @Override protected void onTick() { rememberScroll(); }
    @Override protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        g.text(tr("selected", effects ? draft.effects().size() : draft.attributes().size()), 22, 290, KineticTheme.current().text());
        g.wrappedText(tr(effects ? "preview" : "list_hint"), 394, 150, 84, KineticTheme.current().mutedText());
        int index = list.itemAt(mx, my);
        if (index < 0) index = list.toggleAt(mx, my);
        if (index < 0) index = list.actionAt(mx, my);
        if (effects && index >= 0 && index < visible.size()) {
            var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(visible.get(index).value()));
            if (effect != null) { g.effectIcon(effect, 418, 78, 32); g.wrappedText(effect.getDisplayName(), 394, 120, 84, KineticTheme.current().text()); }
        }
    }
}
