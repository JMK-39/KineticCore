package dev.xyat.kineticcore.config.client;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class KTConfigEntry<T> {
    public enum Type {
        SECTION,
        DESCRIPTION,
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        STRING,
        CHOICE,
        STRING_LIST,
        ITEM_LIST,
        ITEM_RULE_LIST,
        INTEGER_LIST,
        COLOR,
        ACTION,
        ENTITY_LIST
    }

    private final String id;
    private final Type type;
    private final Component label;
    private final Component tooltip;
    private final Supplier<T> reader;
    private final Consumer<T> writer;
    private final T defaultValue;
    private final Number minimum;
    private final Number maximum;
    private final List<String> choices;
    private final Function<Object, T> decoder;
    private final UnaryOperator<T> copier;
    private final Runnable action;

    private KTConfigEntry(
            String id,
            Type type,
            Component label,
            Component tooltip,
            Supplier<T> reader,
            Consumer<T> writer,
            T defaultValue,
            Number minimum,
            Number maximum,
            List<String> choices,
            Function<Object, T> decoder,
            UnaryOperator<T> copier,
            Runnable action
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.label = Objects.requireNonNull(label, "label");
        this.tooltip = tooltip;
        this.reader = reader;
        this.writer = writer;
        this.decoder = decoder;
        this.copier = copier;
        this.defaultValue = copyTyped(defaultValue);
        this.minimum = minimum;
        this.maximum = maximum;
        this.choices = choices == null ? List.of() : List.copyOf(choices);
        this.action = action;
    }

    static KTConfigEntry<Void> structural(String id, Type type, Component label) {
        if (type != Type.SECTION && type != Type.DESCRIPTION) {
            throw new IllegalArgumentException("Not a structural entry type: " + type);
        }
        return new KTConfigEntry<>(
                id, type, label, null,
                null, null, null, null, null, null,
                null, null, null
        );
    }

    static KTConfigEntry<Void> action(String id, Component label, Component tooltip, Runnable action) {
        return new KTConfigEntry<>(
                id, Type.ACTION, label, tooltip,
                null, null, null, null, null, null,
                null, null, Objects.requireNonNull(action, "action")
        );
    }

    static <T> KTConfigEntry<T> value(
            String id,
            Type type,
            Component label,
            Component tooltip,
            Supplier<T> reader,
            Consumer<T> writer,
            T defaultValue,
            Number minimum,
            Number maximum,
            List<String> choices,
            Function<Object, T> decoder,
            UnaryOperator<T> copier
    ) {
        return new KTConfigEntry<>(
                id, type, label, tooltip,
                Objects.requireNonNull(reader, "reader"),
                Objects.requireNonNull(writer, "writer"),
                defaultValue, minimum, maximum, choices,
                Objects.requireNonNull(decoder, "decoder"),
                Objects.requireNonNull(copier, "copier"),
                null
        );
    }

    public String id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public Component label() {
        return label;
    }

    public Component tooltip() {
        return tooltip;
    }

    public T read() {
        if (reader == null) return null;
        return copyTyped(reader.get());
    }

    public void write(T value) {
        if (writer == null) return;
        writer.accept(copyTyped(value));
    }

    public T defaultValue() {
        return copyTyped(defaultValue);
    }

    public Number minimum() {
        return minimum;
    }

    public Number maximum() {
        return maximum;
    }

    public List<String> choices() {
        return choices;
    }

    public boolean isValue() {
        return decoder != null;
    }

    public boolean accepts(Object value) {
        T decoded = decode(value);
        if (decoded == null) return false;

        return switch (type) {
            case INTEGER -> inLongRange(((Integer) decoded).longValue());
            case LONG -> inLongRange((Long) decoded);
            case DOUBLE -> Double.isFinite((Double) decoded) && inDoubleRange((Double) decoded);
            case CHOICE -> choices.contains(decoded);
            case COLOR -> {
                int color = (Integer) decoded;
                yield color >= 0 && color <= 0xFFFFFF;
            }
            case BOOLEAN, STRING, STRING_LIST, ITEM_LIST, ITEM_RULE_LIST, INTEGER_LIST, ENTITY_LIST -> true;
            case SECTION, DESCRIPTION, ACTION -> false;
        };
    }

    public void runAction() {
        if (type != Type.ACTION || action == null) {
            throw new IllegalStateException("Entry is not an action: " + id);
        }
        action.run();
    }

    Object snapshot(Object value) {
        T decoded = decode(value);
        if (decoded == null) return null;
        return copyTyped(decoded);
    }

    Object readSnapshot() {
        return read();
    }

    Object defaultSnapshot() {
        return defaultValue();
    }

    void writeSnapshot(Object value) {
        T decoded = decode(value);
        if (decoded == null || !accepts(decoded)) {
            throw new IllegalArgumentException("Invalid value for " + id + ": " + value);
        }
        write(decoded);
    }

    private T decode(Object value) {
        return decoder == null ? null : decoder.apply(value);
    }

    private T copyTyped(T value) {
        return value == null || copier == null ? value : copier.apply(value);
    }

    private boolean inLongRange(long value) {
        return (minimum == null || value >= minimum.longValue())
                && (maximum == null || value <= maximum.longValue());
    }

    private boolean inDoubleRange(double value) {
        return (minimum == null || value >= minimum.doubleValue())
                && (maximum == null || value <= maximum.doubleValue());
    }
}
