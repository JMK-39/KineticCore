package dev.xyat.kineticcore.api.client.gui.state;

import java.util.Objects;

/**
 * Holds what is being dragged in a screen: a drag kind and an optional payload such as the dragged row. Not
 * thread-safe.
 *
 * @param <T> drag kind type, typically an enum
 */
public class DragStateController<T> {
    private T type;
    private Object payload;

    /**
     * Starts a drag, replacing any current one.
     *
     * @param type drag kind
     * @param payload dragged data, or {@code null}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public void start(T type, Object payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = payload;
    }

    /** Returns whether a drag is in progress. */
    public boolean isActive() {
        return type != null;
    }

    /** Returns the drag kind, or {@code null} when no drag is active. */
    public T type() {
        return type;
    }

    /** Returns the dragged data, or {@code null} when none was given or no drag is active. */
    public Object payload() {
        return payload;
    }

    /** Ends the drag and releases the payload reference. */
    public void clear() {
        type = null;
        payload = null;
    }
}
