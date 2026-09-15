package dev.xyat.kineticcore.api.client.widget;

import dev.xyat.kineticcore.api.client.widget.button.KineticButtons;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll;
import dev.xyat.kineticcore.api.client.widget.render.KineticEntityPreview;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorPreviewButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorSwatchButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.MenuButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.ValidationEditBox;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.TabBar;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 标准控件统一入口。addXxx 由 Screen 注册；createXxx 只创建控件。滚动能力统一使用 Scroll。 */
public final class KineticWidgets {
    private KineticWidgets() {
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static Button createButton( int x, int y, int width, Component text, Component tooltip, Button.OnPress action ) {
        return KineticButtons.createButton(x, y, width, text, tooltip, action);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static Button createCompactButton( int x, int y, int width, Component text, Component tooltip, Button.OnPress action ) {
        return KineticButtons.createCompactButton(x, y, width, text, tooltip, action);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static MenuButton createMenuButton( Component text, boolean enabled, boolean danger, Button.OnPress action ) {
        return KineticButtons.createMenuButton(text, enabled, danger, action);
    }

    public static KineticButtons.TextureButton createTextureButton(
            int x, int y, int width, int height, ResourceLocation texture,
            int u, int v, int hoverVOffset, int textureWidth, int textureHeight,
            Component narration, Component tooltip, Button.OnPress action
    ) {
        return KineticButtons.createTextureButton(
                x, y, width, height, texture, u, v, hoverVOffset, textureWidth, textureHeight,
                narration, tooltip, action
        );
    }

    public static void renderTextureButtonIcon(
            GuiGraphics graphics, int x, int y, int width, int height, ResourceLocation texture,
            int u, int v, int hoverVOffset, int textureWidth, int textureHeight, boolean hovered
    ) {
        KineticButtons.renderTextureButtonIcon(
                graphics, x, y, width, height, texture, u, v, hoverVOffset, textureWidth, textureHeight, hovered
        );
    }

    /** 读取或更新标准按钮状态；普通原版按钮不具备这些状态。 */
    public static void setButtonSelected(Button button, boolean selected) {
        KineticButtons.setButtonSelected(button, selected);
    }

    /** 读取或更新标准按钮状态；普通原版按钮不具备这些状态。 */
    public static void setButtonError(Button button, boolean error) {
        KineticButtons.setButtonError(button, error);
    }

    /** 读取或更新标准按钮状态；普通原版按钮不具备这些状态。 */
    public static boolean isButtonSelected(Button button) {
        return KineticButtons.isButtonSelected(button);
    }

    /** 读取或更新标准按钮状态；普通原版按钮不具备这些状态。 */
    public static boolean isButtonError(Button button) {
        return KineticButtons.isButtonError(button);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static KineticEditBox createTextField( Font font, int x, int y, int width, Component message, Component tooltip ) {
        return KineticTextFields.createTextField(font, x, y, width, message, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static KineticEditBox createTextField( Font font, int x, int y, int width, Component message, Component placeholder, Component tooltip ) {
        return KineticTextFields.createTextField(font, x, y, width, message, placeholder, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static KineticEditBox createTextField(
            Font font, int x, int y, int width,
            Component message, Component placeholder, Predicate<String> validator, Component tooltip
    ) {
        return KineticTextFields.createTextField(font, x, y, width, message, placeholder, validator, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static MultiLineEditBox createMultiLineTextField(
            Font font, int x, int y, int width, int height,
            Component message, Component placeholder, Component tooltip
    ) {
        return KineticTextFields.createMultiLineTextField(font, x, y, width, height, message, placeholder, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static KineticEditBox createCompactTextField( Font font, int x, int y, int width, Component message, Component tooltip ) {
        return KineticTextFields.createCompactTextField(font, x, y, width, message, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static KineticEditBox createCompactTextField(
            Font font, int x, int y, int width,
            Component message, Component placeholder, Component tooltip
    ) {
        return KineticTextFields.createCompactTextField(font, x, y, width, message, placeholder, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static KineticEditBox createCompactTextField(
            Font font, int x, int y, int width,
            Component message, Component placeholder, Predicate<String> validator, Component tooltip
    ) {
        return KineticTextFields.createCompactTextField(font, x, y, width, message, placeholder, validator, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static ValidationEditBox createValidatingCompactTextField( Font font, int x, int y, int width, Component message, Component tooltip ) {
        return KineticTextFields.createValidatingCompactTextField(font, x, y, width, message, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static AutoCompleteBox createAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        return KineticAutoComplete.createAutoCompleteField(font, x, y, width, message, dictionarySupplier, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static AutoCompleteBox createAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        return KineticAutoComplete.createAutoCompleteField(font, x, y, width, message, placeholder, dictionarySupplier, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericEditBox createIntegerField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return KineticNumericFields.createIntegerField(font, x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericEditBox createIntegerField(
            Font font,
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
        return KineticNumericFields.createIntegerField(font, x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericEditBox createLongField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return KineticNumericFields.createLongField(font, x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericEditBox createLongField(
            Font font,
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
        return KineticNumericFields.createLongField(font, x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericEditBox createDecimalField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return KineticNumericFields.createDecimalField(font, x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericEditBox createDecimalField(
            Font font,
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
        return KineticNumericFields.createDecimalField(font, x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericAutoCompleteBox createIntegerAutoCompleteField(
            Font font,
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
        return KineticAutoComplete.createIntegerAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericAutoCompleteBox createIntegerAutoCompleteField(
            Font font,
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
        return KineticAutoComplete.createIntegerAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericAutoCompleteBox createLongAutoCompleteField(
            Font font,
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
        return KineticAutoComplete.createLongAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericAutoCompleteBox createLongAutoCompleteField(
            Font font,
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
        return KineticAutoComplete.createLongAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericAutoCompleteBox createDecimalAutoCompleteField(
            Font font,
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
        return KineticAutoComplete.createDecimalAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static NumericAutoCompleteBox createDecimalAutoCompleteField(
            Font font,
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
        return KineticAutoComplete.createDecimalAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static ToggleButton createToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return KineticButtons.createToggleButton(x, y, width, value, onText, offText, tooltip, responder);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static ToggleButton createToggleButton(
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
        return KineticButtons.createToggleButton(x, y, width, value, onText, offText, tooltip, validator, responder);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static ColorSwatchButton createColorSwatchButton( int x, int y, int rgb, Component tooltip, Runnable action ) {
        return KineticButtons.createColorSwatchButton(x, y, rgb, tooltip, action);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static ColorPreviewButton createColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        return KineticButtons.createColorPreviewButton(x, y, width, color, text, tooltip, action);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static HighZButton createHighZButton( int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action ) {
        return KineticButtons.createHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static HighZButton createCompactHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        return KineticButtons.createCompactHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static Dropdown createDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder,
            Consumer<Dropdown> opener
    ) {
        return KineticDropdowns.createDropdown(x, y, width, options, selectedIndex, tooltip, responder, opener);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static Dropdown createDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder,
            Consumer<Dropdown> opener
    ) {
        return KineticDropdowns.createDropdown(x, y, width, options, selectedIndex, tooltip, validator, responder, opener);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static TabBar createTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return KineticTabs.createTabBar(x, y, totalWidth, labels, selectedIndex, responder);
    }

    /** 创建未注册的标准控件；Screen 内优先使用 addXxx，Helper/Tab/Panel 使用此工厂。 */
    public static TabBar createTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return KineticTabs.createTabBar(x, y, totalWidth, labels, tooltips, selectedIndex, responder);
    }


    public static <T extends AbstractWidget> T attachTooltip(T widget, Component tooltip) {
        if (widget == null) return null;
        widget.setTooltip(tooltip == null || tooltip.getString().isBlank() ? null : Tooltip.create(tooltip));
        return widget;
    }


}
