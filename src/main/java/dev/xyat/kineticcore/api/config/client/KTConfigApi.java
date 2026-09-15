package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.config.ConfigScreens;
import dev.xyat.kineticcore.internal.client.config.ForgeConfigScreenIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public final class KTConfigApi {
    private static final String REQUIRES_WORLD_KEY = "gui.kineticcore.config.requires_world";
    private static final Map<String, KTConfigPage> PAGES = new LinkedHashMap<>();

    private KTConfigApi() {
    }

    public static synchronized void register(KTConfigPage page) {
        Objects.requireNonNull(page, "page");
        KTConfigPage old = PAGES.putIfAbsent(page.id(), page);
        if (old != null && old != page) {
            throw new IllegalStateException("A config page is already registered for " + page.id());
        }
    }

    public static synchronized void unregister(String pageId) {
        PAGES.remove(pageId);
    }

    public static synchronized Optional<KTConfigPage> find(String pageId) {
        return Optional.ofNullable(PAGES.get(pageId));
    }

    public static synchronized List<KTConfigPage> pages() {
        return List.copyOf(PAGES.values());
    }

    public static void notifySaved(String pageId) {
        if (pageId == null || pageId.isBlank()) return;
        find(pageId).ifPresent(KTConfigApi::notifySaved);
    }

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
        GuiOverlay.toast("kineticcore_config_saved:" + page.id(), message);
    }

    public static void notifyModuleSaved(Component moduleTitle) {
        if (moduleTitle == null) return;
        GuiOverlay.toast(
                "kineticcore_config_module_saved",
                KineticText.translatable("gui.kineticcore.config.module_saved", moduleTitle.copy())
        );
    }

    public static boolean canEdit(KTConfigPage page) {
        Objects.requireNonNull(page, "page");
        if (page.scope() != KTConfigScope.SERVER_AUTHORITATIVE) return true;
        if (!page.serverManaged()) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null || minecraft.level == null) return false;
        return KTServerConfigClient.canEdit(page.id());
    }

    public static Component unavailableReason(KTConfigPage page) {
        Objects.requireNonNull(page, "page");
        if (page.scope() != KTConfigScope.SERVER_AUTHORITATIVE) {
            return Component.empty();
        }
        if (!page.serverManaged()) {
            return KineticText.translatable("gui.kineticcore.config.server.unmanaged");
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null || minecraft.level == null) {
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

    public static Screen createScreen(Screen parent) {
        return ConfigScreens.createIndex(parent);
    }

    public static Screen createScreen(Screen parent, String pageId) {
        KTConfigPage page = find(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown config page: " + pageId));
        if (page.scope() == KTConfigScope.SERVER_AUTHORITATIVE && !page.serverManaged()) {
            showUnavailable(page);
            return ConfigScreens.createIndex(parent);
        }
        return ConfigScreens.createPage(parent, page);
    }

    public static Screen createScreen(Screen parent, KTConfigPage page) {
        return ConfigScreens.createPage(parent, Objects.requireNonNull(page, "page"));
    }

    public static void refreshScreenFromSource(Screen screen) {
        ConfigScreens.refreshFromSource(screen);
    }

    public static Screen createScreenForOwner(Screen parent, String ownerModId) {
        String ownerId = Objects.requireNonNull(ownerModId, "ownerModId");
        String ownerPrefix = ownerId + ":";
        List<KTConfigPage> ownedPages = pages().stream()
                .filter(page -> page.id().startsWith(ownerPrefix))
                .toList();
        return ConfigScreens.createOwner(parent, ownerId, ownedPages);
    }

    public static void installConfigHub(String ownerModId) {
        ForgeConfigScreenIntegration.installHub(ownerModId);
    }

    public static void installConfigScreen(String ownerModId) {
        ForgeConfigScreenIntegration.installOwnerScreen(ownerModId);
    }

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
        GuiOverlay.toast(
                "kineticcore_config_unavailable",
                unavailableReason(page)
        );
    }


}
