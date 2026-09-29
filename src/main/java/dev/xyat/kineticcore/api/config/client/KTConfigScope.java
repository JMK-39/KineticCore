package dev.xyat.kineticcore.api.config.client;

/**
 * Describes where a configuration page's source of truth lives. The config screen shows it as a badge with an
 * explanation.
 */
public enum KTConfigScope {
    /** Stored only in this client's configuration. */
    CLIENT_LOCAL(
            "gui.kineticcore.config.scope.client.short",
            "gui.kineticcore.config.scope.client.detail"
    ),
    /**
     * Stored in this Minecraft installation; never changes a connected remote server. This is the default page
     * scope.
     */
    LOCAL_INSTALLATION(
            "gui.kineticcore.config.scope.installation.short",
            "gui.kineticcore.config.scope.installation.detail"
    ),
    /**
     * Owned by the current game server, including the integrated server in singleplayer. Editable by operators
     * (permission level 2) on pages marked {@link KTConfigPage.Builder#serverManaged()}.
     */
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

    /** Returns the language key of the short badge text for this scope. */
    public String shortTranslationKey() {
        return shortTranslationKey;
    }

    /** Returns the language key of the tooltip that explains this scope. */
    public String detailTranslationKey() {
        return detailTranslationKey;
    }

}
