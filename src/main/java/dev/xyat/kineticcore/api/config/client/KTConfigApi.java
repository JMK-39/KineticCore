package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.internal.client.gui.text.KineticText;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.config.ConfigScreens;
import dev.xyat.kineticcore.internal.client.config.ForgeConfigScreenIntegration;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.internal.client.gui.page.PageScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

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

    /** 以当前界面为父打开配置索引 / Opens the config index as a child of the current screen. */
    public static void openIndex() {
        KineticClientRuntime.openScreen(ConfigScreens.createIndex(KineticClientRuntime.currentScreen()));
    }

    /** 打开已注册的配置页 / Opens a registered config page. */
    public static void openPage(String pageId) {
        KineticClientRuntime.openScreen(createRegisteredPageScreen(KineticClientRuntime.currentScreen(), pageId));
    }

    /** 打开配置页定义（可为未注册的临时页）/ Opens a config page definition, including transient unregistered pages. */
    public static void openPage(KTConfigPage page) {
        KineticClientRuntime.openScreen(ConfigScreens.createPage(KineticClientRuntime.currentScreen(),
                Objects.requireNonNull(page, "page")));
    }

    /** 打开某模组的配置中心 / Opens the owner-scoped config hub of a mod. */
    public static void openOwner(String ownerModId) {
        KineticClientRuntime.openScreen(createOwnerScreen(KineticClientRuntime.currentScreen(), ownerModId));
    }

    /**
     * 从配置源重新读取当前导航链上所有已打开的配置界面（例如子编辑器保存后刷新父配置页）。
     * Reloads every open config screen in the current navigation chain from its source (e.g. after a child editor
     * saved).
     */
    public static void refreshOpenScreens() {
        ConfigScreens.refreshNavigationChain(KineticClientRuntime.currentScreen());
    }

    /** 以本模组的配置中心作为 Forge 模组列表的配置按钮 / Installs the owner config hub as the Forge mod-list config button. */
    public static void installConfigHub(String ownerModId) {
        ForgeConfigScreenIntegration.installHub(ownerModId);
    }

    /** 以指定页面作为 Forge 模组列表的配置按钮 / Installs a page as the Forge mod-list config button. */
    public static void installConfigPage(String ownerModId, Supplier<? extends KineticPage> pageFactory) {
        Objects.requireNonNull(pageFactory, "pageFactory");
        ForgeConfigScreenIntegration.installScreen(ownerModId,
                parent -> PageScreens.createWithParent(Objects.requireNonNull(pageFactory.get(), "pageFactory returned null"), parent));
    }

    /** 返回一个“以当前界面为父打开页面”的动作，供配置条目按钮使用 / Returns an action opening a child page, for config entry buttons. */
    public static Runnable pageAction(Supplier<? extends KineticPage> pageFactory) {
        Objects.requireNonNull(pageFactory, "pageFactory");
        return () -> KineticGui.openChild(Objects.requireNonNull(pageFactory.get(), "pageFactory returned null"));
    }

    /** 返回一个“打开配置页定义”的动作 / Returns an action opening a config page definition. */
    public static Runnable configPageAction(Supplier<KTConfigPage> pageFactory) {
        Objects.requireNonNull(pageFactory, "pageFactory");
        return () -> openPage(Objects.requireNonNull(pageFactory.get(), "pageFactory returned null"));
    }

    private static Screen createRegisteredPageScreen(Screen parent, String pageId) {
        KTConfigPage page = find(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown config page: " + pageId));
        if (page.scope() == KTConfigScope.SERVER_AUTHORITATIVE && !page.serverManaged()) {
            showUnavailable(page);
            return ConfigScreens.createIndex(parent);
        }
        return ConfigScreens.createPage(parent, page);
    }

    private static Screen createOwnerScreen(Screen parent, String ownerModId) {
        return ConfigScreens.createOwnerFor(parent, ownerModId);
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
