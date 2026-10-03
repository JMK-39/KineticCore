package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.internal.client.gui.GuiInputCompat;

import dev.xyat.kineticcore.api.client.gui.input.CharInput;
import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.KeyModifiers;
import dev.xyat.kineticcore.api.client.gui.input.MouseButton;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import net.minecraft.client.gui.screens.Screen;

/** Converts vanilla input callbacks into the public Kinetic input records. */
public final class InputRecords {
    private InputRecords() {
    }

    /** Current modifier mask read from the keyboard state. */
    public static int currentModifiers() {
        int modifiers = 0;
        if (GuiInputCompat.shiftDown()) modifiers |= KeyModifiers.SHIFT;
        if (GuiInputCompat.controlDown()) modifiers |= KeyModifiers.CONTROL;
        if (GuiInputCompat.altDown()) modifiers |= KeyModifiers.ALT;
        return modifiers;
    }

    public static MouseInput mouse(double x, double y, int button) {
        return new MouseInput(x, y, MouseButton.of(button), button, currentModifiers());
    }

    public static MouseDragInput drag(double x, double y, int button, double dragX, double dragY) {
        return new MouseDragInput(x, y, MouseButton.of(button), button, dragX, dragY);
    }

    public static ScrollInput scroll(double x, double y, double delta) {
        return new ScrollInput(x, y, 0D, delta);
    }

    public static KeyInput key(int keyCode, int scanCode, int modifiers) {
        return new KeyInput(keyCode, scanCode, modifiers);
    }

    public static CharInput character(char codePoint, int modifiers) {
        return new CharInput(codePoint, modifiers);
    }
}
