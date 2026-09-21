package dev.xyat.kineticcore.internal.config.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class KTCommonConfigRuntime {
    private KTCommonConfigRuntime() {
    }

    public static BuilderHandle newBuilder() {
        return new BuilderHandle(new ForgeConfigSpec.Builder());
    }

    public static void comment(BuilderHandle handle, String... comments) {
        builder(handle).comment(comments);
    }

    public static void translation(BuilderHandle handle, String translationKey) {
        builder(handle).translation(Objects.requireNonNull(translationKey, "translationKey"));
    }

    public static void push(BuilderHandle handle, String path) {
        builder(handle).push(Objects.requireNonNull(path, "path"));
    }

    public static void pop(BuilderHandle handle) {
        builder(handle).pop();
    }

    public static ValueHandle<Boolean> defineBoolean(BuilderHandle handle, String path, boolean defaultValue) {
        return new ValueHandle<>(builder(handle).define(path, defaultValue));
    }

    public static ValueHandle<Integer> defineInt(
            BuilderHandle handle, String path, int defaultValue, int minimum, int maximum
    ) {
        return new ValueHandle<>(builder(handle).defineInRange(path, defaultValue, minimum, maximum));
    }

    public static ValueHandle<Double> defineDouble(
            BuilderHandle handle, String path, double defaultValue, double minimum, double maximum
    ) {
        return new ValueHandle<>(builder(handle).defineInRange(path, defaultValue, minimum, maximum));
    }

    public static ValueHandle<String> defineString(
            BuilderHandle handle, String path, String defaultValue, Predicate<Object> validator
    ) {
        return new ValueHandle<>(builder(handle).define(path, defaultValue, validator));
    }

    public static ValueHandle<List<String>> defineStringList(
            BuilderHandle handle, String path, List<String> defaultValue, Predicate<Object> elementValidator
    ) {
        ForgeConfigSpec.ConfigValue<List<? extends String>> value = builder(handle).defineList(
                path, defaultValue, elementValidator
        );
        return new ValueHandle<>(new StringListAdapter(value));
    }

    public static SpecHandle build(BuilderHandle handle) {
        Objects.requireNonNull(handle, "handle");
        if (handle.built) {
            throw new IllegalStateException("Config builder has already been built");
        }
        ForgeConfigSpec builtSpec = builder(handle).build();
        handle.built = true;
        return new SpecHandle(builtSpec);
    }

    public static void registerCommon(SpecHandle handle, String fileName) {
        SpecHandle safeHandle = Objects.requireNonNull(handle, "handle");
        String normalized = Objects.requireNonNull(fileName, "fileName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        synchronized (safeHandle) {
            if (safeHandle.registeredFile != null) {
                if (safeHandle.registeredFile.equals(normalized)) {
                    return;
                }
                throw new IllegalStateException(
                        "Common config spec already registered as " + safeHandle.registeredFile
                );
            }
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, safeHandle.spec, normalized);
            safeHandle.registeredFile = normalized;
        }
    }

    public static boolean isLoaded(SpecHandle handle) {
        return spec(handle).isLoaded();
    }

    public static <T> T get(ValueHandle<T> handle) {
        return value(handle).get();
    }

    public static <T> T getDefault(ValueHandle<T> handle) {
        return value(handle).getDefault();
    }

    public static <T> void set(ValueHandle<T> handle, T value) {
        value(handle).set(value);
    }

    public static <T> void save(ValueHandle<T> handle) {
        value(handle).save();
    }

    public static void save(SpecHandle handle) {
        spec(handle).save();
    }

    private static ForgeConfigSpec.Builder builder(BuilderHandle handle) {
        Objects.requireNonNull(handle, "handle");
        if (handle.built) {
            throw new IllegalStateException("Config builder has already been built");
        }
        return handle.builder;
    }

    private static ForgeConfigSpec spec(SpecHandle handle) {
        return Objects.requireNonNull(handle, "handle").spec;
    }

    private static <T> ConfigValueAdapter<T> value(ValueHandle<T> handle) {
        return Objects.requireNonNull(handle, "handle").adapter;
    }

    public static final class BuilderHandle {
        private final ForgeConfigSpec.Builder builder;
        private boolean built;

        private BuilderHandle(ForgeConfigSpec.Builder builder) {
            this.builder = builder;
        }
    }

    public static final class SpecHandle {
        private final ForgeConfigSpec spec;
        private String registeredFile;

        private SpecHandle(ForgeConfigSpec spec) {
            this.spec = spec;
        }
    }

    public static final class ValueHandle<T> {
        private final ConfigValueAdapter<T> adapter;

        private ValueHandle(ForgeConfigSpec.ConfigValue<T> value) {
            this.adapter = new DirectAdapter<>(value);
        }

        private ValueHandle(ConfigValueAdapter<T> adapter) {
            this.adapter = adapter;
        }
    }

    private interface ConfigValueAdapter<T> {
        T get();

        T getDefault();

        void set(T value);

        void save();
    }

    private record DirectAdapter<T>(ForgeConfigSpec.ConfigValue<T> value) implements ConfigValueAdapter<T> {
        @Override
        public T get() {
            return value.get();
        }

        @Override
        public T getDefault() {
            return value.getDefault();
        }

        @Override
        public void set(T newValue) {
            value.set(newValue);
        }

        @Override
        public void save() {
            value.save();
        }
    }

    private record StringListAdapter(
            ForgeConfigSpec.ConfigValue<List<? extends String>> value
    ) implements ConfigValueAdapter<List<String>> {
        @Override
        public List<String> get() {
            return copy(value.get());
        }

        @Override
        public List<String> getDefault() {
            return copy(value.getDefault());
        }

        @Override
        public void set(List<String> newValue) {
            value.set(List.copyOf(newValue));
        }

        @Override
        public void save() {
            value.save();
        }

        private static List<String> copy(List<? extends String> source) {
            return source == null ? List.of() : List.copyOf(new ArrayList<>(source));
        }
    }
}
