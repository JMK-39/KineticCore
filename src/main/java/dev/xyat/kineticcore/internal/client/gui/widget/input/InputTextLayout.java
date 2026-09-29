package dev.xyat.kineticcore.internal.client.gui.widget.input;

/**
 * Content-box math shared by every Kinetic single-line input.
 * It has no Minecraft dependencies so the alignment rules stay covered by headless regression checks.
 */
public final class InputTextLayout {
    /** Horizontal gap between the field edge and its text, matching vanilla bordered edit boxes. */
    public static final int HORIZONTAL_PADDING = 4;

    private InputTextLayout() {}

    /**
     * Returns the font Y that vertically centers one text line inside a field.
     *
     * <p>A glyph drawn at Y covers Y through {@code Y + lineHeight - 2}: capitals, digits and descenders.
     * The final line pixel is inter-line spacing and is excluded, which reproduces vanilla EditBox and button
     * centering exactly. Fields shorter than one line keep the text at their top edge.
     */
    public static int textTop(int fieldY, int fieldHeight, int lineHeight) {
        int inkHeight = Math.max(1, lineHeight - 1);
        return fieldY + Math.max(0, (fieldHeight - inkHeight) / 2);
    }

    /** Returns the X where input text, caret and placeholder start. */
    public static int textLeft(int fieldX) {
        return fieldX + HORIZONTAL_PADDING;
    }

    /** Returns the width available to input text, never less than one pixel. */
    public static int textWidth(int fieldWidth) {
        return Math.max(1, fieldWidth - HORIZONTAL_PADDING * 2);
    }
}
