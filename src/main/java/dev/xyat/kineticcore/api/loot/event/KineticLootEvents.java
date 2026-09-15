package dev.xyat.kineticcore.api.loot.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.runtime.event.KineticLootEventRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Objects;

public final class KineticLootEvents {
    public enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

    public interface TableLoadContext {
        ResourceLocation id();

        LootTable table();

        void table(LootTable table);
    }

    @FunctionalInterface
    public interface TableLoadHandler {
        void handle(TableLoadContext context);
    }

    private KineticLootEvents() {
    }

    public static HookRegistration onTableLoad(TableLoadHandler handler) {
        return onTableLoad(Priority.NORMAL, handler);
    }

    public static HookRegistration onTableLoad(Priority priority, TableLoadHandler handler) {
        return KineticLootEventRuntime.register(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
