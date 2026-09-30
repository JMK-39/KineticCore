package dev.xyat.kineticcore.feature.logcleaner;

import java.util.Objects;

/** Tracks one run of consecutive identical messages. */
final class ConsecutiveLogState<K> {
    private K lastKey;
    private int repetitions;

    /** Returns -1 for a duplicate, otherwise the count to summarize before emitting this message. */
    int accept(K key) {
        if (lastKey != null && Objects.equals(lastKey, key)) {
            repetitions++;
            return -1;
        }
        int previous = repetitions;
        lastKey = key;
        repetitions = 0;
        return previous;
    }

    /** Ends the current run and returns its suppressed count. */
    int drain() {
        int previous = repetitions;
        lastKey = null;
        repetitions = 0;
        return previous;
    }
}
