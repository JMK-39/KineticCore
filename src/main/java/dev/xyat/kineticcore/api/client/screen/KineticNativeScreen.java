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
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 使用 Minecraft 原生屏幕坐标的 Kinetic 编辑界面基类。
 * 适合不需要虚拟画布，但仍需要统一返回、草稿回滚和保存边界的界面。
 */
public abstract class KineticNativeScreen extends Screen {
    private final GuiOverlay overlays = new GuiOverlay();
    private final KineticScreenControls controls = new KineticScreenControls(
            () -> font, overlays, this::addRenderableWidget, this::addWidget,
            this::removeWidget, this::openContextMenu
    );
    private final KineticScreenFocus focus = new KineticScreenFocus(this);
    private GuiSession.DraftSession draftSession = new GuiSession.DraftSession();

    protected KineticNativeScreen(Component title) {
        super(title);
        dev.xyat.kineticcore.api.runtime.KineticClientRuntime.ensureReady();
    }

    protected final GuiOverlay overlays() {
        return overlays;
    }

    public final double toVirtualX(double screenX) {
        return screenX;
    }

    public final double toVirtualY(double screenY) {
        return screenY;
    }

    public final int toScreenX(double virtualX) {
        return (int) Math.floor(virtualX);
    }

    public final int toScreenY(double virtualY) {
        return (int) Math.floor(virtualY);
    }

    public final int toScreenRight(double virtualX) {
        return (int) Math.ceil(virtualX);
    }

    public final int toScreenBottom(double virtualY) {
        return (int) Math.ceil(virtualY);
    }

    public final void enableCanvasScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        enableUiScissor(graphics, left, top, right, bottom);
    }

    public final void disableCanvasScissor(GuiGraphics graphics) {
        disableUiScissor(graphics);
    }

    public final void renderTextFieldPlaceholder(GuiGraphics graphics, EditBox field, Component placeholder) {
        if (field == null || placeholder == null) return;
        if (field instanceof KineticEditBox kineticField) {
            kineticField.setPlaceholder(placeholder);
            return;
        }
        if (field.isFocused() || !field.getValue().isEmpty() || placeholder.getString().isBlank()) return;
        graphics.drawString(
                font,
                placeholder,
                field.getX() + 5,
                field.getY() + (field.getHeight() - font.lineHeight) / 2,
                GuiTheme.current().mutedText(),
                false
        );
    }

    /** 按当前 Screen 的 UI 坐标启用裁剪；与 disableUiScissor 配对使用。 */
    public final void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics == null) return;
        graphics.enableScissor(left, top, right, bottom);
    }

    /** 结束通过 enableUiScissor 开启的裁剪，建议放在 finally 中。 */
    public final void disableUiScissor(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.disableScissor();
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
            int x, int y, int width, Component message, Supplier<java.util.List<String>> dictionarySupplier, Component tooltip
    ) {
        return controls.addAutoCompleteField(x, y, width, message, dictionarySupplier, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final AutoCompleteBox addAutoCompleteField(
            int x, int y, int width, Component message, Component placeholder,
            Supplier<java.util.List<String>> dictionarySupplier, Component tooltip
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
    public final ColorSwatchButton addColorSwatchButton( int x, int y, int rgb, Component tooltip, Runnable action ) {
        return controls.addColorSwatchButton(x, y, rgb, tooltip, action);
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
    public final <T extends AbstractWidget> T addControl(T widget, Component tooltip) {
        return controls.addControl(widget, tooltip);
    }

    /** 移除控件及其 Screen Tooltip；固定画布 Screen 同时解除滚动视口绑定。 */
    public final void removeControl(AbstractWidget widget) {
        controls.removeControl(widget);
    }

    /** 仅注册列表的输入事件；调用方负责通过对应 Screen 的列表渲染 API 绘制。 */
    public final <T extends ObjectSelectionList<?>> T addEventListWidget(T list) {
        return controls.addEventListWidget(list);
    }

    /** 为控件登记 Screen Overlay Tooltip；null 或空文本移除登记，不修改控件自带 Tooltip。 */
    public final <T extends AbstractWidget> T registerWidgetTooltip(T widget, Component tooltip) {
        return controls.registerWidgetTooltip(widget, tooltip);
    }

    private void requestWidgetTooltip(double mouseX, double mouseY) {
        controls.requestWidgetTooltip(mouseX, mouseY);
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
    public final void openContextMenu(double x, double y, List<GuiOverlay.MenuItem> items) {
        overlays.openMenu((int) Math.round(x), (int) Math.round(y), items);
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

    /** 在打开前预留独立草稿边界，避免继承父界面的草稿会话。 */
    protected final void reserveStandaloneDraft() {
        draftSession.reserveStandaloneOwner(this);
    }

    /** 设置草稿快照与恢复函数。离开共享草稿会话时回滚；capture 应返回独立且可按 equals 比较的快照。 */
    protected final <T> void configureDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, false);
    }

    /** 建立独立草稿保存边界，不继承父界面草稿；离开该边界时回滚未提交修改。 */
    protected final <T> void configureStandaloneDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
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
        super.init();
        rebuildUi();
    }

    /** 通过 buildUi 重建并重新注册控件，清理旧 Tooltip 和视口绑定；不要直接调用 this.init() 或 clearWidgets。 */
    public final void rebuildUi() {
        clearWidgets();
        controls.clear();
        buildUi();
    }

    protected abstract void buildUi();

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        overlays.beginFrame();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!overlays.blocksInput()) {
            requestWidgetTooltip(mouseX, mouseY);
        }
        renderNativeOverlayRequests(graphics, mouseX, mouseY, partialTick);
        overlays.render(graphics, font, width, height, mouseX, mouseY);
    }

    protected void renderNativeOverlayRequests(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overlays.mouseClicked(mouseX, mouseY, button, width, height, font)) {
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (overlays.blocksInput()) {
            return true;
        }
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overlays.blocksInput()) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overlays.blocksInput()) return true;
        if (GuiSession.routeSelectionListWheel(children(), mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!overlays.blocksInput()) super.mouseMoved(mouseX, mouseY);
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
        if (sharedDraft != null && sharedDraft.enabled()) draftSession = sharedDraft;
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
