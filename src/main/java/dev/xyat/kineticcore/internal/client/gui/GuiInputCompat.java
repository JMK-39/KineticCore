package dev.xyat.kineticcore.internal.client.gui;

import net.minecraft.client.gui.components.events.GuiEventListener;

/** Vanilla GUI input calls whose signature changed between the supported Minecraft versions. */
public final class GuiInputCompat {
    private GuiInputCompat() {
    }

    /** Sends a vertical wheel step; 1.20.2 added a horizontal component, which Kinetic leaves at zero. */
    public static boolean mouseScrolled(GuiEventListener listener, double mouseX, double mouseY, double delta) {
        //? if >=1.20.2 {
        /*return listener.mouseScrolled(mouseX, mouseY, 0.0D, delta);
        *///?} else {
        return listener.mouseScrolled(mouseX, mouseY, delta);
        //?}
    }
}
