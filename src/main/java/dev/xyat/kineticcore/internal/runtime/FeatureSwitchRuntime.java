package dev.xyat.kineticcore.internal.runtime;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeatureSwitchRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_PATH = Paths.get("config", "kineticcore", "startup_features.toml");
    private static final List<Definition> CORE_DEFINITIONS = createDefinitions();
    private static final Map<String, Definition> EXTERNAL_DEFINITIONS = new LinkedHashMap<>();
    private static final Map<String, Boolean> ACTIVE = new LinkedHashMap<>();
    private static final Map<String, Boolean> CONFIGURED = new LinkedHashMap<>();
    private static final Map<String, Boolean> LOADED_VALUES = new LinkedHashMap<>();
    private static boolean initialized;

    private FeatureSwitchRuntime() {
    }

    public static synchronized void register(
            String id,
            String sectionId,
            boolean defaultEnabled,
            String sectionTranslationKey,
            String nameTranslationKey,
            String tooltipTranslationKey
    ) {
        Definition definition = new Definition(
                requireIdentifier(id, "id"),
                requireIdentifier(sectionId, "sectionId"),
                defaultEnabled,
                requireText(sectionTranslationKey, "sectionTranslationKey"),
                requireText(nameTranslationKey, "nameTranslationKey"),
                requireText(tooltipTranslationKey, "tooltipTranslationKey"),
                List.of()
        );

        Definition existing = findDefinition(definition.id());
        if (existing != null) {
            if (samePublicDescriptor(existing, definition)) return;
            throw new IllegalStateException("Startup feature is already registered: " + definition.id());
        }

        EXTERNAL_DEFINITIONS.put(definition.id(), definition);
        if (initialized) {
            boolean active = LOADED_VALUES.getOrDefault(definition.id(), definition.defaultEnabled());
            ACTIVE.put(definition.id(), active);
            CONFIGURED.put(definition.id(), active);
            try {
                save(CONFIGURED);
            } catch (RuntimeException | Error failure) {
                // A failed persistence must not consume an addon's feature ID or
                // publish a setting which never reached the on-disk config.
                CONFIGURED.remove(definition.id());
                ACTIVE.remove(definition.id());
                EXTERNAL_DEFINITIONS.remove(definition.id());
                throw failure;
            }
        }
    }

    private static List<Definition> definitions() {
        if (EXTERNAL_DEFINITIONS.isEmpty()) return CORE_DEFINITIONS;
        List<Definition> result = new ArrayList<>(CORE_DEFINITIONS.size() + EXTERNAL_DEFINITIONS.size());
        result.addAll(CORE_DEFINITIONS);
        result.addAll(EXTERNAL_DEFINITIONS.values());
        return result;
    }

    private static Definition findDefinition(String featureId) {
        for (Definition definition : definitions()) {
            if (definition.id().equals(featureId)) return definition;
        }
        return null;
    }

    private static boolean samePublicDescriptor(Definition first, Definition second) {
        return first.id().equals(second.id())
                && first.sectionId().equals(second.sectionId())
                && first.defaultEnabled() == second.defaultEnabled()
                && first.sectionTranslationKey().equals(second.sectionTranslationKey())
                && first.nameTranslationKey().equals(second.nameTranslationKey())
                && first.tooltipTranslationKey().equals(second.tooltipTranslationKey());
    }

    private static String requireText(String value, String name) {
        if (value == null) throw new NullPointerException(name);
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank");
        return normalized;
    }

    private static String requireIdentifier(String value, String name) {
        String normalized = requireText(value, name);
        if (!normalized.matches("[A-Za-z0-9_.:-]+")) {
            throw new IllegalArgumentException(name + " contains unsupported characters: " + normalized);
        }
        return normalized;
    }

    public static synchronized void initialize() {
        if (initialized) return;

        for (Definition definition : definitions()) {
            ACTIVE.put(definition.id(), definition.defaultEnabled());
        }

        Map<String, Boolean> loaded = Files.exists(CONFIG_PATH)
                ? readConfig(CONFIG_PATH)
                : new LinkedHashMap<>();
        LOADED_VALUES.clear();
        LOADED_VALUES.putAll(loaded);

        for (Definition definition : definitions()) {
            Boolean value = loaded.get(definition.id());
            if (value != null) {
                ACTIVE.put(definition.id(), value);
            }
        }

        CONFIGURED.clear();
        CONFIGURED.putAll(ACTIVE);
        save(CONFIGURED);
        initialized = true;
    }

    public static boolean isEnabled(String featureId) {
        ensureInitialized();
        Definition definition = definition(featureId);
        return ACTIVE.getOrDefault(featureId, definition.defaultEnabled());
    }

    public static boolean configuredEnabled(String featureId) {
        ensureInitialized();
        Definition definition = definition(featureId);
        return CONFIGURED.getOrDefault(featureId, definition.defaultEnabled());
    }

    public static synchronized void setConfiguredEnabled(String featureId, boolean enabled) {
        ensureInitialized();
        definition(featureId);
        CONFIGURED.put(featureId, enabled);
    }

    public static synchronized void saveConfigured() {
        ensureInitialized();
        save(CONFIGURED);
    }

    public static List<Definition> descriptors() {
        ensureInitialized();
        return List.copyOf(definitions());
    }

    public static boolean shouldApplyMixin(String mixinClassName) {
        ensureInitialized();
        String topLevelSimpleName = topLevelSimpleName(mixinClassName);
        for (Definition definition : definitions()) {
            if (definition.mixinTopLevelNames().contains(topLevelSimpleName)) {
                return ACTIVE.getOrDefault(definition.id(), definition.defaultEnabled());
            }
        }
        return true;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    private static Definition definition(String featureId) {
        Definition definition = findDefinition(featureId);
        if (definition != null) return definition;
        throw new IllegalArgumentException("Unknown startup feature: " + featureId);
    }

    private static String topLevelSimpleName(String mixinClassName) {
        int packageSeparator = mixinClassName.lastIndexOf('.');
        String simple = packageSeparator >= 0 ? mixinClassName.substring(packageSeparator + 1) : mixinClassName;
        int innerSeparator = simple.indexOf('$');
        return innerSeparator >= 0 ? simple.substring(0, innerSeparator) : simple;
    }

    private static Map<String, Boolean> readConfig(Path path) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String rawValue = parts[1].split("#", 2)[0].trim();
                    // Boolean.parseBoolean silently interprets every typo as false.
                    // Invalid values must preserve the feature's declared default.
                    if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false")) {
                        result.put(parts[0].trim(), Boolean.parseBoolean(rawValue));
                    }
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to read startup feature config {}", path, exception);
        }
        return result;
    }

    private static void save(Map<String, Boolean> values) {
        // Keep the last successfully persisted snapshot intact if writing fails.
        Map<String, Boolean> nextLoadedValues = new LinkedHashMap<>(LOADED_VALUES);
        nextLoadedValues.putAll(values);
        Path temporaryFile = null;
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, "startup_features-", ".tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                writer.write("# KineticCore startup feature switches");
                writer.newLine();
                writer.write("# Stable feature IDs are used here. Restart the game/server after changing a value.");
                writer.newLine();

                String previousSection = null;
                for (Definition definition : definitions()) {
                    if (!definition.sectionTranslationKey().equals(previousSection)) {
                        writer.newLine();
                        writer.write("# [" + definition.sectionId() + "]");
                        writer.newLine();
                        previousSection = definition.sectionTranslationKey();
                    }
                    writer.write(definition.id() + " = " + values.getOrDefault(definition.id(), definition.defaultEnabled()));
                    writer.newLine();
                }

                List<String> registeredIds = definitions().stream().map(Definition::id).toList();
                boolean wroteUnknownHeader = false;
                for (Map.Entry<String, Boolean> entry : nextLoadedValues.entrySet()) {
                    if (registeredIds.contains(entry.getKey())) continue;
                    if (!wroteUnknownHeader) {
                        writer.newLine();
                        writer.write("# [unregistered]");
                        writer.newLine();
                        wroteUnknownHeader = true;
                    }
                    writer.write(entry.getKey() + " = " + entry.getValue());
                    writer.newLine();
                }
            }

            try {
                Files.move(temporaryFile, CONFIG_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            temporaryFile = null;
            LOADED_VALUES.clear();
            LOADED_VALUES.putAll(nextLoadedValues);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save startup feature config", exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException exception) {
                    LOGGER.warn("Failed to remove startup feature temp file {}", temporaryFile, exception);
                }
            }
        }
    }

    private static List<Definition> createDefinitions() {
        List<Definition> definitions = new ArrayList<>();
        definitions.add(def("movement.flight_server", "movement", true,
                List.of("FlightServerMixins")));
        definitions.add(def("movement.flight_client", "movement", true,
                List.of("FlightClientMixins")));
        definitions.add(def("player.crawling", "movement", true,
                List.of("PlayerCrawlPoseMixin")));

        definitions.add(def("world.spawn_override", "world", true,
                List.of("SetSpawnMixins")));
        definitions.add(def("world.management", "world", true,
                List.of("WorldManagementMixins")));

        definitions.add(def("entity.bee_logic", "entity", true,
                List.of("BeeMixins")));
        definitions.add(def("entity.bee_render", "entity", true,
                List.of("BeeRendererMixin")));
        definitions.add(def("entity.despawn_rules", "entity", true,
                List.of("MobDespawnMixins")));

        definitions.add(def("vanilla.recipe_book_server", "vanilla", true,
                List.of("RecipeBookServerMixins")));
        definitions.add(def("vanilla.recipe_book_client", "vanilla", true,
                List.of("RecipeBookClientMixins", "ButtonAccess")));
        definitions.add(def("attributes.range_limits", "vanilla", true,
                List.of("RangedAttributeAccessor")));

        definitions.add(def("monitoring.tps", "monitoring", true,
                List.of("ServerMixin")));

        definitions.add(def("performance.damage_indicator_particles", "performance", true,
                List.of("ServerLevelMixin")));
        definitions.add(def("performance.gpu_cleanup", "performance", true,
                List.of("RenderTargetMixin")));

        definitions.add(def("client.copy_item_container_access", "client", true,
                List.of("AbstractContainerScreenAccessor")));
        definitions.add(def("client.default_options", "client", true,
                List.of()));
        definitions.add(def("client.interface_automation", "client", true,
                List.of("ClientInterfaceMixins")));
        definitions.add(def("client.status_effect_hud", "client", true,
                List.of("MiniEffectsMixins", "LivingEntityAccessor")));

        definitions.add(def("network.protocol_limits", "network", true,
                List.of("NetworkLimitMixins")));
        return List.copyOf(definitions);
    }

    private static Definition def(
            String id,
            String sectionId,
            boolean defaultEnabled,
            List<String> mixinTopLevelNames
    ) {
        String base = "cfg.kineticcore.startup_features.entry." + id;
        return new Definition(
                id,
                sectionId,
                defaultEnabled,
                "cfg.kineticcore.startup_features.section." + sectionId,
                base + ".name",
                base + ".tooltip",
                List.copyOf(mixinTopLevelNames)
        );
    }

    public record Definition(
            String id,
            String sectionId,
            boolean defaultEnabled,
            String sectionTranslationKey,
            String nameTranslationKey,
            String tooltipTranslationKey,
            List<String> mixinTopLevelNames
    ) {
    }
}
