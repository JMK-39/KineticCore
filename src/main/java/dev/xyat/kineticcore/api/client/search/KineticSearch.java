package dev.xyat.kineticcore.api.client.search;

import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.Suggestion;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.search.KineticSearchRuntime;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Public search facade for Kinetic text matching, localized suggestion dictionaries, and reusable filtered models.
 * Text normalization and pinyin indexing remain internal so add-ons depend only on stable matching behavior.
 */
public final class KineticSearch {
    private static final Pattern UNRESOLVED_FORMAT_ARGUMENT = Pattern.compile("%(?:\\d+\\$)?[a-zA-Z]");
    private static List<Suggestion> itemDictionary;
    private static List<Suggestion> enchantmentDictionary;
    private static List<Suggestion> attributeDictionary;
    private static List<Suggestion> potionDictionary;
    private static List<Suggestion> damageDictionary;
    private static List<Suggestion> specificDamageDictionary;
    private static String dictionaryLanguage;
    private static long damageDictionaryConnectionRevision = Long.MIN_VALUE;
    private static Object damageDictionaryLevelToken;

    private KineticSearch() {
    }

    /** Returns whether the supplied text matches the query using Kinetic search and pinyin rules. */
    public static boolean match(String text, String query) {
        return KineticSearchRuntime.match(text, query);
    }

    /** Prepares reusable search data once for lists that repeatedly match the same source text. */
    public static PreparedText prepare(String text) {
        return new PreparedText(text);
    }

    /** Returns the reusable pinyin search text for compatibility with existing Kinetic add-ons. */
    public static String pinyin(String text) {
        return KineticSearchRuntime.pinyinSearchData(text);
    }

    /** Returns reusable pinyin parts for callers that need stable search ranking. */
    public static PinyinData preparePinyin(String text) {
        KineticSearchRuntime.PinyinSnapshot snapshot = KineticSearchRuntime.pinyinSnapshot(text);
        return new PinyinData(
                snapshot.rawLower(),
                snapshot.full(),
                snapshot.initials(),
                snapshot.syllables(),
                snapshot.searchData()
        );
    }

    /** Immutable pinyin data data exposed by this API. */
    public record PinyinData(
            String rawLower,
            String full,
            String initials,
            String[] syllables,
            String searchData
    ) {
        /** Normalizes nullable pinyin fields to stable non-null values. */
        public PinyinData {
            syllables = syllables == null ? new String[0] : syllables.clone();
        }

        @Override
        public String[] syllables() {
            return syllables.clone();
        }

        /** Returns whether this cached pinyin/search snapshot matches the supplied query. */
        public boolean matches(String query) {
            return KineticSearchRuntime.match(rawLower, query);
        }
    }

    /** Opaque reusable search data that keeps pinyin/index implementation details behind the public facade. */
    public static final class PreparedText {
        private final KineticSearchRuntime.PreparedSearch prepared;

        private PreparedText(String text) {
            this.prepared = KineticSearchRuntime.prepare(text);
        }

        /** Returns whether this prepared source matches the supplied query using Kinetic search rules. */
        public boolean matches(String query) {
            return prepared.matches(query);
        }

        /**
         * Returns a stable search rank: exact/empty=0, direct prefix=1, full-pinyin prefix=2,
         * pinyin-initial prefix=3, other Kinetic match=4, and no match=-1.
         */
        public int matchRank(String query) {
            return prepared.matchRank(query);
        }
    }

    /**
     * Resolves the first available translation for the current game language.
     * English locales intentionally omit translations because registry IDs already provide the English-facing value.
     */
    public static String resolveTranslation(String... keys) {
        if (KineticClientRuntime.isEnglishLanguage()) return null;
        for (String key : keys) {
            if (key == null || key.isBlank() || !KineticText.hasTranslation(key)) continue;
            String translated = KineticText.get(key);
            if (!translated.equals(key) && !translated.isBlank() && hasResolvedFormat(translated)) {
                return translated;
            }
        }
        return null;
    }

    /** Literal percent signs are valid labels; only unresolved formatting tokens need another candidate. */
    private static boolean hasResolvedFormat(String text) {
        return !UNRESOLVED_FORMAT_ARGUMENT.matcher(text).find();
    }

    /** Returns the display translation for a suggestion value, or the raw value when no translation is available. */
    public static String dictionaryName(String value, List<Suggestion> dictionary) {
        return suggestionDisplayName(value, dictionary);
    }

