package dev.xyat.kineticcore.feature.mining.client;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.feature.mining.network.MiningModeNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class MiningModeClient {
    private static boolean registered;

    public static boolean isSingleModeClientSide;

    private MiningModeClient() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        KineticKeyBindings.builder("key.kineticcore.toggle_mining_mode")
                .category("key.kineticcore.category")
                .context(KineticKeyBindings.Context.IN_GAME)
                .modifier(KineticKeyBindings.Modifier.ALT)
                .keyboardKey(GLFW.GLFW_KEY_H)
                .onPressed(MiningModeClient::toggleMode)
                .register();
    }

    private static boolean toggleMode() {
        if (Minecraft.getInstance().player == null) return false;

        isSingleModeClientSide = !isSingleModeClientSide;
        GuiOverlay.toast(
                "mining_mode_toggle",
                Component.translatable(isSingleModeClientSide
                        ? "tip.kineticcore.mining.mode.single"
                        : "tip.kineticcore.mining.mode.normal")
        );
        MiningModeNetwork.sendToggleToServer();
        return true;
    }
}
