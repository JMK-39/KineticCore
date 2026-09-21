package dev.xyat.kineticcore.api.client.widget.state;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Small reusable state holders for Kinetic GUI business screens.
 */
public final class KineticUiState {
    private KineticUiState() {
    }

    /** Tracks one active drag type and its business payload. */
    public static final class Drag<T> {
        private T type;
        private Object payload;

        /** Starts or replaces the active drag state. */
        public void start(T type, Object payload) {
            this.type = Objects.requireNonNull(type, "type");
            this.payload = payload;
        }

        /** Returns whether a drag is currently active. */
        public boolean isActive() {
            return type != null;
        }

        /** Returns the active drag type, or {@code null} when inactive. */
        public T type() {
            return type;
        }

        /** Returns the active business payload, or {@code null} when absent. */
        public Object payload() {
            return payload;
        }

        /** Clears the active drag state. */
        public void clear() {
            type = null;
            payload = null;
        }
    }

    /** Tracks edited entries and keeps newly edited entries ahead of unchanged entries. */
    public static final class EditedEntries<T> {
        private final Map<T, Long> editedOrder = new HashMap<>();
        private long sequence;

        /** Rebuilds the edited-entry snapshot from the supplied collection and predicate. */
        public void refresh(Collection<T> entries, Predicate<T> editedPredicate) {
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

        /** Updates one entry and returns whether tracked edited-state membership changed. */
        public boolean update(T entry, boolean edited) {
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

        /** Returns whether the supplied entry is currently tracked as edited. */
        public boolean isEdited(T entry) {
            return editedOrder.containsKey(entry);
        }

        /** Returns a comparator that prioritizes edited entries before the supplied fallback ordering. */
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

        /** Clears all tracked edited entries. */
        public void clear() {
            editedOrder.clear();
        }
    }

    /** Tracks one active logical GUI layer at a time. */
    public static final class Layer<L> {
        private L activeLayer;

        /** Opens the supplied layer and replaces any previously active layer. */
        public void open(L layer) {
            activeLayer = Objects.requireNonNull(layer, "layer");
        }

        /** Closes the supplied layer only when it is currently active. */
        public void close(L layer) {
            if (Objects.equals(activeLayer, layer)) activeLayer = null;
        }

        /** Closes any active layer. */
        public void closeAll() {
            activeLayer = null;
        }

        /** Returns whether the supplied layer is currently open. */
        public boolean isOpen(L layer) {
            return activeLayer != null && Objects.equals(activeLayer, layer);
        }

        /** Returns whether any layer is currently open. */
        public boolean isAnyOpen() {
            return activeLayer != null;
        }

        /** Returns the active layer, or {@code null} when no layer is open. */
        public L activeLayer() {
            return activeLayer;
        }
    }
}