    /** Returns the display translation for a suggestion value, or the raw value when no translation is available. */
    public static String suggestionDisplayName(String value, List<Suggestion> dictionary) {
        if (value == null) return null;
        // A cached dictionary may have been built under the previous language.
        // English-facing controls always display the persistent raw identifier.
        if (KineticClientRuntime.isEnglishLanguage() || dictionary == null || dictionary.isEmpty()) return value;
        for (Suggestion suggestion : dictionary) {
            if (suggestion == null || !value.equals(suggestion.value())) continue;
            String translated = suggestion.translation().getString();
            return translated.isBlank() ? value : translated;
        }
        return value;
    }


    /** Returns the cached item-ID suggestion dictionary for the current game language. */
    public static List<Suggestion> itemDictionary() {
        ensureDictionaryLanguage();
        if (itemDictionary == null) {
            itemDictionary = KineticRegistries.items().values().stream().map(value -> {
                ResourceLocation registryId = KineticRegistries.items().id(value);
                String id = registryId == null ? "" : registryId.toString();
                String translated = resolveTranslation(value.getDescriptionId());
                return new Suggestion(id, translated == null ? Component.empty() : Component.literal(translated));
            }).toList();
        }
        return itemDictionary;
    }

    /** Returns the cached enchantment-ID suggestion dictionary for the current game language. */
    public static List<Suggestion> enchantmentDictionary() {
        ensureDictionaryLanguage();
        if (enchantmentDictionary == null) {
            enchantmentDictionary = KineticRegistries.enchantments().values().stream().map(value -> {
                ResourceLocation registryId = KineticRegistries.enchantments().id(value);
                String id = registryId == null ? "" : registryId.toString();
                String translated = resolveTranslation(value.getDescriptionId());
                return new Suggestion(id, translated == null ? Component.empty() : Component.literal(translated));
            }).toList();
        }
        return enchantmentDictionary;
    }

    /** Returns the cached attribute-ID suggestion dictionary for the current game language. */
    public static List<Suggestion> attributeDictionary() {
        ensureDictionaryLanguage();
        if (attributeDictionary == null) {
            attributeDictionary = KineticRegistries.attributes().values().stream().map(value -> {
                ResourceLocation registryId = KineticRegistries.attributes().id(value);
                String id = registryId == null ? "" : registryId.toString();
                String translated = resolveTranslation(value.getDescriptionId());
                return new Suggestion(id, translated == null ? Component.empty() : Component.literal(translated));
            }).toList();
        }
        return attributeDictionary;
    }

    /** Returns the cached mob-effect-ID suggestion dictionary for the current game language. */
    public static List<Suggestion> potionDictionary() {
        ensureDictionaryLanguage();
        if (potionDictionary == null) {
            potionDictionary = KineticRegistries.mobEffects().values().stream().map(value -> {
                ResourceLocation registryId = KineticRegistries.mobEffects().id(value);
                String id = registryId == null ? "" : registryId.toString();
                String translated = resolveTranslation(value.getDescriptionId());
                return new Suggestion(id, translated == null ? Component.empty() : Component.literal(translated));
            }).toList();
        }
        return potionDictionary;
    }

    /** Returns damage suggestions containing {@code all}, damage-type tags, and concrete damage message ids. */
    public static List<Suggestion> damageDictionary() {
        ensureDictionaryLanguage();
        ensureDamageDictionaryContext();
        if (damageDictionary == null) damageDictionary = buildDamageDictionary(true);
        return damageDictionary;
    }

    /** Returns damage suggestions containing only concrete damage message ids. */
    public static List<Suggestion> specificDamageDictionary() {
        ensureDictionaryLanguage();
        ensureDamageDictionaryContext();
        if (specificDamageDictionary == null) specificDamageDictionary = buildDamageDictionary(false);
        return specificDamageDictionary;
    }

    private static void ensureDictionaryLanguage() {
        String currentLanguage = KineticClientRuntime.selectedLanguage();
        if (Objects.equals(dictionaryLanguage, currentLanguage)) return;
        itemDictionary = null;
        enchantmentDictionary = null;
        attributeDictionary = null;
        potionDictionary = null;
        damageDictionary = null;
        specificDamageDictionary = null;
        dictionaryLanguage = currentLanguage;
    }

