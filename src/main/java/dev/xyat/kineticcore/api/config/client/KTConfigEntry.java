package dev.xyat.kineticcore.api.config.client;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Immutable configuration-entry descriptors consumed by Kinetic configuration screens.
 */
public final class KTConfigEntry<T> {
    /** Identifies the editor/control kind represented by a configuration entry. */
    public enum Type {
        SECTION,
        DIVIDER,
        DESCRIPTION,
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        STRING,
        LONG_TEXT,
        CHOICE,
        STRING_LIST,
        ITEM_LIST,
        ITEM_RULE_LIST,
        INTEGER_LIST,
        COLOR,
        ACTION,
        ENTITY_LIST
    }

    /**
     * One configuration choice with a persisted raw value and optional display-only metadata.
     * <p>Configuration persistence, validation, and callbacks always use {@code value}. The
     * {@code translation} component is presentation-only: Kinetic choice controls omit it for English
     * game languages and may show it in the theme translation color for non-English languages. It is
     * never appended to or written back as part of the raw value.</p>
     */
    public record ChoiceOption(String value, Component translation, Component tooltip) {
        /** Normalizes persisted value and display-only metadata for one choice option. */
        public ChoiceOption {
            value = Objects.requireNonNull(value, "value").trim();
            if (value.isEmpty()) throw new IllegalArgumentException("choice value cannot be blank");
            translation = Objects.requireNonNullElse(translation, Component.empty());
            tooltip = Objects.requireNonNullElse(tooltip, Component.empty());
        }
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
    private final List<ChoiceOption> choices;
    private final Function<Object, T> decoder;
    private final UnaryOperator<T> copier;
    private final Predicate<T> validator;
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
            List<ChoiceOption> choices,
            Function<Object, T> decoder,
            UnaryOperator<T> copier,
            Predicate<T> validator,
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
        this.validator = validator == null ? value -> true : validator;
        this.defaultValue = copyTyped(defaultValue);
        this.minimum = minimum;
        this.maximum = maximum;
        this.choices = choices == null ? List.of() : List.copyOf(choices);
        this.action = action;
    }

    static KTConfigEntry<Void> structural(String id, Type type, Component label) {
        if (type != Type.SECTION && type != Type.DIVIDER && type != Type.DESCRIPTION) {
            throw new IllegalArgumentException("Not a structural entry type: " + type);
        }
        return new KTConfigEntry<>(
                id, type, label, null,
                null, null, null, null, null, null,
                null, null, null, null
        );
    }

