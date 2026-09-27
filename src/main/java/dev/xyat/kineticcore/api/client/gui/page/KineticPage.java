package dev.xyat.kineticcore.api.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.input.CharInput;
import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.layout.KineticLayout;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import dev.xyat.kineticcore.api.client.gui.widget.KineticEntityPreview;
import dev.xyat.kineticcore.internal.client.gui.page.PageBridge;
import dev.xyat.kineticcore.internal.client.gui.page.PageHost;
import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 所有 Kinetic 界面的基类。页面不是原版 Screen：KineticCore 用内部 Screen 托管它，负责坐标缩放、控件注册、
 * 提示、菜单、弹窗、焦点、草稿与返回导航。子类在 {@link #build(KineticUi)} 中创建控件，在渲染钩子中用
 * {@link KineticGraphics} 绘制，在输入钩子中处理 {@link MouseInput} 等输入记录。
 * <p>
 * Base class of every Kinetic interface. A page is not a vanilla Screen: KineticCore hosts it in an internal
 * screen that owns coordinate scaling, control registration, tooltips, menus, dialogs, focus, drafts and back
 * navigation. Subclasses create controls in {@link #build(KineticUi)}, draw with {@link KineticGraphics} in the
 * render hooks and react to input records such as {@link MouseInput} in the input hooks.
 */
public abstract class KineticPage {
    /** 标准逻辑画布宽度 / Standard logical canvas width. */
    public static final int CANVAS_WIDTH = KineticLayout.MAX_CANVAS_WIDTH;
    /** 标准逻辑画布高度 / Standard logical canvas height. */
    public static final int CANVAS_HEIGHT = KineticLayout.MAX_CANVAS_HEIGHT;
    /** 标准安全边距 / Standard safe-area margin. */
    public static final int SAFE_MARGIN = 6;
    /** 标准控件高度 / Standard control height. */
    public static final int CONTROL_HEIGHT = 16;
    /** 物品卡片按钮高度 / Item card button height. */
    public static final int ITEM_BUTTON_HEIGHT = 38;
    /** 内容卡片按钮高度 / Content card button height. */
    public static final int CARD_BUTTON_HEIGHT = 26;

    static {
        PageBridge.install(new Access());
    }

    private final Component title;
    private final PageLayout layout;
    private int canvasDesignWidth = CANVAS_WIDTH;
    private int canvasDesignHeight = CANVAS_HEIGHT;
    private int canvasSafeMargin = SAFE_MARGIN;
    private boolean pausesGame = true;
    private PageHost host;
    private final List<Runnable> pendingHostActions = new java.util.ArrayList<>();

    /** 画布布局页面 / Creates a canvas-layout page. */
    protected KineticPage(Component title) {
        this(title, PageLayout.CANVAS);
    }

    /** 指定布局的页面 / Creates a page with the given layout. */
    protected KineticPage(Component title, PageLayout layout) {
        this.title = title == null ? Component.empty() : title;
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    /** 标题 / Title. */
    public final Component title() {
        return title;
    }

    /** 坐标系 / Coordinate system. */
    public final PageLayout layout() {
        return layout;
    }

    /**
     * 更改画布逻辑尺寸（最大 640×360）与安全边距，在构造函数中调用。
     * Changes the canvas design size (max 640×360) and safe margin; call from the constructor.
     */
    protected final void useCanvas(int designWidth, int designHeight, int safeMargin) {
        canvasDesignWidth = Math.max(1, Math.min(CANVAS_WIDTH, designWidth));
        canvasDesignHeight = Math.max(1, Math.min(CANVAS_HEIGHT, designHeight));
        canvasSafeMargin = Math.max(0, safeMargin);
    }

    /** 打开时是否暂停单人游戏（默认 true）/ Whether the page pauses single-player (default true). */
    protected final void setPausesGame(boolean pausesGame) {
        this.pausesGame = pausesGame;
    }

    // ---------------------------------------------------------------- lifecycle hooks

    /**
     * 创建本页全部控件；打开、窗口尺寸变化及 {@link #rebuild()} 时都会先清空再调用。
     * Creates every control of this page; called after clearing on open, on resize and on {@link #rebuild()}.
     */
    protected abstract void build(KineticUi ui);

    /** 每个客户端 tick 调用，控件已先 tick / Called every client tick after controls ticked. */
    protected void onTick() {
    }

    /** 页面被移除时调用 / Called when the page is removed. */
    protected void onRemoved() {
    }

    /**
     * 关闭请求（Esc 或 {@link #close()}）。返回 true 表示已自行处理（例如弹出确认框），否则执行返回导航。
     * Close request (Escape or {@link #close()}). Return true when handled (e.g. a confirmation dialog was opened);
     * otherwise standard back navigation runs.
     */
    protected boolean onCloseRequested() {
        return false;
    }

    // ---------------------------------------------------------------- render hooks

    /** 在控件之下绘制 / Draws below the controls. */
    protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** 在控件之上绘制 / Draws above the controls. */
    protected void renderForeground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /**
     * 控件提示已检查且无弹层遮挡时调用，在此调用 showTooltip 系列方法显示业务提示。
     * Called after control tooltips were checked and no overlay blocks the page; call the showTooltip methods here.
     */
    protected void renderTooltips(int mouseX, int mouseY) {
    }

    // ---------------------------------------------------------------- input hooks

    /** 控件之前处理按下（捕获阶段）/ Handles a press before the controls (capture phase). */
    protected boolean onMouseClickCapture(MouseInput input) {
        return false;
    }

    /** 控件未处理的按下 / Handles a press no control consumed. */
    protected boolean onMouseClick(MouseInput input) {
        return false;
    }

    /** 控件未处理的松开 / Handles a release no control consumed. */
    protected boolean onMouseRelease(MouseInput input) {
        return false;
    }

    /** 控件未处理的拖动 / Handles a drag no control consumed. */
    protected boolean onMouseDrag(MouseDragInput input) {
        return false;
    }

    /** 控件未处理的滚轮 / Handles wheel input no control consumed. */
    protected boolean onMouseScroll(ScrollInput input) {
        return false;
    }

    /** 指针移动 / Pointer moved. */
    protected void onMouseMove(double x, double y) {
    }

    /** 按键：在焦点控件与 Esc 之前调用 / Key press, called before the focused control and Escape handling. */
    protected boolean onKeyPress(KeyInput input) {
        return false;
    }

    /** 按键松开 / Key release. */
    protected boolean onKeyRelease(KeyInput input) {
        return false;
    }

    /** 字符输入：在焦点控件之前调用 / Character input, called before the focused control. */
    protected boolean onCharTyped(CharInput input) {
        return false;
    }

    // ---------------------------------------------------------------- services

    private PageHost requireHost() {
        if (host == null) throw new IllegalStateException("Page is not open: " + getClass().getName());
        return host;
    }

    /** 页面是否已被托管 / Whether the page is currently hosted. */
    public final boolean isAttached() {
        return host != null;
    }

    /** 页面当前是否正在显示 / Whether the page is currently displayed. */
    public final boolean isOpen() {
        return host != null && host.isOpen();
    }

    /** 控件构建器 / The control builder. */
    protected final KineticUi ui() {
        return requireHost().ui();
    }

    /** 清空并重新构建控件 / Clears and rebuilds all controls. */
    public final void rebuild() {
        requireHost().rebuild();
    }

    /** 页面坐标系宽度 / Width of the page coordinate space. */
    public final int width() {
        return requireHost().pageWidth();
    }

    /** 页面坐标系高度 / Height of the page coordinate space. */
    public final int height() {
        return requireHost().pageHeight();
    }

    /** 响应式布局度量 / Responsive layout metrics. */
    public final KineticLayout.Metrics layoutMetrics() {
        return requireHost().metrics();
    }

    /** 是否紧凑布局 / Whether compact spacing rules apply. */
    public final boolean isCompactLayout() {
        return layoutMetrics().isCompact();
    }

    /** 是否竖屏布局 / Whether portrait layout rules apply. */
    public final boolean isPortraitLayout() {
        return layoutMetrics().isPortrait();
    }

    /** 是否超宽布局 / Whether ultrawide layout rules apply. */
    public final boolean isUltrawideLayout() {
        return layoutMetrics().isUltrawide();
    }

    /** 本帧显示一行提示 / Shows a one-line tooltip this frame. */
    protected final void showTooltip(Component line) {
        requireHost().screenHost().showTooltipLine(line);
    }

    /** 本帧显示多行提示（不自动换行）/ Shows tooltip lines this frame without wrapping. */
    protected final void showTooltip(List<? extends Component> lines) {
        requireHost().screenHost().showTooltip(lines, null);
    }

    /** 本帧显示多行提示并按宽度换行 / Shows tooltip lines wrapped to a width this frame. */
    protected final void showTooltip(List<? extends Component> lines, int maxWidth) {
        requireHost().screenHost().showTooltip(lines, maxWidth);
    }

    /** 本帧显示已排版提示 / Shows formatted tooltip lines this frame. */
    protected final void showFormattedTooltip(List<FormattedCharSequence> lines) {
        requireHost().screenHost().showFormattedTooltip(lines);
    }

    /** 本帧显示物品提示 / Shows an item tooltip this frame. */
    protected final void showItemTooltip(ItemStack stack) {
        requireHost().screenHost().showItemTooltip(stack);
    }

    /** 在页面坐标处打开右键菜单 / Opens a context menu at page coordinates. */
    protected final void openContextMenu(double x, double y, List<KineticOverlays.MenuItem> items) {
        requireHost().screenHost().openContextMenu(x, y, items);
    }

    /** 以固定逻辑宽度打开右键菜单 / Opens a context menu with a fixed logical width. */
    protected final void openContextMenu(double x, double y, List<KineticOverlays.MenuItem> items, int width) {
        requireHost().screenHost().openContextMenu(x, y, items, width);
    }

    /** 关闭右键菜单 / Closes the context menu. */
    protected final void closeContextMenu() {
        requireHost().screenHost().closeContextMenu();
    }

    /** 打开模态确认框 / Opens a modal confirmation dialog. */
    protected final void openDialog(Component title, Component message, Component confirmText, Component cancelText,
                                    Runnable onConfirm, Runnable onCancel) {
        requireHost().screenHost().openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
    }

    /** 菜单或弹窗是否正在遮挡页面输入 / Whether a menu or dialog blocks page input. */
    protected final boolean overlayBlocksInput() {
        return requireHost().screenHost().overlayBlocksInput();
    }

    /** 让控件获得焦点，null 清除焦点 / Focuses a control; null clears focus. */
    protected final void focus(KineticControl control) {
        if (control == null) {
            clearFocus();
        } else {
            requireHost().screenHost().focusControl(internal(control));
        }
    }

    /** 若控件持有焦点则清除 / Clears focus if the control owns it. */
    protected final void blur(KineticControl control) {
        if (control != null) requireHost().screenHost().blurControl(internal(control));
    }

    /** 清除焦点 / Clears focus. */
    protected final void clearFocus() {
        requireHost().screenHost().clearControlFocus();
    }

    /** 当前焦点控件，无则 null / The focused control, or null. */
    protected final KineticControl focusedControl() {
        return dev.xyat.kineticcore.internal.client.gui.page.CustomControlSupport.publicControl(
                requireHost().screenHost().focusedControl());
    }

    /** 指定控件当前是否持有焦点 / Whether the given control currently holds focus. */
    protected final boolean isFocused(KineticControl control) {
        return control != null && focusedControl() == control;
    }

    private void whenHosted(Runnable action) {
        if (host != null) {
            action.run();
        } else {
            pendingHostActions.add(action);
        }
    }

    private static InternalControl internal(KineticControl control) {
        if (control instanceof InternalControl internal) return internal;
        return dev.xyat.kineticcore.internal.client.gui.page.CustomControlSupport.widget(control);
    }

    /**
     * 在打开前预留独立草稿边界。草稿相关方法可在构造函数中调用，会在页面被托管时立即生效。
     * Reserves a standalone draft boundary before opening. Draft configuration may be called from the constructor;
     * it takes effect as soon as the page is hosted.
     */
    protected final void reserveStandaloneDraft() {
        whenHosted(() -> requireHost().screenHost().reserveStandaloneDraft());
    }

    /**
     * 配置草稿快照与恢复：离开共享草稿会话时回滚；capture 应返回可 equals 比较的独立快照。
     * Configures draft capture/restore: rolled back when leaving the shared draft session; capture must return an
     * independent snapshot comparable with equals.
     */
    protected final <T> void configureDraft(Supplier<T> capture, Consumer<T> restore) {
        whenHosted(() -> requireHost().screenHost().configureDraft(capture, restore));
    }

    /** 建立独立草稿边界 / Creates a standalone draft boundary. */
    protected final <T> void configureStandaloneDraft(Supplier<T> capture, Consumer<T> restore) {
        whenHosted(() -> requireHost().screenHost().configureStandaloneDraft(capture, restore));
    }

    /** 业务持久化完成后提交草稿基线 / Commits the draft baseline after persisting. */
    protected final void commitDraft() {
        requireHost().screenHost().commitDraft();
    }

    /** 恢复到草稿基线 / Restores the draft baseline. */
    protected final void discardDraft() {
        requireHost().screenHost().discardDraft();
    }

    /** 是否有未提交修改 / Whether there are uncommitted edits. */
    protected final boolean hasUnsavedEdits() {
        return requireHost().screenHost().hasUnsavedEdits();
    }

    /** 本帧登记实体预览的 Ctrl+滚轮缩放区域 / Registers an entity preview's ctrl+wheel zoom area for this frame. */
    protected final void registerPreviewZoomArea(KineticEntityPreview preview, String key, int x, int y, int width, int height) {
        if (preview != null) requireHost().registerPreviewWheelTarget(dev.xyat.kineticcore.internal.client.gui.page.PreviewAccess.renderer(preview), key, x, y, width, height);
    }

    /** 关闭页面（先经过 {@link #onCloseRequested()}）/ Closes the page through {@link #onCloseRequested()}. */
    public final void close() {
        requireHost().close();
    }

    /** 直接返回父界面（不经过关闭钩子）/ Returns to the parent screen without the close hook. */
    public final void navigateBack() {
        requireHost().navigateBack();
    }

    /** 以本页为父页面打开子页面 / Opens a child page whose parent is this page. */
    public final void openChild(KineticPage child) {
        requireHost().openChild(Objects.requireNonNull(child, "child"));
    }

    private static final class Access implements PageBridge.Accessor {
        @Override
        public void attach(KineticPage page, Object hostObject) {
            PageHost host = (PageHost) hostObject;
            if (page.host != null && page.host != host) {
                throw new IllegalStateException("A page instance can only be hosted once: " + page.getClass().getName());
            }
            page.host = host;
            List<Runnable> pending = List.copyOf(page.pendingHostActions);
            page.pendingHostActions.clear();
            pending.forEach(Runnable::run);
        }

        @Override
        public void detach(KineticPage page) {
            page.host = null;
        }

        @Override
        public Object host(KineticPage page) {
            return page.host;
        }

        @Override
        public PageLayout layout(KineticPage page) {
            return page.layout;
        }

        @Override
        public int canvasDesignWidth(KineticPage page) {
            return page.canvasDesignWidth;
        }

        @Override
        public int canvasDesignHeight(KineticPage page) {
            return page.canvasDesignHeight;
        }

        @Override
        public int canvasSafeMargin(KineticPage page) {
            return page.canvasSafeMargin;
        }

        @Override
        public boolean pausesGame(KineticPage page) {
            return page.pausesGame;
        }

        @Override
        public void build(KineticPage page, KineticUi ui) {
            page.build(ui);
        }

        @Override
        public void renderBackground(KineticPage page, KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
            page.renderBackground(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void renderForeground(KineticPage page, KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
            page.renderForeground(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void renderTooltips(KineticPage page, int mouseX, int mouseY) {
            page.renderTooltips(mouseX, mouseY);
        }

        @Override
        public boolean onMouseClickCapture(KineticPage page, MouseInput input) {
            return page.onMouseClickCapture(input);
        }

        @Override
        public boolean onMouseClick(KineticPage page, MouseInput input) {
            return page.onMouseClick(input);
        }

        @Override
        public boolean onMouseRelease(KineticPage page, MouseInput input) {
            return page.onMouseRelease(input);
        }

        @Override
        public boolean onMouseDrag(KineticPage page, MouseDragInput input) {
            return page.onMouseDrag(input);
        }

        @Override
        public boolean onMouseScroll(KineticPage page, ScrollInput input) {
            return page.onMouseScroll(input);
        }

        @Override
        public void onMouseMove(KineticPage page, double x, double y) {
            page.onMouseMove(x, y);
        }

        @Override
        public boolean onKeyPress(KineticPage page, KeyInput input) {
            return page.onKeyPress(input);
        }

        @Override
        public boolean onKeyRelease(KineticPage page, KeyInput input) {
            return page.onKeyRelease(input);
        }

        @Override
        public boolean onCharTyped(KineticPage page, CharInput input) {
            return page.onCharTyped(input);
        }

        @Override
        public void onTick(KineticPage page) {
            page.onTick();
        }

        @Override
        public void onRemoved(KineticPage page) {
            page.onRemoved();
        }

        @Override
        public boolean onCloseRequested(KineticPage page) {
            return page.onCloseRequested();
        }

        @Override
        public int[] containerLayout(KineticContainerPage<?> page) {
            int inventoryY = page.layoutInventoryY == Integer.MIN_VALUE ? page.layoutImageHeight - 94 : page.layoutInventoryY;
            return new int[]{page.layoutImageWidth, page.layoutImageHeight, page.layoutTitleX, page.layoutTitleY,
                    page.layoutInventoryX, inventoryY};
        }

        @Override
        public void renderContainerBackground(KineticContainerPage<?> page, KineticGraphics graphics, int mouseX,
                                              int mouseY, float partialTick) {
            page.renderContainerBackground(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void renderScreenOverlay(KineticContainerPage<?> page, KineticGraphics graphics, int mouseX, int mouseY,
                                        float partialTick) {
            page.renderScreenOverlay(graphics, mouseX, mouseY, partialTick);
        }
    }
}
