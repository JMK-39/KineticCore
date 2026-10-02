package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.api.config.common.KineticConfigNumbers;
import dev.xyat.kineticcore.internal.config.client.KineticClientConfigSpecRuntime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Loader-independent description of a local client configuration.
 *
 * <p>Add-ons declare values through {@link #builder()}, register the result once with
 * {@link KTClientConfigAdapter#registerSpec(KTClientConfigSpec, String)} during mod construction, and keep the
 * returned {@link Value} handles to read and write settings. KineticCore owns the underlying config-loader
 * implementation, file persistence and config-screen adaptation.
 *
 * <p>A built spec is immutable. {@link Value#get()} is safe to call from any thread; writes and {@link #save()} are
 * intended for the client thread.
 */
public final class KTClientConfigSpec {
    private final List<Operation> operations;

    private KTClientConfigSpec(List<Operation> operations) {
        this.operations = List.copyOf(operations);
    }

    /**
     * Starts a new client config definition.
     *
     * @return an empty builder at the root section
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the declaration steps in order: section starts, entries and section ends. Used by KineticCore to
     * build the native config and the config page; add-ons rarely need it.
     *
     * @return an unmodifiable list of operations
     */
    public List<Operation> operations() {
        return operations;
    }

    /**
     * Writes the current values of this spec to its config file.
     *
     * <p>Does nothing until the spec has been registered or adapted into a page, because no file exists before
     * that.
     */
    public void save() {
        KineticClientConfigSpecRuntime.save(this);
    }

    /** One declaration step recorded by {@link Builder}. */
    public sealed interface Operation permits SectionStart, SectionEnd, EntryDefinition {
    }

    /**
     * Opens a named section; every entry until the matching {@link SectionEnd} is nested under it.
     *
     * @param name section key used in the file and in dot-separated paths
     * @param translationKey optional language key for the section title; empty when unset
     * @param comments file comments written above the section; empty when unset
     */
    public record SectionStart(
            String name,
            String translationKey,
            List<String> comments
    ) implements Operation {
        /**
         * Normalizes optional metadata: a {@code null} translation key becomes empty and comments are copied.
         *
         * @throws NullPointerException if {@code name} is {@code null}
         */
        public SectionStart {
            Objects.requireNonNull(name, "name");
            translationKey = translationKey == null ? "" : translationKey;
            comments = comments == null ? List.of() : List.copyOf(comments);
        }
    }

    /** Closes the most recently opened {@link SectionStart}. */
    public record SectionEnd() implements Operation {
    }

    /** Storage type of a declared value; decides the file format and the editor shown in the config screen. */
    public enum ValueType {
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        STRING,
        ENUM
    }

    /**
     * One declared value with its metadata.
     *
     * @param value typed handle returned to the add-on
     * @param name entry key inside its section
     * @param type storage type
     * @param translationKey optional language key for the row label; empty when unset
     * @param comments file comments written above the entry; empty when unset
     * @param minimum inclusive lower bound for numeric values, or {@code null} when unbounded
     * @param maximum inclusive upper bound for numeric values, or {@code null} when unbounded
     * @param validator accepts raw values read from disk or typed in the editor
     */
    public record EntryDefinition(
            Value<?> value,
            String name,
            ValueType type,
            String translationKey,
            List<String> comments,
            Number minimum,
            Number maximum,
            Predicate<Object> validator
    ) implements Operation {
        /**
         * Normalizes optional metadata: a {@code null} translation key becomes empty, comments are copied and a
         * {@code null} validator accepts everything.
         *
         * @throws NullPointerException if {@code value}, {@code name} or {@code type} is {@code null}
         */
        public EntryDefinition {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            translationKey = translationKey == null ? "" : translationKey;
            comments = comments == null ? List.of() : List.copyOf(comments);
            validator = validator == null ? ignored -> true : validator;
        }
    }

    /**
     * Typed handle to one declared client setting.
     *
     * <p>Handles are created only by {@link Builder} and stay valid for the lifetime of the game. Before the spec
     * is loaded by Forge, reads and writes use an in-memory copy that starts at the default value.
     *
     * @param <T> stored value type
     */
    public abstract static class Value<T> {
        private final T defaultValue;
        private volatile T localValue;
        private Predicate<Object> validator = ignored -> true;

        private Value(T defaultValue) {
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
            this.localValue = defaultValue;
        }

        /**
         * Returns the current value.
         *
         * @return the value from the loaded config file, or the in-memory value while the spec is not loaded
         *   yet; never {@code null}
         */
        public final T get() {
            return KineticClientConfigSpecRuntime.get(this, localValue);
        }

        /**
         * Changes the value in memory. Call {@link KTClientConfigSpec#save()} afterwards to write it to disk.
         *
         * @param value new value; must pass the declared range or validator
         * @throws NullPointerException if {@code value} is {@code null}
         * @throws IllegalArgumentException if the value is rejected; the previous value is kept
         */
        public final void set(T value) {
            T next = Objects.requireNonNull(value, "value");
            if (!validator.test(next)) {
                throw new IllegalArgumentException("Value rejected by client config validator");
            }
            // Publish the local fallback only after the native config accepted the update.
            KineticClientConfigSpecRuntime.set(this, next);
            localValue = next;
        }

        /** Returns the declared default value, which never changes after declaration. */
        public final T defaultValue() {
            return defaultValue;
        }
    }

    /** Handle for an on/off setting. */
    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(boolean defaultValue) {
            super(defaultValue);
        }
    }

    /** Handle for an {@code int} setting limited to an inclusive range. */
    public static final class IntValue extends Value<Integer> {
        private IntValue(int defaultValue) {
            super(defaultValue);
        }
    }

    /** Handle for a {@code long} setting limited to an inclusive range. */
    public static final class LongValue extends Value<Long> {
        private LongValue(long defaultValue) {
            super(defaultValue);
        }
    }

    /** Handle for a finite decimal setting. */
    public static final class DoubleValue extends Value<Double> {
        private DoubleValue(double defaultValue) {
            super(defaultValue);
        }
    }

    /** Handle for a text setting checked by a caller-supplied validator. */
    public static final class StringValue extends Value<String> {
        private StringValue(String defaultValue) {
            super(defaultValue);
        }
    }

    /**
     * Handle for a setting restricted to one enum type's constants.
     *
     * @param <E> enum type
     */
    public static final class EnumValue<E extends Enum<E>> extends Value<E> {
        private EnumValue(E defaultValue) {
            super(defaultValue);
        }
    }

    /**
     * Declares sections and values in file order.
     *
     * <p>Metadata set with {@link #comment(String...)} and {@link #translation(String)} applies to the next section
     * or value only. Builders are single-use and not thread-safe.
     */
    public static final class Builder {
        private final List<Operation> operations = new ArrayList<>();
        private final List<String> sectionPath = new ArrayList<>();
        private final Set<List<String>> entryPaths = new HashSet<>();
        private List<String> pendingComments = List.of();
        private String pendingTranslation = "";
        private int depth;
        private boolean built;

        private Builder() {
        }

        /**
         * Sets file comments for the next section or value. Blank and {@code null} lines are dropped; calling it
         * again replaces the pending comments.
         *
         * @param comments comment lines, or none to clear
         * @return this builder
         * @throws IllegalStateException if the builder was already built
         */
        public Builder comment(String... comments) {
            ensureMutable();
            if (comments == null || comments.length == 0) {
                pendingComments = List.of();
            } else {
                List<String> values = new ArrayList<>(comments.length);
                for (String comment : comments) {
                    if (comment != null && !comment.isBlank()) values.add(comment);
                }
                pendingComments = List.copyOf(values);
            }
            return this;
        }

        /**
         * Sets the language key used as the label of the next section or value in the config screen.
         *
         * @param translationKey language key, or {@code null} to clear
         * @return this builder
         * @throws IllegalStateException if the builder was already built
         */
        public Builder translation(String translationKey) {
            ensureMutable();
            pendingTranslation = translationKey == null ? "" : translationKey.trim();
            return this;
        }

        /**
         * Opens a nested section. Every value declared until the matching {@link #pop()} is stored under it.
         *
         * @param name section key; trimmed, must not be blank
         * @return this builder
         * @throws NullPointerException if {@code name} is {@code null}
         * @throws IllegalArgumentException if {@code name} is blank
         * @throws IllegalStateException if the builder was already built
         */
        public Builder push(String name) {
            ensureMutable();
            String normalized = requireName(name);
            operations.add(new SectionStart(normalized, pendingTranslation, pendingComments));
            sectionPath.add(normalized);
            clearPendingMetadata();
            depth++;
            return this;
        }

        /**
         * Closes the most recently opened section.
         *
         * @return this builder
         * @throws IllegalStateException if no section is open or the builder was already built
         */
        public Builder pop() {
            ensureMutable();
            if (depth <= 0) throw new IllegalStateException("No client config section to close");
            operations.add(new SectionEnd());
            depth--;
            sectionPath.remove(sectionPath.size() - 1);
            clearPendingMetadata();
            return this;
        }

        /**
         * Declares an on/off value in the current section.
         *
         * @param name entry key; trimmed, must not be blank and must be unique in its section
         * @param defaultValue value used when the file has none
         * @return the value handle
         * @throws IllegalArgumentException if the name is blank or already used in this section
         */
        public BooleanValue defineBoolean(String name, boolean defaultValue) {
            BooleanValue value = new BooleanValue(defaultValue);
            addEntry(value, name, ValueType.BOOLEAN, null, null, ignored -> true);
            return value;
        }

        /**
         * Declares an {@code int} value limited to an inclusive range. Negative bounds are allowed.
         *
         * @param name entry key; trimmed, must not be blank and must be unique in its section
         * @param defaultValue value used when the file has none; must lie within the range
         * @param minimum smallest accepted value
         * @param maximum largest accepted value
         * @return the value handle
         * @throws IllegalArgumentException if {@code minimum > maximum}, the default is out of range, or the name
         *   is blank or already used
         */
        public IntValue defineInt(String name, int defaultValue, int minimum, int maximum) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum cannot exceed maximum");
            if (defaultValue < minimum || defaultValue > maximum) {
                throw new IllegalArgumentException("defaultValue is outside the configured range");
            }
            IntValue value = new IntValue(defaultValue);
            addEntry(value, name, ValueType.INTEGER, minimum, maximum,
                    raw -> isExactIntegerInRange(raw, minimum, maximum));
            return value;
        }

        /**
         * Declares a {@code long} value limited to an inclusive range. Negative bounds are allowed.
         *
         * @param name entry key; trimmed, must not be blank and must be unique in its section
         * @param defaultValue value used when the file has none; must lie within the range
         * @param minimum smallest accepted value
         * @param maximum largest accepted value
         * @return the value handle
         * @throws IllegalArgumentException if {@code minimum > maximum}, the default is out of range, or the name
         *   is blank or already used
         */
        public LongValue defineLong(String name, long defaultValue, long minimum, long maximum) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum cannot exceed maximum");
            if (defaultValue < minimum || defaultValue > maximum) {
                throw new IllegalArgumentException("defaultValue is outside the configured range");
            }
            LongValue value = new LongValue(defaultValue);
            addEntry(value, name, ValueType.LONG, minimum, maximum,
                    raw -> isExactIntegerInRange(raw, minimum, maximum));
            return value;
        }

        /**
         * Declares a finite decimal value limited to an inclusive range. Negative bounds are allowed.
         *
         * @param name entry key; trimmed, must not be blank and must be unique in its section
         * @param defaultValue value used when the file has none; must lie within the range
         * @param minimum smallest accepted value, finite
         * @param maximum largest accepted value, finite
         * @return the value handle
         * @throws IllegalArgumentException if any number is not finite, {@code minimum > maximum}, the default is
         *   out of range, or the name is blank or already used
         */
        public DoubleValue defineDouble(String name, double defaultValue, double minimum, double maximum) {
            if (!Double.isFinite(defaultValue) || !Double.isFinite(minimum) || !Double.isFinite(maximum)) {
                throw new IllegalArgumentException("Double config values must be finite");
            }
            if (minimum > maximum) throw new IllegalArgumentException("minimum cannot exceed maximum");
            if (defaultValue < minimum || defaultValue > maximum) {
                throw new IllegalArgumentException("defaultValue is outside the configured range");
            }
            DoubleValue value = new DoubleValue(defaultValue);
            addEntry(value, name, ValueType.DOUBLE, minimum, maximum,
                    raw -> isFiniteNumberInRange(raw, minimum, maximum));
            return value;
        }

        /**
         * Declares a finite decimal value checked by a custom rule instead of a range.
         *
         * @param name entry key; trimmed, must not be blank and must be unique in its section
         * @param defaultValue value used when the file has none; finite and accepted by {@code validator}
         * @param validator receives the candidate {@link Double}; non-finite values are rejected before it runs
         * @return the value handle
         * @throws NullPointerException if {@code validator} is {@code null}
         * @throws IllegalArgumentException if the default is not finite or is rejected, or the name is blank or
         *   already used
         */
        public DoubleValue defineDoubleValidated(String name, double defaultValue, Predicate<Object> validator) {
            Objects.requireNonNull(validator, "validator");
            if (!Double.isFinite(defaultValue)) {
                throw new IllegalArgumentException("Double config values must be finite");
            }
            if (!validator.test(defaultValue)) {
                throw new IllegalArgumentException("defaultValue is rejected by validator");
            }
            DoubleValue value = new DoubleValue(defaultValue);
            addEntry(value, name, ValueType.DOUBLE, null, null,
                    raw -> raw instanceof Double number && Double.isFinite(number) && validator.test(number));
            return value;
        }

        /**
         * Declares a text value checked by a custom rule.
         *
         * @param name entry key; trimmed, must not be blank and must be unique in its section
         * @param defaultValue value used when the file has none; must be non-null and accepted by {@code validator}
         * @param validator accepts or rejects candidate text
         * @return the value handle
         * @throws NullPointerException if {@code validator} or {@code defaultValue} is {@code null}
         * @throws IllegalArgumentException if the default is rejected, or the name is blank or already used
         */
        public StringValue defineString(String name, String defaultValue, Predicate<String> validator) {
            Objects.requireNonNull(validator, "validator");
            if (!validator.test(defaultValue)) {
                throw new IllegalArgumentException("defaultValue is rejected by validator");
            }
            StringValue value = new StringValue(defaultValue);
            addEntry(value, name, ValueType.STRING, null, null,
                    raw -> raw instanceof String text && validator.test(text));
            return value;
        }

        /**
         * Declares a value restricted to the constants of {@code defaultValue}'s enum type. The config screen shows
         * a dropdown of the constant names.
         *
         * @param name entry key; trimmed, must not be blank and must be unique in its section
         * @param defaultValue value used when the file has none
         * @param <E> enum type
         * @return the value handle
         * @throws NullPointerException if {@code defaultValue} is {@code null}
         * @throws IllegalArgumentException if the name is blank or already used
         */
        public <E extends Enum<E>> EnumValue<E> defineEnum(String name, E defaultValue) {
            EnumValue<E> value = new EnumValue<>(Objects.requireNonNull(defaultValue, "defaultValue"));
            addEntry(value, name, ValueType.ENUM, null, null,
                    raw -> raw != null && raw.getClass() == defaultValue.getDeclaringClass());
            return value;
        }

        /**
         * Finishes the definition. The builder cannot be used afterwards.
         *
         * @return the immutable spec
         * @throws IllegalStateException if a section is still open or the builder was already built
         */
        public KTClientConfigSpec build() {
            ensureMutable();
            if (depth != 0) throw new IllegalStateException("Unclosed client config section");
            built = true;
            clearPendingMetadata();
            return new KTClientConfigSpec(operations);
        }

        /** Compare precise decimal metadata before it can be rounded into an allowed double range. */
        private static boolean isFiniteNumberInRange(Object raw, double minimum, double maximum) {
            return raw instanceof Number number
                    && KineticConfigNumbers.finiteDoubleInRange(number, minimum, maximum) != null;
        }

        /** Reject fractional, overflowing and non-finite values before exposing config metadata. */
        private static boolean isExactIntegerInRange(Object raw, long minimum, long maximum) {
            if (!(raw instanceof Number number)) return false;
            try {
                BigInteger integer = new BigDecimal(number.toString()).toBigIntegerExact();
                return integer.compareTo(BigInteger.valueOf(minimum)) >= 0
                        && integer.compareTo(BigInteger.valueOf(maximum)) <= 0;
            } catch (NumberFormatException | ArithmeticException invalid) {
                return false;
            }
        }

        private void addEntry(
                Value<?> value,
                String name,
                ValueType type,
                Number minimum,
                Number maximum,
                Predicate<Object> validator
        ) {
            ensureMutable();
            String normalizedName = requireName(name);
            List<String> path = new ArrayList<>(sectionPath);
            path.add(normalizedName);
            if (entryPaths.contains(path)) {
                throw new IllegalArgumentException("Duplicate client config entry path: " + String.join(".", path));
            }
            EntryDefinition entry = new EntryDefinition(
                    value,
                    normalizedName,
                    type,
                    pendingTranslation,
                    pendingComments,
                    minimum,
                    maximum,
                    validator
            );
            operations.add(entry);
            entryPaths.add(List.copyOf(path));
            value.validator = validator;
            clearPendingMetadata();
        }

        private void clearPendingMetadata() {
            pendingComments = List.of();
            pendingTranslation = "";
        }

        private void ensureMutable() {
            if (built) throw new IllegalStateException("Client config builder has already been built");
        }

        private static String requireName(String name) {
            String normalized = Objects.requireNonNull(name, "name").trim();
            if (normalized.isEmpty()) throw new IllegalArgumentException("name cannot be blank");
            return normalized;
        }
    }
}
