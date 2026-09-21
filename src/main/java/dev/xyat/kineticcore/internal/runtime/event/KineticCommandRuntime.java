package dev.xyat.kineticcore.internal.runtime.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Objects;
import java.util.function.Consumer;

public final class KineticCommandRuntime {
    private static Consumer<CommandDispatcher<CommandSourceStack>> registrar;
    private static boolean initialized;

    private KineticCommandRuntime() {
    }

    public static synchronized void initialize(Consumer<CommandDispatcher<CommandSourceStack>> rootRegistrar) {
        Objects.requireNonNull(rootRegistrar, "rootRegistrar");
        if (initialized) return;
        MinecraftForge.EVENT_BUS.addListener(KineticCommandRuntime::onRegisterCommands);
        registrar = rootRegistrar;
        initialized = true;
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        Consumer<CommandDispatcher<CommandSourceStack>> current = registrar;
        if (current != null) {
            current.accept(event.getDispatcher());
        }
    }
}
