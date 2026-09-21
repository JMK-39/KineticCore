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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class FlightClient {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    private static KineticKeyBindings.Binding speedModifierKey;
    private static KineticKeyBindings.Binding superFlightFreeLookKey;

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
                () -> KineticFlightClient.installSuperFlightRollRequestHandler(FlightNetwork::requestSuperFlightRoll),
                () -> KineticFlightClient.setSuperFlightSelectedSpeedMultiplier(SuperFlightClientConfig.selectedSpeed()),
                () -> KineticFlightClient.installNoclipRequestHandler(FlightClient::setNoclip),
                () -> KineticClientEvents.onLogin(FlightClient::onLogin),
                () -> KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, KineticFlightClient::tickSuperFlight),
                () -> KineticClientEvents.onCameraAngles(FlightClient::onCameraAngles),
                () -> KineticClientEvents.onHudRender(KineticClientEvents.HudStage.END, FlightClient::renderSuperFlightHorizon),
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

    private static void renderSuperFlightHorizon(GuiGraphics graphics, float partialTick) {
        if (!KineticFlightClient.superFlightActive() || KineticClientRuntime.guiHidden()) return;
        if (KineticClientRuntime.localPlayer() == null || KineticClientRuntime.currentScreen() != null) return;

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        float roll = KineticFlightClient.superFlightRoll(partialTick);

        int referenceColor = 0x6640FF80;
        int currentColor = 0xFF52FF7A;
        int centerColor = 0xCC8CFFAA;

        PoseStack pose = graphics.pose();

        pose.pushPose();
        pose.translate(centerX, centerY, 0.0F);

        graphics.fill(-26, 0, -10, 1, referenceColor);
        graphics.fill(10, 0, 26, 1, referenceColor);
        graphics.fill(-26, -4, -25, 5, centerColor);
        graphics.fill(25, -4, 26, 5, centerColor);
        graphics.fill(-4, -1, -2, 1, centerColor);
        graphics.fill(2, -1, 4, 1, centerColor);

        pose.popPose();

        pose.pushPose();
        pose.translate(centerX, centerY, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(-roll));

        graphics.fill(-18, 0, -5, 1, currentColor);
        graphics.fill(5, 0, 18, 1, currentColor);
        graphics.fill(-18, -2, -17, 3, currentColor);
        graphics.fill(17, -2, 18, 3, currentColor);
        graphics.fill(-1, -6, 0, 6, currentColor);
        graphics.fill(1, -6, 2, 6, currentColor);

        pose.popPose();
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
