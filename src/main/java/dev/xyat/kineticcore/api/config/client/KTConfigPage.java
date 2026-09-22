package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/** Public API type for kt config page. */
public final class KTConfigPage {
    /** Supported apply timing values exposed by this API. */
    public enum ApplyTiming {
        IMMEDIATE(
                "gui.kineticcore.config.apply.immediate.short",
                "gui.kineticcore.config.apply.immediate.detail",
                "gui.kineticcore.config.saved.immediate"
        ),
        RELOAD_REQUIRED(
                "gui.kineticcore.config.apply.reload_required.short",
                "gui.kineticcore.config.apply.reload_required.detail",
                "gui.kineticcore.config.saved.reload_required"
        ),
        NEXT_WORLD_LOAD(
                "gui.kineticcore.config.apply.next_world_load.short",
                "gui.kineticcore.config.apply.next_world_load.detail",
                "gui.kineticcore.config.saved.next_world_load"
        ),
        RESTART_GAME(
                "gui.kineticcore.config.apply.restart_game.short",
                "gui.kineticcore.config.apply.restart_game.detail",
                "gui.kineticcore.config.saved.restart_game"
        ),
        MIXED(
                "gui.kineticcore.config.apply.mixed.short",
                "gui.kineticcore.config.apply.mixed.detail",
                "gui.kineticcore.config.saved.mixed"
        );

        private final String shortTranslationKey;
        private final String detailTranslationKey;
        private final String savedTranslationKey;

        ApplyTiming(
                String shortTranslationKey,
                String detailTranslationKey,
                String savedTranslationKey
        ) {
            this.shortTranslationKey = shortTranslationKey;
            this.detailTranslationKey = detailTranslationKey;
            this.savedTranslationKey = savedTranslationKey;
        }

        String shortTranslationKey() {
            return shortTranslationKey;
        }

        String detailTranslationKey() {
            return detailTranslationKey;
        }

        String savedTranslationKey() {
            return savedTranslationKey;
        }

    }

    private static final Pattern PAGE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern ENTRY_ID = Pattern.compile("[a-z0-9_.-]+");

    private final String id;
    private final Component title;
    private final Component description;
    private final KTConfigScope scope;
    private final boolean serverManaged;
    private final ApplyTiming applyTiming;
    private final Component applyNotice;
    private final List<KTConfigEntry<?>> entries;
    private final Runnable saver;

