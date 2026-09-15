package dev.xyat.kineticcore.api.config.common;

import dev.xyat.kineticcore.internal.config.common.KTCommonConfigRuntime;

import java.util.Objects;

public final class KTCommonConfigApi {
    private KTCommonConfigApi() {
    }

    public static void register(KTCommonConfigSpec spec, String fileName) {
        Objects.requireNonNull(spec, "spec");
        String normalized = Objects.requireNonNull(fileName, "fileName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        KTCommonConfigRuntime.registerCommon(spec.internalHandle(), normalized);
    }
}
