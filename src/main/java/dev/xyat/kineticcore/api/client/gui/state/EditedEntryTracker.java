package dev.xyat.kineticcore.api.client.gui.state;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Remembers which list entries were edited and in what order, so editors can list modified entries first with the
 * most recent edit on top. Entries are compared with {@code equals}/{@code hashCode}. Not thread-safe.
 *
 * @param <T> entry key type
 */
public class EditedEntryTracker<T> {
    private final Map<T, Long> editedOrder = new HashMap<>();
    private long sequence;

    /**
     * Rebuilds the edited set from the current data, for example after loading or undoing. Entries that were
     * already edited keep their position; newly edited entries go on top. On the first refresh every edited entry
     * gets the same rank, so the fallback order decides between them. If the predicate throws, the previous state
     * is kept.
     *
     * @param entries all current entries
     * @param editedPredicate returns whether an entry differs from its default
     * @throws NullPointerException if an argument is {@code null}
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
     * Records that one entry became edited or went back to its default.
     *
     * @param entry entry that changed
     * @param edited whether the entry now differs from its default
     * @return {@code true} if the edited set changed, meaning the list order should be refreshed
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

    /** Returns whether the entry is currently marked as edited. */
    public boolean isEdited(T entry) {
        return editedOrder.containsKey(entry);
    }

    /**
     * Returns a comparator that puts edited entries first, most recent edit first, and orders everything else with
     * {@code fallback}. The comparator reads the live state, so re-sort after {@link #update(Object, boolean)}.
     *
     * @param fallback order for unedited entries and for ties
     * @throws NullPointerException if {@code fallback} is {@code null}
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

    /** Forgets every edit. */
    public void clear() {
        editedOrder.clear();
    }
}
