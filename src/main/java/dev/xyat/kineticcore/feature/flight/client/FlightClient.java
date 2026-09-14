package dev.xyat.kineticcore.feature.flight.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.flight.KineticFlightClient;
import dev.xyat.kineticcore.feature.flight.network.FlightNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;

public final class FlightClient {
    private static boolean registered;
    private static KineticKeyBindings.Binding speedModifierKey;

    private FlightClient() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        KineticKeyBindings.builder("key.kineticcore.flying.noclip")
                .category("key.categories.movement")
                .context(KineticKeyBindings.Context.IN_GAME)
                .modifier(KineticKeyBindings.Modifier.ALT)
                .keyboardKey(GLFW.GLFW_KEY_K)
                .onPressed(FlightClient::handleNoclipKey)
                .register();

        speedModifierKey = KineticKeyBindings.builder("key.kineticcore.flying.speed.modifier")
                .category("key.categories.movement")
                .context(KineticKeyBindings.Context.IN_GAME)
                .keyboardKey(GLFW.GLFW_KEY_LEFT_SHIFT)
                .register();

        KineticKeyBindings.builder("key.kineticcore.flying.inertia")
                .category("key.kineticcore.category")
                .context(KineticKeyBindings.Context.IN_GAME)
                .modifier(KineticKeyBindings.Modifier.SHIFT)
                .keyboardKey(GLFW.GLFW_KEY_F)
                .onPressed(() -> {
                    toggleInertia();
                    return true;
                })
                .register();

        KineticFlightClient.installSpeedModifierState(() -> speedModifierKey != null && speedModifierKey.isDown());
        KineticFlightClient.installNoclipRequestHandler(FlightClient::setNoclip);

        KineticClientEvents.onLogin(FlightClient::onLogin);
        MinecraftForge.EVENT_BUS.addListener(FlightClient::onBlockOverlay);
    }

    public static void applyServerNoclip(boolean state) {
        KineticFlightClient.applyServerNoclip(state);
    }

    public static void setNoclip(boolean state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (KineticFlightClient.noclipEnabled() == state) return;

        KineticFlightClient.applyLocalNoclip(state);
        FlightNetwork.requestNoclip(state);

        Component status = Component.translatable(
                state ? "msg.kineticcore.flying.on" : "msg.kineticcore.flying.off"
        );
        mc.player.displayClientMessage(Component.translatable("msg.kineticcore.flying.noclip_status", status), true);
    }

    public static void toggleNoclip() {
        setNoclip(!KineticFlightClient.noclipEnabled());
    }

    public static void toggleInertia() {
        boolean enabled = !KineticFlightClient.inertiaEnabled();
        KineticFlightClient.setInertiaEnabled(enabled);
        Component status = Component.translatable(
                enabled ? "msg.kineticcore.flying.on" : "msg.kineticcore.flying.off"
        );
        GuiOverlay.toast("flight_inertia_toggle", Component.translatable("msg.kineticcore.flying.inertia_status", status));
    }

    private static boolean handleNoclipKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return false;
        toggleNoclip();
        return true;
    }

    private static void onLogin() {
        KineticFlightClient.applyLocalNoclip(false);
    }

    private static void onBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (KineticFlightClient.noclipEnabled()
                && event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK) {
            event.setCanceled(true);
        }
    }
}
