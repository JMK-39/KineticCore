package dev.xyat.kineticcore.api.client.input;

import dev.xyat.kineticcore.internal.client.input.KineticKeyBindingRuntime;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class KineticKeyBindings {
    private KineticKeyBindings() {
    }

    public enum Context {
        IN_GAME,
        GUI,
        UNIVERSAL
    }

    public enum Modifier {
        NONE,
        SHIFT,
        CONTROL,
        ALT
    }

    public enum Device {
        KEYBOARD,
        MOUSE
    }

    @FunctionalInterface
    public interface PressHandler {
        boolean onPressed();
    }

    public record Definition(
            String translationKey,
            String categoryTranslationKey,
            Context context,
            Modifier modifier,
            Device device,
            int keyCode,
            BooleanSupplier registerWhen,
            BooleanSupplier enabledWhen,
            PressHandler onPressed,
            boolean exactModifiers
    ) {
        public Definition {
            translationKey = requireText(translationKey, "translationKey");
            categoryTranslationKey = requireText(categoryTranslationKey, "categoryTranslationKey");
            context = Objects.requireNonNull(context, "context");
            modifier = Objects.requireNonNull(modifier, "modifier");
            device = Objects.requireNonNull(device, "device");
            registerWhen = registerWhen == null ? () -> true : registerWhen;
            enabledWhen = enabledWhen == null ? () -> true : enabledWhen;
            onPressed = onPressed == null ? () -> false : onPressed;
        }
    }

    public static Builder builder(String translationKey) {
        return new Builder(translationKey);
    }

    public static Binding register(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        long id = KineticKeyBindingRuntime.register(definition);
        return new Binding(id, definition.translationKey());
    }

    public static final class Binding {
        private final long id;
        private final String translationKey;

        private Binding(long id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        public boolean isRegistered() {
            return KineticKeyBindingRuntime.isRegistered(id);
        }

        public boolean isDown() {
            return KineticKeyBindingRuntime.isDown(id);
        }
    }

    public static final class Builder {
        private final String translationKey;
        private String categoryTranslationKey = "key.categories.misc";
        private Context context = Context.IN_GAME;
        private Modifier modifier = Modifier.NONE;
        private Device device = Device.KEYBOARD;
        private int keyCode;
        private boolean keyConfigured;
        private BooleanSupplier registerWhen = () -> true;
        private BooleanSupplier enabledWhen = () -> true;
        private PressHandler onPressed = () -> false;
        private boolean exactModifiers;

        private Builder(String translationKey) {
            this.translationKey = requireText(translationKey, "translationKey");
        }

        public Builder category(String categoryTranslationKey) {
            this.categoryTranslationKey = requireText(categoryTranslationKey, "categoryTranslationKey");
            return this;
        }

        public Builder context(Context context) {
            this.context = Objects.requireNonNull(context, "context");
            return this;
        }

        public Builder modifier(Modifier modifier) {
            this.modifier = Objects.requireNonNull(modifier, "modifier");
            return this;
        }

        public Builder keyboardKey(int keyCode) {
            this.device = Device.KEYBOARD;
            this.keyCode = keyCode;
            this.keyConfigured = true;
            return this;
        }

        public Builder mouseButton(int button) {
            this.device = Device.MOUSE;
            this.keyCode = button;
            this.keyConfigured = true;
            return this;
        }

        public Builder registerWhen(BooleanSupplier condition) {
            this.registerWhen = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public Builder enabledWhen(BooleanSupplier condition) {
            this.enabledWhen = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public Builder onPressed(PressHandler handler) {
            this.onPressed = Objects.requireNonNull(handler, "handler");
            return this;
        }

        public Builder exactModifiers(boolean exactModifiers) {
            this.exactModifiers = exactModifiers;
            return this;
        }

        public Definition build() {
            if (!keyConfigured) {
                throw new IllegalStateException("A keyboard key or mouse button must be configured");
            }
            return new Definition(
                    translationKey,
                    categoryTranslationKey,
                    context,
                    modifier,
                    device,
                    keyCode,
                    registerWhen,
                    enabledWhen,
                    onPressed,
                    exactModifiers
            );
        }

        public Binding register() {
            return KineticKeyBindings.register(build());
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
