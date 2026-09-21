package dev.xyat.kineticcore.api.client.widget.state;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Public API type for edited entry tracker. */
public class EditedEntryTracker<T> {
    private final Map<T, Long> editedOrder = new HashMap<>();
    private long sequence;

    /**
     * Refreshes the current API state.
     */
    public void refresh(
            Collection<T> entries,
            Predicate<T> editedPredicate
    ) {
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(editedPredicate, "editedPredicate");
        Map<T, Long> previousOrder = new HashMap<>(editedOrder);
        boolean initialSnapshot = previousOrder.isEmpty();
        Map<T, Long> nextOrder = new HashMap<>();
        long nextSequence = sequence;
        for (T entry : entries) {
            if (editedPredicate.test(entry)) {
                // Keep the previously committed snapshot untouched until the scan succeeds.
                Long previous = previousOrder.get(entry);
                nextOrder.put(entry, previous != null ? previous : initialSnapshot ? 0L : ++nextSequence);
            }
        }
        editedOrder.clear();
        editedOrder.putAll(nextOrder);
        sequence = nextSequence;
    }

    /**
     * Performs the update API operation.
     */
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

    /**
     * Returns whether edited.
     */
    public boolean isEdited(T entry) {
        return editedOrder.containsKey(entry);
    }

    /**
     * Performs the comparator API operation.
     */
    public Comparator<T> comparator(Comparator<T> fallback) {
        Objects.requireNonNull(fallback, "fallback");
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

    /**
     * Clears the current API state.
     */
    public void clear() {
        editedOrder.clear();
    }
}
