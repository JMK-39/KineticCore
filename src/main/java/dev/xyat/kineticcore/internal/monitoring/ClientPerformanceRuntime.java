package dev.xyat.kineticcore.internal.monitoring;

import dev.xyat.kineticcore.api.monitoring.KineticClientPerformance;
import net.minecraft.client.Minecraft;
//? if forge {
import net.minecraft.util.FrameTimer;
//?} else {
/*import net.minecraft.Util;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?}

public final class ClientPerformanceRuntime {
    //? if neoforge {
    /*// Since 1.20.5 vanilla keeps frame times in a private debug-chart logger, so KineticCore records them itself,
    // in the same ring layout as the old FrameTimer: 240 entries, logStart is the oldest, logEnd the next write.
    private static final int CAPACITY = 240;
    private static final long[] FRAME_NANOS = new long[CAPACITY];
    private static int logStart;
    private static int logLength;
    private static int logEnd;
    private static long lastFrameNanos;
    private static boolean recording;
    *///?}

    private ClientPerformanceRuntime() {
    }

    public static KineticClientPerformance.FrameSnapshot snapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        //? if forge {
        FrameTimer timer = minecraft.getFrameTimer();
        return new KineticClientPerformance.FrameSnapshot(
                minecraft.getFps(),
                timer.getLogStart(),
                timer.getLogEnd(),
                timer.getLog()
        );
        //?} else {
        /*synchronized (ClientPerformanceRuntime.class) {
            startRecording();
            return new KineticClientPerformance.FrameSnapshot(minecraft.getFps(), logStart, logEnd, FRAME_NANOS.clone());
        }
        *///?}
    }

    //? if neoforge {
    /*private static void startRecording() {
        if (recording) return;
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post event) -> recordFrame());
        recording = true;
    }

    private static synchronized void recordFrame() {
        long now = Util.getNanos();
        if (lastFrameNanos != 0L) {
            FRAME_NANOS[logEnd] = now - lastFrameNanos;
            logEnd = (logEnd + 1) % CAPACITY;
            if (logLength < CAPACITY) {
                logStart = 0;
                logLength++;
            } else {
                logStart = (logEnd + 1) % CAPACITY;
            }
        }
        lastFrameNanos = now;
    }
    *///?}
}
