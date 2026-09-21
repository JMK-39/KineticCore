package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.config.ConfigScreens;
import dev.xyat.kineticcore.internal.client.config.ForgeConfigScreenIntegration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Registers client configuration pages and opens their Kinetic configuration screens.
 */
public final class KTConfigApi {
    private static final String REQUIRES_WORLD_KEY = "gui.kineticcore.config.requires_world";
    private static final Map<String, KTConfigPage> PAGES = new LinkedHashMap<>();

    private KTConfigApi() {
    }

    /** Registers one client config page. */
    public static synchronized void register(KTConfigPage page) {
        Objects.requireNonNull(page, "page");
        KTConfigPage old = PAGES.putIfAbsent(page.id(), page);
        if (old != null && old != page) {
            throw new IllegalStateException("A config page is already registered for " + page.id());
        }
    }

    /** Removes the client config page identified by the supplied page id. */
    public static synchronized void unregister(String pageId) {
        PAGES.remove(pageId);
    }

    /** Finds one registered config page by its stable namespaced page id. */
    public static synchronized Optional<KTConfigPage> find(String pageId) {
        return Optional.ofNullable(PAGES.get(pageId));
    }

    /** Returns the registered configuration pages. */
    public static synchronized List<KTConfigPage> pages() {
        return List.copyOf(PAGES.values());
    }

    /** Shows the standard saved notification for the registered page id when present. */
    public static void notifySaved(String pageId) {
        if (pageId == null) return;
        find(pageId).ifPresent(KTConfigApi::notifySaved);
    }

    /** Shows the standard saved notification for the supplied page. */
    public static void notifySaved(KTConfigPage page) {
        Objects.requireNonNull(page, "page");
        Component message = page.applyNotice() == null
                ? KineticText.translatable(
                        page.applyTiming().savedTranslationKey(),
                        page.title().copy()
                )
                : KineticText.translatable(
                        "gui.kineticcore.config.saved.notice",
                        page.title().copy(),
                        page.applyNotice()
                );
        KineticOverlays.toast("kineticcore_config_saved:" + page.id(), message, KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    /** Shows the standard module-level saved notification for a multi-page/module editor. */
    public static void notifyModuleSaved(Component moduleTitle) {
        if (moduleTitle == null) return;
        KineticOverlays.toast(
                          "kineticcore_config_module_saved",
                          KineticText.translatable("gui.kineticcore.config.module_saved", moduleTitle.copy()),
                          KineticOverlays.Position.BOTTOM_CENTER,
                          5000,
                          0,
                          -30
                  );
    }

    /** Returns whether the supplied page is currently editable in this client context. */
    public static boolean canEdit(KTConfigPage page) {
        Objects.requireNonNull(page, "page");
        if (page.scope() != KTConfigScope.SERVER_AUTHORITATIVE) return true;
        if (!page.serverManaged()) return false;

        if (!KineticClientRuntime.connected()
                || KineticClientRuntime.localPlayer() == null
                || KineticClientRuntime.currentLevel() == null) return false;
        return KTServerConfigClient.canEdit(page.id());
    }

    /** Returns an empty component when editing is available, otherwise the player-facing reason it is unavailable. */
    public static Component unavailableReason(KTConfigPage page) {
        Objects.requireNonNull(page, "page");
        if (page.scope() != KTConfigScope.SERVER_AUTHORITATIVE) {
            return Component.empty();
        }
        if (!page.serverManaged()) {
            return KineticText.translatable("gui.kineticcore.config.server.unmanaged");
        }

        if (!KineticClientRuntime.connected()
                || KineticClientRuntime.localPlayer() == null
                || KineticClientRuntime.currentLevel() == null) {
            return KineticText.translatable(REQUIRES_WORLD_KEY);
        }
        if (!KTServerConfigClient.isLoaded(page.id())) {
            String failureKey = KTServerConfigClient.loadFailureKey(page.id());
            if (!failureKey.isBlank()) {
                return KineticText.translatable(failureKey);
            }
            return KineticText.translatable("gui.kineticcore.config.server.loading");
        }
        if (!KTServerConfigClient.canEdit(page.id())) {
            return KineticText.translatable("gui.kineticcore.config.server.op_required");
        }
        return Component.empty();
    }

    /** Creates the default registered Kinetic configuration screen for the supplied parent. */
    public static Screen createScreen(Screen parent) {
        return createIndexScreen(parent);
    }

    /** Creates a registered Kinetic configuration screen opened to the requested page id. */
    public static Screen createScreen(Screen parent, String pageId) {
        return createRegisteredPageScreen(parent, pageId);
    }

    /** Creates a Kinetic configuration screen for the supplied page definition. */
    public static Screen createScreen(Screen parent, KTConfigPage page) {
        return createPageScreen(parent, page);
    }

    /** Creates the registered-page index screen. */
    public static Screen createIndexScreen(Screen parent) {
        return ConfigScreens.createIndex(parent);
    }

    /** Creates a screen for one registered page id. */
    public static Screen createRegisteredPageScreen(Screen parent, String pageId) {
        KTConfigPage page = find(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown config page: " + pageId));
        if (page.scope() == KTConfigScope.SERVER_AUTHORITATIVE && !page.serverManaged()) {
            showUnavailable(page);
            return ConfigScreens.createIndex(parent);
        }
        return ConfigScreens.createPage(parent, page);
    }

    /** Creates a screen for a supplied page, including transient pages that are not registered. */
    public static Screen createPageScreen(Screen parent, KTConfigPage page) {
        return ConfigScreens.createPage(parent, Objects.requireNonNull(page, "page"));
    }

    /** Reloads pending editor values for a Kinetic config screen from its backing config source. */
    public static void refreshScreenFromSource(Screen screen) {
        ConfigScreens.refreshFromSource(screen);
    }

    /** Creates an owner-scoped config hub containing pages whose ids use the supplied mod-id namespace. */
    public static Screen createScreenForOwner(Screen parent, String ownerModId) {
        String ownerId = Objects.requireNonNull(ownerModId, "ownerModId");
        String ownerPrefix = ownerId + ":";
        List<KTConfigPage> ownedPages = pages().stream()
                .filter(page -> page.id().startsWith(ownerPrefix))
                .toList();
        return ConfigScreens.createOwner(parent, ownerId, ownedPages);
    }

    /** Installs Kinetic's owner-scoped config hub as the Forge config screen for the supplied mod id. */
    public static void installConfigHub(String ownerModId) {
        ForgeConfigScreenIntegration.installHub(ownerModId);
    }

    /** Installs one explicit Forge config-screen factory for an owner mod. */
    public static void installConfigScreen(String ownerModId, Function<Screen, ? extends Screen> screenFactory) {
        ForgeConfigScreenIntegration.installScreen(ownerModId, Objects.requireNonNull(screenFactory, "screenFactory"));
    }

    /** Builds an action that opens a specialized editor with the current page as its parent. */
    public static Runnable screenAction(Function<Screen, ? extends Screen> screenFactory) {
        Objects.requireNonNull(screenFactory, "screenFactory");
        return () -> KineticClientRuntime.openScreen(Objects.requireNonNull(
                screenFactory.apply(KineticClientRuntime.currentScreen()),
                "screenFactory returned null"
        ));
    }

    private static void showUnavailable(KTConfigPage page) {
        KineticOverlays.toast(
                          "kineticcore_config_unavailable",
                          unavailableReason(page),
                          KineticOverlays.Position.BOTTOM_CENTER,
                          5000,
                          0,
                          -30
                  );
    }


}
