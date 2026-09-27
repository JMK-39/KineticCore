package dev.xyat.kineticcore.api.client.gui.input;

/** 一次字符输入 / One typed character. */
public record CharInput(int codePoint, int modifiers) {
    /** 字符串形式 / The character as a string. */
    public String text() {
        return Character.toString(codePoint);
    }
}
