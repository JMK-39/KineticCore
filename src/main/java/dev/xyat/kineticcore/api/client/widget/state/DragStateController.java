package dev.xyat.kineticcore.api.client.widget.state;

import java.util.Objects;

/** Public API type for drag state controller. */
public class DragStateController<T> {
    private T type;
    private Object payload;

    /**
     * Performs the start API operation.
     */
    public void start(T type, Object payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = payload;
    }

    /**
     * Returns whether active.
     */
    public boolean isActive() {
        return type != null;
    }

    /**
     * Returns the type.
     */
    public T type() {
        return type;
    }

    /**
     * Performs the payload API operation.
     */
    public Object payload() {
        return payload;
    }

    /**
     * Clears the current API state.
     */
    public void clear() {
        type = null;
        payload = null;
    }
}
