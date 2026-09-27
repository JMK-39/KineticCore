package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.widget.KineticEntityPreview;
import dev.xyat.kineticcore.internal.client.gui.widget.render.KineticEntityPreview.EntityPreviewRenderer;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Resolves the internal renderer behind a public entity preview. */
public final class PreviewAccess {
    private static final Map<KineticEntityPreview, EntityPreviewRenderer> RENDERERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PreviewAccess() {
    }

    public static void register(KineticEntityPreview preview, EntityPreviewRenderer renderer) {
        RENDERERS.put(preview, renderer);
    }

    public static EntityPreviewRenderer renderer(KineticEntityPreview preview) {
        return RENDERERS.get(preview);
    }
}
