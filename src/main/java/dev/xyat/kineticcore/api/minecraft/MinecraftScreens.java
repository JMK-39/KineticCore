package dev.xyat.kineticcore.api.minecraft;

import net.minecraft.client.gui.screens.Screen;

/** Helpers for vanilla screens. */
public final class MinecraftScreens {
    private MinecraftScreens() {
    }

    /**
     * Implemented on vanilla screens that remember their parent by a KineticCore mixin. Add-ons use
     * {@link #parent(Screen)} instead.
     */
    public interface ParentAccess {
        /** Returns the screen that opened this one, or {@code null}. */
        Screen kineticcore$getLastScreen();
    }

    /**
     * Returns the screen that opened {@code screen}, for vanilla screens that keep a reference to it (such as
     * options screens).
     *
     * @return the parent, or {@code null} when unknown
     */
    public static Screen parent(Screen screen) {
        if (screen instanceof ParentAccess access) {
            return access.kineticcore$getLastScreen();
        }
        return null;
    }
}
