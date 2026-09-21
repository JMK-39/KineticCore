package dev.xyat.kineticcore.api.client.widget.state;

import java.util.Objects;

/** Public API type for layer state. */
public class LayerState<L> {
    private L activeLayer;

    /**
     * Opens the requested API resource or view.
     */
    public void open(L layer) {
        activeLayer = Objects.requireNonNull(layer);
    }

    /**
     * Closes the current API resource or view.
     */
    public void close(L layer) {
        if (Objects.equals(activeLayer, layer)) activeLayer = null;
    }

    /**
     * Performs the close all API operation.
     */
    public void closeAll() {
        activeLayer = null;
    }

    /**
     * Returns whether open.
     */
    public boolean isOpen(L layer) {
        return activeLayer != null && Objects.equals(activeLayer, layer);
    }

    /**
     * Returns whether any open.
     */
    public boolean isAnyOpen() {
        return activeLayer != null;
    }

    /**
     * Performs the active layer API operation.
     */
    public L activeLayer() {
        return activeLayer;
    }
}
