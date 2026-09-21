package dev.xyat.kineticcore.internal.runtime;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KineticModLifecycleRuntime {
    private static final List<Runnable> COMMON_SETUP_ACTIONS = new ArrayList<>();
    private static final List<Runnable> CLIENT_SETUP_ACTIONS = new ArrayList<>();
    private static final List<Runnable> LOAD_COMPLETE_ACTIONS = new ArrayList<>();
    private static boolean commonListenerRegistered;
    private static boolean clientListenerRegistered;
    private static boolean completionListenerRegistered;
    private static boolean commonSetupRegistrationClosed;
    private static boolean clientSetupRegistrationClosed;
    private static boolean loadCompleteEventSeen;
    private static boolean loadComplete;

    private KineticModLifecycleRuntime() {
    }

    public static synchronized void onCommonSetup(Runnable action) {
        Objects.requireNonNull(action, "action");
        ensureListener();
        if (commonSetupRegistrationClosed) {
            throw new IllegalStateException("Common setup registration window has already closed");
        }
        COMMON_SETUP_ACTIONS.add(action);
    }

    public static synchronized void onClientSetup(Runnable action) {
        Objects.requireNonNull(action, "action");
        ensureListener();
        if (clientSetupRegistrationClosed) {
            throw new IllegalStateException("Client setup registration window has already closed");
        }
        CLIENT_SETUP_ACTIONS.add(action);
    }

    public static synchronized void requireClientSetupRegistrationOpen(String capability) {
        ensureListener();
        if (clientSetupRegistrationClosed) {
            throw new IllegalStateException("Client setup registration window has already closed: " + Objects.requireNonNull(capability, "capability"));
        }
    }

    public static void onLoadComplete(Runnable action) {
        Objects.requireNonNull(action, "action");
        synchronized (KineticModLifecycleRuntime.class) {
            ensureListener();
            if (!loadComplete) {
                LOAD_COMPLETE_ACTIONS.add(action);
                return;
            }
        }
        // A user callback may wait for another thread to register a callback.
        // Never run it while holding the lifecycle registration monitor.
        action.run();
    }

    private static void ensureListener() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (!commonListenerRegistered) {
            modEventBus.addListener(KineticModLifecycleRuntime::onCommonSetupEvent);
            commonListenerRegistered = true;
        }
        if (!clientListenerRegistered) {
            modEventBus.addListener(KineticModLifecycleRuntime::onClientSetupEvent);
            clientListenerRegistered = true;
        }
        if (!completionListenerRegistered) {
            modEventBus.addListener(KineticModLifecycleRuntime::onLoadCompleteEvent);
            completionListenerRegistered = true;
        }
    }

    private static void onCommonSetupEvent(FMLCommonSetupEvent event) {
        List<Runnable> actions;
        synchronized (KineticModLifecycleRuntime.class) {
            commonSetupRegistrationClosed = true;
            actions = List.copyOf(COMMON_SETUP_ACTIONS);
            COMMON_SETUP_ACTIONS.clear();
        }
        RuntimeException failure = null;
        for (Runnable action : actions) {
            try {
                event.enqueueWork(action);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    private static void onClientSetupEvent(FMLClientSetupEvent event) {
        List<Runnable> actions;
        synchronized (KineticModLifecycleRuntime.class) {
            clientSetupRegistrationClosed = true;
            actions = List.copyOf(CLIENT_SETUP_ACTIONS);
            CLIENT_SETUP_ACTIONS.clear();
        }
        RuntimeException failure = null;
        for (Runnable action : actions) {
            try {
                event.enqueueWork(action);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    private static void onLoadCompleteEvent(FMLLoadCompleteEvent event) {
        List<Runnable> actions;
        synchronized (KineticModLifecycleRuntime.class) {
            if (loadCompleteEventSeen) return;
            loadCompleteEventSeen = true;
            actions = List.copyOf(LOAD_COMPLETE_ACTIONS);
            LOAD_COMPLETE_ACTIONS.clear();
        }
        RuntimeException failure = null;
        for (Runnable action : actions) {
            try {
                event.enqueueWork(action);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        // Even if an earlier enqueue fails, schedule the completion barrier after
        // the successfully queued callbacks; otherwise later registrations never run.
        try {
            event.enqueueWork(KineticModLifecycleRuntime::finishLoadComplete);
        } catch (RuntimeException exception) {
            if (failure == null) failure = exception;
            else if (failure != exception) failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }

    private static void finishLoadComplete() {
        List<Runnable> lateActions;
        synchronized (KineticModLifecycleRuntime.class) {
            if (loadComplete) return;
            loadComplete = true;
            lateActions = List.copyOf(LOAD_COMPLETE_ACTIONS);
            LOAD_COMPLETE_ACTIONS.clear();
        }
        RuntimeException failure = null;
        for (Runnable action : lateActions) {
            try {
                action.run();
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }
}
