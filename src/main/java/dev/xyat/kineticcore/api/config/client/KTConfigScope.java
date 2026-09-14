package dev.xyat.kineticcore.api.config.client;

/** Describes where a configuration page's source of truth lives. */
public enum KTConfigScope {
    CLIENT_LOCAL(
            "gui.kineticcore.config.scope.client.short",
            "gui.kineticcore.config.scope.client.detail"
    ),
    LOCAL_INSTALLATION(
            "gui.kineticcore.config.scope.installation.short",
            "gui.kineticcore.config.scope.installation.detail"
    ),
    SERVER_AUTHORITATIVE(
            "gui.kineticcore.config.scope.server.short",
            "gui.kineticcore.config.scope.server.detail"
    );

    private final String shortTranslationKey;
    private final String detailTranslationKey;

    KTConfigScope(String shortTranslationKey, String detailTranslationKey) {
        this.shortTranslationKey = shortTranslationKey;
        this.detailTranslationKey = detailTranslationKey;
    }

    public String shortTranslationKey() {
        return shortTranslationKey;
    }

    public String detailTranslationKey() {
        return detailTranslationKey;
    }

}
