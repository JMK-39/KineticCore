package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.*;
import dev.xyat.kineticcore.api.client.gui.widget.KineticNumberField;
import dev.xyat.kineticcore.api.itemcreator.AttributeSpec;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class AttributeEditPage extends ItemCreatorPage {
    private final ItemCreatorDraft draft;
    private final String id;
    private AttributeSpec.Operation operation;
    private String value;
    private KineticNumberField field;
    public AttributeEditPage(ItemCreatorDraft draft, String id) {
        super("attribute_title"); this.draft = draft; this.id = id;
        var initial = draft.attribute(id); operation = initial.operation();
        value = Double.toString(operation == AttributeSpec.Operation.MULTIPLY_TOTAL ? initial.amount() + 1 : initial.amount());
    }
    @Override protected void build(KineticUi ui) {
        header(ui, "cancel");
        ui.cycleButton(100, 112, 300, List.of(tr("addition"), tr("multiply_total"), tr("set")))
                .index(operation.ordinal()).onChange(index -> { operation = AttributeSpec.Operation.values()[index]; rebuild(); }).build();
        field = ui.numberField(100, 166, 300, NumberType.DECIMAL).allowNegative(true)
                .onChange(text -> value = text).build();
        field.setTextValue(value);
        ui.button(180, 212, 140).text(tr("reset")).onClick(() -> { operation = AttributeSpec.Operation.ADDITION; value = "0.0"; rebuild(); }).build();
        ui.button(180, 242, 140).text(tr("save")).onClick(this::save).build();
    }
    private void save() {
        Double number = field.getDoubleValue();
        if (number == null) { KineticOverlays.toast(tr("invalid_number")); return; }
        try {
            draft.saveAttribute(new AttributeSpec(id, operation == AttributeSpec.Operation.MULTIPLY_TOTAL ? number - 1 : number, operation));
            navigateBack();
        } catch (IllegalArgumentException e) { KineticOverlays.toast(tr("invalid_number")); }
        catch (IllegalStateException e) { KineticOverlays.toast(tr("selection_limit", dev.xyat.kineticcore.api.itemcreator.ItemCreatorValidation.MAX_ENTRIES)); }
    }
    @Override protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        g.scrollingText(Component.literal(id), 100, 76, 300, KineticTheme.current().text(), false);
        g.text(tr(operation == AttributeSpec.Operation.MULTIPLY_TOTAL ? "multiplier" : "value"), 100, 148, KineticTheme.current().text());
    }
}
