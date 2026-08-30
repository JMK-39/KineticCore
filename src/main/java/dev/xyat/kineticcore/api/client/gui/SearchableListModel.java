package dev.xyat.kineticcore.api.client.gui;

import dev.xyat.kineticcore.api.client.AdvancedSearchUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class SearchableListModel<T> {
    private final List<T> source = new ArrayList<>();
    private final List<T> visible = new ArrayList<>();
    private final BiPredicate<T, String> matcher;
    private Comparator<T> comparator;

    public SearchableListModel(
            Collection<T> source,
            Function<T, String> searchData
    ) {
        this(
                source,
                (entry, query) -> AdvancedSearchUtil.match(
                        searchData.apply(entry),
                        query
                )
        );
    }

    public SearchableListModel(
            Collection<T> source,
            BiPredicate<T, String> matcher
    ) {
        this.matcher = matcher;
        setSource(source);
    }

    public void setSource(Collection<T> entries) {
        source.clear();
        source.addAll(entries);
    }

    public void setComparator(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    public void refresh(String query) {
        String normalized = query == null
                ? ""
                : query.toLowerCase(Locale.ROOT).trim();

        visible.clear();

        for (T entry : source) {
            if (normalized.isEmpty() || matcher.test(entry, normalized)) {
                visible.add(entry);
            }
        }

        if (comparator != null) {
            visible.sort(comparator);
        }
    }

    public List<T> items() {
        return visible;
    }
}
