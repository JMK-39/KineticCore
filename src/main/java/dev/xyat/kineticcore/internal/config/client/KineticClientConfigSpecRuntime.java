package dev.xyat.kineticcore.internal.config.client;

import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public final class KineticClientConfigSpecRuntime {
    private static final Map<KTClientConfigSpec, ForgeConfigSpec> SPECS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<KTClientConfigSpec.Value<?>, ForgeConfigSpec.ConfigValue<?>> VALUES =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<KTClientConfigSpec, String> REGISTERED_FILES =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private KineticClientConfigSpecRuntime() {
    }

    public static ForgeConfigSpec nativeSpec(KTClientConfigSpec spec) {
        Objects.requireNonNull(spec, "spec");
        ForgeConfigSpec existing = SPECS.get(spec);
        if (existing != null) return existing;
        synchronized (SPECS) {
            existing = SPECS.get(spec);
            if (existing != null) return existing;
            ForgeConfigSpec built = build(spec);
            SPECS.put(spec, built);
            return built;
        }
    }

    public static void register(KTClientConfigSpec spec, String fileName) {
        Objects.requireNonNull(spec, "spec");
        String normalized = Objects.requireNonNull(fileName, "fileName").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("fileName cannot be blank");
        String existing = REGISTERED_FILES.get(spec);
        if (existing != null) {
            if (!existing.equals(normalized)) {
                throw new IllegalStateException("Client config spec already registered as " + existing);
            }
            return;
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, nativeSpec(spec), normalized);
        REGISTERED_FILES.put(spec, normalized);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(KTClientConfigSpec.Value<T> value, T fallback) {
        ForgeConfigSpec.ConfigValue<?> nativeValue = VALUES.get(value);
        return nativeValue == null ? fallback : (T) nativeValue.get();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> void set(KTClientConfigSpec.Value<T> value, T next) {
        ForgeConfigSpec.ConfigValue nativeValue = VALUES.get(value);
        if (nativeValue != null) nativeValue.set(next);
    }

    public static void save(KTClientConfigSpec spec) {
        ForgeConfigSpec nativeSpec = SPECS.get(spec);
        if (nativeSpec != null) nativeSpec.save();
    }

    private static ForgeConfigSpec build(KTClientConfigSpec spec) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        for (KTClientConfigSpec.Operation operation : spec.operations()) {
            if (operation instanceof KTClientConfigSpec.SectionStart section) {
                applyMetadata(builder, section.comments(), section.translationKey());
                builder.push(section.name());
                continue;
            }
            if (operation instanceof KTClientConfigSpec.SectionEnd) {
                builder.pop();
                continue;
            }
            if (operation instanceof KTClientConfigSpec.EntryDefinition entry) {
                applyMetadata(builder, entry.comments(), entry.translationKey());
                ForgeConfigSpec.ConfigValue<?> nativeValue = define(builder, entry);
                VALUES.put(entry.value(), nativeValue);
            }
        }
        return builder.build();
    }

    private static void applyMetadata(ForgeConfigSpec.Builder builder, java.util.List<String> comments, String translationKey) {
        if (comments != null && !comments.isEmpty()) {
            builder.comment(comments.toArray(String[]::new));
        }
        if (translationKey != null && !translationKey.isBlank()) {
            builder.translation(translationKey);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ForgeConfigSpec.ConfigValue<?> define(
            ForgeConfigSpec.Builder builder,
            KTClientConfigSpec.EntryDefinition entry
    ) {
        Object defaultValue = entry.value().defaultValue();
        return switch (entry.type()) {
            case BOOLEAN -> builder.define(entry.name(), (Boolean) defaultValue);
            case INTEGER -> builder.defineInRange(
                    entry.name(),
                    ((Number) defaultValue).intValue(),
                    entry.minimum().intValue(),
                    entry.maximum().intValue()
            );
            case LONG -> builder.defineInRange(
                    entry.name(),
                    ((Number) defaultValue).longValue(),
                    entry.minimum().longValue(),
                    entry.maximum().longValue()
            );
            case DOUBLE -> {
                if (entry.minimum() != null && entry.maximum() != null) {
                    yield builder.defineInRange(
                            entry.name(),
                            ((Number) defaultValue).doubleValue(),
                            entry.minimum().doubleValue(),
                            entry.maximum().doubleValue()
                    );
                }
                yield builder.define(entry.name(), defaultValue, entry.validator());
            }
            case STRING -> builder.define(entry.name(), defaultValue, entry.validator());
            case ENUM -> builder.defineEnum(entry.name(), (Enum) defaultValue);
        };
    }
}
