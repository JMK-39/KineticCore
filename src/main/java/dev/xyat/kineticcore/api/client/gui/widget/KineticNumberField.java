package dev.xyat.kineticcore.api.client.gui.widget;

/** 数字输入框（整数、长整数或小数）/ Numeric input (integer, long or decimal). */
public interface KineticNumberField extends KineticTextField {
    /** 解析为 int，无效时 null / Parsed int value, or null when invalid. */
    Integer getIntValue();

    /** 解析为 long，无效时 null / Parsed long value, or null when invalid. */
    Long getLongValue();

    /** 解析为 double，无效时 null / Parsed double value, or null when invalid. */
    Double getDoubleValue();

    /** 设置 int 值 / Sets an int value. */
    void setIntValue(int value);

    /** 设置 long 值 / Sets a long value. */
    void setLongValue(long value);

    /** 设置 double 值 / Sets a double value. */
    void setDoubleValue(double value);
}
