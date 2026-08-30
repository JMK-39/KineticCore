package dev.xyat.kineticcore.api.client.gui;

import java.util.Objects;

public final class GuiLayerManager<L> {
    private L activeLayer;

    public void open(L layer) {
        activeLayer = Objects.requireNonNull(layer);
    }

    public void close(L layer) {
        if (Objects.equals(activeLayer, layer)) {
            activeLayer = null;
        }
    }

    public void closeAll() {
        activeLayer = null;
    }

    public boolean isOpen(L layer) {
        return Objects.equals(activeLayer, layer);
    }

    public boolean isAnyOpen() {
        return activeLayer != null;
    }

    public L activeLayer() {
        return activeLayer;
    }
}
