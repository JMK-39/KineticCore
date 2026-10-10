package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Immutable description of one configuration page shown by the Kinetic config screens.
 *
 * <p>A page lists its rows (values, headings, dividers, descriptions and actions), says where its source of truth
 * lives ({@link KTConfigScope}) and when saved values take effect ({@link ApplyTiming}). Pages are built once with
 * {@link #builder(String, Component)} and registered through {@code KTConfigApi}; the screen reads and writes
 * values only through the supplied reader and writer callbacks, so persistence stays owned by the add-on.
 *
 * <p>Pages are immutable after {@link Builder#build()} and may be shared. The reader, writer and saver callbacks
 * run on the client thread while a config screen is open.
 */
public final class KTConfigPage {
    /** Opens a visual string-list editor. Apply returns a draft; the config page still owns saving and permissions. */
    @FunctionalInterface
    public interface ListEditor {
        /** Receives an immutable draft and a callback to replace it; cancelling must not call apply. */
        void open(Component title, List<String> initial, Consumer<List<String>> apply);
    }
    /**
     * When a saved value takes effect. The screen shows a short badge, a detail tooltip and a saved toast derived
     * from this value unless the page supplies its own {@link Builder#applyNotice(Component) notice}.
     */
    public enum ApplyTiming {
        /** Saved values take effect immediately. */
        IMMEDIATE(
                "gui.kineticcore.config.apply.immediate.short",
                "gui.kineticcore.config.apply.immediate.detail",
                "gui.kineticcore.config.saved.immediate"
        ),
        /** The feature's own reload operation must run after saving. */
        RELOAD_REQUIRED(
                "gui.kineticcore.config.apply.reload_required.short",
                "gui.kineticcore.config.apply.reload_required.detail",
                "gui.kineticcore.config.saved.reload_required"
        ),
        /** Values apply the next time an uninitialized world is loaded. */
        NEXT_WORLD_LOAD(
                "gui.kineticcore.config.apply.next_world_load.short",
                "gui.kineticcore.config.apply.next_world_load.detail",
                "gui.kineticcore.config.saved.next_world_load"
        ),
        /** The game or server must restart before values apply. */
        RESTART_GAME(
                "gui.kineticcore.config.apply.restart_game.short",
                "gui.kineticcore.config.apply.restart_game.detail",
                "gui.kineticcore.config.saved.restart_game"
        ),
        /** Rows apply at different times; each row's description explains its timing. This is the default. */
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
    private final boolean mirrorOnJoin;
    private final ApplyTiming applyTiming;
    private final Component applyNotice;
    private final List<KTConfigEntry<?>> entries;
    private final Runnable saver;
    private final Map<String, ListEditor> listEditors;

    private KTConfigPage(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.scope = builder.scope;
        this.serverManaged = builder.serverManaged;
        this.mirrorOnJoin = builder.mirrorOnJoin;
        this.applyTiming = builder.applyTiming;
        this.applyNotice = builder.applyNotice;
        this.entries = List.copyOf(builder.entries);
        this.saver = builder.saver;
        this.listEditors = Map.copyOf(builder.listEditors);
    }

    /**
     * Starts a page definition.
     *
     * @param id namespaced page id such as {@code mymod:general}; lower-case letters, digits and {@code _ . - /}
     * @param title page title shown in the config index and screen header
     * @return a new builder with {@link KTConfigScope#LOCAL_INSTALLATION} scope and {@link ApplyTiming#MIXED}
     *   timing
     * @throws NullPointerException if {@code id} or {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code id} is not namespaced
     */
    public static Builder builder(String id, Component title) {
        return new Builder(id, title);
    }

    /** Returns the namespaced page id used for registration, navigation and server sync. */
    public String id() {
        return id;
    }

    /** Returns the page title shown in the config index and screen header. */
    public Component title() {
        return title;
    }

    /**
     * Returns the optional page description shown under the title.
     *
     * @return the description, or {@code null} when none was set
     */
    public Component description() {
        return description;
    }

    /** Returns where this page's values live: this client, this installation or the connected server. */
    public KTConfigScope scope() {
        return scope;
    }

    /**
     * Returns whether a {@link KTConfigScope#SERVER_AUTHORITATIVE} page is edited through Kinetic's server config
     * sync.
     *
     * <p>A server-authoritative page that is not server managed is shown read-only with an explanation instead of
     * writing values locally.
     */
    public boolean serverManaged() {
        return serverManaged;
    }

    /** Whether the client loads this server-managed page's values from the server when it joins (see {@link Builder#mirrorOnJoin()}). */
    public boolean mirrorOnJoin() {
        return mirrorOnJoin;
    }

    /** Returns when saved values from this page take effect. */
    public ApplyTiming applyTiming() {
        return applyTiming;
    }

    /**
     * Returns the custom apply notice that replaces the standard {@link #applyTiming()} text.
     *
     * @return the notice, or {@code null} when the standard timing text is used
     */
    public Component applyNotice() {
        return applyNotice;
    }

    /** Returns the page rows in display order as an unmodifiable list. */
    public List<KTConfigEntry<?>> entries() {
        return entries;
    }

    /** Returns the custom visual editor for an entry, or null to use the standard editor. */
    public ListEditor listEditor(String entryId) {
        return listEditors.get(entryId);
    }

    /**
     * Returns whether the screen should show when changes take effect.
     *
     * <p>This is {@code true} when a custom notice was set or at least one row stores a value; pages that only
     * contain headings, descriptions or actions have nothing to apply.
     */
    public boolean showsApplyTiming() {
        return applyNotice != null || entries.stream().anyMatch(KTConfigEntry::isValueEntry);
    }

    /**
     * Returns the player-facing explanation of when saved values apply: the custom notice if set, otherwise the
     * standard text for {@link #applyTiming()}.
     */
    public Component applyDetail() {
        return applyNotice != null
                ? applyNotice
                : KineticI18n.translatable(applyTiming.detailTranslationKey());
    }

    /**
     * Runs the page's {@link Builder#onSave(Runnable) save callback}.
     *
     * <p>Screens call this after every changed value has been written through its writer, so the callback should
     * persist the add-on's config (for example flush it to disk). Exceptions propagate to the caller.
     */
    public void save() {
        saver.run();
    }

    /**
     * Collects rows and page options for a {@link KTConfigPage}.
     *
     * <p>Every value method validates its arguments immediately and throws on bad input, so a page that builds is
     * always renderable. Methods ending in {@code Validated} accept an extra business rule; the others accept every
     * value that passes the built-in type and range checks. Builders are not thread-safe and are meant to be used
     * once.
     */
    public static final class Builder {
        private final String id;
        private final Component title;
        private final List<KTConfigEntry<?>> entries = new ArrayList<>();
        private final Set<String> entryIds = new HashSet<>();
        private final Map<String, ListEditor> listEditors = new HashMap<>();
        private Component description;
        private KTConfigScope scope = KTConfigScope.LOCAL_INSTALLATION;
        private boolean serverManaged;
        private boolean mirrorOnJoin;
        private ApplyTiming applyTiming = ApplyTiming.MIXED;
        private Component applyNotice;
        private Runnable saver = () -> { };
        private int structuralIndex;

        private Builder(String id, Component title) {
            this.id = requirePageId(id);
            this.title = Objects.requireNonNull(title, "title");
        }

        /**
         * Replaces a string-list row's text editor with an add-on's visual editor, without changing its
         * storage format, validation, draft handling or server authorization. Register after the row.
         * @param entryId an existing string-list row id
         * @param editor visual editor callback
         * @return this builder
         */
        public Builder listEditor(String entryId, ListEditor editor) {
            if (entries.stream().noneMatch(entry -> entry.id().equals(entryId)
                    && entry.type() == KTConfigEntry.Type.STRING_LIST)) {
                throw new IllegalArgumentException("Visual editor requires a string-list row: " + entryId);
            }
            listEditors.put(entryId, Objects.requireNonNull(editor, "editor"));
            return this;
        }

        /**
         * Sets the optional description shown under the page title.
         *
         * @param description description text, or {@code null} to show none
         * @return this builder
         */
        public Builder pageDescription(Component description) {
            this.description = description;
            return this;
        }

        /**
         * Sets where this page's values live. The scope controls the badge shown on the page and, for
         * {@link KTConfigScope#SERVER_AUTHORITATIVE}, whether the page is editable in the current client context.
         *
         * @param scope value source of truth
         * @return this builder
         * @throws NullPointerException if {@code scope} is {@code null}
         */
        public Builder scope(KTConfigScope scope) {
            this.scope = Objects.requireNonNull(scope, "scope");
            return this;
        }

        /**
         * Marks a {@link KTConfigScope#SERVER_AUTHORITATIVE} page as edited through Kinetic's server config sync:
         * values load from the connected server and saves are sent back to it when the player has permission.
         *
         * @return this builder
         * @see #build()
         */
        public Builder serverManaged() {
            this.serverManaged = true;
            return this;
        }

        /**
         * For a server-managed page whose values the client itself uses (for example to show tooltips): when the
         * player joins a server, the client asks for this page's values once and applies them, so the client never
         * runs on its own local copy. Only the joining player is answered; nothing is sent to other players, and
         * later admin edits reach other players when they next join.
         *
         * @return this builder
         */
        public Builder mirrorOnJoin() {
            this.mirrorOnJoin = true;
            return this;
        }

        /**
         * Sets when saved values take effect. Defaults to {@link ApplyTiming#MIXED}.
         *
         * @param applyTiming apply timing shown on the page and in the saved toast
         * @return this builder
         * @throws NullPointerException if {@code applyTiming} is {@code null}
         */
        public Builder applyTiming(ApplyTiming applyTiming) {
            this.applyTiming = Objects.requireNonNull(applyTiming, "applyTiming");
            return this;
        }

        /**
         * Replaces the standard apply-timing text with a custom notice, for pages whose rules do not fit one
         * {@link ApplyTiming} value.
         *
         * @param applyNotice player-facing notice shown on the page and in the saved toast
         * @return this builder
         * @throws NullPointerException if {@code applyNotice} is {@code null}
         */
        public Builder applyNotice(Component applyNotice) {
            this.applyNotice = Objects.requireNonNull(applyNotice, "applyNotice");
            return this;
        }

        /**
         * Sets the callback that persists the page after its values have been written.
         *
         * @param saver persistence callback; replaces any previous callback
         * @return this builder
         * @throws NullPointerException if {@code saver} is {@code null}
         */
        public Builder onSave(Runnable saver) {
            this.saver = Objects.requireNonNull(saver, "saver");
            return this;
        }

        /**
         * Adds a section heading row. Headings group the rows that follow them and take part in search.
         *
         * @param label heading text
         * @return this builder
         * @throws NullPointerException if {@code label} is {@code null}
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
         * Adds a read-only paragraph row between value rows, for example to explain the rows below it. Use
         * {@link #pageDescription(Component)} for the page-level description.
         *
         * @param text paragraph text
         * @return this builder
         * @throws NullPointerException if {@code text} is {@code null}
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
         * Adds an on/off toggle row.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder booleanValue(
                String id, Component label, Supplier<Boolean> reader, Consumer<Boolean> writer,
                boolean defaultValue, Component tooltip
        ) {
            return booleanValueValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds an on/off toggle row with an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds an integer input row limited to an inclusive range.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must lie within the range
         * @param minimum smallest accepted value, may be negative
         * @param maximum largest accepted value
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or {@code minimum > maximum}
         */
        public Builder intValue(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, int minimum, int maximum, Component tooltip
        ) {
            return intValueValidated(id, label, reader, writer, defaultValue, minimum, maximum, value -> true, tooltip);
        }

        /**
         * Adds an integer input row limited to an inclusive range, with an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must lie within the range and pass {@code validator}
         * @param minimum smallest accepted value, may be negative
         * @param maximum largest accepted value
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or {@code minimum > maximum}
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
         * Adds a {@code long} input row limited to an inclusive range.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must lie within the range
         * @param minimum smallest accepted value, may be negative
         * @param maximum largest accepted value
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or {@code minimum > maximum}
         */
        public Builder longValue(
                String id, Component label, Supplier<Long> reader, Consumer<Long> writer,
                long defaultValue, long minimum, long maximum, Component tooltip
        ) {
            return longValueValidated(id, label, reader, writer, defaultValue, minimum, maximum, value -> true, tooltip);
        }

        /**
         * Adds a {@code long} input row limited to an inclusive range, with an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must lie within the range and pass {@code validator}
         * @param minimum smallest accepted value, may be negative
         * @param maximum largest accepted value
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or {@code minimum > maximum}
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
         * Adds an integer input row that accepts the whole {@code int} range, including negatives.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds an integer input row that accepts the whole {@code int} range and applies an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a {@code long} input row that accepts the whole {@code long} range, including negatives.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a {@code long} input row that accepts the whole {@code long} range and applies an extra business
         * rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a decimal input row limited to a finite inclusive range.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must lie within the range
         * @param minimum smallest accepted value, finite and may be negative
         * @param maximum largest accepted value, finite
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or a bound is not finite or {@code minimum > maximum}
         */
        public Builder doubleValue(
                String id, Component label, Supplier<Double> reader, Consumer<Double> writer,
                double defaultValue, double minimum, double maximum, Component tooltip
        ) {
            return doubleValueValidated(id, label, reader, writer, defaultValue, minimum, maximum, value -> true, tooltip);
        }

        /**
         * Adds a decimal input row limited to a finite inclusive range, with an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must lie within the range and pass {@code validator}
         * @param minimum smallest accepted value, finite and may be negative
         * @param maximum largest accepted value, finite
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or a bound is not finite or {@code minimum > maximum}
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
         * Adds a decimal input row that accepts every finite {@code double}, including negatives.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a decimal input row that accepts every finite {@code double} and applies an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a single-line text input row.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder stringValue(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip
        ) {
            return stringValueValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds a single-line text input row with an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a text row edited in the multi-line text editor, for long values such as commands or scripts.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder longTextValue(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip
        ) {
            return longTextValueValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds a multi-line text row with an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a dropdown row whose options are raw strings shown as-is.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must be one of {@code choices}
         * @param choices allowed raw values, non-empty and unique
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or {@code choices} is empty or contains duplicates
         */
        public Builder choice(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip, String... choices
        ) {
            return choiceValidated(id, label, reader, writer, defaultValue, value -> true, tooltip, choices);
        }

        /**
         * Adds a dropdown row of raw string options with an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must be one of {@code choices} and pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param choices allowed raw values, non-empty and unique
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or {@code choices} is empty or contains duplicates
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
         * Adds a dropdown row whose options carry optional translations and tooltips.
         *
         * <p>The persisted value is always {@link KTConfigEntry.ChoiceOption#value()}; translations and tooltips
         * are display-only.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must match one option value
         * @param choices options, non-empty with unique values
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or {@code choices} is empty or contains duplicate values
         */
        public Builder choiceOptions(
                String id, Component label, Supplier<String> reader, Consumer<String> writer,
                String defaultValue, Component tooltip, KTConfigEntry.ChoiceOption... choices
        ) {
            return choiceOptionsValidated(id, label, reader, writer, defaultValue, value -> true, tooltip, choices);
        }

        /**
         * Adds a dropdown row of described options with an extra business rule.
         *
         * <p>The persisted value is always {@link KTConfigEntry.ChoiceOption#value()}; translations and tooltips
         * are display-only.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must match one option value and pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param choices options, non-empty with unique values
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or {@code choices} is empty or contains duplicate values
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
         * Adds a dropdown row whose display names come from language keys.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must be one of {@code choices}
         * @param translationKeyPrefix key prefix; each option uses
         *   {@code prefix + "." + value.toLowerCase(Locale.ROOT)}
         * @param choices allowed raw values, non-empty and unique
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or the prefix is blank, or {@code choices} is empty or contains duplicates
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
         * Adds a dropdown row with translated display names and an extra business rule.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue value restored by reset; must be one of {@code choices} and pass {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param translationKeyPrefix key prefix; each option uses
         *   {@code prefix + "." + value.toLowerCase(Locale.ROOT)}
         * @param choices allowed raw values, non-empty and unique
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used, {@code defaultValue} is
         *   rejected, or the prefix is blank, or {@code choices} is empty or contains duplicates
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
         * Adds a free-text string list row edited in the list editor.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue list restored by reset; copied, must not be {@code null}
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder stringList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return stringListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds a free-text string list row with an extra business rule on the whole list.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue list restored by reset; copied, must not be {@code null} and must pass
         *   {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds an entity-id list row edited with the entity selector.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue entity ids restored by reset; copied, must not be {@code null}
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder entityList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return entityListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds an entity-id list row with an extra business rule on the whole list.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue entity ids restored by reset; copied, must not be {@code null} and must pass
         *   {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds an item-id list row edited with the item selector.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue item ids restored by reset; copied, must not be {@code null}
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder itemList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return itemListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds an item-id list row with an extra business rule on the whole list.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue item ids restored by reset; copied, must not be {@code null} and must pass
         *   {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds an item-rule list row whose entries may be item ids, tags or other selector rules understood by the
         * add-on.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue rules restored by reset; copied, must not be {@code null}
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder itemRuleList(
                String id, Component label, Supplier<List<String>> reader, Consumer<List<String>> writer,
                List<String> defaultValue, Component tooltip
        ) {
            return itemRuleListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds an item-rule list row with an extra business rule on the whole list.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue rules restored by reset; copied, must not be {@code null} and must pass
         *   {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a duration row that stores game ticks but lets the player edit seconds.
         *
         * <p>The editor shows {@code ticks / 20} and converts edits back by rounding to the nearest tick, clamped
         * to the tick range. When {@code minimumTicks} is {@code 0}, any positive input rounds up to at least one
         * tick.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param tickReader supplies the live value in ticks
         * @param tickWriter receives the accepted value in ticks when the page is saved
         * @param defaultTicks ticks restored by reset; must lie within the tick range
         * @param minimumTicks smallest accepted tick count
         * @param maximumTicks largest accepted tick count
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if {@code minimumTicks > maximumTicks}, or the id or default is rejected
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
         * Adds a seconds-edited, tick-stored duration row with an extra business rule on the seconds value.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param tickReader supplies the live value in ticks
         * @param tickWriter receives the accepted value in ticks when the page is saved
         * @param defaultTicks ticks restored by reset; must lie within the tick range and pass {@code validator}
         * @param minimumTicks smallest accepted tick count
         * @param maximumTicks largest accepted tick count
         * @param validator business rule applied to the edited value in seconds
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws NullPointerException if {@code validator} is {@code null}
         * @throws IllegalArgumentException if {@code minimumTicks > maximumTicks}, or the id or default is rejected
         * @see #tickSecondsValue(String, Component, Supplier, Consumer, int, int, int, Component)
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
         * Adds an integer list row edited in the list editor.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue list restored by reset; copied, must not be {@code null}
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder intList(
                String id, Component label, Supplier<List<Integer>> reader, Consumer<List<Integer>> writer,
                List<Integer> defaultValue, Component tooltip
        ) {
            return intListValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds an integer list row with an extra business rule on the whole list.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue list restored by reset; copied, must not be {@code null} and must pass
         *   {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds an RGB color row edited with the color picker. Values are {@code 0xRRGGBB} without alpha.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue color restored by reset, between {@code 0x000000} and {@code 0xFFFFFF}
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
         */
        public Builder color(
                String id, Component label, Supplier<Integer> reader, Consumer<Integer> writer,
                int defaultValue, Component tooltip
        ) {
            return colorValidated(id, label, reader, writer, defaultValue, value -> true, tooltip);
        }

        /**
         * Adds an RGB color row with an extra business rule. Values are {@code 0xRRGGBB} without alpha.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label row label shown to the player
         * @param reader supplies the live value each time the editor loads or refreshes the row
         * @param writer receives an accepted value when the page is saved
         * @param defaultValue color restored by reset, between {@code 0x000000} and {@code 0xFFFFFF}, and must pass
         *   {@code validator}
         * @param validator extra business rule run after the built-in type and range checks; a rejected value
         *   blocks saving
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws IllegalArgumentException if the id is malformed or already used or {@code defaultValue} is
         *   rejected
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
         * Adds a button row that runs an action instead of storing a value, for example "open editor" or "reset
         * statistics". Actions are not part of save or reset.
         *
         * @param id entry id, unique within this page; lower-case {@code [a-z0-9_.-]+}, not starting with
         *   {@code __}
         * @param label button text
         * @param action code run on the client thread when the button is pressed
         * @param tooltip optional hover text, or {@code null} for none
         * @return this builder
         * @throws NullPointerException if {@code label} or {@code action} is {@code null}
         * @throws IllegalArgumentException if the id is malformed or already used
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
         * Creates the immutable page.
         *
         * @return the page
         * @throws IllegalStateException if {@link #serverManaged()} was called on a page whose scope is not
         *   {@link KTConfigScope#SERVER_AUTHORITATIVE}
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
