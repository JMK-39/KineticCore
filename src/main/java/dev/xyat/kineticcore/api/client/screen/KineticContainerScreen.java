package dev.xyat.kineticcore.api.client.screen;

import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.TabBar;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorPreviewButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorSwatchButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.layout.GuiLayout;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class KineticContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private float uiScale = 1f;
    private int uiX;
    private int uiY;
    private int uiWidth = 1;
    private int uiHeight = 1;
    private int designWidth = KineticScreen.STANDARD_CANVAS_WIDTH;
    private int designHeight = KineticScreen.STANDARD_CANVAS_HEIGHT;
    private int safeMargin = KineticScreen.STANDARD_SAFE_MARGIN;
    private final GuiOverlay overlays = new GuiOverlay();
    private final KineticScreenControls controls = new KineticScreenControls(
            () -> font, overlays, this::addRenderableWidget, this::addWidget,
            this::removeWidget, this::openContextMenu
    );
    private final KineticScreenFocus focus = new KineticScreenFocus(this);
    private GuiSession.DraftSession draftSession = new GuiSession.DraftSession();

    protected KineticContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        dev.xyat.kineticcore.api.runtime.KineticClientRuntime.ensureReady();
    }

    public final void useResponsiveContainer(float designWidth, float designHeight, int safeMargin) {
        this.designWidth = Math.max(1, Math.round(designWidth));
        this.designHeight = Math.max(1, Math.round(designHeight));
        this.safeMargin = Math.max(0, safeMargin);
    }

    public final int uiWidth() {
        return uiWidth;
    }

    public final int uiHeight() {
        return uiHeight;
    }

    public final float uiScale() {
        return uiScale;
    }

    public final double toVirtualX(double screenX) {
        return (screenX - uiX) / uiScale;
    }

    public final double toVirtualY(double screenY) {
        return (screenY - uiY) / uiScale;
    }

    public final int toScreenX(double virtualX) {
        return uiX + (int) Math.floor(virtualX * uiScale);
    }

    public final int toScreenY(double virtualY) {
        return uiY + (int) Math.floor(virtualY * uiScale);
    }

    public final int toScreenRight(double virtualX) {
        return uiX + (int) Math.ceil(virtualX * uiScale);
    }

    public final int toScreenBottom(double virtualY) {
        return uiY + (int) Math.ceil(virtualY * uiScale);
    }

    /** 按当前 Screen 的 UI 坐标启用裁剪；与 disableUiScissor 配对使用。 */
    public final void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics instanceof UiCanvasGraphics) {
            graphics.enableScissor(left, top, right, bottom);
            return;
        }
        graphics.enableScissor(toScreenX(left), toScreenY(top), toScreenRight(right), toScreenBottom(bottom));
    }

    /** 结束通过 enableUiScissor 开启的裁剪，建议放在 finally 中。 */
    public final void disableUiScissor(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    protected final GuiOverlay overlays() {
        return overlays;
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final KineticEditBox addTextField(int x, int y, int width, Component message) {
        return controls.addTextField(x, y, width, message);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final KineticEditBox addTextField(int x, int y, int width, Component message, Component tooltip) {
        return controls.addTextField(x, y, width, message, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final KineticEditBox addTextField( int x, int y, int width, Component message, Component placeholder, Component tooltip ) {
        return controls.addTextField(x, y, width, message, placeholder, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final KineticEditBox addTextField(
            int x, int y, int width, Component message, Component placeholder,
            Predicate<String> validator, Component tooltip
    ) {
        return controls.addTextField(x, y, width, message, placeholder, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final MultiLineEditBox addMultiLineTextField(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Component placeholder,
            Component tooltip
    ) {
        return controls.addMultiLineTextField(x, y, width, height, message, placeholder, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Supplier<List<String>> dictionarySupplier, Component tooltip
    ) {
        return controls.addAutoCompleteField(x, y, width, message, dictionarySupplier, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Component placeholder,
            Supplier<List<String>> dictionarySupplier, Component tooltip
    ) {
        return controls.addAutoCompleteField(x, y, width, message, placeholder, dictionarySupplier, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return controls.addIntegerAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addIntegerAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return controls.addLongAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addLongAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return controls.addDecimalAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addDecimalAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return controls.addIntegerField(x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addIntegerField(x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return controls.addLongField(x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addLongField(x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return controls.addDecimalField(x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addDecimalField(x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return controls.addTabBar(x, y, totalWidth, labels, selectedIndex, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return controls.addTabBar(x, y, totalWidth, labels, tooltips, selectedIndex, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Button addButton(int x, int y, int width, Component text, Component tooltip, Runnable action) {
        return controls.addButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Button addButton(int x, int y, int width, Component text, Component tooltip, Button.OnPress action) {
        return controls.addButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Button addCompactButton( int x, int y, int width, Component text, Component tooltip, Runnable action ) {
        return controls.addCompactButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Button addCompactButton( int x, int y, int width, Component text, Component tooltip, Button.OnPress action ) {
        return controls.addCompactButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final HighZButton addHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action
    ) {
        return controls.addHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final HighZButton addCompactHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action
    ) {
        return controls.addCompactHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final HighZButton addCompactHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action
    ) {
        return controls.addCompactHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return controls.addToggleButton(x, y, width, value, onText, offText, tooltip, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder
    ) {
        return controls.addToggleButton(x, y, width, value, onText, offText, tooltip, validator, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final ColorSwatchButton addColorSwatchButton( int x, int y, int rgb, Component tooltip, Runnable action ) {
        return controls.addColorSwatchButton(x, y, rgb, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final ColorPreviewButton addColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        return controls.addColorPreviewButton(x, y, width, color, text, tooltip, action);
    }

    /** 注册已有控件并加入渲染和输入列表；非空 Tooltip 交由 Screen Overlay 显示。null Tooltip 保留控件自带 Tooltip。 */
    public final <W extends AbstractWidget> W addControl(W widget, Component tooltip) {
        return controls.addControl(widget, tooltip);
    }

    /** 移除控件及其 Screen Tooltip；固定画布 Screen 同时解除滚动视口绑定。 */
    public final void removeControl(AbstractWidget widget) {
        controls.removeControl(widget);
    }

    /** 仅注册列表的输入事件；调用方负责通过对应 Screen 的列表渲染 API 绘制。 */
    public final <W extends ObjectSelectionList<?>> W addEventListWidget(W list) {
        return controls.addEventListWidget(list);
    }

    /** 为控件登记 Screen Overlay Tooltip；null 或空文本移除登记，不修改控件自带 Tooltip。 */
    public final <W extends AbstractWidget> W registerWidgetTooltip(W widget, Component tooltip) {
        return controls.registerWidgetTooltip(widget, tooltip);
    }

    private boolean requestWidgetTooltip(double mouseX, double mouseY) {
        return controls.requestWidgetTooltip(mouseX, mouseY);
    }

    /** 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。 */
    public final void showTooltip(Component component) {
        controls.showTooltip(component);
    }

    /** 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。 */
    public final void showTooltip(List<? extends Component> lines) {
        controls.showTooltip(lines);
    }

    /** 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。 */
    public final void showTooltip(Component component, int maxWidth) {
        controls.showTooltip(component, maxWidth);
    }

    /** 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。 */
    public final void showTooltip(List<? extends Component> lines, int maxWidth) {
        controls.showTooltip(lines, maxWidth);
    }

    /** 请求本帧的格式化 Tooltip，由统一 Overlay 渲染。 */
    public final void showFormattedTooltip(List<FormattedCharSequence> lines) {
        controls.showFormattedTooltip(lines);
    }

    /** 请求本帧的物品 Tooltip，由统一 Overlay 渲染。 */
    public final void showItemTooltip(ItemStack stack) {
        controls.showItemTooltip(stack);
    }

    public final void closeContextMenu() {
        controls.closeContextMenu();
    }

    /** 转移焦点并清除旧控件的焦点状态；传 null 等同 clearControlFocus。 */
    public final void focusControl(GuiEventListener control) {
        focus.focusControl(control);
    }

    /** 清除指定控件的焦点；仅当它是当前焦点时解除 Screen 焦点。 */
    public final void blurControl(GuiEventListener control) {
        focus.blurControl(control);
    }

    /** 同时清除 Screen 当前焦点和该控件的焦点状态。 */
    public final void clearControlFocus() {
        focus.clearControlFocus();
    }

    /** 判断 Screen 当前焦点和控件自身焦点是否一致。 */
    public final boolean isControlFocused(GuiEventListener control) {
        return focus.isControlFocused(control);
    }

    /** 在当前 Screen 的 UI 坐标处打开统一菜单；Screen 负责坐标转换。 */
    public final void openContextMenu(double virtualX, double virtualY, List<GuiOverlay.MenuItem> items) {
        overlays.openMenu(toScreenX(virtualX), toScreenY(virtualY), items);
    }

    /** 打开统一模态确认框；保存或回滚动作由 onConfirm/onCancel 回调决定。 */
    public final void openDialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        controls.openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return controls.addDropdown(x, y, width, options, selectedIndex, tooltip, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        return controls.addDropdown(x, y, width, options, selectedIndex, tooltip, validator, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return controls.addDropdown(x, y, width, options, optionTooltips, selectedIndex, tooltip, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        return controls.addDropdown(x, y, width, options, optionTooltips, selectedIndex, tooltip, validator, responder);
    }

    /** 设置草稿快照与恢复函数。离开共享草稿会话时回滚；capture 应返回独立且可按 equals 比较的快照。 */
    protected final <S> void configureDraft(java.util.function.Supplier<S> capture, java.util.function.Consumer<S> restore) {
        draftSession.configureDraft(this, capture, restore, false);
    }

    /** 建立独立草稿保存边界，不继承父界面草稿；离开该边界时回滚未提交修改。 */
    protected final <S> void configureStandaloneDraft(java.util.function.Supplier<S> capture, java.util.function.Consumer<S> restore) {
        draftSession.configureDraft(this, capture, restore, true);
    }

    /** 仅草稿所有者可更新已保存基线；先完成业务持久化，再调用本方法。本方法不写入配置。 */
    protected final void commitDraft() {
        draftSession.commitBaseline(this);
    }

    /** 恢复最近保存的草稿基线；不负责关闭界面。 */
    protected final void discardDraft() {
        draftSession.discardToBaseline();
    }

    /** 比较当前快照与保存基线，判断是否有未提交修改。 */
    protected final boolean hasUnsavedEdits() {
        return draftSession.isDirty();
    }

    @Override
    protected final void init() {
        updateMetrics();
        int screenWidth = this.width;
        int screenHeight = this.height;
        this.width = uiWidth;
        this.height = uiHeight;
        try {
            super.init();
            controls.clear();
            buildUi();
        } finally {
            this.width = screenWidth;
            this.height = screenHeight;
        }
    }

    protected abstract void buildUi();

    /** 通过 buildUi 重建并重新注册控件，清理旧 Tooltip 和视口绑定；不要直接调用 this.init() 或 clearWidgets。 */
    public final void rebuildUi() {
        updateMetrics();
        int screenWidth = this.width;
        int screenHeight = this.height;
        this.width = uiWidth;
        this.height = uiHeight;
        try {
            clearWidgets();
            controls.clear();
            super.init();
            buildUi();
        } finally {
            this.width = screenWidth;
            this.height = screenHeight;
        }
    }

    private void updateMetrics() {
        GuiLayout.SafeArea safeArea = GuiLayout.SafeArea.of(
                width,
                height,
                safeMargin
        );
        GuiLayout.Metrics metrics = GuiLayout.measure(
                safeArea.width(),
                safeArea.height(),
                designWidth,
                designHeight
        );
        uiScale = Math.max(0.0001f, metrics.fitScale());
        uiWidth = designWidth;
        uiHeight = designHeight;
        uiX = safeArea.left() + Math.round(
                (safeArea.width() - uiWidth * uiScale) / 2f
        );
        uiY = safeArea.top() + Math.round(
                (safeArea.height() - uiHeight * uiScale) / 2f
        );
    }

    @Override
    public final void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        overlays.beginFrame();
        int virtualMouseX = (int) Math.floor(toVirtualX(mouseX));
        int virtualMouseY = (int) Math.floor(toVirtualY(mouseY));

        GuiGraphics uiGraphics = new ContainerGuiGraphics(graphics);
        uiGraphics.pose().pushPose();
        uiGraphics.pose().translate(uiX, uiY, 0);
        uiGraphics.pose().scale(uiScale, uiScale, 1f);
        try {
            super.render(uiGraphics, virtualMouseX, virtualMouseY, partialTick);
            if (!overlays.blocksInput()) {
                requestContainerTooltips(uiGraphics, virtualMouseX, virtualMouseY, mouseX, mouseY);
            }
            renderUiForeground(uiGraphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            uiGraphics.pose().popPose();
        }

        renderScreenOverlay(graphics, virtualMouseX, virtualMouseY, mouseX, mouseY, partialTick);
        overlays.render(graphics, font, width, height, mouseX, mouseY);
    }

    private interface UiCanvasGraphics {
    }

    private final class ContainerGuiGraphics extends GuiGraphics implements UiCanvasGraphics {
        private ContainerGuiGraphics(GuiGraphics source) {
            super(KineticContainerScreen.this.minecraft, source.bufferSource());
        }

        @Override
        public void enableScissor(int left, int top, int right, int bottom) {
            super.enableScissor(
                    toScreenX(left),
                    toScreenY(top),
                    toScreenRight(right),
                    toScreenBottom(bottom)
            );
        }
    }

    protected void requestContainerTooltips(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY
    ) {
        if (requestWidgetTooltip(virtualMouseX, virtualMouseY)) return;
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            showItemTooltip(hoveredSlot.getItem());
        }
    }

    protected void renderUiForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderScreenOverlay(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY,
            float partialTick
    ) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (overlays.mouseClicked(mouseX, mouseY, button, width, height, font)) {
            return true;
        }
        boolean handled = super.mouseClicked(toVirtualX(mouseX), toVirtualY(mouseY), button);
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (overlays.blocksInput()) {
            return true;
        }
        boolean handled = super.mouseReleased(toVirtualX(mouseX), toVirtualY(mouseY), button);
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overlays.blocksInput()) return true;
        return super.mouseDragged(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button,
                dragX / uiScale,
                dragY / uiScale
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overlays.blocksInput()) return true;
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (GuiSession.routeSelectionListWheel(children(), virtualMouseX, virtualMouseY, delta)) return true;
        return super.mouseScrolled(virtualMouseX, virtualMouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!overlays.blocksInput()) super.mouseMoved(toVirtualX(mouseX), toVirtualY(mouseY));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (overlays.keyPressed(keyCode)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        return handled;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean handled = super.charTyped(codePoint, modifiers);
        return handled;
    }

    final GuiSession.DraftSession draftSession() {
        return draftSession;
    }

    final void adoptDraftSession(GuiSession.DraftSession sharedDraft) {
        if (sharedDraft != null && sharedDraft.enabled()) {
            this.draftSession = sharedDraft;
        }
    }

    final void discardPendingEditsForNavigation() {
        draftSession.discardToBaseline();
    }

    /** 返回父界面；离开共享草稿会话时回滚未提交修改，容器界面同时关闭容器。 */
    public final void navigateBack() {
        GuiSession.back(this);
    }

    @Override
    public void onClose() {
        navigateBack();
    }
}
