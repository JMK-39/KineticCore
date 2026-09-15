package dev.xyat.kineticcore.api.config.common;

import dev.xyat.kineticcore.internal.config.common.KTCommonConfigRuntime;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class KTCommonConfigSpec {
    private final KTCommonConfigRuntime.SpecHandle handle;

    private KTCommonConfigSpec(KTCommonConfigRuntime.SpecHandle handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public static Builder builder() {
        return new Builder(KTCommonConfigRuntime.newBuilder());
    }

    public void save() {
        KTCommonConfigRuntime.save(handle);
    }

    public static class Value<T> {
        private final KTCommonConfigRuntime.ValueHandle<T> handle;

        private Value(KTCommonConfigRuntime.ValueHandle<T> handle) {
            this.handle = Objects.requireNonNull(handle, "handle");
        }

        public T get() {
            return KTCommonConfigRuntime.get(handle);
        }

        public T getDefault() {
            return KTCommonConfigRuntime.getDefault(handle);
        }

        public void set(T value) {
            KTCommonConfigRuntime.set(handle, value);
        }

        public void save() {
            KTCommonConfigRuntime.save(handle);
        }
    }

    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(KTCommonConfigRuntime.ValueHandle<Boolean> handle) {
            super(handle);
        }
    }

    public static final class IntValue extends Value<Integer> {
        private IntValue(KTCommonConfigRuntime.ValueHandle<Integer> handle) {
            super(handle);
        }
    }

    public static final class DoubleValue extends Value<Double> {
        private DoubleValue(KTCommonConfigRuntime.ValueHandle<Double> handle) {
            super(handle);
        }
    }

    public static final class StringValue extends Value<String> {
        private StringValue(KTCommonConfigRuntime.ValueHandle<String> handle) {
            super(handle);
        }
    }

    public static final class StringListValue extends Value<List<String>> {
        private StringListValue(KTCommonConfigRuntime.ValueHandle<List<String>> handle) {
            super(handle);
        }
    }

    public static final class Builder {
        private final KTCommonConfigRuntime.BuilderHandle handle;

        private Builder(KTCommonConfigRuntime.BuilderHandle handle) {
            this.handle = Objects.requireNonNull(handle, "handle");
        }

        public Builder comment(String... comments) {
            KTCommonConfigRuntime.comment(handle, comments);
            return this;
        }

        public Builder translation(String translationKey) {
            KTCommonConfigRuntime.translation(handle, translationKey);
            return this;
        }

        public Builder push(String path) {
            KTCommonConfigRuntime.push(handle, path);
            return this;
        }

        public Builder pop() {
            KTCommonConfigRuntime.pop(handle);
            return this;
        }

        public BooleanValue define(String path, boolean defaultValue) {
            return new BooleanValue(KTCommonConfigRuntime.defineBoolean(handle, path, defaultValue));
        }

        public IntValue defineInRange(String path, int defaultValue, int minimum, int maximum) {
            return new IntValue(KTCommonConfigRuntime.defineInt(handle, path, defaultValue, minimum, maximum));
        }

        public DoubleValue defineInRange(String path, double defaultValue, double minimum, double maximum) {
            return new DoubleValue(KTCommonConfigRuntime.defineDouble(handle, path, defaultValue, minimum, maximum));
        }

        public StringValue define(String path, String defaultValue) {
            return new StringValue(KTCommonConfigRuntime.defineString(handle, path, defaultValue, value -> true));
        }

        public StringValue define(String path, String defaultValue, Predicate<Object> validator) {
            return new StringValue(KTCommonConfigRuntime.defineString(
                    handle, path, defaultValue, Objects.requireNonNull(validator, "validator")
            ));
        }

        public StringListValue defineList(String path, List<String> defaultValue, Predicate<Object> elementValidator) {
            return new StringListValue(KTCommonConfigRuntime.defineStringList(
                    handle,
                    path,
                    List.copyOf(Objects.requireNonNull(defaultValue, "defaultValue")),
                    Objects.requireNonNull(elementValidator, "elementValidator")
            ));
        }

        public KTCommonConfigSpec build() {
            return new KTCommonConfigSpec(KTCommonConfigRuntime.build(handle));
        }
    }

    KTCommonConfigRuntime.SpecHandle internalHandle() {
        return handle;
    }
}
