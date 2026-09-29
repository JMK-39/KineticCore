package dev.xyat.kineticcore.api.client.gui.state;

import java.util.Objects;

/**
 * Tracks which one of several mutually exclusive popup layers (menu, dialog, picker) is open. Opening a layer
 * closes the previous one. Not thread-safe; use it from the screen that owns it.
 *
 * @param <L> layer identifier type, typically an enum
 */
public class LayerState<L> {
    private L activeLayer;

    /**
     * Opens a layer, replacing any open layer.
     *
     * @throws NullPointerException if {@code layer} is {@code null}
     */
    public void open(L layer) {
        activeLayer = Objects.requireNonNull(layer, "layer");
    }

    /** Closes the layer if it is the open one; closing any other layer does nothing. */
    public void close(L layer) {
        if (Objects.equals(activeLayer, layer)) activeLayer = null;
    }

    /** Closes whatever layer is open. */
    public void closeAll() {
        activeLayer = null;
    }

    /** Returns whether this layer is the open one. */
    public boolean isOpen(L layer) {
        return activeLayer != null && Objects.equals(activeLayer, layer);
    }

    /** Returns whether any layer is open. */
    public boolean isAnyOpen() {
        return activeLayer != null;
    }

    /** Returns the open layer, or {@code null} when none is open. */
    public L activeLayer() {
        return activeLayer;
    }
}
