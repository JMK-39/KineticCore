package dev.xyat.kineticcore.config.client;

/** Describes where a configuration page's source of truth lives. */
public enum KTConfigScope {
    CLIENT_LOCAL(
            "gui.kineticcore.config.scope.client.short",
            "gui.kineticcore.config.scope.client.detail",
            0xFF55FFFF
    ),
    LOCAL_INSTALLATION(
            "gui.kineticcore.config.scope.installation.short",
            "gui.kineticcore.config.scope.installation.detail",
            0xFFFFDD55
    ),
    SERVER_AUTHORITATIVE(
            "gui.kineticcore.config.scope.server.short",
            "gui.kineticcore.config.scope.server.detail",
            0xFFFFAA55
    );

    private final String shortTranslationKey;
    private final String detailTranslationKey;
    private final int displayColor;

    KTConfigScope(String shortTranslationKey, String detailTranslationKey, int displayColor) {
        this.shortTranslationKey = shortTranslationKey;
        this.detailTranslationKey = detailTranslationKey;
        this.displayColor = displayColor;
    }

    public String shortTranslationKey() {
        return shortTranslationKey;
    }

    public String detailTranslationKey() {
        return detailTranslationKey;
    }

    public int displayColor() {
        return displayColor;
    }
}
