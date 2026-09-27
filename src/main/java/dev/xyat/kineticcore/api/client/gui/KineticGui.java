package dev.xyat.kineticcore.api.client.gui;

import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.page.PageScreens;

/**
 * 打开与关闭 Kinetic 页面的入口。页面内部请优先使用 {@link KineticPage#openChild(KineticPage)} 与
 * {@link KineticPage#close()}。
 * Entry point for opening and closing Kinetic pages. Inside a page prefer {@link KineticPage#openChild(KineticPage)}
 * and {@link KineticPage#close()}.
 */
public final class KineticGui {
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
}
