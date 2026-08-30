package dev.xyat.kineticcore.api.client.gui;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class EditedEntryTracker<T> {
    private final Map<T, Long> editedOrder = new HashMap<>();
    private long sequence;

    public void refresh(
            Collection<T> entries,
            Predicate<T> editedPredicate
    ) {
        editedOrder.clear();

        for (T entry : entries) {
            if (editedPredicate.test(entry)) {
                editedOrder.put(entry, 0L);
            }
        }
    }

    public boolean update(
            T entry,
            boolean edited
    ) {
        boolean wasEdited = editedOrder.containsKey(entry);

        if (edited) {
            if (!wasEdited) {
                editedOrder.put(entry, ++sequence);
                return true;
            }

            return false;
        }

        return editedOrder.remove(entry) != null;
    }

    public boolean isEdited(T entry) {
        return editedOrder.containsKey(entry);
    }

    public Comparator<T> comparator(Comparator<T> fallback) {
        return (left, right) -> {
            boolean leftEdited = isEdited(left);
            boolean rightEdited = isEdited(right);

            if (leftEdited != rightEdited) {
                return leftEdited ? -1 : 1;
            }

            if (leftEdited) {
                long leftOrder = editedOrder.getOrDefault(left, 0L);
                long rightOrder = editedOrder.getOrDefault(right, 0L);
                int recentCompare = Long.compare(rightOrder, leftOrder);

                if (recentCompare != 0) {
                    return recentCompare;
                }
            }

            return fallback.compare(left, right);
        };
    }

    public void clear() {
        editedOrder.clear();
    }
}
