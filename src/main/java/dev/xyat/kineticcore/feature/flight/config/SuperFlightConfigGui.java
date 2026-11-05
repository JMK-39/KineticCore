package dev.xyat.kineticcore.feature.flight.config;

import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;

/** F6 page for the super-flight sounds and effects of this client. */
public final class SuperFlightConfigGui {
    public static final String PAGE_ID = "kineticcore:super_flight_effects";

    private SuperFlightConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTClientConfigAdapter.filteredPageBuilder(
                        PAGE_ID,
                        tr("title"),
                        SuperFlightClientConfig.SPEC,
                        path -> path.startsWith("SuperFlightEffects.") && !path.endsWith("Color")
                )
                .pageDescription(tr("description"))
                .color("vapor_color", tr("vapor_color"), SuperFlightClientConfig.VAPOR_COLOR::get,
                        SuperFlightClientConfig.VAPOR_COLOR::set, 0xE6F2FF, tr("vapor_color.tooltip"))
                .color("plasma_color", tr("plasma_color"), SuperFlightClientConfig.PLASMA_COLOR::get,
                        SuperFlightClientConfig.PLASMA_COLOR::set, 0xFF7A2E, tr("plasma_color.tooltip"))
                .build());
    }

    private static Component tr(String key) {
        return KineticI18n.translatable("cfg.kineticcore.super_flight_effects." + key);
    }
}
