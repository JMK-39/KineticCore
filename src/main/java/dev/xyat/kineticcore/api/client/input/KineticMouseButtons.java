package dev.xyat.kineticcore.api.client.input;

import dev.xyat.kineticcore.internal.client.input.KineticMouseButtonRuntime;

/** Provides platform-independent checks for mouse button codes received from Minecraft screens. */
public final class KineticMouseButtons {
    private KineticMouseButtons() {
    }

    /** Returns whether the supplied input action represents a press. */
    public static boolean isPressAction(int action) {
        return KineticMouseButtonRuntime.isPressAction(action);
    }

    /** Returns whether the supplied code represents the primary mouse button. */
    public static boolean isPrimary(int button) {
        return KineticMouseButtonRuntime.isPrimary(button);
    }

    /** Returns whether the supplied code represents the secondary mouse button. */
    public static boolean isSecondary(int button) {
        return KineticMouseButtonRuntime.isSecondary(button);
    }

    /** Returns whether the supplied code represents the middle mouse button. */
    public static boolean isMiddle(int button) {
        return KineticMouseButtonRuntime.isMiddle(button);
    }
}
