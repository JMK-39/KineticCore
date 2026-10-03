package dev.xyat.kineticcore.internal.client.gui;

import net.minecraft.client.gui.components.events.GuiEventListener;
//? if >=26.1 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
*///?} else {
import net.minecraft.client.gui.screens.Screen;
//?}

/**
 * Vanilla GUI input calls whose signature changed between the supported Minecraft versions. Since 1.21.9 listeners
 * receive event objects; KineticCore keeps passing coordinates, buttons and key codes and builds the events here.
 */
public final class GuiInputCompat {
    // GLFW modifier bits, as carried by key and mouse events.
    private static final int MOD_SHIFT = 1;
    private static final int MOD_CONTROL = 2;
    private static final int MOD_ALT = 4;

    private GuiInputCompat() {
    }

    public static boolean shiftDown() {
        //? if >=26.1 {
        /*return Minecraft.getInstance().hasShiftDown();
        *///?} else {
        return Screen.hasShiftDown();
        //?}
    }

    public static boolean controlDown() {
        //? if >=26.1 {
        /*return Minecraft.getInstance().hasControlDown();
        *///?} else {
        return Screen.hasControlDown();
        //?}
    }

    public static boolean altDown() {
        //? if >=26.1 {
        /*return Minecraft.getInstance().hasAltDown();
        *///?} else {
        return Screen.hasAltDown();
        //?}
    }

    /** Ctrl+A (Cmd+A on macOS) with the modifiers held right now. */
    public static boolean isSelectAll(int keyCode) {
        //? if >=26.1 {
        /*return new KeyEvent(keyCode, 0, currentModifiers()).isSelectAll();
        *///?} else {
        return Screen.isSelectAll(keyCode);
        //?}
    }

    public static boolean isCopy(int keyCode) {
        //? if >=26.1 {
        /*return new KeyEvent(keyCode, 0, currentModifiers()).isCopy();
        *///?} else {
        return Screen.isCopy(keyCode);
        //?}
    }

    public static boolean isPaste(int keyCode) {
        //? if >=26.1 {
        /*return new KeyEvent(keyCode, 0, currentModifiers()).isPaste();
        *///?} else {
        return Screen.isPaste(keyCode);
        //?}
    }

    public static boolean isCut(int keyCode) {
        //? if >=26.1 {
        /*return new KeyEvent(keyCode, 0, currentModifiers()).isCut();
        *///?} else {
        return Screen.isCut(keyCode);
        //?}
    }

    /** The modifier keys held right now, as GLFW modifier bits. */
    public static int currentModifiers() {
        return (shiftDown() ? MOD_SHIFT : 0) | (controlDown() ? MOD_CONTROL : 0) | (altDown() ? MOD_ALT : 0);
    }

    /** Sends a vertical wheel step; 1.20.2 added a horizontal component, which Kinetic leaves at zero. */
    public static boolean mouseScrolled(GuiEventListener listener, double mouseX, double mouseY, double delta) {
        //? if >=1.20.2 {
        /*return listener.mouseScrolled(mouseX, mouseY, 0.0D, delta);
        *///?} else {
        return listener.mouseScrolled(mouseX, mouseY, delta);
        //?}
    }

    public static boolean mouseClicked(GuiEventListener listener, double mouseX, double mouseY, int button) {
        //? if >=26.1 {
        /*return listener.mouseClicked(mouseEvent(mouseX, mouseY, button), false);
        *///?} else {
        return listener.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    public static boolean mouseReleased(GuiEventListener listener, double mouseX, double mouseY, int button) {
        //? if >=26.1 {
        /*return listener.mouseReleased(mouseEvent(mouseX, mouseY, button));
        *///?} else {
        return listener.mouseReleased(mouseX, mouseY, button);
        //?}
    }

    public static boolean mouseDragged(GuiEventListener listener, double mouseX, double mouseY, int button,
                                       double dragX, double dragY) {
        //? if >=26.1 {
        /*return listener.mouseDragged(mouseEvent(mouseX, mouseY, button), dragX, dragY);
        *///?} else {
        return listener.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        //?}
    }

    public static boolean keyPressed(GuiEventListener listener, int keyCode, int scanCode, int modifiers) {
        //? if >=26.1 {
        /*return listener.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
        *///?} else {
        return listener.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    public static boolean keyReleased(GuiEventListener listener, int keyCode, int scanCode, int modifiers) {
        //? if >=26.1 {
        /*return listener.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
        *///?} else {
        return listener.keyReleased(keyCode, scanCode, modifiers);
        //?}
    }

    public static boolean charTyped(GuiEventListener listener, char codePoint, int modifiers) {
        //? if >=26.1 {
        /*return listener.charTyped(new CharacterEvent(codePoint));
        *///?} else {
        return listener.charTyped(codePoint, modifiers);
        //?}
    }

    //? if >=26.1 {
    /*private static MouseButtonEvent mouseEvent(double mouseX, double mouseY, int button) {
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, currentModifiers()));
    }
    *///?}
}
