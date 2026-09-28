package dev.xyat.kineticcore.api.client.gui.widget;

import java.util.function.Consumer;

/** 多行文本输入框 / Multi-line text input. */
public interface KineticTextArea extends KineticControl {
    /** 当前文本 / Current text. */
    String textValue();

    /** 设置文本 / Sets the text. */
    void setTextValue(String value);

    /** 最大字符数 / Maximum character count. */
    void limitTextLength(int limit);

    /** 文本变更回调 / Change callback. */
    void onTextChange(Consumer<String> listener);
}
