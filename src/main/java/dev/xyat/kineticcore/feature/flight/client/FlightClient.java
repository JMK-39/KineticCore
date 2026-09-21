package dev.xyat.kineticcore.feature.flight.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.flight.KineticFlightClient;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.feature.flight.config.SuperFlightClientConfig;
import dev.xyat.kineticcore.feature.flight.network.FlightNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class FlightClient {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    private static KineticKeyBindings.Binding speedModifierKey;
    private static KineticKeyBindings.Binding superFlightFreeLookKey;
    private static boolean rollLeftDown;
    private static boolean rollRightDown;

    private FlightClient() {
    }

    public static void register() {
        REGISTRATION.run(
                () -> KineticKeyBindings.builder("key.kineticcore.flying.noclip")
                        .category("key.categories.movement")
                        .context(KineticKeyBindings.Context.IN_GAME)
                        .modifier(KineticKeyBindings.Modifier.ALT)
                        .keyboard(KineticKeyBindings.Key.K)
                        .onPressed(FlightClient::handleNoclipKey)
                        .register(),
                () -> KineticKeyBindings.builder("key.kineticcore.flying.super.toggle")
                        .category("key.categories.movement")
                        .context(KineticKeyBindings.Context.IN_GAME)
                        .keyboard(KineticKeyBindings.Key.V)
                        .onPressed(FlightClient::handleSuperFlightToggle)
                        .register(),
                () -> {
                    superFlightFreeLookKey = KineticKeyBindings.builder("key.kineticcore.flying.super.freelook")
                            .category("key.categories.movement")
                            .context(KineticKeyBindings.Context.IN_GAME)
                            .keyboard(KineticKeyBindings.Key.LEFT_ALT)
                            .register();
                },
                () -> {
                    speedModifierKey = KineticKeyBindings.builder("key.kineticcore.flying.speed.modifier")
                            .category("key.categories.movement")
                            .context(KineticKeyBindings.Context.IN_GAME)
                            .keyboard(KineticKeyBindings.Key.LEFT_SHIFT)
                            .register();
                },
                () -> KineticKeyBindings.builder("key.kineticcore.flying.inertia")
                        .category("key.kineticcore.category")
                        .context(KineticKeyBindings.Context.IN_GAME)
                        .modifier(KineticKeyBindings.Modifier.SHIFT)
                        .keyboard(KineticKeyBindings.Key.F)
                        .onPressed(() -> {
                            toggleInertia();
                            return true;
                        })
                        .register(),
                () -> KineticFlightClient.installSpeedModifierState(() -> speedModifierKey != null && speedModifierKey.isDown()),
                () -> KineticFlightClient.installSuperFlightFreeLookState(() -> superFlightFreeLookKey != null && superFlightFreeLookKey.isDown()),
                () -> KineticFlightClient.installSuperFlightFallFlyingRequestHandler(FlightNetwork::requestSuperFlightFallFlying),
                () -> KineticFlightClient.setSuperFlightSelectedSpeedMultiplier(SuperFlightClientConfig.selectedSpeed()),
                () -> KineticFlightClient.installNoclipRequestHandler(FlightClient::setNoclip),
                () -> KineticClientEvents.onLogin(FlightClient::onLogin),
                () -> KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, KineticFlightClient::tickSuperFlight),
                () -> KineticClientEvents.onMouseButtonBefore(FlightClient::onMouseButton),
                () -> KineticClientEvents.onCameraAngles(FlightClient::onCameraAngles),
                () -> KineticClientEvents.onBlockScreenEffect(FlightClient::onBlockOverlay)
        );
    }

    public static void applyServerNoclip(boolean state) {
        KineticFlightClient.applyServerNoclip(state);
    }

    public static void setNoclip(boolean state) {
        var player = KineticClientRuntime.localPlayer();
        if (player == null) return;
        if (KineticFlightClient.noclipEnabled() == state) return;

        KineticFlightClient.applyLocalNoclip(state);
        FlightNetwork.requestNoclip(state);

        Component status = KineticI18n.translatable(
                state ? "msg.kineticcore.flying.on" : "msg.kineticcore.flying.off"
        );
        player.displayClientMessage(KineticText.translatable("msg.kineticcore.flying.noclip_status", status), true);
    }

    public static void toggleNoclip() {
        setNoclip(!KineticFlightClient.noclipEnabled());
    }

    public static void toggleInertia() {
        boolean enabled = !KineticFlightClient.inertiaEnabled();
        KineticFlightClient.setInertiaEnabled(enabled);
        Component status = KineticI18n.translatable(
                enabled ? "msg.kineticcore.flying.on" : "msg.kineticcore.flying.off"
        );
        KineticOverlays.toast("flight_inertia_toggle", KineticText.translatable("msg.kineticcore.flying.inertia_status", status), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    private static boolean handleNoclipKey() {
        var player = KineticClientRuntime.localPlayer();
        if (player == null || !player.isCreative()) return false;
        toggleNoclip();
        return true;
    }

    private static void onLogin() {
        rollLeftDown = false;
        rollRightDown = false;
        KineticFlightClient.setSuperFlightRollInput(false, false);
        KineticFlightClient.setSuperFlightSelectedSpeedMultiplier(SuperFlightClientConfig.selectedSpeed());
        KineticFlightClient.applyLocalNoclip(false);
        KineticFlightClient.applySuperFlightState(false);
    }

    private static boolean handleSuperFlightToggle() {
        var player = KineticClientRuntime.localPlayer();
        if (player == null || !KineticSuperFlight.available(player)) return false;
        FlightNetwork.requestSuperFlight(!KineticFlightClient.superFlightActive());
        return true;
    }

    private static void onMouseButton(KineticClientEvents.MouseButtonContext context) {
        if (!context.leftButton() && !context.rightButton()) return;
        if (KineticClientRuntime.currentScreen() != null) {
            rollLeftDown = false;
            rollRightDown = false;
            KineticFlightClient.setSuperFlightRollInput(false, false);
            return;
        }

        boolean down = context.pressed();
        if (context.leftButton()) rollLeftDown = down;
        if (context.rightButton()) rollRightDown = down;

        if (KineticFlightClient.superFlightActive()) {
            KineticFlightClient.setSuperFlightRollInput(rollLeftDown, rollRightDown);
        } else {
            KineticFlightClient.setSuperFlightRollInput(false, false);
        }

        if (KineticFlightClient.superFlightManeuvering()) {
            context.cancel();
        }
    }

    private static void onCameraAngles(KineticClientEvents.CameraAnglesContext context) {
        if (!KineticFlightClient.superFlightActive()) return;
        float partialTick = context.partialTick();
        context.setYaw(context.yaw() + KineticFlightClient.superFlightCameraYawOffset(partialTick));
        context.setPitch(Mth.clamp(
                context.pitch() + KineticFlightClient.superFlightCameraPitchOffset(partialTick),
                -89.9F,
                89.9F
        ));
        if (KineticFlightClient.superFlightManeuvering()) {
            context.setRoll(context.roll() + KineticFlightClient.superFlightRoll(partialTick));
        }
    }

    private static void onBlockOverlay(KineticClientEvents.BlockScreenEffectContext context) {
        if (KineticFlightClient.noclipEnabled()) {
            context.cancel();
        }
    }
}
