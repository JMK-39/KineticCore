package dev.xyat.kineticcore.api.client.gui;

import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.page.PageScreens;
import dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime;
import net.minecraft.client.gui.screens.Screen;

/**
 * 打开与关闭 Kinetic 页面的入口。页面内部请优先使用 {@link KineticPage#openChild(KineticPage)} 与
 * {@link KineticPage#close()}。
 * Entry point for opening and closing Kinetic pages. Inside a page prefer {@link KineticPage#openChild(KineticPage)}
 * and {@link KineticPage#close()}.
 */
public final class KineticGui {
    /**
     * 延迟打开页面时使用的不透明返回目标。附属只能捕获并交还给 {@link KineticGui}，不接触底层 Screen。
     * Opaque back-navigation target for pages opened after an asynchronous round-trip. Addons only capture and
     * return it to {@link KineticGui}; the underlying screen stays private to the core.
     */
    public static final class NavigationParent {
        private final Object handle;

        private NavigationParent(Object handle) {
            this.handle = handle;
        }
    }

    private KineticGui() {
    }

    /**
     * 以新导航根打开页面（返回时回到游戏）。
     * Opens a page as a new navigation root (going back returns to the game).
     */
    public static void open(KineticPage page) {
        PageScreens.open(page);
    }

    /**
     * 打开页面，返回时回到当前界面（包括原版界面）。
     * Opens a page whose back navigation returns to the current screen (vanilla screens included).
     */
    public static void openChild(KineticPage page) {
        PageScreens.openChild(page);
    }

    /**
     * 使用先前捕获的返回目标打开子页。适用于“请求服务端 → 稍后收到回包 → 再打开页面”的流程。
     * Opens a child page with a previously captured navigation parent. Intended for request/response flows where
     * the page is opened after an asynchronous server round-trip.
     */
    public static void openChild(KineticPage page, NavigationParent parent) {
        if (parent == null) {
            open(page);
            return;
        }
        PageScreens.openChild(page, parent.handle);
    }

    /**
     * 捕获当前界面作为稍后子页的返回目标；当前没有界面时返回 null。
     * Captures the current screen as a future child's back-navigation target, or null when no screen is open.
     */
    public static NavigationParent captureNavigationParent() {
        Object handle = PageScreens.captureNavigationParent();
        return handle == null ? null : new NavigationParent(handle);
    }

    /** 关闭当前界面回到游戏 / Closes the current screen and returns to the game. */
    public static void closeScreen() {
        KineticClientRuntimeImpl.openScreen(null);
    }

    /** 当前是否有任何界面打开 / Whether any screen is open. */
    public static boolean isScreenOpen() {
        return KineticClientRuntimeImpl.currentScreen() != null;
    }

    /** 当前显示的 Kinetic 页面，无则 null / The currently displayed Kinetic page, or null. */
    public static KineticPage currentPage() {
        return PageScreens.pageOf(KineticClientRuntimeImpl.currentScreen());
    }

    /** 当前页面若为指定类型则返回，否则 null / The current page if it is of the given type, else null. */
    public static <P extends KineticPage> P currentPage(Class<P> type) {
        KineticPage page = currentPage();
        return type.isInstance(page) ? type.cast(page) : null;
    }

    /**
     * 在当前页面及其返回链（父页面、祖父页面……）中查找第一个指定类型的页面，找不到返回 null。
     * 用于子页面打开期间，服务器数据仍能更新到下层的父页面。
     * Finds the first page of the given type in the current page or its back-navigation chain, or null; lets
     * server data reach a parent page while one of its child pages is shown.
     */
    public static <P extends KineticPage> P findPage(Class<P> type) {
        Screen screen = KineticClientRuntimeImpl.currentScreen();
        for (int depth = 0; screen != null && depth < 64; depth++) {
            KineticPage page = PageScreens.pageOf(screen);
            if (type.isInstance(page)) return type.cast(page);
            screen = GuiSessionRuntime.navigationParent(screen);
        }
        return null;
    }
}
