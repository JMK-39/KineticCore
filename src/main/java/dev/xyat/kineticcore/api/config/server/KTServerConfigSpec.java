package dev.xyat.kineticcore.api.config.server;

import net.minecraft.server.MinecraftServer;

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

    public static Builder builder(String pageId) {
        return new Builder(pageId);
    }

    public String pageId() {
        return pageId;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Entry entry : entries.values()) {
            result.put(entry.id, entry.read());
        }
        return result;
    }

    public Object value(String entryId) {
        Entry entry = entries.get(Objects.requireNonNull(entryId, "entryId"));
        if (entry == null) {
            throw new IllegalArgumentException("Unknown server config entry: " + pageId + "/" + entryId);
        }
        return entry.read();
    }

    public void apply(Map<String, Object> values) {
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
        for (Runnable commit : commits) {
            commit.run();
        }
    }

    public void save(MinecraftServer server) {
        saver.run();
        afterSave.accept(server);
    }

    public void applyAndSave(MinecraftServer server, Map<String, Object> values) throws Throwable {
        Map<String, Object> previous = snapshot();

        try {
            apply(values);
            saver.run();
            afterSave.accept(server);
        } catch (Throwable failure) {
            try {
                apply(previous);
            } catch (Throwable rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
                throw failure;
            }

            try {
                saver.run();
            } catch (Throwable rollbackSaveFailure) {
                failure.addSuppressed(rollbackSaveFailure);
            }

            try {
                afterSave.accept(server);
            } catch (Throwable rollbackApplyFailure) {
                failure.addSuppressed(rollbackApplyFailure);
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

    public static final class Builder {
        private final String pageId;
        private final Map<String, Entry> entries = new LinkedHashMap<>();
        private Runnable saver = () -> { };
        private Consumer<MinecraftServer> afterSave = server -> { };

        private Builder(String pageId) {
            String normalized = Objects.requireNonNull(pageId, "pageId").trim();
            if (normalized.isEmpty() || !normalized.contains(":")) {
                throw new IllegalArgumentException("Invalid server config page id: " + pageId);
            }
            this.pageId = normalized;
        }

        public Builder booleanValue(String id, Supplier<Boolean> reader, Consumer<Boolean> writer) {
            return booleanValue(id, reader, writer, value -> true);
        }

        public Builder booleanValue(
                String id, Supplier<Boolean> reader, Consumer<Boolean> writer, Predicate<Boolean> validator
        ) {
            Predicate<Boolean> rule = Objects.requireNonNull(validator, "validator");
            return add(
                    id, reader, writer,
                    raw -> raw instanceof Boolean value ? value : null,
                    value -> value != null && rule.test(value)
            );
        }

        public Builder intValue(String id, Supplier<Integer> reader, Consumer<Integer> writer, int minimum, int maximum) {
            return intValue(id, reader, writer, minimum, maximum, value -> true);
        }

        public Builder intValue(
                String id, Supplier<Integer> reader, Consumer<Integer> writer,
                int minimum, int maximum, Predicate<Integer> validator
        ) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            Predicate<Integer> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeInteger,
                    value -> value != null && value >= minimum && value <= maximum && rule.test(value));
        }

        public Builder longValue(String id, Supplier<Long> reader, Consumer<Long> writer, long minimum, long maximum) {
            return longValue(id, reader, writer, minimum, maximum, value -> true);
        }

        public Builder longValue(
                String id, Supplier<Long> reader, Consumer<Long> writer,
                long minimum, long maximum, Predicate<Long> validator
        ) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            Predicate<Long> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeLong,
                    value -> value != null && value >= minimum && value <= maximum && rule.test(value));
        }

        public Builder doubleValue(String id, Supplier<Double> reader, Consumer<Double> writer, double minimum, double maximum) {
            return doubleValue(id, reader, writer, minimum, maximum, value -> true);
        }

        public Builder doubleValue(
                String id, Supplier<Double> reader, Consumer<Double> writer,
                double minimum, double maximum, Predicate<Double> validator
        ) {
            if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
                throw new IllegalArgumentException("Invalid double range for " + id);
            }
            Predicate<Double> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer,
                    raw -> raw instanceof Number number ? number.doubleValue() : null,
                    value -> value != null
                            && Double.isFinite(value)
                            && value >= minimum
                            && value <= maximum
                            && rule.test(value));
        }

        public Builder colorValue(String id, Supplier<Integer> reader, Consumer<Integer> writer) {
            return colorValue(id, reader, writer, value -> true);
        }

        public Builder colorValue(
                String id, Supplier<Integer> reader, Consumer<Integer> writer, Predicate<Integer> validator
        ) {
            return intValue(id, reader, writer, 0, 0xFFFFFF, validator);
        }

        public Builder stringValue(String id, Supplier<String> reader, Consumer<String> writer) {
            return stringValue(id, reader, writer, value -> true);
        }

        public Builder stringValue(
                String id, Supplier<String> reader, Consumer<String> writer, Predicate<String> validator
        ) {
            Predicate<String> rule = Objects.requireNonNull(validator, "validator");
            return add(
                    id, reader, writer,
                    raw -> raw instanceof String value ? value : null,
                    value -> value != null && rule.test(value)
            );
        }

        public Builder choiceValue(
                String id, Supplier<String> reader, Consumer<String> writer, String... allowedValues
        ) {
            return choiceValue(id, reader, writer, value -> true, allowedValues);
        }

        public Builder choiceValue(
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
            return stringValue(id, reader, writer, value -> allowed.contains(value) && rule.test(value));
        }

        public Builder stringList(String id, Supplier<List<String>> reader, Consumer<List<String>> writer) {
            return stringList(id, reader, writer, value -> true);
        }

        public Builder stringList(
                String id, Supplier<List<String>> reader, Consumer<List<String>> writer, Predicate<List<String>> validator
        ) {
            Predicate<List<String>> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeStringList,
                    value -> value != null && rule.test(List.copyOf(value)));
        }

        public Builder intList(String id, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer) {
            return intList(id, reader, writer, value -> true);
        }

        public Builder intList(
                String id, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer, Predicate<List<Integer>> validator
        ) {
            Predicate<List<Integer>> rule = Objects.requireNonNull(validator, "validator");
            return add(id, reader, writer, Builder::decodeIntegerList,
                    value -> value != null && rule.test(List.copyOf(value)));
        }

        public Builder onSave(Runnable saver) {
            this.saver = Objects.requireNonNull(saver, "saver");
            return this;
        }

        public Builder afterSave(Consumer<MinecraftServer> afterSave) {
            this.afterSave = Objects.requireNonNull(afterSave, "afterSave");
            return this;
        }

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
            if (!(raw instanceof Number number)) return null;
            double value = number.doubleValue();
            if (!Double.isFinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE || value != Math.rint(value)) {
                return null;
            }
            return (int) value;
        }

        private static Long decodeLong(Object raw) {
            if (!(raw instanceof Number number)) return null;
            if (raw instanceof Byte || raw instanceof Short || raw instanceof Integer || raw instanceof Long) {
                return number.longValue();
            }
            double value = number.doubleValue();
            if (!Double.isFinite(value) || value < Long.MIN_VALUE || value > Long.MAX_VALUE || value != Math.rint(value)) {
                return null;
            }
            return number.longValue();
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
