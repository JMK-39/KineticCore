package dev.xyat.kineticcore.feature.spawnegg.client;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.registry.KineticClientRenderers;
import dev.xyat.kineticcore.feature.spawnegg.SpawnEggInit;
import dev.xyat.kineticcore.feature.spawnegg.network.SpawnEggNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class SpawnEggClient {
    private static final String MODE_KEY = "DisableEggThrow";
    private static final String TOAST_ID = "spawn_egg_toggle";

    private static boolean registered;

    private SpawnEggClient() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        KineticKeyBindings.builder("key.kineticcore.toggle_egg")
                .category("key.kineticcore.category")
                .context(KineticKeyBindings.Context.IN_GAME)
                .modifier(KineticKeyBindings.Modifier.ALT)
                .keyboardKey(GLFW.GLFW_KEY_O)
                .onPressed(SpawnEggClient::toggleMode)
                .register();

        KineticClientRenderers.registerEntityRenderer(
                SpawnEggInit.THROWABLE_SPAWN_EGG,
                ThrowSpawnEggRenderer::new
        );
    }

    private static boolean toggleMode() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        boolean disabled = !player.getPersistentData().getBoolean(MODE_KEY);
        player.getPersistentData().putBoolean(MODE_KEY, disabled);
        GuiOverlay.toast(
                TOAST_ID,
                Component.translatable(disabled ? "tip.kineticcore.egg.vanilla" : "tip.kineticcore.egg.throw")
        );
        SpawnEggNetwork.sendModeToServer(disabled);
        return true;
    }
}
