package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.page.KineticContainerPage;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.page.PageLayout;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;

/** Creates and opens the internal host screens for pages. */
public final class PageScreens {
    private PageScreens() {
    }

    /** Creates the host screen for a (non-container) page. */
    public static Screen create(KineticPage page) {
        Objects.requireNonNull(page, "page");
        if (page instanceof KineticContainerPage<?>) {
            throw new IllegalArgumentException("Container pages are opened by the server menu; register them with KineticClientMenus");
        }
        Object existing = PageBridge.access().host(page);
        if (existing instanceof Screen screen) return screen;
        return PageBridge.access().layout(page) == PageLayout.NATIVE ? new PageNativeScreen(page) : new PageCanvasScreen(page);
    }

    /** Creates the host screen for a page with an explicit back-navigation parent (may be null). */
    public static Screen createWithParent(KineticPage page, Screen parent) {
        Screen screen = create(page);
        GuiSessionRuntime.setExplicitParent(screen, parent);
        return screen;
    }

    /** Opens a page as a new navigation root. */
    public static void open(KineticPage page) {
        KineticClientRuntimeImpl.openScreen(createWithParent(page, null));
    }

    /** Opens a page whose back navigation returns to the current screen. */
    public static void openChild(KineticPage page) {
        KineticClientRuntimeImpl.openScreen(createWithParent(page, KineticClientRuntimeImpl.currentScreen()));
    }

    /** Returns the page hosted by a screen, or null. */
    public static KineticPage pageOf(Screen screen) {
        return screen instanceof PageHost host ? host.page() : null;
    }
}
