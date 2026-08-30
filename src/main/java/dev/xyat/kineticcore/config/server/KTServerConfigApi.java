package dev.xyat.kineticcore.config.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class KTServerConfigApi {
    private static final Map<String, KTServerConfigSpec> SPECS = new LinkedHashMap<>();

    private KTServerConfigApi() {
    }

    public static synchronized void register(KTServerConfigSpec spec) {
        Objects.requireNonNull(spec, "spec");
        KTServerConfigSpec old = SPECS.putIfAbsent(spec.pageId(), spec);
        if (old != null && old != spec) {
            throw new IllegalStateException("Server config page is already registered: " + spec.pageId());
        }
    }

    public static synchronized void registerActionPage(String pageId) {
        if (SPECS.containsKey(pageId)) return;
        register(KTServerConfigSpec.builder(pageId).build());
    }

    public static synchronized Optional<KTServerConfigSpec> find(String pageId) {
        return Optional.ofNullable(SPECS.get(pageId));
    }

    public static synchronized boolean isRegistered(String pageId) {
        return SPECS.containsKey(pageId);
    }
}
