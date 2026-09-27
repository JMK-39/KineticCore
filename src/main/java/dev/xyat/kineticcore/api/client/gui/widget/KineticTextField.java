package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** 单行文本输入框 / Single-line text input. */
public interface KineticTextField extends KineticControl {
    /** 当前文本 / Current text. */
    String getValue();

    /** 设置文本（会触发变更回调）/ Sets the text (invokes the change callback). */
    void setValue(String value);

    /** 文本变更回调 / Change callback. */
    void setResponder(Consumer<String> responder);

    /** 最大长度 / Maximum length. */
    void setMaxLength(int maxLength);

    /** 输入过滤：返回 false 的编辑被拒绝 / Input filter; edits producing rejected text are refused. */
    void setFilter(Predicate<String> filter);

    /** 显示格式化（如着色）/ Display formatter, e.g. for syntax coloring. */
    void setFormatter(BiFunction<String, Integer, FormattedCharSequence> formatter);

    /** 占位提示 / Placeholder. */
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
    int getCursorPosition();

    /** 移动光标 / Moves the cursor. */
    void setCursorPosition(int position);
}
