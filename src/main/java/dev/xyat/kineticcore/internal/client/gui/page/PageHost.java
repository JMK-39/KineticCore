package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.layout.KineticLayout;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import dev.xyat.kineticcore.internal.client.gui.widget.render.KineticEntityPreview.EntityPreviewRenderer;

/** Internal view of the vanilla screen hosting one {@link KineticPage}. */
public interface PageHost {
    /** The hosted page. */
    KineticPage page();

    /** Shared control/overlay/focus/draft services of the host screen. */
    KineticScreenHost screenHost();

    /** Builder registering controls with this host. */
    KineticUi ui();

    /** Width of the page coordinate space. */
    int pageWidth();

    /** Height of the page coordinate space. */
    int pageHeight();

    /** Responsive layout metrics of the page coordinate space. */
    KineticLayout.Metrics metrics();

    /** Rebuilds all controls through the page's build hook. */
    void rebuild();

    /** Runs the standard close path (close hook, then back navigation). */
    void close();

    /** Opens a child page whose parent is this host. */
    void openChild(KineticPage child);

    /** Registers a ctrl+wheel zoom target for an entity preview for the current frame. */
    void registerPreviewWheelTarget(EntityPreviewRenderer renderer, String key, int x, int y, int width, int height);

    /** Whether this host is the currently displayed screen. */
    boolean isOpen();
}
