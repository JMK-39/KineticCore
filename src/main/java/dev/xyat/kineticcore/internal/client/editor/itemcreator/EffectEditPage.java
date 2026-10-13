package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.*;
import dev.xyat.kineticcore.api.client.gui.widget.KineticNumberField;
import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.network.chat.Component;

public final class EffectEditPage extends ItemCreatorPage {
    private final ItemCreatorDraft draft;
    private final String id;
    private String level, seconds;
    private boolean permanent, particles, icon;
    private KineticNumberField levelField, timeField;
    public EffectEditPage(ItemCreatorDraft draft, String id) {
        super("effect_title"); this.draft = draft; this.id = id;
        var initial = draft.effect(id);
        level = Integer.toString(initial.amplifier() + 1); seconds = Double.toString(initial.durationTicks() < 0 ? 30 : initial.durationTicks() / 20D);
        permanent = initial.durationTicks() == -1; particles = initial.particles(); icon = initial.icon();
    }
    @Override protected void build(KineticUi ui) {
        header(ui, "cancel");
        levelField = ui.numberField(100, 116, 140, NumberType.INT).range(1, 256).onChange(text -> level = text).build(); levelField.setTextValue(level);
        timeField = ui.numberField(260, 116, 140, NumberType.DECIMAL).allowNegative(false).enabled(!permanent).onChange(text -> seconds = text).build(); timeField.setTextValue(seconds);
        var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(id));
        ui.toggle(100, 160, 300).value(permanent).labels(tr("permanent"), tr("permanent"))
                .enabled(effect != null && !effect.isInstantenous()).onChange(on -> { permanent = on; timeField.setEnabled(!on); }).build();
        ui.toggle(100, 196, 140).value(particles).labels(tr("particles"), tr("particles")).onChange(on -> particles = on).build();
        ui.toggle(260, 196, 140).value(icon).labels(tr("icon"), tr("icon")).onChange(on -> icon = on).build();
        ui.button(180, 226, 140).text(tr("reset")).onClick(() -> { level = "1"; seconds = "30.0"; permanent = false; particles = true; icon = true; rebuild(); }).build();
        ui.button(180, 254, 140).text(tr("save")).onClick(this::save).build();
    }
    private void save() {
        Integer number = levelField.getIntValue(); Double duration = timeField.getDoubleValue();
        if (number == null || !permanent && duration == null) { KineticOverlays.toast(tr("invalid_number")); return; }
        try {
            var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(id));
            if (permanent && effect != null && effect.isInstantenous()) { KineticOverlays.toast(tr("instant_permanent")); return; }
            draft.saveEffect(new EffectSpec(id, number - 1, permanent ? -1 : ItemCreatorValidation.secondsToTicks(duration), particles, icon)); navigateBack();
        } catch (IllegalArgumentException e) { KineticOverlays.toast(tr("invalid_number")); }
        catch (IllegalStateException e) { KineticOverlays.toast(tr("selection_limit", ItemCreatorValidation.MAX_ENTRIES)); }
    }
    @Override protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(id));
        if (effect != null) g.effectIcon(effect, 100, 62, 24);
        g.scrollingText(Component.literal(id), 134, 70, 266, KineticTheme.current().text(), false);
        g.text(tr("level"), 100, 98, KineticTheme.current().text());
        g.text(tr("duration"), 260, 98, KineticTheme.current().text());
    }
}