    private KTConfigPage(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.scope = builder.scope;
        this.serverManaged = builder.serverManaged;
        this.applyTiming = builder.applyTiming;
        this.applyNotice = builder.applyNotice;
        this.entries = List.copyOf(builder.entries);
        this.saver = builder.saver;
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder(String id, Component title) {
        return new Builder(id, title);
    }

    /**
     * Returns the id.
     */
    public String id() {
        return id;
    }

    /**
     * Returns the title.
     */
    public Component title() {
        return title;
    }

    /**
     * Returns the description.
     */
    public Component description() {
        return description;
    }

    /**
     * Returns the scope.
     */
    public KTConfigScope scope() {
        return scope;
    }

    /**
     * Performs the server managed API operation.
     */
    public boolean serverManaged() {
        return serverManaged;
    }

    /**
     * Applies timing.
     */
    public ApplyTiming applyTiming() {
        return applyTiming;
    }

    /**
     * Applies notice.
     */
    public Component applyNotice() {
        return applyNotice;
    }

    /**
     * Returns the entries.
     */
    public List<KTConfigEntry<?>> entries() {
        return entries;
    }

    /**
     * Performs the shows apply timing API operation.
     */
    public boolean showsApplyTiming() {
        return applyNotice != null || entries.stream().anyMatch(KTConfigEntry::isValueEntry);
    }

    /**
     * Applies detail.
     */
    public Component applyDetail() {
        return applyNotice != null
                ? applyNotice
                : KineticI18n.translatable(applyTiming.detailTranslationKey());
    }

    /**
     * Saves the current values.
     */
    public void save() {
        saver.run();
    }

    /** Builder for definitions owned by the enclosing API. */
    public static final class Builder {
        private final String id;
        private final Component title;
        private final List<KTConfigEntry<?>> entries = new ArrayList<>();
        private final Set<String> entryIds = new HashSet<>();
        private Component description;
        private KTConfigScope scope = KTConfigScope.LOCAL_INSTALLATION;
        private boolean serverManaged;
        private ApplyTiming applyTiming = ApplyTiming.MIXED;
        private Component applyNotice;
        private Runnable saver = () -> { };
        private int structuralIndex;

        private Builder(String id, Component title) {
            this.id = requirePageId(id);
            this.title = Objects.requireNonNull(title, "title");
        }

        /**
         * Performs the page description API operation.
         */
        public Builder pageDescription(Component description) {
            this.description = description;
            return this;
        }

        /**
         * Returns the scope.
         */
        public Builder scope(KTConfigScope scope) {
            this.scope = Objects.requireNonNull(scope, "scope");
            return this;
        }

        /**
         * Performs the server managed API operation.
         */
        public Builder serverManaged() {
            this.serverManaged = true;
            return this;
        }

        /**
         * Applies timing.
         */
        public Builder applyTiming(ApplyTiming applyTiming) {
            this.applyTiming = Objects.requireNonNull(applyTiming, "applyTiming");
            return this;
        }

        /**
         * Applies notice.
         */
        public Builder applyNotice(Component applyNotice) {
            this.applyNotice = Objects.requireNonNull(applyNotice, "applyNotice");
            return this;
        }

        /**
         * Registers a listener for save.
         */
        public Builder onSave(Runnable saver) {
            this.saver = Objects.requireNonNull(saver, "saver");
            return this;
        }

        /**
         * Performs the section API operation.
         */
        public Builder section(Component label) {
            entries.add(KTConfigEntry.structural(
                    "__section_" + structuralIndex++,
                    KTConfigEntry.Type.SECTION,
                    Objects.requireNonNull(label, "label")
            ));
            return this;
        }

        /**
         * Adds a visual divider between configuration groups without adding a text heading.
         * Dividers are rendered in the gap before the next visible row and do not consume
         * a full configuration row. Leading and repeated dividers are collapsed.
         */
        public Builder divider() {
            entries.add(KTConfigEntry.structural(
                    "__divider_" + structuralIndex++,
                    KTConfigEntry.Type.DIVIDER,
                    Component.empty()
            ));
            return this;
        }

        /**
         * Returns the description.
         */
        public Builder description(Component text) {
            entries.add(KTConfigEntry.structural(
                    "__description_" + structuralIndex++,
                    KTConfigEntry.Type.DESCRIPTION,
                    Objects.requireNonNull(text, "text")
            ));
            return this;
        }

        /**
         * Returns the boolean value.
         */
        public Builder booleanValue(
                String id, Component label, Supplier<Boolean> reader, Consumer<Boolean> writer,
                boolean defaultValue, Component tooltip
        ) {
            return booleanValueValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the boolean value validated API operation.
         */
        public Builder booleanValueValidated(
                String id, Component label, Supplier<Boolean> reader, Consumer<Boolean> writer,
                boolean defaultValue, Predicate<Boolean> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.BOOLEAN, label, tooltip, reader, writer,
                    defaultValue, null, null, null,
                    decoder(Boolean.class), UnaryOperator.identity(), validator
            );
        }

        /**
         * Returns the int value.
         */
        public Builder intValue(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, int minimum, int maximum, Component tooltip
        ) {
            return intValueValidated(id, label, reader, writer, defaultValue, minimum, maximum, value -> true, tooltip);
        }

        /**
         * Performs the int value validated API operation.
         */
        public Builder intValueValidated(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, int minimum, int maximum, Predicate<Integer> validator, Component tooltip
        ) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            return add(
                    id, KTConfigEntry.Type.INTEGER, label, tooltip, reader, writer,
                    defaultValue, minimum, maximum, null,
                    decoder(Integer.class), UnaryOperator.identity(), validator
            );
        }

        /**
         * Returns the long value.
         */
        public Builder longValue(
                String id, Component label, Supplier<Long> reader, Consumer<Long> writer,
                long defaultValue, long minimum, long maximum, Component tooltip
        ) {
            return longValueValidated(id, label, reader, writer, defaultValue, minimum, maximum, value -> true, tooltip);
        }

        /**
         * Performs the long value validated API operation.
         */
        public Builder longValueValidated(
                String id, Component label, Supplier<Long> reader, Consumer<Long> writer,
                long defaultValue, long minimum, long maximum, Predicate<Long> validator, Component tooltip
        ) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            return add(
                    id, KTConfigEntry.Type.LONG, label, tooltip, reader, writer,
                    defaultValue, minimum, maximum, null,
                    decoder(Long.class), UnaryOperator.identity(), validator
            );
        }

