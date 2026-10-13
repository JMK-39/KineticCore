package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;

public abstract class ItemCreatorPage extends KineticPage {
    protected ItemCreatorPage(String title) {
        super(tr(title)); useCanvas(500, 320, 8); setPausesGame(false);
    }
    public static Component tr(String key, Object... args) { return KineticI18n.translatable("gui.kineticcore.item_creator." + key, args); }
    protected void header(KineticUi ui, String back) { ui.button(12, 12, 62).text(tr(back)).onClick(this::navigateBack).build(); }
    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        KineticTheme.surface(g, 10, 8, 480, 32, KineticTheme.Surface.PANEL);
        KineticTheme.surface(g, 10, 42, 480, 266, KineticTheme.Surface.PANEL);
        g.outline(10, 42, 480, 266, KineticTheme.current().border());
        g.scrollingText(title(), 84, 18, 302, KineticTheme.current().text(), false);
    }
}
