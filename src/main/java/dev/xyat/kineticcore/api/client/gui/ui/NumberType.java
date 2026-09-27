package dev.xyat.kineticcore.api.client.gui.ui;

/** 数字输入类型 / Numeric input type. */
public enum NumberType {
    /** 32 位整数 / 32-bit integer. */
    INT,
    /** 64 位整数 / 64-bit integer. */
    LONG,
    /** 小数 / Decimal. */
    DECIMAL;

    /** 格式化初始值 / Formats an initial value. */
    public String format(Number value) {
        if (value == null) return "";
        return switch (this) {
            case INT -> Integer.toString(value.intValue());
            case LONG -> Long.toString(value.longValue());
            case DECIMAL -> dev.xyat.kineticcore.internal.client.gui.widget.input.KineticNumericFields.formatDecimal(value.doubleValue());
        };
    }
}
