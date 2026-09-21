package dev.xyat.kineticcore.internal.monitoring;

import dev.xyat.kineticcore.api.monitoring.KineticClientPerformance;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FrameTimer;

public final class ClientPerformanceRuntime {
    private ClientPerformanceRuntime() {
    }

    public static KineticClientPerformance.FrameSnapshot snapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        FrameTimer timer = minecraft.getFrameTimer();
        return new KineticClientPerformance.FrameSnapshot(
                minecraft.getFps(),
                timer.getLogStart(),
                timer.getLogEnd(),
                timer.getLog()
        );
    }
}
