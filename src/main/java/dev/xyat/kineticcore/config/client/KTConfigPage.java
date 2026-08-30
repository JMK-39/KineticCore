package dev.xyat.kineticcore.config.client;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class KTConfigPage {
    public enum ApplyTiming {
        IMMEDIATE(
                "gui.kineticcore.config.apply.immediate.short",
                "gui.kineticcore.config.apply.immediate.detail",
                "gui.kineticcore.config.saved.immediate",
                0xFF55FF55
        ),
        RELOAD_REQUIRED(
                "gui.kineticcore.config.apply.reload_required.short",
                "gui.kineticcore.config.apply.reload_required.detail",
                "gui.kineticcore.config.saved.reload_required",
                0xFFFFFF55
        ),
        NEXT_WORLD_LOAD(
                "gui.kineticcore.config.apply.next_world_load.short",
                "gui.kineticcore.config.apply.next_world_load.detail",
                "gui.kineticcore.config.saved.next_world_load",
                0xFFFFFF55
        ),
        RESTART_GAME(
                "gui.kineticcore.config.apply.restart_game.short",
                "gui.kineticcore.config.apply.restart_game.detail",
                "gui.kineticcore.config.saved.restart_game",
                0xFFFFAA00
        ),
        MIXED(
                "gui.kineticcore.config.apply.mixed.short",
                "gui.kineticcore.config.apply.mixed.detail",
                "gui.kineticcore.config.saved.mixed",
                0xFFFFDD55
        );

        private final String shortTranslationKey;
        private final String detailTranslationKey;
        private final String savedTranslationKey;
        private final int displayColor;

        ApplyTiming(
                String shortTranslationKey,
                String detailTranslationKey,
                String savedTranslationKey,
                int displayColor
        ) {
            this.shortTranslationKey = shortTranslationKey;
            this.detailTranslationKey = detailTranslationKey;
            this.savedTranslationKey = savedTranslationKey;
            this.displayColor = displayColor;
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

        int displayColor() {
            return displayColor;
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

    public static Builder builder(String id, Component title) {
        return new Builder(id, title);
    }

    public String id() {
        return id;
    }

    public Component title() {
        return title;
    }

    public Component description() {
        return description;
    }

    public KTConfigScope scope() {
        return scope;
    }

    public boolean serverManaged() {
        return serverManaged;
    }

    public ApplyTiming applyTiming() {
        return applyTiming;
    }

    public Component applyNotice() {
        return applyNotice;
    }

    public List<KTConfigEntry<?>> entries() {
        return entries;
    }

    public boolean showsApplyTiming() {
        return applyNotice != null || entries.stream().anyMatch(KTConfigEntry::isValue);
    }

    public Component applyDetail() {
        return applyNotice != null
                ? applyNotice
                : Component.translatable(applyTiming.detailTranslationKey());
    }

    public void save() {
        saver.run();
    }

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

        public Builder pageDescription(Component description) {
            this.description = description;
            return this;
        }

        public Builder scope(KTConfigScope scope) {
            this.scope = Objects.requireNonNull(scope, "scope");
            return this;
        }

        public Builder serverManaged() {
            this.serverManaged = true;
            return this;
        }

        public Builder applyTiming(ApplyTiming applyTiming) {
            this.applyTiming = Objects.requireNonNull(applyTiming, "applyTiming");
            return this;
        }

        public Builder applyNotice(Component applyNotice) {
            this.applyNotice = Objects.requireNonNull(applyNotice, "applyNotice");
            return this;
        }

        public Builder onSave(Runnable saver) {
            this.saver = Objects.requireNonNull(saver, "saver");
            return this;
        }

        public Builder section(Component label) {
            entries.add(KTConfigEntry.structural(
                    "__section_" + structuralIndex++,
                    KTConfigEntry.Type.SECTION,
                    Objects.requireNonNull(label, "label")
            ));
            return this;
        }

        public Builder description(Component text) {
            entries.add(KTConfigEntry.structural(
                    "__description_" + structuralIndex++,
                    KTConfigEntry.Type.DESCRIPTION,
                    Objects.requireNonNull(text, "text")
            ));
            return this;
        }

        public Builder booleanValue(
                String id, Component label, Supplier<Boolean> reader, Consumer<Boolean> writer,
                boolean defaultValue, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.BOOLEAN, label, tooltip, reader, writer,
                    defaultValue, null, null, null,
                    decoder(Boolean.class), UnaryOperator.identity()
            );
        }

        public Builder intValue(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, int minimum, int maximum, Component tooltip
        ) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            return add(
                    id, KTConfigEntry.Type.INTEGER, label, tooltip, reader, writer,
                    defaultValue, minimum, maximum, null,
                    decoder(Integer.class), UnaryOperator.identity()
            );
        }

        public Builder longValue(
                String id, Component label, Supplier<Long> reader, Consumer<Long> writer,
                long defaultValue, long minimum, long maximum, Component tooltip
        ) {
            if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum for " + id);
            return add(
                    id, KTConfigEntry.Type.LONG, label, tooltip, reader, writer,
                    defaultValue, minimum, maximum, null,
                    decoder(Long.class), UnaryOperator.identity()
            );
        }

        public Builder intValue(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, Component tooltip
        ) {
            return intValue(
                    id, label, reader, writer, defaultValue,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, tooltip
            );
        }

        public Builder doubleValue(
                String id, Component label, Supplier<Double> reader, Consumer<Double> writer,
                double defaultValue, double minimum, double maximum, Component tooltip
        ) {
            if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
                throw new IllegalArgumentException("invalid range for " + id);
            }
            return add(
                    id, KTConfigEntry.Type.DOUBLE, label, tooltip, reader, writer,
                    defaultValue, minimum, maximum, null,
                    decoder(Double.class), UnaryOperator.identity()
            );
        }

        public Builder doubleValue(
                String id, Component label, Supplier<Double> reader, Consumer<Double> writer,
                double defaultValue, Component tooltip
        ) {
            return doubleValue(
                    id, label, reader, writer, defaultValue,
                    -Double.MAX_VALUE, Double.MAX_VALUE, tooltip
            );
        }

        public Builder stringValue(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.STRING, label, tooltip, reader, writer,
                    defaultValue, null, null, null,
                    decoder(String.class), UnaryOperator.identity()
            );
        }

        public Builder choice(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip, String... choices
        ) {
            List<String> values = List.copyOf(Arrays.asList(choices));
            if (values.isEmpty()) throw new IllegalArgumentException("choices cannot be empty for " + id);
            if (!values.contains(defaultValue)) {
                throw new IllegalArgumentException("default value is not a choice for " + id);
            }
            return add(
                    id, KTConfigEntry.Type.CHOICE, label, tooltip, reader, writer,
                    defaultValue, null, null, values,
                    decoder(String.class), UnaryOperator.identity()
            );
        }

        public Builder stringList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.STRING_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeStringList, ArrayList::new
            );
        }

        public Builder entityList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.ENTITY_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeStringList, ArrayList::new
            );
        }

        public Builder itemList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.ITEM_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeStringList, ArrayList::new
            );
        }

        public Builder itemRuleList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.ITEM_RULE_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeStringList, ArrayList::new
            );
        }


        public Builder tickSecondsValue(
                String id, Component label, Supplier<Integer> tickReader, Consumer<Integer> tickWriter,
                int defaultTicks, int minimumTicks, int maximumTicks, Component tooltip
        ) {
            if (minimumTicks > maximumTicks) {
                throw new IllegalArgumentException("minimumTicks > maximumTicks for " + id);
            }
            Supplier<Double> secondsReader = () -> ticksToSeconds(
                    clampTicks(tickReader.get(), minimumTicks, maximumTicks));
            Consumer<Double> secondsWriter = seconds -> tickWriter.accept(
                    secondsToTicks(seconds, minimumTicks, maximumTicks));
            return doubleValue(
                    id, label, secondsReader, secondsWriter,
                    ticksToSeconds(defaultTicks),
                    ticksToSeconds(minimumTicks),
                    ticksToSeconds(maximumTicks),
                    tooltip
            );
        }

        public Builder intList(
                String id, Component label, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer,
                List<Integer> defaultValue, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.INTEGER_LIST, label, tooltip, reader, writer,
                    new ArrayList<>(defaultValue), null, null, null,
                    Builder::decodeIntegerList, ArrayList::new
            );
        }

        public Builder color(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, Component tooltip
        ) {
            return add(
                    id, KTConfigEntry.Type.COLOR, label, tooltip, reader, writer,
                    defaultValue, 0, 0xFFFFFF, null,
                    decoder(Integer.class), UnaryOperator.identity()
            );
        }

        public Builder action(String id, Component label, Runnable action, Component tooltip) {
            requireEntryId(id);
            requireUniqueEntryId(id);
            entries.add(KTConfigEntry.action(
                    id,
                    Objects.requireNonNull(label, "label"),
                    tooltip,
                    Objects.requireNonNull(action, "action")
            ));
            return this;
        }

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
                List<String> choices,
                Function<Object, T> decoder,
                UnaryOperator<T> copier
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
                    copier
            );
            if (!entry.accepts(defaultValue)) {
                throw new IllegalArgumentException("Invalid default value for " + id + ": " + defaultValue);
            }
            entries.add(entry);
            return this;
        }

        private void requireUniqueEntryId(String id) {
            if (!entryIds.add(id)) {
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
