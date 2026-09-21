package dev.xyat.kineticcore.api.config.server;

import net.minecraft.server.MinecraftServer;
import dev.xyat.kineticcore.api.config.common.KineticConfigNumbers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Public API type for kt server config spec. */
public final class KTServerConfigSpec {
    private final String pageId;
    private final Map<String, Entry> entries;
    private final Runnable saver;
    private final Consumer<MinecraftServer> afterSave;

    private KTServerConfigSpec(Builder builder) {
        this.pageId = builder.pageId;
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(builder.entries));
        this.saver = builder.saver;
        this.afterSave = builder.afterSave;
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder(String pageId) {
        return new Builder(pageId);
    }

    /**
     * Returns the page id.
     */
    public String pageId() {
        return pageId;
    }

    /**
     * Returns a snapshot of the current values.
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Entry entry : entries.values()) {
            result.put(entry.id, entry.read());
        }
        return result;
    }

    /**
     * Returns the value.
     */
    public Object value(String entryId) {
        Entry entry = entries.get(Objects.requireNonNull(entryId, "entryId"));
        if (entry == null) {
            throw new IllegalArgumentException("Unknown server config entry: " + pageId + "/" + entryId);
        }
        return entry.read();
    }

    /**
     * Applies validated changes atomically in memory without invoking save callbacks.
     * If a writer partially mutates a value and fails, restoration is attempted for
     * every submitted entry, preserving the original exception.
     */
    public void apply(Map<String, Object> values) {
        List<Runnable> commits = prepareCommits(values);
        Map<String, Object> previous = new LinkedHashMap<>();
        for (String key : values.keySet()) {
            previous.put(key, entries.get(key).read());
        }
        int attemptedCommits = 0;
        try {
            for (Runnable commit : commits) {
                // The writer may mutate its field before throwing; count it first.
                attemptedCommits++;
                commit.run();
            }
        } catch (Throwable failure) {
            // Unattempted writers must never run; restore attempted dependencies in reverse order.
            List<Map.Entry<String, Object>> snapshots = new ArrayList<>(previous.entrySet());
            for (int i = attemptedCommits - 1; i >= 0; i--) {
                Map.Entry<String, Object> oldValue = snapshots.get(i);
                try {
                    entries.get(oldValue.getKey()).prepare(oldValue.getValue()).run();
                } catch (Throwable rollbackFailure) {
                    if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        }
    }

    /** Validates every submitted value before any writer or persistence callback can run. */
    private List<Runnable> prepareCommits(Map<String, Object> values) {
        Objects.requireNonNull(values, "values");
        for (String key : values.keySet()) {
            if (!entries.containsKey(key)) {
                throw new IllegalArgumentException("Unknown server config entry: " + pageId + "/" + key);
            }
        }
        List<Runnable> commits = new ArrayList<>(values.size());
        for (Map.Entry<String, Object> update : values.entrySet()) {
            commits.add(entries.get(update.getKey()).prepare(update.getValue()));
        }
        return commits;
    }

    /**
     * Saves the current values.
     */
    public void save(MinecraftServer server) {
        saver.run();
        afterSave.accept(server);
    }

    /**
     * Applies and save.
     */
    public void applyAndSave(MinecraftServer server, Map<String, Object> values) {
        // Reject the request before reading any config values. Only submitted entries
        // belong to this transaction; unrelated entries must not be read or rewritten.
        List<Runnable> commits = prepareCommits(values);
        Map<String, Object> previous = new LinkedHashMap<>();
        for (String key : values.keySet()) {
            previous.put(key, entries.get(key).read());
        }

        int attemptedCommits = 0;
        try {
            for (Runnable commit : commits) {
                attemptedCommits++;
                commit.run();
            }
            saver.run();
            afterSave.accept(server);
        } catch (Throwable failure) {
            // A rollback writer may also throw after mutating its field. Restore every
            // independent entry in reverse order rather than abandoning the remaining fields
            // at the first rollback error.
            boolean fullyRestored = true;
            List<Map.Entry<String, Object>> snapshots = new ArrayList<>(previous.entrySet());
            for (int i = attemptedCommits - 1; i >= 0; i--) {
                Map.Entry<String, Object> previousValue = snapshots.get(i);
                try {
                    entries.get(previousValue.getKey()).prepare(previousValue.getValue()).run();
                } catch (Throwable rollbackFailure) {
                    fullyRestored = false;
                    if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
                }
            }
            if (!fullyRestored) throw failure;

            try {
                saver.run();
            } catch (Throwable rollbackSaveFailure) {
                if (rollbackSaveFailure != failure) failure.addSuppressed(rollbackSaveFailure);
                // Do not apply a rollback that could not be persisted to disk.
                throw failure;
            }

            try {
                afterSave.accept(server);
            } catch (Throwable rollbackApplyFailure) {
                if (rollbackApplyFailure != failure) failure.addSuppressed(rollbackApplyFailure);
            }

            throw failure;
        }
    }

    private record Entry(String id, Supplier<Object> reader, Function<Object, Runnable> prepareWriter) {
        private Object read() {
            return copyValue(reader.get());
        }

        private Runnable prepare(Object raw) {
            return prepareWriter.apply(raw);
        }
    }

    /** Builder for definitions owned by the enclosing API. */
    public static final class Builder {
        private final String pageId;
        private final Map<String, Entry> entries = new LinkedHashMap<>();
        private Runnable saver = () -> { };
        private Consumer<MinecraftServer> afterSave = server -> { };

        private Builder(String pageId) {
            String normalized = Objects.requireNonNull(pageId, "pageId").trim();
            if (!normalized.contains(":")) {
                throw new IllegalArgumentException("Invalid server config page id: " + pageId);
            }
            this.pageId = normalized;
        }

        /**
         * Returns the boolean value.
         */
        public Builder booleanValue(String id, Supplier<Boolean> reader, Consumer<Boolean> writer) {
            return booleanValueValidated(id, reader, writer, value -> true);
        }

        /**
         * Performs the boolean value validated API operation.
         */
        public Builder booleanValueValidated(
                String id, Supplier<Boolean> reader, Consumer<Boolean> writer, Predicate<Boolean> validator
        ) {
            Predicate<Boolean> rule = Objects.requireNonNull(validator, "validator");
            return add(
                    id, reader, writer,
                    raw -> raw instanceof Boolean value ? value : null,
                    value -> value != null && rule.test(value)
            );
        }

        /**
         * Returns the int value.
         */
        public Builder intValue(String id, Supplier<Integer> reader, Consumer<Integer> writer, int minimum, int maximum) {
            return intValueValidated(id, reader, writer, minimum, maximum, value -> true);
        }

        /**
         * Performs the int value validated API operation.
         */
        public Builder intValueValidated(
                String id, Supplier<Integer> reader, Consumer<Integer> writer,
                int minimum, int maximum, Predicate<Integer> validator
        ) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            Predicate<Integer> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeInteger,
                    value -> value != null && value >= minimum && value <= maximum && rule.test(value));
        }

        /**
         * Returns the long value.
         */
        public Builder longValue(String id, Supplier<Long> reader, Consumer<Long> writer, long minimum, long maximum) {
            return longValueValidated(id, reader, writer, minimum, maximum, value -> true);
        }

        /**
         * Performs the long value validated API operation.
         */
        public Builder longValueValidated(
                String id, Supplier<Long> reader, Consumer<Long> writer,
                long minimum, long maximum, Predicate<Long> validator
        ) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            Predicate<Long> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeLong,
                    value -> value != null && value >= minimum && value <= maximum && rule.test(value));
        }

        /**
         * Returns the double value.
         */
        public Builder doubleValue(String id, Supplier<Double> reader, Consumer<Double> writer, double minimum, double maximum) {
            return doubleValueValidated(id, reader, writer, minimum, maximum, value -> true);
        }

        /**
         * Performs the double value validated API operation.
         */
        public Builder doubleValueValidated(
                String id, Supplier<Double> reader, Consumer<Double> writer,
                double minimum, double maximum, Predicate<Double> validator
        ) {
            if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
                throw new IllegalArgumentException("Invalid double range for " + id);
            }
            Predicate<Double> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer,
                    raw -> raw instanceof Number number
                            ? KineticConfigNumbers.finiteDoubleInRange(number, minimum, maximum) : null,
                    value -> value != null
                            && Double.isFinite(value)
                            && value >= minimum
                            && value <= maximum
                            && rule.test(value));
        }

        /**
         * Returns the color value.
         */
        public Builder colorValue(String id, Supplier<Integer> reader, Consumer<Integer> writer) {
            return colorValueValidated(id, reader, writer, value -> true);
        }

        /**
         * Performs the color value validated API operation.
         */
        public Builder colorValueValidated(
                String id, Supplier<Integer> reader, Consumer<Integer> writer, Predicate<Integer> validator
        ) {
            return intValueValidated(id, reader, writer, 0, 0xFFFFFF, validator);
        }

        /**
         * Returns the string value.
         */
        public Builder stringValue(String id, Supplier<String> reader, Consumer<String> writer) {
            return stringValueValidated(id, reader, writer, value -> true);
        }

        /**
         * Performs the string value validated API operation.
         */
        public Builder stringValueValidated(
                String id, Supplier<String> reader, Consumer<String> writer, Predicate<String> validator
        ) {
            Predicate<String> rule = Objects.requireNonNull(validator, "validator");
            return add(
                    id, reader, writer,
                    raw -> raw instanceof String value ? value : null,
                    value -> value != null && rule.test(value)
            );
        }

        /**
         * Returns the choice value.
         */
        public Builder choiceValue(
                String id, Supplier<String> reader, Consumer<String> writer, String... allowedValues
        ) {
            return choiceValueValidated(id, reader, writer, value -> true, allowedValues);
        }

        /**
         * Performs the choice value validated API operation.
         */
        public Builder choiceValueValidated(
                String id,
                Supplier<String> reader,
                Consumer<String> writer,
                Predicate<String> validator,
                String... allowedValues
        ) {
            Objects.requireNonNull(allowedValues, "allowedValues");
            Predicate<String> rule = Objects.requireNonNull(validator, "validator");
            Set<String> allowed = new LinkedHashSet<>(Arrays.asList(allowedValues));
            if (allowed.isEmpty() || allowed.contains(null)) {
                throw new IllegalArgumentException("choice values cannot be empty or null for " + id);
            }
            if (allowed.size() != allowedValues.length) {
                throw new IllegalArgumentException("duplicate choice value for " + id);
            }
            return stringValueValidated(id, reader, writer, value -> allowed.contains(value) && rule.test(value));
        }

        /**
         * Performs the string list API operation.
         */
        public Builder stringList(String id, Supplier<List<String>> reader, Consumer<List<String>> writer) {
            return stringListValidated(id, reader, writer, value -> true);
        }

        /**
         * Performs the string list validated API operation.
         */
        public Builder stringListValidated(
                String id, Supplier<List<String>> reader, Consumer<List<String>> writer, Predicate<List<String>> validator
        ) {
            Predicate<List<String>> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeStringList,
                    value -> value != null && rule.test(List.copyOf(value)));
        }

        /**
         * Performs the int list API operation.
         */
        public Builder intList(String id, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer) {
            return intListValidated(id, reader, writer, value -> true);
        }

        /**
         * Performs the int list validated API operation.
         */
        public Builder intListValidated(
                String id, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer, Predicate<List<Integer>> validator
        ) {
            Predicate<List<Integer>> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeIntegerList,
                    value -> value != null && rule.test(List.copyOf(value)));
        }

        /**
         * Registers a listener for save.
         */
        public Builder onSave(Runnable saver) {
            this.saver = Objects.requireNonNull(saver, "saver");
            return this;
        }

        /**
         * Performs the after save API operation.
         */
        public Builder afterSave(Consumer<MinecraftServer> afterSave) {
            this.afterSave = Objects.requireNonNull(afterSave, "afterSave");
            return this;
        }

        /**
         * Builds the configured API value.
         */
        public KTServerConfigSpec build() {
            return new KTServerConfigSpec(this);
        }

        private <T> Builder add(
                String id,
                Supplier<T> reader,
                Consumer<T> writer,
                Function<Object, T> decoder,
                Predicate<T> validator
        ) {
            String normalized = Objects.requireNonNull(id, "id").trim();
            if (normalized.isEmpty()) throw new IllegalArgumentException("Empty server config entry id");
            Objects.requireNonNull(reader, "reader");
            Objects.requireNonNull(writer, "writer");
            Objects.requireNonNull(decoder, "decoder");
            Objects.requireNonNull(validator, "validator");

            Entry entry = new Entry(
                    normalized,
                    () -> {
                        T value = reader.get();
                        if (!validator.test(value)) {
                            throw new IllegalStateException("Invalid server config value: " + normalized);
                        }
                        return copyValue(value);
                    },
                    raw -> {
                        T value = decoder.apply(raw);
                        if (value == null || !validator.test(value)) {
                            throw new IllegalArgumentException("Invalid server config value: " + normalized);
                        }
                        return () -> writer.accept(value);
                    }
            );
            if (entries.putIfAbsent(normalized, entry) != null) {
                throw new IllegalStateException("Duplicate server config entry: " + pageId + "/" + normalized);
            }
            return this;
        }

        private static Integer decodeInteger(Object raw) {
            return raw instanceof Number number ? KineticConfigNumbers.exactInt(number) : null;
        }

        private static Long decodeLong(Object raw) {
            return raw instanceof Number number ? KineticConfigNumbers.exactLong(number) : null;
        }

        private static List<String> decodeStringList(Object raw) {
            if (!(raw instanceof List<?> list)) return null;
            List<String> result = new ArrayList<>(list.size());
            for (Object value : list) {
                if (!(value instanceof String string)) return null;
                result.add(string);
            }
            return result;
        }

        private static List<Integer> decodeIntegerList(Object raw) {
            if (!(raw instanceof List<?> list)) return null;
            List<Integer> result = new ArrayList<>(list.size());
            for (Object value : list) {
                Integer decoded = decodeInteger(value);
                if (decoded == null) return null;
                result.add(decoded);
            }
            return result;
        }
    }

    private static Object copyValue(Object value) {
        if (value instanceof List<?> list) return new ArrayList<>(list);
        return value;
    }
}