        /**
         * Performs the int value unbounded API operation.
         */
        public Builder intValueUnbounded(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, Component tooltip
        ) {
            return intValue(
                    id, label, reader, writer, defaultValue,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, tooltip
            );
        }

        /**
         * Performs the int value unbounded validated API operation.
         */
        public Builder intValueUnboundedValidated(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, Predicate<Integer> validator, Component tooltip
        ) {
            return intValueValidated(
                    id, label, reader, writer, defaultValue,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, validator, tooltip
            );
        }

        /**
         * Performs the long value unbounded API operation.
         */
        public Builder longValueUnbounded(
                String id, Component label, Supplier<Long> reader, Consumer<Long> writer,
                long defaultValue, Component tooltip
        ) {
            return longValue(
                    id, label, reader, writer, defaultValue,
                    Long.MIN_VALUE, Long.MAX_VALUE, tooltip
            );
        }

        /**
         * Performs the long value unbounded validated API operation.
         */
        public Builder longValueUnboundedValidated(
                String id, Component label, Supplier<Long> reader, Consumer<Long> writer,
                long defaultValue, Predicate<Long> validator, Component tooltip
        ) {
            return longValueValidated(
                    id, label, reader, writer, defaultValue,
                    Long.MIN_VALUE, Long.MAX_VALUE, validator, tooltip
            );
        }

        /**
         * Returns the double value.
         */
        public Builder doubleValue(
                String id, Component label, Supplier<Double> reader, Consumer<Double> writer,
                double defaultValue, double minimum, double maximum, Component tooltip
        ) {
            return doubleValueValidated(id, label, reader, writer, defaultValue, minimum, maximum, value -> true, tooltip);
        }

