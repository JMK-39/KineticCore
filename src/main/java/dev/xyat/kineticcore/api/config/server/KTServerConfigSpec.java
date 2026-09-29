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

/**
 * Server-side half of a {@code SERVER_AUTHORITATIVE} config page: which values the server exposes, how they are
 * validated and how accepted changes are applied and persisted.
 *
 * <p>The client page with the same id is only a view. When an operator saves, the server decodes the submitted
 * values, rejects the whole request if any value is invalid, applies the changes, runs {@link Builder#onSave} and
 * {@link Builder#afterSave}, and restores the previous values if anything fails.
 *
 * <p>Register specs with {@link KTServerConfigApi#register(KTServerConfigSpec)}. Readers and writers run on the
 * server thread.
 */
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
     * Starts a server config definition.
     *
     * @param pageId namespaced id shared with the client page, for example {@code mymod:server}
     * @return a new builder
     * @throws NullPointerException if {@code pageId} is {@code null}
     * @throws IllegalArgumentException if {@code pageId} has no namespace
     */
    public static Builder builder(String pageId) {
        return new Builder(pageId);
    }

    /** Returns the namespaced page id shared with the client config page. */
    public String pageId() {
        return pageId;
    }

    /**
     * Reads every entry through its reader.
     *
     * @return a new mutable map from entry id to value in declaration order; lists are copied
     * @throws IllegalStateException if a reader returns a value that fails its own rules
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Entry entry : entries.values()) {
            result.put(entry.id, entry.read());
        }
        return result;
    }

    /**
     * Reads one entry through its reader.
     *
     * @param entryId entry id
     * @return the current value; lists are copied
     * @throws NullPointerException if {@code entryId} is {@code null}
     * @throws IllegalArgumentException if the entry does not exist
     * @throws IllegalStateException if the reader returns a value that fails its own rules
     */
    public Object value(String entryId) {
        Entry entry = entries.get(Objects.requireNonNull(entryId, "entryId"));
        if (entry == null) {
            throw new IllegalArgumentException("Unknown server config entry: " + pageId + "/" + entryId);
        }
        return entry.read();
    }

    /**
     * Applies validated changes in memory without running the save callbacks.
     *
     * <p>Every submitted value is decoded and validated before any writer runs, so an invalid request changes
     * nothing. If a writer throws, the writers that already ran are restored in reverse order and the original
     * exception is rethrown with rollback failures attached as suppressed exceptions.
     *
     * @param values changed values keyed by entry id; entries not listed are left untouched
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if an id is unknown or a value is rejected
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
     * Runs the persistence callback and then the after-save callback without changing any value.
     *
     * @param server running server passed to the after-save callback
     */
    public void save(MinecraftServer server) {
        saver.run();
        afterSave.accept(server);
    }

    /**
     * Applies validated changes, persists them and runs the after-save callback as one transaction.
     *
     * <p>If a writer or a callback fails, the submitted entries are restored in reverse order and, when the restore
     * succeeded, persisted again so disk and memory agree. The original exception is always rethrown.
     *
     * @param server running server passed to the after-save callback
     * @param values changed values keyed by entry id; entries not listed are neither read nor rewritten
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if an id is unknown or a value is rejected; nothing is changed
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

    /**
     * Declares the values a server config page exposes. Numeric ranges and validators given here are enforced on
     * the server regardless of what the client sends. Builders are single-use and not thread-safe.
     */
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
         * Adds an on/off value.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder booleanValue(String id, Supplier<Boolean> reader, Consumer<Boolean> writer) {
            return booleanValueValidated(id, reader, writer, value -> true);
        }

        /**
         * Adds an on/off value with an extra business rule.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param validator extra business rule applied after the type and range checks
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
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
         * Adds an {@code int} value limited to an inclusive range. Fractional or out-of-range numbers are rejected,
         * never truncated.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param minimum smallest accepted value, may be negative
         * @param maximum largest accepted value
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank, or {@code minimum > maximum}
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder intValue(String id, Supplier<Integer> reader, Consumer<Integer> writer, int minimum, int maximum) {
            return intValueValidated(id, reader, writer, minimum, maximum, value -> true);
        }

        /**
         * Adds an {@code int} value limited to an inclusive range, with an extra business rule.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param minimum smallest accepted value, may be negative
         * @param maximum largest accepted value
         * @param validator extra business rule applied after the type and range checks
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank, or {@code minimum > maximum}
         * @throws IllegalStateException if the id is already used on this page
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
         * Adds a {@code long} value limited to an inclusive range. Fractional or out-of-range numbers are rejected,
         * never truncated.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param minimum smallest accepted value, may be negative
         * @param maximum largest accepted value
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank, or {@code minimum > maximum}
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder longValue(String id, Supplier<Long> reader, Consumer<Long> writer, long minimum, long maximum) {
            return longValueValidated(id, reader, writer, minimum, maximum, value -> true);
        }

        /**
         * Adds a {@code long} value limited to an inclusive range, with an extra business rule.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param minimum smallest accepted value, may be negative
         * @param maximum largest accepted value
         * @param validator extra business rule applied after the type and range checks
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank, or {@code minimum > maximum}
         * @throws IllegalStateException if the id is already used on this page
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
         * Adds a finite decimal value limited to an inclusive range.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param minimum smallest accepted value, finite and may be negative
         * @param maximum largest accepted value, finite
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank, or a bound is not finite or
         *   {@code minimum > maximum}
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder doubleValue(String id, Supplier<Double> reader, Consumer<Double> writer, double minimum, double maximum) {
            return doubleValueValidated(id, reader, writer, minimum, maximum, value -> true);
        }

        /**
         * Adds a finite decimal value limited to an inclusive range, with an extra business rule.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param minimum smallest accepted value, finite and may be negative
         * @param maximum largest accepted value, finite
         * @param validator extra business rule applied after the type and range checks
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank, or a bound is not finite or
         *   {@code minimum > maximum}
         * @throws IllegalStateException if the id is already used on this page
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
         * Adds an RGB color value between {@code 0x000000} and {@code 0xFFFFFF} (no alpha).
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder colorValue(String id, Supplier<Integer> reader, Consumer<Integer> writer) {
            return colorValueValidated(id, reader, writer, value -> true);
        }

        /**
         * Adds an RGB color value between {@code 0x000000} and {@code 0xFFFFFF} with an extra business rule.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param validator extra business rule applied after the type and range checks
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder colorValueValidated(
                String id, Supplier<Integer> reader, Consumer<Integer> writer, Predicate<Integer> validator
        ) {
            return intValueValidated(id, reader, writer, 0, 0xFFFFFF, validator);
        }

        /**
         * Adds a text value.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder stringValue(String id, Supplier<String> reader, Consumer<String> writer) {
            return stringValueValidated(id, reader, writer, value -> true);
        }

        /**
         * Adds a text value with an extra business rule.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param validator extra business rule applied after the type and range checks
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
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
         * Adds a text value restricted to a fixed set of choices.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param allowedValues accepted values, non-empty, without {@code null} or duplicates
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank, or {@code allowedValues} is empty, contains
         *   {@code null} or duplicates
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder choiceValue(
                String id, Supplier<String> reader, Consumer<String> writer, String... allowedValues
        ) {
            return choiceValueValidated(id, reader, writer, value -> true, allowedValues);
        }

        /**
         * Adds a text value restricted to a fixed set of choices, with an extra business rule.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param validator extra business rule applied after the type and range checks
         * @param allowedValues accepted values, non-empty, without {@code null} or duplicates
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank, or {@code allowedValues} is empty, contains
         *   {@code null} or duplicates
         * @throws IllegalStateException if the id is already used on this page
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
         * Adds a list of strings. The writer receives a new list; every element must be a string.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder stringList(String id, Supplier<List<String>> reader, Consumer<List<String>> writer) {
            return stringListValidated(id, reader, writer, value -> true);
        }

        /**
         * Adds a list of strings with an extra business rule on the whole list.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param validator receives an unmodifiable copy of the candidate list
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder stringListValidated(
                String id, Supplier<List<String>> reader, Consumer<List<String>> writer, Predicate<List<String>> validator
        ) {
            Predicate<List<String>> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeStringList,
                    value -> value != null && rule.test(List.copyOf(value)));
        }

        /**
         * Adds a list of integers. Elements that are fractional or outside the {@code int} range reject the whole
         * list.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder intList(String id, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer) {
            return intListValidated(id, reader, writer, value -> true);
        }

        /**
         * Adds a list of integers with an extra business rule on the whole list.
         *
         * @param id entry id, unique within the page; trimmed, must not be blank
         * @param reader returns the live server value; it must satisfy the same rules, or reads fail
         * @param writer applies an accepted value on the server thread
         * @param validator receives an unmodifiable copy of the candidate list
         * @return this builder
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if the id is blank
         * @throws IllegalStateException if the id is already used on this page
         */
        public Builder intListValidated(
                String id, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer, Predicate<List<Integer>> validator
        ) {
            Predicate<List<Integer>> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeIntegerList,
                    value -> value != null && rule.test(List.copyOf(value)));
        }

        /**
         * Sets the callback that writes the current values to disk after a successful apply.
         *
         * @param saver persistence callback run on the server thread; replaces any previous callback
         * @return this builder
         * @throws NullPointerException if {@code saver} is {@code null}
         */
        public Builder onSave(Runnable saver) {
            this.saver = Objects.requireNonNull(saver, "saver");
            return this;
        }

        /**
         * Sets the callback run after values were applied and persisted, for example to rebuild caches or resync
         * players.
         *
         * @param afterSave callback run on the server thread; replaces any previous callback
         * @return this builder
         * @throws NullPointerException if {@code afterSave} is {@code null}
         */
        public Builder afterSave(Consumer<MinecraftServer> afterSave) {
            this.afterSave = Objects.requireNonNull(afterSave, "afterSave");
            return this;
        }

        /** Creates the immutable spec. Register it with {@link KTServerConfigApi#register(KTServerConfigSpec)}. */
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
