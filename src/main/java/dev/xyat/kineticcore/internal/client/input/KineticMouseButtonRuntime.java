package dev.xyat.kineticcore.internal.client.input;

import org.lwjgl.glfw.GLFW;

/** Internal mapping between Minecraft mouse-button codes and platform input constants. */
public final class KineticMouseButtonRuntime {
    private KineticMouseButtonRuntime() {
    }

    /** Returns whether the supplied input action represents a press. */
    public static boolean isPressAction(int action) {
        return action == GLFW.GLFW_PRESS;
    }

    /** Returns whether the supplied button code is the primary mouse button. */
    public static boolean isPrimary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    /** Returns whether the supplied button code is the secondary mouse button. */
    public static boolean isSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    /** Returns whether the supplied button code is the middle mouse button. */
    public static boolean isMiddle(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
    }
}
