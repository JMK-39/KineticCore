package dev.xyat.kineticcore.api.client.input;

import dev.xyat.kineticcore.internal.client.input.KineticKeyBindingRuntime;
import dev.xyat.kineticcore.internal.client.input.KineticKeyCodeRuntime;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Registers client key bindings through one builder-based API.
 * <p>
 * Configure the key, context, optional modifier and predicates with {@link #builder(String)}, then call
 * {@link Builder#register()}. The returned {@link Binding} is the read-only runtime handle for querying the
 * registered key state; add-ons do not construct or register key definitions through any secondary path.
 */
public final class KineticKeyBindings {
    private KineticKeyBindings() {
    }

    /** Determines where a registered binding may consume input. */
    public enum Context {
        IN_GAME,
        GUI,
        UNIVERSAL
    }

    /** Modifier required by a key binding. */
    public enum Modifier {
        NONE,
        SHIFT,
        CONTROL,
        ALT
    }

    /** Platform-neutral named keyboard keys for default Kinetic bindings. */
    public enum Key {
        UNKNOWN,
        SPACE,
        APOSTROPHE,
        COMMA,
        MINUS,
        PERIOD,
        SLASH,
        NUM_0,
        NUM_1,
        NUM_2,
        NUM_3,
        NUM_4,
        NUM_5,
        NUM_6,
        NUM_7,
        NUM_8,
        NUM_9,
        SEMICOLON,
        EQUAL,
        A,
        B,
        C,
        D,
        E,
        F,
        G,
        H,
        I,
        J,
        K,
        L,
        M,
        N,
        O,
        P,
        Q,
        R,
        S,
        T,
        U,
        V,
        W,
        X,
        Y,
        Z,
        LEFT_BRACKET,
        BACKSLASH,
        RIGHT_BRACKET,
        GRAVE_ACCENT,
        WORLD_1,
        WORLD_2,
        ESCAPE,
        ENTER,
        TAB,
        BACKSPACE,
        INSERT,
        DELETE,
        RIGHT,
        LEFT,
        DOWN,
        UP,
        PAGE_UP,
        PAGE_DOWN,
        HOME,
        END,
        CAPS_LOCK,
        SCROLL_LOCK,
        NUM_LOCK,
        PRINT_SCREEN,
        PAUSE,
        F1,
        F2,
        F3,
        F4,
        F5,
        F6,
        F7,
        F8,
        F9,
        F10,
        F11,
        F12,
        F13,
        F14,
        F15,
        F16,
        F17,
        F18,
        F19,
        F20,
        F21,
        F22,
        F23,
        F24,
        F25,
        KP_0,
        KP_1,
        KP_2,
        KP_3,
        KP_4,
        KP_5,
        KP_6,
        KP_7,
        KP_8,
        KP_9,
        KP_DECIMAL,
        KP_DIVIDE,
        KP_MULTIPLY,
        KP_SUBTRACT,
        KP_ADD,
        KP_ENTER,
        KP_EQUAL,
        LEFT_SHIFT,
        LEFT_CONTROL,
        LEFT_ALT,
        LEFT_SUPER,
        RIGHT_SHIFT,
        RIGHT_CONTROL,
        RIGHT_ALT,
        RIGHT_SUPER,
        MENU
    }

    /** Returns whether the supplied named keyboard key is currently held. */
    public static boolean isKeyDown(Key key) {
        Objects.requireNonNull(key, "key");
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.keyDown(KineticKeyCodeRuntime.code(key));
    }

    /** Returns whether a raw key code matches the supplied named keyboard key. */
    public static boolean matchesKeyCode(Key key, int keyCode) {
        Objects.requireNonNull(key, "key");
        return KineticKeyCodeRuntime.code(key) == keyCode;
    }

    /**
     * Handles press callbacks dispatched by Kinetic Key Bindings. Runtime callback failures are reported by the
     * core and treated as an unconsumed press so one broken binding does not block later bindings.
     */
    @FunctionalInterface
    public interface PressHandler {
        /**
         * Handles one accepted press.
         *
         * @return {@code true} when the matching GUI input should be consumed; ignored for in-game tick dispatch
         */
        boolean onPressed();
    }

    /**
     * Starts a key-binding definition.
     *
     * @param translationKey translation key used by Minecraft controls and {@link Binding#translationKey()}
     * @return a builder that must be completed with one keyboard or mouse input before registration
     */
    public static Builder builder(String translationKey) {
        return new Builder(translationKey);
    }

    /** Represents one registered Kinetic key binding. */
    public static final class Binding {
        private final long id;
        private final String translationKey;

        private Binding(long id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        /** Returns the translation key used for this registered control. */
        public String translationKey() {
            return translationKey;
        }

        /** Returns whether this entry has already been registered. */
        public boolean isRegistered() {
            return KineticKeyBindingRuntime.isRegistered(id);
        }

        /** Returns whether this key binding is currently held down. */
        public boolean isDown() {
            return KineticKeyBindingRuntime.isDown(id);
        }

        /**
         * Returns Minecraft's display name for the current bound input, or the configured default input before
         * Forge registration has produced a live key mapping.
         */
        public Component translatedKeyMessage() {
            return KineticKeyBindingRuntime.translatedKeyMessage(id);
        }
    }

    /** Collects the complete settings for one key binding before registration. */
    public static final class Builder {
        private final String translationKey;
        private String categoryTranslationKey = "key.categories.misc";
        private Context context = Context.IN_GAME;
        private Modifier modifier = Modifier.NONE;
        private boolean mouseInput;
        private int keyCode;
        private boolean keyConfigured;
        private BooleanSupplier registerWhen = () -> true;
        private BooleanSupplier enabledWhen = () -> true;
        private PressHandler onPressed = () -> false;
        private boolean exactModifiers;

        private Builder(String translationKey) {
            this.translationKey = requireText(translationKey, "translationKey");
        }

        /** Sets the controls-menu category translation key. */
        public Builder category(String categoryTranslationKey) {
            this.categoryTranslationKey = requireText(categoryTranslationKey, "categoryTranslationKey");
            return this;
        }

        /** Sets the input context in which this binding may fire. */
        public Builder context(Context context) {
            this.context = Objects.requireNonNull(context, "context");
            return this;
        }

        /** Sets the required modifier key. */
        public Builder modifier(Modifier modifier) {
            this.modifier = Objects.requireNonNull(modifier, "modifier");
            return this;
        }

        /** Configures this binding to use a named platform-neutral keyboard key. */
        public Builder keyboard(Key key) {
            Objects.requireNonNull(key, "key");
            this.mouseInput = false;
            this.keyCode = KineticKeyCodeRuntime.code(key);
            this.keyConfigured = true;
            return this;
        }

        /** Configures this binding to use the supplied raw keyboard key code. */
        public Builder keyboardKey(int keyCode) {
            this.mouseInput = false;
            this.keyCode = keyCode;
            this.keyConfigured = true;
            return this;
        }

        /** Configures this binding to use the supplied mouse button. */
        public Builder mouseButton(int button) {
            this.mouseInput = true;
            this.keyCode = button;
            this.keyConfigured = true;
            return this;
        }

        /** Sets the condition checked when Minecraft registers key mappings. */
        public Builder registerWhen(BooleanSupplier condition) {
            this.registerWhen = Objects.requireNonNull(condition, "condition");
            return this;
        }

        /**
         * Sets the runtime condition that must be true before the binding can fire or report as held. A failing
         * predicate is reported by the core and treated as disabled for that check.
         */
        public Builder enabledWhen(BooleanSupplier condition) {
            this.enabledWhen = Objects.requireNonNull(condition, "condition");
            return this;
        }

        /** Sets the callback invoked for each accepted key or mouse press. */
        public Builder onPressed(PressHandler handler) {
            this.onPressed = Objects.requireNonNull(handler, "handler");
            return this;
        }

        /**
         * Controls whether no additional modifier keys may be held beyond {@link #modifier(Modifier)}.
         */
        public Builder exactModifiers(boolean exactModifiers) {
            this.exactModifiers = exactModifiers;
            return this;
        }

        /**
         * Registers this completed binding definition and returns its runtime handle.
         *
         * @return the registered binding handle
         * @throws IllegalStateException if no keyboard/mouse input was configured or key registration has finished
         */
        public Binding register() {
            if (!keyConfigured) {
                throw new IllegalStateException("A keyboard key or mouse button must be configured");
            }
            long id = KineticKeyBindingRuntime.register(
                    translationKey,
                    categoryTranslationKey,
                    context,
                    modifier,
                    mouseInput,
                    keyCode,
                    registerWhen,
                    enabledWhen,
                    onPressed,
                    exactModifiers
            );
            return new Binding(id, translationKey);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
