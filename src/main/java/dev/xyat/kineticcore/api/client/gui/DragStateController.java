package dev.xyat.kineticcore.api.client.gui;

public final class DragStateController<T> {
    private T type;
    private Object payload;

    public void start(T type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public boolean isActive() {
        return type != null;
    }

    public T type() {
        return type;
    }

    public Object payload() {
        return payload;
    }

    public void clear() {
        type = null;
        payload = null;
    }
}
