package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;

/** One framed panel on the standard canvas, with Back and the title in its first row. */
public abstract class ItemCreatorPage extends KineticPage {
    protected ItemCreatorPage(String title) {
        super(tr(title)); setPausesGame(false);
    }
    public static Component tr(String key, Object... args) { return KineticI18n.translatable("gui.kineticcore.item_creator." + key, args); }
    protected void header(KineticUi ui, String back) { ui.button(4, 4, 40).text(tr(back)).onClick(this::navigateBack).build(); }
    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        KineticTheme.panel(g, 0, 0, width(), height());
        // Centred; the pages keep their actions at the top right.
        g.scrollingTextCentered(title(), 320, 8, 2 * (320 - 48) - 2 * 128, 0xFFFFAA00, false);
    }
}
