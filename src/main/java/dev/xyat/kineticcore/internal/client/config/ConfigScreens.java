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
}
