package dev.xyat.kineticcore.api.client.search;

import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.Suggestion;
import net.minecraft.network.chat.Component;
import java.util.List;

/** English must show the saved raw ID, independent of an older localized dictionary. */
public final class SearchEnglishDisplayRegression {
    public static void main(String[] args) {
        List<Suggestion> localized = List.of(new Suggestion("minecraft:stone", Component.literal("石头")));
        check("minecraft:stone".equals(KineticSearch.suggestionDisplayName("minecraft:stone", localized)),
                "an English client displayed a cached foreign-language name");
        check("minecraft:dirt".equals(KineticSearch.suggestionDisplayName("minecraft:dirt", localized)),
                "unknown raw identifier was changed");
        check(KineticSearch.suggestionDisplayName(null, localized) == null,
                "null input must remain null");
        System.out.println("PASS: 3 search language-display checks");
    }
    private static void check(boolean yes, String message) { if (!yes) throw new AssertionError(message); }
}
