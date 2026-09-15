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

    public static void registerSpec(KTClientConfigSpec spec, String fileName) {
        KTClientConfigAdapterRuntime.registerSpec(spec, fileName);
    }

    public static KTConfigPage.Builder pageBuilder(
            String pageId,
            Component title,
            KTClientConfigSpec spec
    ) {
        return KTClientConfigAdapterRuntime.pageBuilder(pageId, title, spec);
    }

    public static KTConfigPage.Builder pageBuilder(
            String pageId,
            Component title,
            KTClientConfigSpec spec,
            Predicate<String> includePath
    ) {
        return KTClientConfigAdapterRuntime.pageBuilder(pageId, title, spec, includePath);
    }

    public static KTConfigPage.Builder appendEntries(
            KTConfigPage.Builder page,
            KTClientConfigSpec spec
    ) {
        return KTClientConfigAdapterRuntime.appendEntries(page, spec);
    }

    public static KTConfigPage.Builder appendEntries(
            KTConfigPage.Builder page,
            KTClientConfigSpec spec,
            Predicate<String> includePath
    ) {
        return KTClientConfigAdapterRuntime.appendEntries(page, spec, includePath);
    }

    public static KTConfigPage.ApplyTiming inferApplyTiming(KTClientConfigSpec spec) {
        return KTClientConfigAdapterRuntime.inferApplyTiming(spec);
    }

    public static KTConfigPage.ApplyTiming inferApplyTiming(
            KTClientConfigSpec spec,
            Predicate<String> includePath
    ) {
        return KTClientConfigAdapterRuntime.inferApplyTiming(spec, includePath);
    }
}
