package dev.xyat.kineticcore.api.minecraft;

import net.minecraft.client.gui.screens.Screen;

public final class MinecraftScreens {
    private MinecraftScreens() {
    }

    public interface ParentAccess {
        Screen kineticcore$getLastScreen();
    }

    public static Screen parent(Screen screen) {
        if (screen instanceof ParentAccess access) {
            return access.kineticcore$getLastScreen();
        }
        return null;
    }
}
