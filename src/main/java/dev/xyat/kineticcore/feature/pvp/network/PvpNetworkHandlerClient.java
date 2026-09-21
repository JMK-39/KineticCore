package dev.xyat.kineticcore.feature.pvp.network;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;

public class PvpNetworkHandlerClient {

    public static void handleState(boolean enabled) {
        String key = enabled ? "cmd.kineticcore.pvp.enabled" : "cmd.kineticcore.pvp.disabled";
        KineticOverlays.toast("pvp_toggle", KineticI18n.translatable(key), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
    }
}
