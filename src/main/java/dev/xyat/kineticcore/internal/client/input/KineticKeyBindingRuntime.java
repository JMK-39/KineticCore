package dev.xyat.kineticcore.internal.client.input;

import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.internal.runtime.KineticCallbackQueries;
import com.mojang.blaze3d.platform.InputConstants;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

public final class KineticKeyBindingRuntime {
    private static final AtomicLong NEXT_ID = new AtomicLong(1L);
    private static final Map<Long, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<String, Long> IDS_BY_TRANSLATION_KEY = new LinkedHashMap<>();

    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;
    private static boolean registrationFinished;

    private KineticKeyBindingRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        

        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;
        attempt.install(slot++, () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticKeyBindingRuntime::onRegisterKeyMappings));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticKeyBindingRuntime::onClientTick));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticKeyBindingRuntime::onScreenKeyPressed));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(KineticKeyBindingRuntime::onScreenMousePressed));
        attempt.finish();
        initialized = true;
    }

    public static synchronized long register(
            String translationKey,
            String categoryTranslationKey,
            KineticKeyBindings.Context context,
            KineticKeyBindings.Modifier modifier,
            boolean mouseInput,
            int keyCode,
            BooleanSupplier registerWhen,
            BooleanSupplier enabledWhen,
            KineticKeyBindings.PressHandler onPressed,
            boolean exactModifiers
    ) {
        Objects.requireNonNull(translationKey, "translationKey");
        Objects.requireNonNull(categoryTranslationKey, "categoryTranslationKey");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(modifier, "modifier");
        Objects.requireNonNull(registerWhen, "registerWhen");
        Objects.requireNonNull(enabledWhen, "enabledWhen");
        Objects.requireNonNull(onPressed, "onPressed");
        if (registrationFinished) {
            throw new IllegalStateException("Key bindings must be registered before RegisterKeyMappingsEvent completes: " + translationKey);
        }
        if (IDS_BY_TRANSLATION_KEY.containsKey(translationKey)) {
            throw new IllegalArgumentException("Duplicate key binding translation key: " + translationKey);
        }

        Definition definition = new Definition(
                translationKey,
                categoryTranslationKey,
                context,
                modifier,
                mouseInput,
                keyCode,
                registerWhen,
                enabledWhen,
                onPressed,
                exactModifiers
        );
        long id = NEXT_ID.getAndIncrement();
        ENTRIES.put(id, new Entry(definition));
        IDS_BY_TRANSLATION_KEY.put(translationKey, id);
        return id;
    }

    public static synchronized boolean isRegistered(long id) {
        Entry entry = ENTRIES.get(id);
        return entry != null && entry.mapping != null;
    }

    public static synchronized boolean isDown(long id) {
        Entry entry = ENTRIES.get(id);
        if (entry == null || entry.mapping == null) return false;
        if (disabledSafely(entry.definition)) return false;
        return entry.mapping.isDown();
    }

    public static synchronized Component translatedKeyMessage(long id) {
        Entry entry = ENTRIES.get(id);
        if (entry == null) return Component.empty();
        if (entry.mapping != null) return entry.mapping.getTranslatedKeyMessage();
        return inputType(entry.definition.mouseInput())
                .getOrCreate(entry.definition.keyCode())
                .getDisplayName();
    }

    private static synchronized void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        if (registrationFinished) return;
        RuntimeException failure = null;
        for (Entry entry : ENTRIES.values()) {
            // Forge may retry registration after an earlier entry threw. Preserve successful
            // mappings instead of submitting duplicate key bindings on that second pass.
            if (entry.mapping != null) continue;
            try {
                if (!entry.definition.registerWhen().getAsBoolean()) continue;
                KeyMapping mapping = createMapping(entry.definition);
                event.register(mapping);
                entry.mapping = mapping;
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
        registrationFinished = true;
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().screen != null) return;

        for (Entry entry : snapshot()) {
            KeyMapping mapping = entry.mapping;
            if (mapping == null) continue;
            if (entry.definition.context() == KineticKeyBindings.Context.GUI) continue;

            while (mapping.consumeClick()) {
                if (disabledSafely(entry.definition)) continue;
                if (modifiersMismatch(entry.definition)) continue;
                pressSafely(entry.definition);
            }
        }
    }

    private static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        InputConstants.Key input = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        handleScreenPress(input, event::setCanceled);
    }

    private static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
        handleScreenPress(input, event::setCanceled);
    }

    private static void handleScreenPress(InputConstants.Key input, CancelAction cancelAction) {
        for (Entry entry : snapshot()) {
            KeyMapping mapping = entry.mapping;
            if (mapping == null) continue;
            if (entry.definition.context() == KineticKeyBindings.Context.IN_GAME) continue;
            if (!mapping.isActiveAndMatches(input)) continue;

            drainClicks(mapping);
            if (disabledSafely(entry.definition)) continue;
            if (modifiersMismatch(entry.definition)) continue;

            if (pressSafely(entry.definition)) {
                cancelAction.cancel(true);
                return;
            }
        }
    }

    private static KeyMapping createMapping(Definition definition) {
        return new KeyMapping(
                definition.translationKey(),
                conflictContext(definition.context()),
                keyModifier(definition.modifier()),
                inputType(definition.mouseInput()),
                definition.keyCode(),
                definition.categoryTranslationKey()
        );
    }

    private static KeyConflictContext conflictContext(KineticKeyBindings.Context context) {
        return switch (context) {
            case IN_GAME -> KeyConflictContext.IN_GAME;
            case GUI -> KeyConflictContext.GUI;
            case UNIVERSAL -> KeyConflictContext.UNIVERSAL;
        };
    }

    private static KeyModifier keyModifier(KineticKeyBindings.Modifier modifier) {
        return switch (modifier) {
            case NONE -> KeyModifier.NONE;
            case SHIFT -> KeyModifier.SHIFT;
            case CONTROL -> KeyModifier.CONTROL;
            case ALT -> KeyModifier.ALT;
        };
    }

    private static InputConstants.Type inputType(boolean mouseInput) {
        return mouseInput ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
    }

    private static boolean disabledSafely(Definition definition) {
        try {
            return !definition.enabledWhen().getAsBoolean();
        } catch (RuntimeException failure) {
            KineticCallbackQueries.reportFailure(failure);
            return true;
        }
    }

    private static boolean pressSafely(Definition definition) {
        try {
            return definition.onPressed().onPressed();
        } catch (RuntimeException failure) {
            KineticCallbackQueries.reportFailure(failure);
            return false;
        }
    }

    private static boolean modifiersMismatch(Definition definition) {
        if (!definition.exactModifiers()) return false;

        boolean shift = Screen.hasShiftDown();
        boolean control = Screen.hasControlDown();
        boolean alt = Screen.hasAltDown();

        return switch (definition.modifier()) {
            case NONE -> shift || control || alt;
            case SHIFT -> !shift || control || alt;
            case CONTROL -> shift || !control || alt;
            case ALT -> shift || control || !alt;
        };
    }

    private static synchronized Entry[] snapshot() {
        return ENTRIES.values().toArray(Entry[]::new);
    }

    private static void drainClicks(KeyMapping mapping) {
        while (mapping.consumeClick()) {
        }
    }

    private interface CancelAction {
        void cancel(boolean canceled);
    }

    private record Definition(
            String translationKey,
            String categoryTranslationKey,
            KineticKeyBindings.Context context,
            KineticKeyBindings.Modifier modifier,
            boolean mouseInput,
            int keyCode,
            BooleanSupplier registerWhen,
            BooleanSupplier enabledWhen,
            KineticKeyBindings.PressHandler onPressed,
            boolean exactModifiers
    ) {
    }

    private static final class Entry {
        private final Definition definition;
        private KeyMapping mapping;

        private Entry(Definition definition) {
            this.definition = definition;
        }
    }
}