    private static void ensureDamageDictionaryContext() {
        long currentConnectionRevision = KineticClientRuntime.connectionRevision();
        Object currentLevel = KineticClientRuntime.currentLevel();
        if (damageDictionaryConnectionRevision == currentConnectionRevision && damageDictionaryLevelToken == currentLevel) {
            return;
        }
        damageDictionary = null;
        specificDamageDictionary = null;
        damageDictionaryConnectionRevision = currentConnectionRevision;
        damageDictionaryLevelToken = currentLevel;
    }

    private static List<Suggestion> buildDamageDictionary(boolean includeAllAndTags) {
        List<Suggestion> result = new ArrayList<>();
        if (includeAllAndTags) {
            String translated = resolveTranslation("gui.kineticcore.damage.all");
            result.add(new Suggestion("all", translated == null ? Component.empty() : Component.literal(translated)));
        }

        var level = KineticClientRuntime.currentLevel();
        if (level == null) return List.copyOf(result);
        var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Set<String> addedMessageIds = new HashSet<>();
        registry.entrySet().forEach(entry -> {
            ResourceLocation location = entry.getKey().location();
            String messageId = entry.getValue().msgId();
            if (!addedMessageIds.add(messageId)) return;
            String translated = resolveTranslation(
                    "damage_type." + messageId.replace(":", "."),
                    "dmg." + messageId,
                    "damage_type." + location.getNamespace() + "." + location.getPath()
            );
            result.add(new Suggestion(messageId, translated == null ? Component.empty() : Component.literal(translated)));
        });

        if (includeAllAndTags) {
            registry.getTagNames().forEach(tag -> {
                ResourceLocation location = tag.location();
                String translated = resolveTranslation(
                        "tag.damage_type." + location.getNamespace() + "." + location.getPath(),
                        "tag." + location.getNamespace() + "." + location.getPath(),
                        "tag." + location.getPath()
                );
                result.add(new Suggestion("#" + location, translated == null ? Component.empty() : Component.literal(translated)));
            });
        }
        return List.copyOf(result);
    }

    /** Reusable searchable model that keeps raw values separate from prepared search data. */
    public static final class Model<T> {
        private final List<T> source = new ArrayList<>();
        private final List<T> visible = new ArrayList<>();
        private final List<T> visibleView = Collections.unmodifiableList(visible);
        private final BiPredicate<T, String> matcher;
        private Comparator<T> comparator;
        private Comparator<T> lastSuccessfulComparator;

        /** Creates a searchable model backed by an internal copy of the supplied source collection. */
        public Model(Collection<T> source, BiPredicate<T, String> matcher) {
            this.matcher = Objects.requireNonNull(matcher, "matcher");
            setSource(source);
        }

        /**
         * Creates a searchable model from a reusable text extractor; matching still uses the standard Kinetic search rules.
         */
        public Model(Collection<T> source, Function<T, String> searchText) {
            Function<T, String> safeSearchText = Objects.requireNonNull(searchText, "searchText");
            this.matcher = (value, query) -> {
                String text = safeSearchText.apply(value);
                return KineticSearch.match(text == null ? "" : text, query);
            };
            setSource(source);
        }

        /** Replaces the model source; call {@link #refresh(String)} to rebuild visible items for a query. */
        public void setSource(Collection<T> entries) {
            // Build the replacement before touching the old source. A failing iterator
            // must not leave the search model without its previously valid entries.
            List<T> replacement = entries == null ? List.of() : new ArrayList<>(entries);
            source.clear();
            source.addAll(replacement);
        }

        /** Sets the optional comparator applied to visible matches after each refresh; {@code null} keeps source order. */
        public void setComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        /** Refreshes the filtered model using the supplied search query. */
        public void refresh(String query) {
            String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
            // Match and sort in a temporary list; commit only after both operations
            // succeed so an addon callback cannot destroy the last valid results.
            List<T> replacement = new ArrayList<>();
            for (T entry : source) {
                if (normalized.isEmpty() || matcher.test(entry, normalized)) replacement.add(entry);
            }
            if (comparator != null) {
                try {
                    replacement.sort(comparator);
                } catch (RuntimeException | Error failure) {
                    // A rejected replacement comparator must not poison all later refreshes.
                    comparator = lastSuccessfulComparator;
                    throw failure;
                }
            }
            visible.clear();
            visible.addAll(replacement);
            lastSuccessfulComparator = comparator;
        }

        /** Returns an unmodifiable live view of the items produced by the most recent {@link #refresh(String)}. */
        public List<T> items() {
            return visibleView;
        }
    }

}
