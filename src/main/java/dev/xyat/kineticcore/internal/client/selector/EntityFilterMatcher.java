package dev.xyat.kineticcore.internal.client.selector;

import java.util.Set;

/**
 * Combines independent category, mod namespace and text filters. Empty sets
 * are unrestricted; filters within the same group are OR, groups are AND.
 * This class never edits the entity selection itself.
 */
final class EntityFilterMatcher {
    private EntityFilterMatcher() {
    }

    static boolean matches(
            Set<String> categories, String category,
            Set<String> namespaces, String namespace,
            boolean searchMatches
    ) {
        return searchMatches
                && (categories == null || categories.isEmpty() || categories.contains(category))
                && (namespaces == null || namespaces.isEmpty() || namespaces.contains(namespace));
    }
}