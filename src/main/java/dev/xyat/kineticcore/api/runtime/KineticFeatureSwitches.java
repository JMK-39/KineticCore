package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.FeatureSwitchRuntime;

import java.util.List;
import java.util.Objects;

/**
 * Startup feature switches: features that can be turned off before the game loads, including their Mixins.
 *
 * <p>Each switch has two values. The active value was read at startup and stays fixed for the whole session,
 * because Mixins cannot be undone at runtime. The configured value is what the player chose for the next launch.
 * All methods are thread-safe.
 */
public final class KineticFeatureSwitches {
    private KineticFeatureSwitches() {
    }

    /**
     * Registers a stable feature ID and its localized section, name and description.
     * Supply both zh_cn and en_us entries for all three translation keys.
     * Keep implementation Mixin class names out of the GUI and saved config.
     */
    public static void register(Descriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        FeatureSwitchRuntime.register(
                descriptor.id(),
                descriptor.sectionId(),
                descriptor.defaultEnabled(),
                descriptor.sectionTranslationKey(),
                descriptor.nameTranslationKey(),
                descriptor.tooltipTranslationKey()
        );
    }

    /**
     * Returns whether the feature is active in this session.
     *
     * @param featureId registered feature id
     * @return the value read at startup, or the default when no saved value exists
     * @throws IllegalArgumentException if the feature is not registered
     */
    public static boolean isEnabled(String featureId) {
        return FeatureSwitchRuntime.isEnabled(featureId);
    }

    /**
     * Returns the value chosen for the next launch.
     *
     * @param featureId registered feature id
     * @throws IllegalArgumentException if the feature is not registered
     */
    public static boolean configuredEnabled(String featureId) {
        return FeatureSwitchRuntime.configuredEnabled(featureId);
    }

    /**
     * Changes the value for the next launch in memory. Call {@link #saveConfigured()} to persist it. The active
     * value is not affected.
     *
     * @param featureId registered feature id
     * @param enabled value to use from the next launch
     * @throws IllegalArgumentException if the feature is not registered
     */
    public static void setConfiguredEnabled(String featureId, boolean enabled) {
        FeatureSwitchRuntime.setConfiguredEnabled(featureId, enabled);
    }

    /** Writes every configured value to the startup feature file. */
    public static void saveConfigured() {
        FeatureSwitchRuntime.saveConfigured();
    }

    /** Returns every registered switch in registration order as an unmodifiable list. */
    public static List<Descriptor> descriptors() {
        return FeatureSwitchRuntime.descriptors().stream()
                .map(definition -> new Descriptor(
                        definition.id(),
                        definition.sectionId(),
                        definition.defaultEnabled(),
                        definition.sectionTranslationKey(),
                        definition.nameTranslationKey(),
                        definition.tooltipTranslationKey()
                ))
                .toList();
    }

    /**
     * Identity and display keys of one startup feature switch.
     *
     * @param id stable feature id saved in the config file; letters, digits and {@code _ . : -}
     * @param sectionId id of the group the switch is listed under; same character rules
     * @param defaultEnabled value used when the config file has no entry
     * @param sectionTranslationKey language key of the group title
     * @param nameTranslationKey language key of the switch name
     * @param tooltipTranslationKey language key of the switch description
     */
    public record Descriptor(
            String id,
            String sectionId,
            boolean defaultEnabled,
            String sectionTranslationKey,
            String nameTranslationKey,
            String tooltipTranslationKey
    ) {
        /**
         * Trims every text field and checks it.
         *
         * @throws NullPointerException if a text field is {@code null}
         * @throws IllegalArgumentException if a field is blank or an id contains unsupported characters
         */
        public Descriptor {
            id = requireIdentifier(id, "id");
            sectionId = requireIdentifier(sectionId, "sectionId");
            sectionTranslationKey = requireText(sectionTranslationKey, "sectionTranslationKey");
            nameTranslationKey = requireText(nameTranslationKey, "nameTranslationKey");
            tooltipTranslationKey = requireText(tooltipTranslationKey, "tooltipTranslationKey");
        }

        private static String requireText(String value, String name) {
            String normalized = Objects.requireNonNull(value, name).trim();
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
    }
}
