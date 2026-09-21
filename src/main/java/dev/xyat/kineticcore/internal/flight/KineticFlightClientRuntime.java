package dev.xyat.kineticcore.internal.flight;

import net.minecraft.client.Minecraft;

/** Internal client implementation for Kinetic flight state that touches the Minecraft singleton. */
public final class KineticFlightClientRuntime {
    private KineticFlightClientRuntime() {
    }

    /** Applies the local noclip state to the active client player when available. */
    public static void applyLocalNoclip(boolean enabled) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.player.getPersistentData().putBoolean("kt_noclip", enabled);
        minecraft.player.noPhysics = enabled;
        minecraft.player.refreshDimensions();
    }
}
