package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticEntityPreview;
import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SpawnEggItem;
import java.util.*;

public final class ItemCreatorEditorPage extends ItemCreatorPage {
    private final boolean potion;
    private ItemCreatorDraft draft;
    private final ItemCreatorPresets presets;
    private final KineticEntityPreview preview = KineticEntityPreview.create();
    private final ItemCreatorInlineList attributes = new ItemCreatorInlineList(false), effects = new ItemCreatorInlineList(true);
    private boolean showEffects, searchExpanded, pending;
    private boolean saveErrorShown;
    private Component notice = Component.empty();
    private dev.xyat.kineticcore.api.client.gui.widget.KineticButton attributeTab, effectTab;
    public ItemCreatorEditorPage(boolean potion) { this(potion, ItemCreatorPresets.local()); }
    public ItemCreatorEditorPage(boolean potion, ItemCreatorPresets presets) { this(potion, presets.active(potion), presets); }
    public ItemCreatorEditorPage(boolean potion, ItemCreatorDraft draft) { this(potion, draft, null); }
    private ItemCreatorEditorPage(boolean potion, ItemCreatorDraft draft, ItemCreatorPresets presets) {
        super(potion ? "potion_title" : "spawn_title"); this.potion = potion; this.draft = draft; this.presets = presets; showEffects = potion;
    }
    @Override protected void build(KineticUi ui) {
        retain(); header(ui, "back");
        if (presets != null) ui.button(410, 12, 76).text(tr("presets")).enabled(!pending).onClick(this::choosePreset).build();
        ui.textField(22, 70, 140).value(draft.name).placeholder(tr("name_hint")).maxLength(ItemCreatorValidation.MAX_NAME_LENGTH)
                .onChange(text -> { draft.name = text; retain(); }).build();
        if (potion) ui.button(22, 100, 140).text(tr(draft.kind.name().toLowerCase(Locale.ROOT))).onClick(this::chooseForm).build();
        else {
            ui.button(22, 100, 140).text(tr("choose_entity")).onClick(this::chooseEntity).build();
            attributeTab = ui.button(180, 52, 102).text(tr("attributes", draft.attributes().size())).onClick(() -> { remember(); showEffects = false; rebuild(); }).build();
            effectTab = ui.button(286, 52, 102).text(tr("effects", draft.effects().size())).onClick(() -> { remember(); showEffects = true; rebuild(); }).build();
            ui.button(394, 52, 84).text(tr("search")).onClick(() -> { remember(); searchExpanded = !searchExpanded; rebuild(); }).build();
        }
        var catalog = showEffects ? effects : attributes;
        catalog.build(ui, draft, potion || searchExpanded, id -> {
            catalog.remember(); retain(); openChild(showEffects ? new EffectEditPage(draft, id) : new AttributeEditPage(draft, id));
        });
        if (presets != null) ui.button(22, 254, 140).text(tr("new_preset")).enabled(!pending).onClick(() -> { retain(); presets.create(potion); draft = presets.active(potion); notice = Component.empty(); rebuild(); }).build();
        boolean allowed = Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2);
        ui.button(22, 280, 140).text(tr(pending ? "pending" : "generate")).enabled(allowed && !pending)
                .tooltip(tr(allowed ? "generate" : "permission")).onClick(this::generate).build();
    }
    private void retain() {
        if (presets == null) return;
        try {
            presets.retain(potion, draft);
            if (presets.saveFailed() && !saveErrorShown) KineticOverlays.toast(tr("preset_save_failed"));
            saveErrorShown = presets.saveFailed();
        } catch (IllegalArgumentException error) { notice = tr("invalid"); }
    }
    private void remember() { attributes.remember(); effects.remember(); }
    @Override protected void onTick() { remember(); retain(); if (!potion) { attributeTab.setText(tr("attributes", draft.attributes().size())); effectTab.setText(tr("effects", draft.effects().size())); } }
    @Override protected boolean onCloseRequested() { retain(); return false; }
    private void chooseForm() {
        openContextMenu(22, 118, Arrays.stream(KineticItemCreator.PotionKind.values()).map(kind -> KineticOverlays.MenuItem.choice(
                tr(kind.name().toLowerCase(Locale.ROOT)), tr(kind.name().toLowerCase(Locale.ROOT)), draft.kind == kind,
                () -> { draft.kind = kind; retain(); rebuild(); })).toList(), 140);
    }
    private void choosePreset() {
        retain(); var rows = new ArrayList<KineticOverlays.MenuItem>();
        for (var entry : presets.entries(potion)) {
            var snapshot = entry.draft(); Component label = snapshot.name().isEmpty() ? (potion ? tr(snapshot.kind().name().toLowerCase(Locale.ROOT)) : Component.literal(snapshot.entityId())) : Component.literal(snapshot.name());
            rows.add(KineticOverlays.MenuItem.choice(label, label, entry.id().equals(presets.activeId(potion)), () -> {
                retain(); presets.activate(potion, entry.id()); draft = presets.active(potion); notice = Component.empty(); rebuild();
            }));
        }
        openContextMenu(330, 32, rows, 156);
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
        retain(); KineticSelectors.openSingleEntitySelector(tr("choose_entity"), draft.entityId, allowed, id -> { draft.entityId = id; retain(); });
    }
    private void generate() {
        if (pending) return; retain(); pending = true; notice = tr("pending");
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
    @Override protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        g.text(tr("name"), 22, 52, KineticTheme.current().text());
        if (!potion) {
            KineticTheme.itemSlot(g, 28, 124, 128, 128, 2, false, false, false);
            preview.render(g, draft.entityId, draft.entityId, 31, 127, 122, 122, mx >= 28 && mx < 156 && my >= 124 && my < 252);
            registerPreviewZoomArea(preview, draft.entityId, 28, 124, 128, 128);
        } else {
            g.text(tr("effects", draft.effects().size()), 180, 55, KineticTheme.current().text());
            g.clipped(22, 132, 162, 244, () -> g.wrappedText(tr("potion_help"), 22, 132, 140, KineticTheme.current().mutedText()));
        }
        g.clipped(180, 284, 478, 305, () -> g.wrappedText(notice.getString().isEmpty() ? tr("list_hint") : notice, 180, 286, 298, KineticTheme.current().text()));
    }
}
