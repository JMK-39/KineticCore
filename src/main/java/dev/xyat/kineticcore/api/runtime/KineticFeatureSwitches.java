package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.FeatureSwitchRuntime;

import java.util.List;
import java.util.Objects;

public final class KineticFeatureSwitches {
    private KineticFeatureSwitches() {
    }

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

    public static void register(
            String id,
            String sectionId,
            boolean defaultEnabled,
            String sectionTranslationKey,
            String nameTranslationKey,
            String tooltipTranslationKey
    ) {
        register(new Descriptor(
                id,
                sectionId,
                defaultEnabled,
                sectionTranslationKey,
                nameTranslationKey,
                tooltipTranslationKey
        ));
    }

    public static boolean isEnabled(String featureId) {
        return FeatureSwitchRuntime.isEnabled(featureId);
    }

    public static boolean configuredEnabled(String featureId) {
        return FeatureSwitchRuntime.configuredEnabled(featureId);
    }

    public static void setConfiguredEnabled(String featureId, boolean enabled) {
        FeatureSwitchRuntime.setConfiguredEnabled(featureId, enabled);
    }

    public static void saveConfigured() {
        FeatureSwitchRuntime.saveConfigured();
    }

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

    public record Descriptor(
            String id,
            String sectionId,
            boolean defaultEnabled,
            String sectionTranslationKey,
            String nameTranslationKey,
            String tooltipTranslationKey
    ) {
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
