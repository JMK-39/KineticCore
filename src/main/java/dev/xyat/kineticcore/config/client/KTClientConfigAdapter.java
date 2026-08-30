package dev.xyat.kineticcore.config.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dev.xyat.kineticcore.KineticCore;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Adapts a Forge {@link ForgeConfigSpec} registered as
 * {@code ModConfig.Type.CLIENT} to a page in the KT configuration hub.
 *
 * <p>The adapter reads labels, comments, numeric ranges, defaults and restart
 * requirements from the spec itself. It deliberately does not support COMMON
 * or SERVER specs: those need an authenticated server snapshot/save protocol
 * instead of writing the client's local copy.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class KTClientConfigAdapter {
    private KTClientConfigAdapter() {
    }

    /**
     * Creates a page builder populated from a loaded Forge CLIENT spec.
     * Callers may add specialized editor actions before building the page.
     */
    public static KTConfigPage.Builder pageBuilder(
            String pageId,
            Component title,
            ForgeConfigSpec spec
    ) {
        return pageBuilder(pageId, title, spec, ignored -> true);
    }

    /**
     * Creates a page containing only paths accepted by {@code includePath}.
     * Paths use Forge's dot-separated form, for example {@code Hud.enabled}.
     * This is useful when a field has a purpose-built visual editor.
     */
    public static KTConfigPage.Builder pageBuilder(
            String pageId,
            Component title,
            ForgeConfigSpec spec,
            Predicate<String> includePath
    ) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(includePath, "includePath");
        KTConfigPage.Builder page = KTConfigPage.builder(pageId, title)
                .scope(KTConfigScope.CLIENT_LOCAL)
                .applyTiming(inferApplyTiming(spec, includePath))
                .onSave(spec::save);
        appendEntries(page, spec, includePath);
        return page;
    }

    /**
     * Appends all supported values from a Forge CLIENT spec to an existing
     * page builder. Saving remains the caller's responsibility.
     */
    public static KTConfigPage.Builder appendEntries(
            KTConfigPage.Builder page,
            ForgeConfigSpec spec
    ) {
        return appendEntries(page, spec, ignored -> true);
    }

    /** Appends only values whose dot-separated Forge path is accepted. */
    public static KTConfigPage.Builder appendEntries(
            KTConfigPage.Builder page,
            ForgeConfigSpec spec,
            Predicate<String> includePath
    ) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(includePath, "includePath");

        List<SpecEntry> entries = new ArrayList<>();
        collectEntries(spec.getValues(), List.of(), spec, entries);
        entries.removeIf(entry -> !includePath.test(String.join(".", entry.path())));

        List<String> lastSection = null;
        for (SpecEntry entry : entries) {
            List<String> sectionPath = entry.path().subList(0, entry.path().size() - 1);
            if (!sectionPath.isEmpty() && !sectionPath.equals(lastSection)) {
                page.section(sectionLabel(spec, sectionPath));
                Component sectionComment = localizedComment(
                        spec.getLevelTranslationKey(sectionPath),
                        spec.getLevelComment(sectionPath)
                );
                if (sectionComment != null) page.description(sectionComment);
                lastSection = List.copyOf(sectionPath);
            } else if (sectionPath.isEmpty()) {
                lastSection = List.of();
            }
            appendValue(page, entry);
        }
        return page;
    }

    public static KTConfigPage.ApplyTiming inferApplyTiming(ForgeConfigSpec spec) {
        return inferApplyTiming(spec, ignored -> true);
    }

    public static KTConfigPage.ApplyTiming inferApplyTiming(
            ForgeConfigSpec spec,
            Predicate<String> includePath
    ) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(includePath, "includePath");
        List<SpecEntry> entries = new ArrayList<>();
        collectEntries(spec.getValues(), List.of(), spec, entries);
        return entries.stream()
                .filter(entry -> includePath.test(String.join(".", entry.path())))
                .anyMatch(entry -> entry.valueSpec().needsWorldRestart())
                ? KTConfigPage.ApplyTiming.RESTART_GAME
                : KTConfigPage.ApplyTiming.IMMEDIATE;
    }

    private static void collectEntries(
            UnmodifiableConfig values,
            List<String> parentPath,
            ForgeConfigSpec spec,
            List<SpecEntry> result
    ) {
        for (var mapEntry : values.valueMap().entrySet()) {
            List<String> path = new ArrayList<>(parentPath);
            path.add(mapEntry.getKey());
            Object value = mapEntry.getValue();
            if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                Object rawSpec = spec.getSpec().get(path);
                if (rawSpec instanceof ForgeConfigSpec.ValueSpec valueSpec) {
                    result.add(new SpecEntry(List.copyOf(path), configValue, valueSpec));
                }
            } else if (value instanceof UnmodifiableConfig nested) {
                collectEntries(nested, path, spec, result);
            }
        }
    }

    private static void appendValue(KTConfigPage.Builder page, SpecEntry entry) {
        Object defaultValue = entry.configValue().getDefault();
        Component label = valueLabel(entry);
        Component tooltip = localizedComment(
                entry.valueSpec().getTranslationKey(),
                entry.valueSpec().getComment()
        );
        String id = stableEntryId(entry.path());

        if (defaultValue instanceof Boolean value) {
            page.booleanValue(
                    id, label,
                    () -> (Boolean) entry.configValue().get(),
                    next -> set(entry, next),
                    value, tooltip
            );
            return;
        }
        if (defaultValue instanceof Integer value) {
            ForgeConfigSpec.Range<?> range = entry.valueSpec().getRange();
            int minimum = range == null ? Integer.MIN_VALUE : ((Number) range.getMin()).intValue();
            int maximum = range == null ? Integer.MAX_VALUE : ((Number) range.getMax()).intValue();
            page.intValue(
                    id, label,
                    () -> ((Number) entry.configValue().get()).intValue(),
                    next -> set(entry, next),
                    value, minimum, maximum, tooltip
            );
            return;
        }
        if (defaultValue instanceof Long value) {
            ForgeConfigSpec.Range<?> range = entry.valueSpec().getRange();
            long minimum = range == null ? Long.MIN_VALUE : ((Number) range.getMin()).longValue();
            long maximum = range == null ? Long.MAX_VALUE : ((Number) range.getMax()).longValue();
            page.longValue(
                    id, label,
                    () -> ((Number) entry.configValue().get()).longValue(),
                    next -> set(entry, next),
                    value, minimum, maximum, tooltip
            );
            return;
        }
        if (defaultValue instanceof Double value) {
            if (!Double.isFinite(value)) {
                appendUnsupported(page, entry, "non-finite double default");
                return;
            }
            ForgeConfigSpec.Range<?> range = entry.valueSpec().getRange();
            double minimum = finiteBound(range == null ? null : range.getMin(), -Double.MAX_VALUE);
            double maximum = finiteBound(range == null ? null : range.getMax(), Double.MAX_VALUE);
            page.doubleValue(
                    id, label,
                    () -> ((Number) entry.configValue().get()).doubleValue(),
                    next -> set(entry, next),
                    value, minimum, maximum, tooltip
            );
            return;
        }
        if (defaultValue instanceof Float value) {
            if (!Float.isFinite(value)) {
                appendUnsupported(page, entry, "non-finite float default");
                return;
            }
            ForgeConfigSpec.Range<?> range = entry.valueSpec().getRange();
            double minimum = finiteBound(range == null ? null : range.getMin(), -Float.MAX_VALUE);
            double maximum = finiteBound(range == null ? null : range.getMax(), Float.MAX_VALUE);
            page.doubleValue(
                    id, label,
                    () -> ((Number) entry.configValue().get()).doubleValue(),
                    next -> set(entry, next.floatValue()),
                    value.doubleValue(), minimum, maximum, tooltip
            );
            return;
        }
        if (defaultValue instanceof String value) {
            page.stringValue(
                    id, label,
                    () -> String.valueOf(entry.configValue().get()),
                    next -> set(entry, next),
                    value, tooltip
            );
            return;
        }
        if (defaultValue instanceof Enum<?> value) {
            Class<? extends Enum<?>> enumClass = enumClass(value);
            String[] choices = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .toArray(String[]::new);
            page.choice(
                    id, label,
                    () -> ((Enum<?>) entry.configValue().get()).name(),
                    next -> set(entry, enumValue(enumClass, next)),
                    value.name(), tooltip, choices
            );
            return;
        }
        if (defaultValue instanceof List<?> values) {
            appendList(page, entry, id, label, tooltip, values);
            return;
        }

        appendUnsupported(
                page,
                entry,
                defaultValue == null ? "null" : defaultValue.getClass().getName()
        );
    }

    private static void appendList(
            KTConfigPage.Builder page,
            SpecEntry entry,
            String id,
            Component label,
            Component tooltip,
            List<?> defaults
    ) {
        if (defaults.isEmpty()) {
            // Java erases a list's element type. Guessing here could corrupt a
            // valid empty integer list by writing strings (or vice versa).
            appendUnsupported(page, entry, "empty list without an element type hint");
            return;
        }
        if (defaults.stream().allMatch(Integer.class::isInstance)) {
            List<Integer> defaultValues = defaults.stream().map(Integer.class::cast).toList();
            page.intList(
                    id, label,
                    () -> integerList(entry.configValue().get()),
                    next -> set(entry, new ArrayList<>(next)),
                    defaultValues, tooltip
            );
            return;
        }
        if (defaults.stream().allMatch(String.class::isInstance)) {
            List<String> defaultValues = defaults.stream().map(String.class::cast).toList();
            page.stringList(
                    id, label,
                    () -> stringList(entry.configValue().get()),
                    next -> set(entry, new ArrayList<>(next)),
                    defaultValues, tooltip
            );
            return;
        }

        appendUnsupported(page, entry, "unsupported list element type");
    }

    private static void appendUnsupported(
            KTConfigPage.Builder page,
            SpecEntry entry,
            String reason
    ) {
        KineticCore.LOGGER.warn(
                "Skipping unsupported Forge client config value {} ({})",
                String.join(".", entry.path()),
                reason
        );
        page.description(Component.translatable(
                "gui.kineticcore.config.unsupported_field",
                Component.literal(String.join(".", entry.path()))
        ));
    }

    private static List<Integer> integerList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Integer> result = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element instanceof Number number) result.add(number.intValue());
        }
        return result;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(String::valueOf).toList();
    }

    private static Component valueLabel(SpecEntry entry) {
        String translationKey = entry.valueSpec().getTranslationKey();
        return translationKey == null || translationKey.isBlank()
                ? Component.literal(humanize(entry.path().get(entry.path().size() - 1)))
                : Component.translatable(translationKey);
    }

    private static Component sectionLabel(ForgeConfigSpec spec, List<String> path) {
        String translationKey = spec.getLevelTranslationKey(path);
        return translationKey == null || translationKey.isBlank()
                ? Component.literal(humanize(path.get(path.size() - 1)))
                : Component.translatable(translationKey);
    }

    private static Component localizedComment(String translationKey, String rawComment) {
        if (rawComment == null || rawComment.isBlank()) return null;
        if (translationKey == null || translationKey.isBlank()) return null;
        return Component.translatable(translationKey + ".tooltip");
    }

    private static String humanize(String value) {
        String text = value
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .replace('-', ' ')
                .trim();
        if (text.isEmpty()) return value;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String stableEntryId(List<String> path) {
        String joined = String.join(".", path).toLowerCase(Locale.ROOT);
        return "spec_" + HexFormat.of().formatHex(joined.getBytes(StandardCharsets.UTF_8));
    }

    private static double finiteBound(Object value, double fallback) {
        if (!(value instanceof Number number)) return fallback;
        double result = number.doubleValue();
        return Double.isFinite(result) ? result : fallback;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void set(SpecEntry entry, Object next) {
        if (!entry.valueSpec().test(next)) {
            throw new IllegalArgumentException(
                    "Value rejected by Forge config validator: " + String.join(".", entry.path())
            );
        }
        ((ForgeConfigSpec.ConfigValue) entry.configValue()).set(next);
    }

    private static Class<? extends Enum<?>> enumClass(Enum<?> value) {
        return value.getDeclaringClass();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Enum<?> enumValue(Class<? extends Enum<?>> type, String name) {
        return Enum.valueOf((Class) type, name);
    }

    private record SpecEntry(
            List<String> path,
            ForgeConfigSpec.ConfigValue<?> configValue,
            ForgeConfigSpec.ValueSpec valueSpec
    ) {
    }
}
