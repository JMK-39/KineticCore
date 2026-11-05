package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticRowList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Saved records on the left of the potion and spawn-egg editors: the generated item's icon and the record's name.
 * The record being edited has a yellow outline; left click opens a record, right click opens its menu.
 */
final class ItemCreatorRecordList extends KineticRowList<ItemCreatorPresets.Entry> {
    static final int ROW_HEIGHT = 24;
    private final Supplier<String> currentId;
    private final Function<ItemCreatorPresets.Entry, ItemStack> icon;
    private final Function<ItemCreatorPresets.Entry, Component> name;
    private final Function<ItemCreatorPresets.Entry, Component> details;
    private final BiConsumer<ItemCreatorPresets.Entry, MouseInput> onClick;

    ItemCreatorRecordList(int x, int y, int width, int height, Supplier<String> currentId,
                          Function<ItemCreatorPresets.Entry, ItemStack> icon, Function<ItemCreatorPresets.Entry, Component> name,
                          Function<ItemCreatorPresets.Entry, Component> details, BiConsumer<ItemCreatorPresets.Entry, MouseInput> onClick) {
        super(x, y, width, height, ROW_HEIGHT);
        this.currentId = currentId;
        this.icon = icon;
        this.name = name;
        this.details = details;
        this.onClick = onClick;
    }

    @Override
    protected void renderRowBackground(KineticGraphics graphics, int index, int x, int y, int width, int height,
                                       boolean hovered, boolean selected) {
        KineticTheme.surface(graphics, x, y + 1, width, height - 2,
                index % 2 == 0 ? KineticTheme.Surface.PANEL_ALT : KineticTheme.Surface.PANEL);
        if (items().get(index).id().equals(currentId.get())) {
            KineticTheme.indicatorOutline(graphics, x, y + 1, width, height - 2, KineticTheme.Indicator.WARNING);
        } else {
            KineticTheme.stateOutline(graphics, x, y + 1, width, height - 2, false, hovered, false);
        }
    }

    @Override
    protected void renderRow(KineticGraphics graphics, ItemCreatorPresets.Entry item, int index, int x, int y, int width,
                             int height, boolean hovered, boolean selected) {
        graphics.item(icon.apply(item), x + 4, y + 4);
        graphics.scrollingText(name.apply(item), x + 24, y + 8, width - 28, KineticTheme.current().text(), false);
    }

    @Override
    protected boolean onRowClick(ItemCreatorPresets.Entry item, int index, MouseInput input) {
        if (!input.isLeft() && !input.isRight()) return false;
        onClick.accept(item, input);
        return true;
    }

    @Override
    protected Component rowTooltip(ItemCreatorPresets.Entry item, int index) {
        return details.apply(item);
    }

    @Override
    protected Component emptyText() {
        return ItemCreatorPage.tr("no_records");
    }
}
