package dev.xyat.kineticcore.bootstrap.client;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KineticCoreConfigKeyBinding {
    private static boolean registered;

    private KineticCoreConfigKeyBinding() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        KineticKeyBindings.builder("key.kineticcore.config.open")
                .category("key.kineticcore.category")
                .context(KineticKeyBindings.Context.IN_GAME)
                .keyboardKey(GLFW.GLFW_KEY_F6)
                .exactModifiers(true)
                .onPressed(KineticCoreConfigKeyBinding::openConfig)
                .register();
    }

    private static boolean openConfig() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return false;
        }
        KineticClientRuntime.openScreen(KTConfigApi.createScreen(null));
        return true;
    }
}