    static KTConfigEntry<Void> action(String id, Component label, Component tooltip, Runnable action) {
        return new KTConfigEntry<>(
                id, Type.ACTION, label, tooltip,
                null, null, null, null, null, null,
                null, null, null, Objects.requireNonNull(action, "action")
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
            List<ChoiceOption> choices,
            Function<Object, T> decoder,
            UnaryOperator<T> copier,
            Predicate<T> validator
    ) {
        return new KTConfigEntry<>(
                id, type, label, tooltip,
                Objects.requireNonNull(reader, "reader"),
                Objects.requireNonNull(writer, "writer"),
                defaultValue, minimum, maximum, choices,
                Objects.requireNonNull(decoder, "decoder"),
                Objects.requireNonNull(copier, "copier"),
                Objects.requireNonNull(validator, "validator"),
                null
        );
    }

    /** Returns the stable entry identifier used for snapshots, pending values, and server synchronization. */
    public String id() {
        return id;
    }

    /** Returns the editor/control kind used to render and validate this entry. */
    public Type type() {
        return type;
    }

    /** Returns the player-facing label rendered for this entry. */
    public Component label() {
        return label;
    }

    /** Returns the optional player-facing tooltip, or {@code null} when this entry has no tooltip. */
    public Component tooltip() {
        return tooltip;
    }

    /** Reads the current business value and returns a defensive copy when the value is mutable. */
    public T read() {
        if (reader == null) return null;
        return copyTyped(reader.get());
    }

    /** Decodes and validates a public write, restoring its previous value if the writer fails. */
    public void write(T value) {
        if (writer == null) return;
        T decoded = decode(value);
        if (decoded == null || !acceptsDecoded(decoded)) {
            throw new IllegalArgumentException("Invalid value for " + id + ": " + value);
        }
        writeAtomically(decoded);
    }

    /** Returns a defensive copy of the default business value configured for this entry. */
    public T defaultValue() {
        return copyTyped(defaultValue);
    }

    /** Returns the inclusive numeric minimum, or {@code null} for non-numeric/unbounded entries. */
    public Number minimum() {
        return minimum;
    }

    /** Returns the inclusive numeric maximum, or {@code null} for non-numeric/unbounded entries. */
    public Number maximum() {
        return maximum;
    }

    /**
     * Returns the immutable choice descriptors for a {@link Type#CHOICE} entry.
     * Each option keeps its persisted raw value separate from display-only translation and tooltip text.
     */
    public List<ChoiceOption> choiceOptions() {
        return choices;
    }

    /** Returns whether this entry stores a business value rather than structural text or an action. */
    public boolean isValueEntry() {
        return decoder != null;
    }

    /**
     * Returns whether the supplied value can be decoded for this entry and passes built-in bounds,
     * choice membership, color bounds, and the business validator supplied by the page builder.
     */
    public boolean accepts(Object value) {
        T decoded = decode(value);
        return decoded != null && acceptsDecoded(decoded);
    }

    /** Validate a value already converted by this entry's decoder exactly once. */
    private boolean acceptsDecoded(T decoded) {
        boolean builtInValid = switch (type) {
            case INTEGER -> inLongRange(((Integer) decoded).longValue());
            case LONG -> inLongRange((Long) decoded);
            case DOUBLE -> Double.isFinite((Double) decoded) && inDoubleRange((Double) decoded);
            case CHOICE -> choices.stream().anyMatch(option -> option.value().equals(decoded));
            case COLOR -> {
                int color = (Integer) decoded;
                yield color >= 0 && color <= 0xFFFFFF;
            }
            case BOOLEAN, STRING, LONG_TEXT, STRING_LIST, ITEM_LIST, ITEM_RULE_LIST, INTEGER_LIST, ENTITY_LIST -> true;
            case SECTION, DIVIDER, DESCRIPTION, ACTION -> false;
        };
        return builtInValid && validator.test(decoded);
    }

    /** Executes this entry's action callback.
     * @throws IllegalStateException when this entry is not {@link Type#ACTION}
     */
    public void runAction() {
        if (type != Type.ACTION || action == null) {
            throw new IllegalStateException("Entry is not an action: " + id);
        }
        action.run();
    }

    /**
     * Converts a compatible value to this entry's typed representation and returns a defensive snapshot.
     * Returns {@code null} when the supplied object cannot be decoded as this entry's value type.
     */
    public Object snapshot(Object value) {
        T decoded = decode(value);
        if (decoded == null) return null;
        return copyTyped(decoded);
    }

    /**
     * Decodes, validates, defensively copies, and writes one snapshot value through this entry.
     * @throws IllegalArgumentException when the supplied value is incompatible or invalid
     */
    public void writeSnapshot(Object value) {
        T decoded = decode(value);
        if (decoded == null || !acceptsDecoded(decoded)) {
            throw new IllegalArgumentException("Invalid value for " + id + ": " + value);
        }
        // Already decoded and validated above: never run user validators twice.
        writeAtomically(decoded);
    }

    /**
     * Validates one authoritative snapshot exactly once, then writes it as an atomic field update.
     * Invalid inputs return {@code false} without reading or changing the existing value. If the
     * business writer changes the value and then fails, its previous snapshot is restored before
     * the original failure is propagated. This operation does not save a config file or notify UI.
     *
     * @return {@code true} if the snapshot passed validation and was written
     */
    public boolean applySnapshotWithRollback(Object value) {
        if (writer == null) return false;
        T decoded = decode(value);
        if (decoded == null || !acceptsDecoded(decoded)) return false;
        writeAtomically(decoded);
        return true;
    }

    /** Apply a validated value atomically; the failing writer may already have changed the field. */
    private void writeAtomically(T decoded) {
        T previous = read();
        try {
            writeDecoded(decoded);
        } catch (RuntimeException | Error failure) {
            try {
                writeDecoded(previous);
            } catch (RuntimeException | Error rollbackFailure) {
                if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    private void writeDecoded(T decoded) {
        writer.accept(copyTyped(decoded));
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
