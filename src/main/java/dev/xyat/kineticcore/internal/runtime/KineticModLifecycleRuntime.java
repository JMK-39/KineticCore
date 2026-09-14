package dev.xyat.kineticcore.internal.runtime;

import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

public final class KineticModLifecycleRuntime {
    private static final List<Runnable> LOAD_COMPLETE_ACTIONS = new ArrayList<>();
    private static boolean listenerRegistered;
    private static boolean loadComplete;

    private KineticModLifecycleRuntime() {
    }

    public static synchronized void onLoadComplete(Runnable action) {
        if (loadComplete) {
            action.run();
            return;
        }
        LOAD_COMPLETE_ACTIONS.add(action);
        ensureListener();
    }

    private static void ensureListener() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticModLifecycleRuntime::onLoadCompleteEvent);
    }

    private static void onLoadCompleteEvent(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            List<Runnable> actions;
            synchronized (KineticModLifecycleRuntime.class) {
                if (loadComplete) return;
                loadComplete = true;
                actions = List.copyOf(LOAD_COMPLETE_ACTIONS);
                LOAD_COMPLETE_ACTIONS.clear();
            }
            for (Runnable action : actions) {
                action.run();
            }
        });
    }
}
