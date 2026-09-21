package dev.xyat.kineticcore.api.config.common;

import dev.xyat.kineticcore.internal.config.common.KTCommonConfigRuntime;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Public common-config specification backed by KineticCore's configuration runtime.
 */
public final class KTCommonConfigSpec {
    private final KTCommonConfigRuntime.SpecHandle handle;

    private KTCommonConfigSpec(KTCommonConfigRuntime.SpecHandle handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    /**
     * Creates a new common-config builder.
     */
    public static Builder builder() {
        return new Builder(KTCommonConfigRuntime.newBuilder());
    }

    /**
     * Returns whether Forge has attached the backing configuration data to this specification.
     */
    public boolean isLoaded() {
        return KTCommonConfigRuntime.isLoaded(handle);
    }

    /**
     * Saves this configuration specification.
     */
    public void save() {
        KTCommonConfigRuntime.save(handle);
    }

    /**
     * Base wrapper for a value stored in the common configuration.
     */
    public static class Value<T> {
        private final KTCommonConfigRuntime.ValueHandle<T> handle;

        private Value(KTCommonConfigRuntime.ValueHandle<T> handle) {
            this.handle = Objects.requireNonNull(handle, "handle");
        }

        /**
         * Returns the current value.
         */
        public T get() {
            return KTCommonConfigRuntime.get(handle);
        }

        /**
         * Returns the configured default value.
         */
        public T getDefault() {
            return KTCommonConfigRuntime.getDefault(handle);
        }

        /**
         * Replaces the current value.
         */
        public void set(T value) {
            KTCommonConfigRuntime.set(handle, value);
        }

        /**
         * Saves this value to its backing configuration.
         */
        public void save() {
            KTCommonConfigRuntime.save(handle);
        }
    }

    /**
     * Boolean common-config value.
     */
    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(KTCommonConfigRuntime.ValueHandle<Boolean> handle) {
            super(handle);
        }
    }

    /**
     * Integer common-config value.
     */
    public static final class IntValue extends Value<Integer> {
        private IntValue(KTCommonConfigRuntime.ValueHandle<Integer> handle) {
            super(handle);
        }
    }

    /**
     * Double common-config value.
     */
    public static final class DoubleValue extends Value<Double> {
        private DoubleValue(KTCommonConfigRuntime.ValueHandle<Double> handle) {
            super(handle);
        }
    }

    /**
     * String common-config value.
     */
    public static final class StringValue extends Value<String> {
        private StringValue(KTCommonConfigRuntime.ValueHandle<String> handle) {
            super(handle);
        }
    }

    /**
     * String-list common-config value.
     */
    public static final class StringListValue extends Value<List<String>> {
        private StringListValue(KTCommonConfigRuntime.ValueHandle<List<String>> handle) {
            super(handle);
        }
    }

    /**
     * Builder for common configuration values and sections.
     */
    public static final class Builder {
        private final KTCommonConfigRuntime.BuilderHandle handle;

        private Builder(KTCommonConfigRuntime.BuilderHandle handle) {
            this.handle = Objects.requireNonNull(handle, "handle");
        }

        /**
         * Adds comments to the next configuration entry or section.
         */
        public Builder comment(String... comments) {
            KTCommonConfigRuntime.comment(handle, comments);
            return this;
        }

        /**
         * Assigns a translation key to the next configuration entry or section.
         */
        public Builder translation(String translationKey) {
            KTCommonConfigRuntime.translation(handle, translationKey);
            return this;
        }

        /**
         * Enters a nested configuration section.
         */
        public Builder push(String path) {
            KTCommonConfigRuntime.push(handle, path);
            return this;
        }

        /**
         * Leaves the current configuration section.
         */
        public Builder pop() {
            KTCommonConfigRuntime.pop(handle);
            return this;
        }

        /**
         * Defines a boolean value.
         */
        public BooleanValue defineBoolean(String path, boolean defaultValue) {
            return new BooleanValue(KTCommonConfigRuntime.defineBoolean(handle, path, defaultValue));
        }

        /**
         * Defines a boolean value. This convenience overload mirrors the familiar config-builder naming
         * while keeping the Forge implementation hidden behind KineticCore.
         */
        public BooleanValue define(String path, boolean defaultValue) {
            return defineBoolean(path, defaultValue);
        }

        /**
         * Defines an integer value with explicit inclusive bounds.
         */
        public IntValue defineInt(String path, int defaultValue, int minimum, int maximum) {
            return new IntValue(KTCommonConfigRuntime.defineInt(handle, path, defaultValue, minimum, maximum));
        }

        /** Defines an integer value with explicit inclusive bounds. */
        public IntValue defineInRange(String path, int defaultValue, int minimum, int maximum) {
            return defineInt(path, defaultValue, minimum, maximum);
        }

        /**
         * Defines a double value with explicit inclusive bounds.
         */
        public DoubleValue defineDouble(String path, double defaultValue, double minimum, double maximum) {
            return new DoubleValue(KTCommonConfigRuntime.defineDouble(handle, path, defaultValue, minimum, maximum));
        }

        /** Defines a double value with explicit inclusive bounds. */
        public DoubleValue defineInRange(String path, double defaultValue, double minimum, double maximum) {
            return defineDouble(path, defaultValue, minimum, maximum);
        }

        /**
         * Defines a string value validated by the supplied predicate.
         */
        public StringValue defineString(String path, String defaultValue, Predicate<Object> validator) {
            return new StringValue(KTCommonConfigRuntime.defineString(
                    handle, path, defaultValue, Objects.requireNonNull(validator, "validator")
            ));
        }

        /** Defines an unrestricted string value. */
        public StringValue define(String path, String defaultValue) {
            return defineString(path, defaultValue, value -> value instanceof String);
        }

        /** Defines a string value validated by the supplied predicate. */
        public StringValue define(String path, String defaultValue, Predicate<Object> validator) {
            return defineString(path, defaultValue, validator);
        }

        /**
         * Defines a list of strings whose elements are validated by the supplied predicate.
         */
        public StringListValue defineStringList(
                String path,
                List<String> defaultValue,
                Predicate<Object> elementValidator
        ) {
            return new StringListValue(KTCommonConfigRuntime.defineStringList(
                    handle,
                    path,
                    List.copyOf(Objects.requireNonNull(defaultValue, "defaultValue")),
                    Objects.requireNonNull(elementValidator, "elementValidator")
            ));
        }

        /** Defines a string list whose elements are validated by the supplied predicate. */
        public StringListValue defineList(
                String path,
                List<String> defaultValue,
                Predicate<Object> elementValidator
        ) {
            return defineStringList(path, defaultValue, elementValidator);
        }

        /**
         * Builds the common configuration specification.
         */
        public KTCommonConfigSpec build() {
            return new KTCommonConfigSpec(KTCommonConfigRuntime.build(handle));
        }
    }

    KTCommonConfigRuntime.SpecHandle internalHandle() {
        return handle;
    }
}
