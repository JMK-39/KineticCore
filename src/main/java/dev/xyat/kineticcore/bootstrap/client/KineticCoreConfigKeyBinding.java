package dev.xyat.kineticcore.bootstrap.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/** The single in-game entry point for every registered KT configuration page. */
public final class KineticCoreConfigKeyBinding {
    private static final Lazy<KeyMapping> OPEN_CONFIG = Lazy.of(() -> new KeyMapping(
            "key.kineticcore.config.open",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "key.kineticcore.category"
    ));

    private static boolean registered;

    private KineticCoreConfigKeyBinding() {
    }

    public static void register(IEventBus modEventBus) {
        if (registered) return;
        modEventBus.addListener(KineticCoreConfigKeyBinding::onRegisterKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(KineticCoreConfigKeyBinding::onClientTick);
        registered = true;
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG.get());
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_CONFIG.get().consumeClick()) {
            // The global entry is intentionally a plain key. Discard modified
            // chords so it cannot overlap a feature shortcut after rebinding.
            if (Screen.hasAltDown() || Screen.hasControlDown() || Screen.hasShiftDown()) {
                continue;
            }
            if (minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
                minecraft.setScreen(KTConfigApi.createScreen(null));
                break;
            }
        }
    }
}
