package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.api.client.effect.KineticEffectDisplay;
import net.minecraftforge.client.event.ScreenEvent;

public final class KineticEffectDisplayBridge {
    private KineticEffectDisplayBridge() {
    }

    public static void onInventoryEffects(ScreenEvent.RenderInventoryMobEffects event) {
        event.setCompact(KineticEffectDisplay.compact(event.getAvailableSpace()));
    }
}
