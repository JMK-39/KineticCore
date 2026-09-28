package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** 单行文本输入框 / Single-line text input. */
public interface KineticTextField extends KineticControl {
    // 名称避开原版 EditBox 方法（getValue/setValue/setResponder 等），见 KineticControl。
    // Names avoid vanilla EditBox methods (getValue/setValue/setResponder, ...); see KineticControl.

    /** 当前文本 / Current text. */
    String textValue();

    /** 设置文本（会触发变更回调）/ Sets the text (invokes the change callback). */
    void setTextValue(String value);

    /** 文本变更回调 / Change callback. */
    void onTextChange(Consumer<String> responder);

    /** 最大长度 / Maximum length. */
    void limitTextLength(int maxLength);

    /** 输入过滤：返回 false 的编辑被拒绝 / Input filter; edits producing rejected text are refused. */
    void filterText(Predicate<String> filter);

    /** 显示格式化（如着色）/ Display formatter, e.g. for syntax coloring. */
    void formatText(BiFunction<String, Integer, FormattedCharSequence> formatter);

    /**
     * 默认值：内容等于默认值时显示黑色，其它非空内容（已修改）显示绿色；null 表示没有默认值（任何输入都算修改）。
     * Default value: text equal to it is drawn black, any other non-empty text (modified) green; null means no
     * default (any entered text counts as modified).
     */
    void setDefaultText(String defaultText);

    /** 当前默认值，没有则 null / Current default value, or null. */
    String defaultText();

    /**
     * 按内容自定义文字颜色（例如货币不足显示红色），返回 null 时使用标准规则（默认值黑、修改过绿）。
     * Custom text color per value (for example red when a price is unaffordable); return null to use the
     * standard rule (default value black, modified green).
     */
    void setValueColor(Function<String, Integer> valueColor);

    /**
     * 占位提示：仅在内容为空且没有焦点时以纯白显示，始终忽略文本自带的颜色样式。
     * Placeholder: shown in pure white only while the field is empty and unfocused; its own color styling is ignored.
     */
    void setPlaceholder(Component placeholder);

    /** 当前占位提示 / Current placeholder. */
    Component placeholder();

    /** 设置是否可编辑（仍可选择、复制）/ Whether the text can be edited (selection stays possible). */
    void setTextEditable(boolean editable);

    /** 是否可编辑 / Whether the text is editable. */
    boolean isTextEditable();

    /** 业务校验器，决定错误边框 / Business validator driving the error border. */
    void setValidator(Predicate<String> validator);

    /** 当前值是否通过校验 / Whether the current value passes validation. */
    boolean isValueValid();

    /** 手动设置错误态 / Forces the validation-error state. */
    void setValidationError(boolean validationError);

    /** 是否处于错误态 / Whether the validation-error state is shown. */
    boolean hasValidationError();

    /** 闪烁一次错误边框 / Flashes the error border once. */
    void flashValidationError();

    /** 光标位置 / Cursor position. */
    int cursorIndex();

    /** 移动光标 / Moves the cursor. */
    void setCursorIndex(int position);
}
