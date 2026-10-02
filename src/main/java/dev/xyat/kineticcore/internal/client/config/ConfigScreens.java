package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class ConfigScreens {
    private ConfigScreens() {
    }

    public static Screen createIndex() {
        return new KTConfigIndexScreen();
    }

    public static Screen createPage(KTConfigPage page) {
        return new KTConfigScreen(page);
    }

    public static Screen createOwner(String ownerId, List<KTConfigPage> pages) {
        if (pages.isEmpty()) return createIndex();
        return new KTModuleConfigScreen(KTConfigIndexScreen.moduleTitle(ownerId), pages);
    }

    public static void refreshFromSource(Screen screen) {
        if (screen instanceof KTConfigScreen configScreen) {
            configScreen.refreshFromSource();
        } else if (screen instanceof KTModuleConfigScreen moduleScreen) {
            moduleScreen.refreshFromSource();
        }
    }

    public static void serverSnapshotUpdated(Screen screen, String pageId) {
        if (screen instanceof KTConfigScreen configScreen) {
            configScreen.serverSnapshotUpdated(pageId);
        } else if (screen instanceof KTModuleConfigScreen moduleScreen) {
            moduleScreen.serverSnapshotUpdated(pageId);
        }
    }

    /** Refreshes the given screen and every navigation ancestor that is a Kinetic config screen. */
    public static void refreshNavigationChain(Screen screen) {
        java.util.Set<Screen> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Screen current = screen;
        while (current != null && seen.add(current)) {
            refreshFromSource(current);
            current = dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime.navigationParent(current);
        }
    }

    /** Creates the owner-scoped hub containing registered pages whose ids use the owner namespace. */
    public static Screen createOwnerFor(String ownerModId) {
        String prefix = java.util.Objects.requireNonNull(ownerModId, "ownerModId") + ":";
        java.util.List<dev.xyat.kineticcore.api.config.client.KTConfigPage> owned =
                dev.xyat.kineticcore.api.config.client.KTConfigApi.pages().stream()
                        .filter(page -> page.id().startsWith(prefix))
                        .toList();
        return createOwner(ownerModId, owned);
    }
}
