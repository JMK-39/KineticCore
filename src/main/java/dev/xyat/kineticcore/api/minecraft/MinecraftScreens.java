package dev.xyat.kineticcore.api.minecraft;

import net.minecraft.client.gui.screens.Screen;

/** Public API type for minecraft screens. */
public final class MinecraftScreens {
    private MinecraftScreens() {
    }

    /** Access contract for parent operations. */
    public interface ParentAccess {
        Screen kineticcore$getLastScreen();
    }

    /**
     * Performs the parent API operation.
     */
    public static Screen parent(Screen screen) {
        if (screen instanceof ParentAccess access) {
            return access.kineticcore$getLastScreen();
        }
        return null;
    }
}
