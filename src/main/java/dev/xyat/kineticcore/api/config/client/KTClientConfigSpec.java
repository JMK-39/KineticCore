package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.internal.config.client.KineticClientConfigSpecRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public static Builder builder() {
        return new Builder();
    }

    public List<Operation> operations() {
        return operations;
    }

    public void save() {
        KineticClientConfigSpecRuntime.save(this);
    }

    public sealed interface Operation permits SectionStart, SectionEnd, EntryDefinition {
    }

    public record SectionStart(
            String name,
            String translationKey,
            List<String> comments
    ) implements Operation {
        public SectionStart {
            Objects.requireNonNull(name, "name");
            translationKey = translationKey == null ? "" : translationKey;
            comments = comments == null ? List.of() : List.copyOf(comments);
        }
    }

    public record SectionEnd() implements Operation {
    }

    public enum ValueType {
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        STRING,
        ENUM
    }

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
        public EntryDefinition {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            translationKey = translationKey == null ? "" : translationKey;
            comments = comments == null ? List.of() : List.copyOf(comments);
            validator = validator == null ? ignored -> true : validator;
        }
    }

    public abstract static class Value<T> {
        private final T defaultValue;
        private volatile T localValue;
        private Predicate<Object> validator = ignored -> true;

        private Value(T defaultValue) {
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
            this.localValue = defaultValue;
        }

        public final T get() {
            return KineticClientConfigSpecRuntime.get(this, localValue);
        }

        public final void set(T value) {
            T next = Objects.requireNonNull(value, "value");
            if (!validator.test(next)) {
                throw new IllegalArgumentException("Value rejected by client config validator");
            }
            localValue = next;
            KineticClientConfigSpecRuntime.set(this, next);
        }

        public final T defaultValue() {
            return defaultValue;
        }
    }

    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(boolean defaultValue) {
            super(defaultValue);
        }
    }

    public static final class IntValue extends Value<Integer> {
        private IntValue(int defaultValue) {
            super(defaultValue);
        }
    }

    public static final class LongValue extends Value<Long> {
        private LongValue(long defaultValue) {
            super(defaultValue);
        }
    }

    public static final class DoubleValue extends Value<Double> {
        private DoubleValue(double defaultValue) {
            super(defaultValue);
        }
    }

    public static final class StringValue extends Value<String> {
        private StringValue(String defaultValue) {
            super(defaultValue);
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends Value<E> {
        private EnumValue(E defaultValue) {
            super(defaultValue);
        }
    }

    public static final class Builder {
        private final List<Operation> operations = new ArrayList<>();
        private List<String> pendingComments = List.of();
        private String pendingTranslation = "";
        private int depth;
        private boolean built;

        private Builder() {
        }

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

        public Builder translation(String translationKey) {
            ensureMutable();
            pendingTranslation = translationKey == null ? "" : translationKey.trim();
            return this;
        }

        public Builder push(String name) {
            ensureMutable();
            String normalized = requireName(name);
            operations.add(new SectionStart(normalized, pendingTranslation, pendingComments));
            clearPendingMetadata();
            depth++;
            return this;
        }

        public Builder pop() {
            ensureMutable();
            if (depth <= 0) throw new IllegalStateException("No client config section to close");
            operations.add(new SectionEnd());
            depth--;
            clearPendingMetadata();
            return this;
        }

        public BooleanValue defineBoolean(String name, boolean defaultValue) {
            BooleanValue value = new BooleanValue(defaultValue);
            addEntry(value, name, ValueType.BOOLEAN, null, null, ignored -> true);
            return value;
        }

        public IntValue defineInt(String name, int defaultValue, int minimum, int maximum) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum cannot exceed maximum");
            if (defaultValue < minimum || defaultValue > maximum) {
                throw new IllegalArgumentException("defaultValue is outside the configured range");
            }
            IntValue value = new IntValue(defaultValue);
            addEntry(value, name, ValueType.INTEGER, minimum, maximum,
                    raw -> raw instanceof Number number
                            && number.intValue() >= minimum
                            && number.intValue() <= maximum);
            return value;
        }

        public LongValue defineLong(String name, long defaultValue, long minimum, long maximum) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum cannot exceed maximum");
            if (defaultValue < minimum || defaultValue > maximum) {
                throw new IllegalArgumentException("defaultValue is outside the configured range");
            }
            LongValue value = new LongValue(defaultValue);
            addEntry(value, name, ValueType.LONG, minimum, maximum,
                    raw -> raw instanceof Number number
                            && number.longValue() >= minimum
                            && number.longValue() <= maximum);
            return value;
        }

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
                    raw -> raw instanceof Number number
                            && Double.isFinite(number.doubleValue())
                            && number.doubleValue() >= minimum
                            && number.doubleValue() <= maximum);
            return value;
        }

        public DoubleValue defineDouble(String name, double defaultValue, Predicate<Object> validator) {
            Objects.requireNonNull(validator, "validator");
            if (!validator.test(defaultValue)) {
                throw new IllegalArgumentException("defaultValue is rejected by validator");
            }
            DoubleValue value = new DoubleValue(defaultValue);
            addEntry(value, name, ValueType.DOUBLE, null, null, validator);
            return value;
        }

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

        public <E extends Enum<E>> EnumValue<E> defineEnum(String name, E defaultValue) {
            EnumValue<E> value = new EnumValue<>(Objects.requireNonNull(defaultValue, "defaultValue"));
            addEntry(value, name, ValueType.ENUM, null, null,
                    raw -> raw != null && raw.getClass() == defaultValue.getDeclaringClass());
            return value;
        }

        public KTClientConfigSpec build() {
            ensureMutable();
            if (depth != 0) throw new IllegalStateException("Unclosed client config section");
            built = true;
            clearPendingMetadata();
            return new KTClientConfigSpec(operations);
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
            value.validator = validator;
            operations.add(new EntryDefinition(
                    value,
                    requireName(name),
                    type,
                    pendingTranslation,
                    pendingComments,
                    minimum,
                    maximum,
                    validator
            ));
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
