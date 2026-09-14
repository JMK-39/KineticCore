package dev.xyat.kineticcore.api.flight;

import net.minecraft.client.Minecraft;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class KineticFlightClient {
    private static volatile boolean noclipEnabled;
    private static volatile float flightSpeedMultiplier = 1.0F;
    private static volatile boolean inertiaEnabled;
    private static volatile BooleanSupplier speedModifierDown = () -> false;
    private static volatile Consumer<Boolean> noclipRequestHandler = KineticFlightClient::applyLocalNoclip;

    private KineticFlightClient() {
    }

    public static void installSpeedModifierState(BooleanSupplier supplier) {
        speedModifierDown = supplier == null ? () -> false : supplier;
    }

    public static void installNoclipRequestHandler(Consumer<Boolean> handler) {
        noclipRequestHandler = handler == null ? KineticFlightClient::applyLocalNoclip : handler;
    }

    public static boolean isSpeedModifierDown() {
        return speedModifierDown.getAsBoolean();
    }

    public static boolean noclipEnabled() {
        return noclipEnabled;
    }

    public static void requestNoclip(boolean enabled) {
        noclipRequestHandler.accept(enabled);
    }

    public static void applyServerNoclip(boolean enabled) {
        applyLocalNoclip(enabled);
    }

    public static void applyLocalNoclip(boolean enabled) {
        Minecraft minecraft = Minecraft.getInstance();
        noclipEnabled = enabled;
        if (minecraft.player != null) {
            minecraft.player.getPersistentData().putBoolean("kt_noclip", enabled);
            minecraft.player.noPhysics = enabled;
            minecraft.player.refreshDimensions();
        }
    }

    public static float flightSpeedMultiplier() {
        return flightSpeedMultiplier;
    }

    public static void setFlightSpeedMultiplier(float multiplier) {
        flightSpeedMultiplier = multiplier;
    }

    public static boolean inertiaEnabled() {
        return inertiaEnabled;
    }

    public static void setInertiaEnabled(boolean enabled) {
        inertiaEnabled = enabled;
    }
}
