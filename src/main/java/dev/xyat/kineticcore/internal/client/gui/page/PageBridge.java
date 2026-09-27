package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.input.CharInput;
import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.page.KineticContainerPage;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.page.PageLayout;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;

import java.util.Objects;

/**
 * Accessor bridge letting internal page hosts attach to pages and invoke their protected hooks. The public page
 * classes install the accessor from their static initializer, so no hook becomes public API.
 */
public final class PageBridge {
    /** Hook and attachment access implemented privately inside {@link KineticPage}. */
    public interface Accessor {
        void attach(KineticPage page, Object host);

        void detach(KineticPage page);

        Object host(KineticPage page);

        void build(KineticPage page, KineticUi ui);

        void renderBackground(KineticPage page, KineticGraphics graphics, int mouseX, int mouseY, float partialTick);

        void renderForeground(KineticPage page, KineticGraphics graphics, int mouseX, int mouseY, float partialTick);

        void renderTooltips(KineticPage page, int mouseX, int mouseY);

        boolean onMouseClickCapture(KineticPage page, MouseInput input);

        boolean onMouseClick(KineticPage page, MouseInput input);

        boolean onMouseRelease(KineticPage page, MouseInput input);

        boolean onMouseDrag(KineticPage page, MouseDragInput input);

        boolean onMouseScroll(KineticPage page, ScrollInput input);

        void onMouseMove(KineticPage page, double x, double y);

        boolean onKeyPress(KineticPage page, KeyInput input);

        boolean onKeyRelease(KineticPage page, KeyInput input);

        boolean onCharTyped(KineticPage page, CharInput input);

        void onTick(KineticPage page);

        void onRemoved(KineticPage page);

        boolean onCloseRequested(KineticPage page);

        PageLayout layout(KineticPage page);

        int canvasDesignWidth(KineticPage page);

        int canvasDesignHeight(KineticPage page);

        int canvasSafeMargin(KineticPage page);

        boolean pausesGame(KineticPage page);

        /** imageWidth, imageHeight, titleX, titleY, inventoryX, inventoryY. */
        int[] containerLayout(KineticContainerPage<?> page);

        void renderContainerBackground(KineticContainerPage<?> page, KineticGraphics graphics, int mouseX, int mouseY, float partialTick);

        void renderScreenOverlay(KineticContainerPage<?> page, KineticGraphics graphics, int mouseX, int mouseY, float partialTick);
    }

    private static volatile Accessor accessor;

    private PageBridge() {
    }

    /** Installs the accessor; called once from the page class initializer. */
    public static void install(Accessor installed) {
        accessor = Objects.requireNonNull(installed, "accessor");
    }

    /** Returns the installed accessor, forcing page class initialization when needed. */
    public static Accessor access() {
        Accessor current = accessor;
        if (current != null) return current;
        try {
            Class.forName(KineticPage.class.getName(), true, KineticPage.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(exception);
        }
        return Objects.requireNonNull(accessor, "KineticPage accessor was not installed");
    }
}