        /**
         * Performs the double value validated API operation.
         */
        public Builder doubleValueValidated(
                String id, Component label, Supplier<Double> reader, Consumer<Double> writer,
                double defaultValue, double minimum, double maximum, Predicate<Double> validator, Component tooltip
        ) {
            if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
                throw new IllegalArgumentException("invalid range for " + id);
            }
            return add(
                    id, KTConfigEntry.Type.DOUBLE, label, tooltip, reader, writer,
                    defaultValue, minimum, maximum, null,
                    decoder(Double.class), UnaryOperator.identity(), validator
            );
        }

        /**
         * Performs the double value unbounded API operation.
         */
        public Builder doubleValueUnbounded(
                String id, Component label, Supplier<Double> reader, Consumer<Double> writer,
                double defaultValue, Component tooltip
        ) {
            return doubleValue(
                    id, label, reader, writer, defaultValue,
                    -Double.MAX_VALUE, Double.MAX_VALUE, tooltip
            );
        }

        /**
         * Performs the double value unbounded validated API operation.
         */
        public Builder doubleValueUnboundedValidated(
                String id, Component label, Supplier<Double> reader, Consumer<Double> writer,
                double defaultValue, Predicate<Double> validator, Component tooltip
        ) {
            return doubleValueValidated(
                    id, label, reader, writer, defaultValue,
                    -Double.MAX_VALUE, Double.MAX_VALUE, validator, tooltip
            );
        }

        /**
         * Returns the string value.
         */
        public Builder stringValue(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip
        ) {
            return stringValueValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the string value validated API operation.
         */
        public Builder stringValueValidated(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Predicate<String> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.STRING, label, tooltip, reader, writer,
                    defaultValue, null, null, null,
                    decoder(String.class), UnaryOperator.identity(), validator
            );
        }

        /**
         * Returns the long text value.
         */
        public Builder longTextValue(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip
        ) {
            return longTextValueValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the long text value validated API operation.
         */
        public Builder longTextValueValidated(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Predicate<String> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.LONG_TEXT, label, tooltip, reader, writer,
                    defaultValue, null, null, null,
                    decoder(String.class), UnaryOperator.identity(), validator
            );
        }

        /**
         * Performs the choice API operation.
         */
        public Builder choice(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip, String... choices
        ) {
            return choiceValidated(id, label, reader, writer, defaultValue, value -> true, tooltip, choices);
        }

        /**
         * Performs the choice validated API operation.
         */
        public Builder choiceValidated(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Predicate<String> validator, Component tooltip, String... choices
        ) {
            KTConfigEntry.ChoiceOption[] options = Arrays.stream(choices)
                    .map(value -> new KTConfigEntry.ChoiceOption(value, Component.empty(), Component.empty()))
                    .toArray(KTConfigEntry.ChoiceOption[]::new);
            return choiceOptionsValidated(id, label, reader, writer, defaultValue, validator, tooltip, options);
        }

        /**
         * Performs the choice options API operation.
         */
        public Builder choiceOptions(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip, KTConfigEntry.ChoiceOption... choices
        ) {
            return choiceOptionsValidated(id, label, reader, writer, defaultValue, value -> true, tooltip, choices);
        }

        /**
         * Performs the choice options validated API operation.
         */
        public Builder choiceOptionsValidated(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Predicate<String> validator, Component tooltip,
                KTConfigEntry.ChoiceOption... choices
        ) {
            List<KTConfigEntry.ChoiceOption> values = List.copyOf(Arrays.asList(choices));
            if (values.isEmpty()) throw new IllegalArgumentException("choices cannot be empty for " + id);
            Set<String> distinctValues = new HashSet<>();
            for (KTConfigEntry.ChoiceOption option : values) {
                if (!distinctValues.add(option.value())) {
                    throw new IllegalArgumentException("Duplicate choice value for " + id + ": " + option.value());
                }
            }
            if (values.stream().noneMatch(option -> option.value().equals(defaultValue))) {
                throw new IllegalArgumentException("default value is not a choice for " + id);
            }
            return add(
                    id, KTConfigEntry.Type.CHOICE, label, tooltip, reader, writer,
                    defaultValue, null, null, values,
                    decoder(String.class), UnaryOperator.identity(), validator
            );
        }

        /**
         * Performs the translated choice API operation.
         */
        public Builder translatedChoice(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip, String translationKeyPrefix, String... choices
        ) {
            return translatedChoiceValidated(
                    id, label, reader, writer, defaultValue, value -> true,
                    tooltip, translationKeyPrefix, choices
            );
        }

        /**
         * Performs the translated choice validated API operation.
         */
        public Builder translatedChoiceValidated(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Predicate<String> validator, Component tooltip,
                String translationKeyPrefix, String... choices
        ) {
            String prefix = Objects.requireNonNull(translationKeyPrefix, "translationKeyPrefix").trim();
            if (prefix.isEmpty()) throw new IllegalArgumentException("translationKeyPrefix cannot be blank");
            KTConfigEntry.ChoiceOption[] options = Arrays.stream(choices)
                    .map(value -> new KTConfigEntry.ChoiceOption(
                            value,
                            KineticI18n.translatable(prefix + "." + value.toLowerCase(java.util.Locale.ROOT)),
                            Component.empty()
                    ))
                    .toArray(KTConfigEntry.ChoiceOption[]::new);
            return choiceOptionsValidated(id, label, reader, writer, defaultValue, validator, tooltip, options);
        }

        /**
         * Performs the string list API operation.
         */
        public Builder stringList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return stringListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the string list validated API operation.
         */
        public Builder stringListValidated(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Predicate<List<String>> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.STRING_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeStringList, ArrayList::new, validator
            );
        }

        /**
         * Performs the entity list API operation.
         */
        public Builder entityList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return entityListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the entity list validated API operation.
         */
        public Builder entityListValidated(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Predicate<List<String>> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.ENTITY_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeStringList, ArrayList::new, validator
            );
        }

        /**
         * Performs the item list API operation.
         */
        public Builder itemList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return itemListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the item list validated API operation.
         */
        public Builder itemListValidated(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Predicate<List<String>> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.ITEM_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeStringList, ArrayList::new, validator
            );
        }

        /**
         * Performs the item rule list API operation.
         */
        public Builder itemRuleList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return itemRuleListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the item rule list validated API operation.
         */
        public Builder itemRuleListValidated(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Predicate<List<String>> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.ITEM_RULE_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeStringList, ArrayList::new, validator
            );
        }


        /**
         * Returns the tick seconds value.
         */
        public Builder tickSecondsValue(
                String id, Component label, Supplier<Integer> tickReader, Consumer<Integer> tickWriter,
                int defaultTicks, int minimumTicks, int maximumTicks, Component tooltip
        ) {
            return tickSecondsValueValidated(
                    id, label, tickReader, tickWriter,
                    defaultTicks, minimumTicks, maximumTicks, value -> true, tooltip
            );
        }

        /**
         * Performs the tick seconds value validated API operation.
         */
        public Builder tickSecondsValueValidated(
                String id, Component label, Supplier<Integer> tickReader, Consumer<Integer> tickWriter,
                int defaultTicks, int minimumTicks, int maximumTicks,
                Predicate<Double> validator, Component tooltip
        ) {
            if (minimumTicks > maximumTicks) {
                throw new IllegalArgumentException("minimumTicks > maximumTicks for " + id);
            }
            Predicate<Double> rule = Objects.requireNonNull(validator, "validator");
            Supplier<Double> secondsReader = () -> ticksToSeconds(
                    clampTicks(tickReader.get(), minimumTicks, maximumTicks));
            Consumer<Double> secondsWriter = seconds -> tickWriter.accept(
                    secondsToTicks(seconds, minimumTicks, maximumTicks));
            return doubleValueValidated(
                    id, label, secondsReader, secondsWriter,
                    ticksToSeconds(defaultTicks),
                    ticksToSeconds(minimumTicks),
                    ticksToSeconds(maximumTicks),
                    rule, tooltip
            );
        }

        /**
         * Performs the int list API operation.
         */
        public Builder intList(
                String id, Component label, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer,
                List<Integer> defaultValue, Component tooltip
        ) {
            return intListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the int list validated API operation.
         */
        public Builder intListValidated(
                String id, Component label, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer,
                List<Integer> defaultValue, Predicate<List<Integer>> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.INTEGER_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeIntegerList, ArrayList::new, validator
            );
        }

        /**
         * Performs the color API operation.
         */
        public Builder color(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, Component tooltip
        ) {
            return colorValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Performs the color validated API operation.
         */
        public Builder colorValidated(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, Predicate<Integer> validator, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.COLOR, label, tooltip, reader, writer,
                    defaultValue, 0, 0xFFFFFF, null,
                    decoder(Integer.class), UnaryOperator.identity(), validator
            );
        }

        /**
         * Performs the action API operation.
         */
        public Builder action(String id, Component label, Runnable action, Component tooltip) {
            requireEntryId(id);
            requireUniqueEntryId(id);
            KTConfigEntry<Void> entry = KTConfigEntry.action(
                    id,
                    Objects.requireNonNull(label, "label"),
                    tooltip,
                    Objects.requireNonNull(action, "action")
            );
            entries.add(entry);
            entryIds.add(id);
            return this;
        }

        /**
         * Builds the configured API value.
         */
        public KTConfigPage build() {
            if (serverManaged && scope != KTConfigScope.SERVER_AUTHORITATIVE) {
                throw new IllegalStateException(
                        "serverManaged is only valid for SERVER_AUTHORITATIVE pages: " + id
                );
            }
            return new KTConfigPage(this);
        }

        private <T> Builder add(
                String id,
                KTConfigEntry.Type type,
                Component label,
                Component tooltip,
                Supplier<T> reader,
                Consumer<T> writer,
                T defaultValue,
                Number minimum,
                Number maximum,
                List<KTConfigEntry.ChoiceOption> choices,
                Function<Object, T> decoder,
                UnaryOperator<T> copier
        ) {
            return add(
                    id, type, label, tooltip, reader, writer,
                    defaultValue, minimum, maximum, choices, decoder, copier,
                    value -> true
            );
        }

        private <T> Builder add(
                String id,
                KTConfigEntry.Type type,
                Component label,
                Component tooltip,
                Supplier<T> reader,
                Consumer<T> writer,
                T defaultValue,
                Number minimum,
                Number maximum,
                List<KTConfigEntry.ChoiceOption> choices,
                Function<Object, T> decoder,
                UnaryOperator<T> copier,
                Predicate<T> validator
        ) {
            requireEntryId(id);
            requireUniqueEntryId(id);
            KTConfigEntry<T> entry = KTConfigEntry.value(
                    id,
                    type,
                    Objects.requireNonNull(label, "label"),
                    tooltip,
                    reader,
                    writer,
                    defaultValue,
                    minimum,
                    maximum,
                    choices,
                    decoder,
                    copier,
                    Objects.requireNonNull(validator, "validator")
            );
            if (!entry.accepts(defaultValue)) {
                throw new IllegalArgumentException("Invalid default value for " + id + ": " + defaultValue);
            }
            entries.add(entry);
            entryIds.add(id);
            return this;
        }

        private void requireUniqueEntryId(String id) {
            if (entryIds.contains(id)) {
                throw new IllegalArgumentException("Duplicate entry id: " + id);
            }
        }

        private static <T> Function<Object, T> decoder(Class<T> type) {
            return value -> type.isInstance(value) ? type.cast(value) : null;
        }

        private static List<String> decodeStringList(Object value) {
            if (!(value instanceof List<?> list)) return null;
            List<String> result = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof String string)) return null;
                result.add(string);
            }
            return result;
        }

        private static List<Integer> decodeIntegerList(Object value) {
            if (!(value instanceof List<?> list)) return null;
            List<Integer> result = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof Integer integer)) return null;
                result.add(integer);
            }
            return result;
        }

        private static String requirePageId(String id) {
            Objects.requireNonNull(id, "id");
            if (!PAGE_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Page id must be namespaced: " + id);
            }
            return id;
        }

        private static void requireEntryId(String id) {
            Objects.requireNonNull(id, "id");
            if (!ENTRY_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid entry id: " + id);
            }
            if (id.startsWith("__")) {
                throw new IllegalArgumentException("Entry ids starting with '__' are reserved: " + id);
            }
        }

        private static double ticksToSeconds(int ticks) {
            return ticks / 20.0D;
        }

        private static int secondsToTicks(double seconds, int minimumTicks, int maximumTicks) {
            if (!Double.isFinite(seconds)) {
                throw new IllegalArgumentException("seconds must be finite");
            }
            long rounded = Math.round(seconds * 20.0D);
            if (minimumTicks == 0 && seconds > 0.0D && rounded == 0L && maximumTicks > 0) {
                rounded = 1L;
            }
            return (int) Math.max(minimumTicks, Math.min(maximumTicks, rounded));
        }

        private static int clampTicks(int ticks, int minimumTicks, int maximumTicks) {
            return Math.max(minimumTicks, Math.min(maximumTicks, ticks));
        }
    }
}
