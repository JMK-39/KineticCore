package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class ConfigScreens {
    private ConfigScreens() {
    }

    public static Screen createIndex(Screen parent) {
        return new KTConfigIndexScreen(parent);
    }

    public static Screen createPage(Screen parent, KTConfigPage page) {
        return new KTConfigScreen(parent, page);
    }

    public static Screen createOwner(Screen parent, String ownerId, List<KTConfigPage> pages) {
        if (pages.isEmpty()) return createIndex(parent);
        return new KTModuleConfigScreen(parent, KTConfigIndexScreen.moduleTitle(ownerId), pages);
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
    public static Screen createOwnerFor(Screen parent, String ownerModId) {
        String prefix = java.util.Objects.requireNonNull(ownerModId, "ownerModId") + ":";
        java.util.List<dev.xyat.kineticcore.api.config.client.KTConfigPage> owned =
                dev.xyat.kineticcore.api.config.client.KTConfigApi.pages().stream()
                        .filter(page -> page.id().startsWith(prefix))
                        .toList();
        return createOwner(parent, ownerModId, owned);
    }
}
