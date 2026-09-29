package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.internal.config.client.KTClientConfigAdapterRuntime;
import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

/**
 * Registers {@link KTClientConfigSpec} files and turns them into Kinetic config pages.
 *
 * <p>Typical use: build a spec, call {@link #registerSpec(KTClientConfigSpec, String)} from the mod constructor,
 * then create the page with {@link #pageBuilder(String, Component, KTClientConfigSpec)}, optionally add action
 * rows, build it and register it with {@code KTConfigApi}.
 */
public final class KTClientConfigAdapter {
    private KTClientConfigAdapter() {
    }

    /**
     * Registers a spec as a Forge CLIENT config file. Call it from the mod constructor. Registering the same spec
     * again with the same file name does nothing.
     *
     * @param spec spec to register
     * @param fileName file name relative to the config folder, for example {@code mymod-client.toml}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is blank
     * @throws IllegalStateException if the spec was already registered under a different file name
     */
    public static void registerSpec(KTClientConfigSpec spec, String fileName) {
        KTClientConfigAdapterRuntime.registerSpec(spec, fileName);
    }

    /**
     * Creates a page builder that already contains a row for every supported value in the spec.
     *
     * <p>The page uses {@link KTConfigScope#CLIENT_LOCAL}, an apply timing inferred with
     * {@link #inferApplyTiming(KTClientConfigSpec)}, and saves through {@link KTClientConfigSpec#save()}. Sections
     * become dividers with their comments as descriptions. Callers may append more rows before building.
     *
     * @param pageId namespaced page id, for example {@code mymod:client}
     * @param title page title
     * @param spec registered spec to show
     * @return a builder that has not been built yet
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code pageId} is not namespaced
     */
    public static KTConfigPage.Builder pageBuilder(
            String pageId,
            Component title,
            KTClientConfigSpec spec
    ) {
        return KTClientConfigAdapterRuntime.pageBuilder(pageId, title, spec);
    }

    /**
     * Creates a page builder containing only the values whose path is accepted, for example to hide a field that
     * has its own visual editor.
     *
     * @param pageId namespaced page id, for example {@code mymod:client}
     * @param title page title
     * @param spec registered spec to show
     * @param includePath receives dot-separated paths such as {@code hud.enabled}; returns {@code true} to include
     * @return a builder that has not been built yet
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code pageId} is not namespaced
     * @see #pageBuilder(String, Component, KTClientConfigSpec)
     */
    public static KTConfigPage.Builder filteredPageBuilder(
            String pageId,
            Component title,
            KTClientConfigSpec spec,
            Predicate<String> includePath
    ) {
        return KTClientConfigAdapterRuntime.pageBuilder(pageId, title, spec, includePath);
    }

    /**
     * Appends a row for every supported value in the spec to an existing page builder. Unlike
     * {@link #pageBuilder(String, Component, KTClientConfigSpec)}, this does not set the scope, timing or save
     * callback; the caller owns them.
     *
     * @param page builder to append to
     * @param spec spec to read values from
     * @return {@code page}, for chaining
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KTConfigPage.Builder appendEntries(
            KTConfigPage.Builder page,
            KTClientConfigSpec spec
    ) {
        return KTClientConfigAdapterRuntime.appendEntries(page, spec);
    }

    /**
     * Appends rows only for values whose dot-separated path is accepted.
     *
     * @param page builder to append to
     * @param spec spec to read values from
     * @param includePath receives dot-separated paths such as {@code hud.enabled}; returns {@code true} to include
     * @return {@code page}, for chaining
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KTConfigPage.Builder appendFilteredEntries(
            KTConfigPage.Builder page,
            KTClientConfigSpec spec,
            Predicate<String> includePath
    ) {
        return KTClientConfigAdapterRuntime.appendEntries(page, spec, includePath);
    }

    /**
     * Derives the page apply timing from the spec's values.
     *
     * @param spec spec to inspect
     * @return {@link KTConfigPage.ApplyTiming#RESTART_GAME} if any value requires a restart, otherwise
     *   {@link KTConfigPage.ApplyTiming#IMMEDIATE}
     * @throws NullPointerException if {@code spec} is {@code null}
     */
    public static KTConfigPage.ApplyTiming inferApplyTiming(KTClientConfigSpec spec) {
        return KTClientConfigAdapterRuntime.inferApplyTiming(spec);
    }

    /**
     * Derives the apply timing from the values whose dot-separated path is accepted.
     *
     * @param spec spec to inspect
     * @param includePath receives dot-separated paths; returns {@code true} to include
     * @return {@link KTConfigPage.ApplyTiming#RESTART_GAME} if any included value requires a restart, otherwise
     *   {@link KTConfigPage.ApplyTiming#IMMEDIATE}
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KTConfigPage.ApplyTiming inferFilteredApplyTiming(
            KTClientConfigSpec spec,
            Predicate<String> includePath
    ) {
        return KTClientConfigAdapterRuntime.inferApplyTiming(spec, includePath);
    }
}
