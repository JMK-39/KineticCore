package dev.xyat.kineticcore.bootstrap.client;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;

public final class KineticCoreConfigKeyBinding {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    private KineticCoreConfigKeyBinding() {
    }

    public static void register() {
        REGISTRATION.run(() -> KineticKeyBindings.builder("key.kineticcore.config.open")
                .category("key.kineticcore.category")
                .context(KineticKeyBindings.Context.IN_GAME)
                .keyboard(KineticKeyBindings.Key.F6)
                .exactModifiers(true)
                .onPressed(KineticCoreConfigKeyBinding::openConfig)
                .register());
    }

    private static boolean openConfig() {
        if (KineticClientRuntime.localPlayer() == null
                || KineticClientRuntime.currentLevel() == null
                || KineticClientRuntime.currentScreen() != null) {
            return false;
        }
        KineticClientRuntime.openScreen(KTConfigApi.createIndexScreen(null));
        return true;
    }
}
