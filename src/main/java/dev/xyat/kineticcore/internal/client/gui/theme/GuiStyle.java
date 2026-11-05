package dev.xyat.kineticcore.internal.client.gui.theme;

import java.util.function.BooleanSupplier;

/** Which look Kinetic interfaces use: modern (rounded) or classic (square). Backed by ui_client.toml. */
public final class GuiStyle {
    private static volatile BooleanSupplier modern = () -> true;
    private static volatile java.util.function.Consumer<Boolean> store = ignored -> { };

    private GuiStyle() {
    }

    /** Called by the UI client config once it exists: how to read the setting and how to save a new value. */
    public static void bind(BooleanSupplier setting, java.util.function.Consumer<Boolean> saver) {
        modern = setting == null ? () -> true : setting;
        store = saver == null ? ignored -> { } : saver;
    }

    /** Switches the style and saves it to ui_client.toml; every Kinetic screen follows from the next frame. */
    public static void setModern(boolean value) {
        store.accept(value);
    }

    /** Whether the modern style is on; also true before the client config has loaded. */
    public static boolean modern() {
        try {
            return modern.getAsBoolean();
        } catch (RuntimeException notLoaded) {
            return true;
        }
    }
}
