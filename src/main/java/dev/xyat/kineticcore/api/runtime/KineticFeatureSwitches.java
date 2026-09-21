package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.FeatureSwitchRuntime;

import java.util.List;
import java.util.Objects;

/** Public Kinetic API facade for feature switches. */
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
     * Returns whether enabled.
     */
    public static boolean isEnabled(String featureId) {
        return FeatureSwitchRuntime.isEnabled(featureId);
    }

    /**
     * Performs the configured enabled API operation.
     */
    public static boolean configuredEnabled(String featureId) {
        return FeatureSwitchRuntime.configuredEnabled(featureId);
    }

    /**
     * Updates configured enabled.
     */
    public static void setConfiguredEnabled(String featureId, boolean enabled) {
        FeatureSwitchRuntime.setConfiguredEnabled(featureId, enabled);
    }

    /**
     * Performs the save configured API operation.
     */
    public static void saveConfigured() {
        FeatureSwitchRuntime.saveConfigured();
    }

    /**
     * Performs the descriptors API operation.
     */
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

    /** Immutable descriptor data exposed by this API. */
    public record Descriptor(
            String id,
            String sectionId,
            boolean defaultEnabled,
            String sectionTranslationKey,
            String nameTranslationKey,
            String tooltipTranslationKey
    ) {
        /**
         * Validates and normalizes this descriptor value.
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
