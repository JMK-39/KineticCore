package dev.xyat.kineticcore.feature.mining.client;


import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.feature.mining.network.MiningModeNetwork;

public final class MiningModeClient {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static boolean isSingleModeClientSide;

    private MiningModeClient() {
    }

    public static void register() {
        REGISTRATION.run(() -> KineticKeyBindings.builder("key.kineticcore.toggle_mining_mode")
                .category("key.kineticcore.category")
                .context(KineticKeyBindings.Context.IN_GAME)
                .modifier(KineticKeyBindings.Modifier.ALT)
                .keyboard(KineticKeyBindings.Key.H)
                .onPressed(MiningModeClient::toggleMode)
                .register());
    }

    private static boolean toggleMode() {
        if (KineticClientRuntime.localPlayer() == null) return false;

        isSingleModeClientSide = !isSingleModeClientSide;
        KineticOverlays.toast(
                          "mining_mode_toggle",
                          KineticI18n.translatable(isSingleModeClientSide
                        ? "tip.kineticcore.mining.mode.single"
                        : "tip.kineticcore.mining.mode.normal"),
                          KineticOverlays.Position.BOTTOM_CENTER,
                          5000,
                          0,
                          -30
                  );
        MiningModeNetwork.sendToggleToServer();
        return true;
    }
}
