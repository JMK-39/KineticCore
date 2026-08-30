package dev.xyat.kineticcore.config.server;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            return add(id, reader, writer, raw -> raw instanceof Boolean value ? value : null, Objects::nonNull);
        }

        public Builder intValue(String id, Supplier<Integer> reader, Consumer<Integer> writer, int minimum, int maximum) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            return add(id, reader, writer, Builder::decodeInteger,
                    value -> value != null && value >= minimum && value <= maximum);
        }

        public Builder longValue(String id, Supplier<Long> reader, Consumer<Long> writer, long minimum, long maximum) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            return add(id, reader, writer, Builder::decodeLong,
                    value -> value != null && value >= minimum && value <= maximum);
        }

        public Builder doubleValue(String id, Supplier<Double> reader, Consumer<Double> writer, double minimum, double maximum) {
            if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
                throw new IllegalArgumentException("Invalid double range for " + id);
            }
            return add(id, reader, writer,
                    raw -> raw instanceof Number number ? number.doubleValue() : null,
                    value -> value != null && Double.isFinite(value) && value >= minimum && value <= maximum);
        }

        public Builder stringValue(String id, Supplier<String> reader, Consumer<String> writer) {
            return add(id, reader, writer, raw -> raw instanceof String value ? value : null, Objects::nonNull);
        }

        public Builder stringList(String id, Supplier<List<String>> reader, Consumer<List<String>> writer) {
            return add(id, reader, writer, Builder::decodeStringList, Objects::nonNull);
        }

        public Builder integerList(String id, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer) {
            return add(id, reader, writer, Builder::decodeIntegerList, Objects::nonNull);
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
