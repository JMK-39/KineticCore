package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.internal.config.client.KTClientConfigAdapterRuntime;
import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

/**
 * Registers loader-independent client config specs and exposes them through the
 * Kinetic configuration UI.
 */
public final class KTClientConfigAdapter {
    private KTClientConfigAdapter() {
    }

    /**
     * Registers spec.
     */
    public static void registerSpec(KTClientConfigSpec spec, String fileName) {
        KTClientConfigAdapterRuntime.registerSpec(spec, fileName);
    }

    /**
     * Performs the page builder API operation.
     */
    public static KTConfigPage.Builder pageBuilder(
            String pageId,
            Component title,
            KTClientConfigSpec spec
    ) {
        return KTClientConfigAdapterRuntime.pageBuilder(pageId, title, spec);
    }

    /**
     * Performs the filtered page builder API operation.
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
     * Appends entries.
     */
    public static KTConfigPage.Builder appendEntries(
            KTConfigPage.Builder page,
            KTClientConfigSpec spec
    ) {
        return KTClientConfigAdapterRuntime.appendEntries(page, spec);
    }

    /**
     * Appends filtered entries.
     */
    public static KTConfigPage.Builder appendFilteredEntries(
            KTConfigPage.Builder page,
            KTClientConfigSpec spec,
            Predicate<String> includePath
    ) {
        return KTClientConfigAdapterRuntime.appendEntries(page, spec, includePath);
    }

    /**
     * Infers apply timing.
     */
    public static KTConfigPage.ApplyTiming inferApplyTiming(KTClientConfigSpec spec) {
        return KTClientConfigAdapterRuntime.inferApplyTiming(spec);
    }

    /**
     * Infers filtered apply timing.
     */
    public static KTConfigPage.ApplyTiming inferFilteredApplyTiming(
            KTClientConfigSpec spec,
            Predicate<String> includePath
    ) {
        return KTClientConfigAdapterRuntime.inferApplyTiming(spec, includePath);
    }
}
