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
 * <p>Addons declare values through this API. KineticCore owns the underlying
 * config-loader implementation, persistence, and config-screen adaptation.</p>
 */
public final class KTClientConfigSpec {
    private final List<Operation> operations;

    private KTClientConfigSpec(List<Operation> operations) {
        this.operations = List.copyOf(operations);
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the operations.
     */
    public List<Operation> operations() {
        return operations;
    }

    /**
     * Saves the current values.
     */
    public void save() {
        KineticClientConfigSpecRuntime.save(this);
    }

    /** Public API contract for operation. */
    public sealed interface Operation permits SectionStart, SectionEnd, EntryDefinition {
    }

    /** Immutable section start data exposed by this API. */
    public record SectionStart(
            String name,
            String translationKey,
            List<String> comments
    ) implements Operation {
        /**
         * Validates and normalizes this section start value.
         */
        public SectionStart {
            Objects.requireNonNull(name, "name");
            translationKey = translationKey == null ? "" : translationKey;
            comments = comments == null ? List.of() : List.copyOf(comments);
        }
    }

    /** Immutable section end data exposed by this API. */
    public record SectionEnd() implements Operation {
    }

    /** Supported value type values exposed by this API. */
    public enum ValueType {
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        STRING,
        ENUM
    }

    /** Immutable entry definition data exposed by this API. */
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
         * Validates and normalizes this entry definition value.
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

    /** Typed value wrapper exposed by the configuration API. */
    public abstract static class Value<T> {
        private final T defaultValue;
        private volatile T localValue;
        private Predicate<Object> validator = ignored -> true;

        private Value(T defaultValue) {
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
            this.localValue = defaultValue;
        }

        /**
         * Performs the get API operation.
         */
        public final T get() {
            return KineticClientConfigSpecRuntime.get(this, localValue);
        }

        /**
         * Performs the set API operation.
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

        /**
         * Returns the default value.
         */
        public final T defaultValue() {
            return defaultValue;
        }
    }

    /** Typed boolean value wrapper exposed by the configuration API. */
    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(boolean defaultValue) {
            super(defaultValue);
        }
    }

    /** Typed int value wrapper exposed by the configuration API. */
    public static final class IntValue extends Value<Integer> {
        private IntValue(int defaultValue) {
            super(defaultValue);
        }
    }

    /** Typed long value wrapper exposed by the configuration API. */
    public static final class LongValue extends Value<Long> {
        private LongValue(long defaultValue) {
            super(defaultValue);
        }
    }

    /** Typed double value wrapper exposed by the configuration API. */
    public static final class DoubleValue extends Value<Double> {
        private DoubleValue(double defaultValue) {
            super(defaultValue);
        }
    }

    /** Typed string value wrapper exposed by the configuration API. */
    public static final class StringValue extends Value<String> {
        private StringValue(String defaultValue) {
            super(defaultValue);
        }
    }

    /** Typed enum value wrapper exposed by the configuration API. */
    public static final class EnumValue<E extends Enum<E>> extends Value<E> {
        private EnumValue(E defaultValue) {
            super(defaultValue);
        }
    }

    /** Builder for definitions owned by the enclosing API. */
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
         * Performs the comment API operation.
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
         * Performs the translation API operation.
         */
        public Builder translation(String translationKey) {
            ensureMutable();
            pendingTranslation = translationKey == null ? "" : translationKey.trim();
            return this;
        }

        /**
         * Performs the push API operation.
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
         * Performs the pop API operation.
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
         * Performs the define boolean API operation.
         */
        public BooleanValue defineBoolean(String name, boolean defaultValue) {
            BooleanValue value = new BooleanValue(defaultValue);
            addEntry(value, name, ValueType.BOOLEAN, null, null, ignored -> true);
            return value;
        }

        /**
         * Performs the define int API operation.
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
         * Performs the define long API operation.
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
         * Performs the define double API operation.
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
         * Performs the define double validated API operation.
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
         * Performs the define string API operation.
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
         * Performs the define enum API operation.
         */
        public <E extends Enum<E>> EnumValue<E> defineEnum(String name, E defaultValue) {
            EnumValue<E> value = new EnumValue<>(Objects.requireNonNull(defaultValue, "defaultValue"));
            addEntry(value, name, ValueType.ENUM, null, null,
                    raw -> raw != null && raw.getClass() == defaultValue.getDeclaringClass());
            return value;
        }

        /**
         * Builds the configured API value.
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
