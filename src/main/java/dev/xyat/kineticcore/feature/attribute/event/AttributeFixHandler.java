package dev.xyat.kineticcore.feature.attribute.event;

import dev.xyat.kineticcore.api.runtime.KineticModLifecycle;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.feature.attribute.config.AttributeConfig;

/** Registers the common attribute-limit lifecycle hook without loading client-only classes. */
public final class AttributeFixHandler {
    private static final KineticRegistrationBatch COMMON_REGISTRATION = new KineticRegistrationBatch();

    private AttributeFixHandler() {
    }

    /** Registers the common attribute configuration apply hook. */
    public static void register() {
        COMMON_REGISTRATION.run(() -> KineticModLifecycle.onLoadComplete(AttributeConfig::loadAndApply));
    }
}
