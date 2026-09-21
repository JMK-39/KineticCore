package dev.xyat.kineticcore.feature.spawnegg.client;


import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.registry.KineticClientRenderers;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.feature.spawnegg.SpawnEggInit;
import dev.xyat.kineticcore.feature.spawnegg.network.SpawnEggNetwork;
import net.minecraft.client.player.LocalPlayer;

public final class SpawnEggClient {
    private static final String MODE_KEY = "DisableEggThrow";
    private static final String TOAST_ID = "spawn_egg_toggle";

    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    private SpawnEggClient() {
    }

    public static void register() {
        REGISTRATION.run(
                () -> KineticKeyBindings.builder("key.kineticcore.toggle_egg")
                        .category("key.kineticcore.category")
                        .context(KineticKeyBindings.Context.IN_GAME)
                        .modifier(KineticKeyBindings.Modifier.ALT)
                        .keyboard(KineticKeyBindings.Key.O)
                        .onPressed(SpawnEggClient::toggleMode)
                        .register(),
                () -> KineticClientRenderers.registerEntityRenderer(
                        SpawnEggInit.THROWABLE_SPAWN_EGG,
                        ThrowSpawnEggRenderer::new
                )
        );
    }

    private static boolean toggleMode() {
        LocalPlayer player = KineticClientRuntime.localPlayer();
        if (player == null) return false;

        boolean disabled = !player.getPersistentData().getBoolean(MODE_KEY);
        player.getPersistentData().putBoolean(MODE_KEY, disabled);
        KineticOverlays.toast(
                          TOAST_ID,
                          KineticI18n.translatable(disabled ? "tip.kineticcore.egg.vanilla" : "tip.kineticcore.egg.throw"),
                          KineticOverlays.Position.BOTTOM_CENTER,
                          5000,
                          0,
                          -30
                  );
        SpawnEggNetwork.sendModeToServer(disabled);
        return true;
    }
}
