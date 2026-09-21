package dev.xyat.kineticcore.api.loot.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.runtime.event.KineticLootEventRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Objects;

/** Public Kinetic API facade for loot events. */
public final class KineticLootEvents {
    /** Context exposed to table load callbacks. */
    public interface TableLoadContext {
        ResourceLocation id();

        LootTable table();

        void table(LootTable table);
    }

    /** Callback contract for table load notifications. */
    @FunctionalInterface
    public interface TableLoadHandler {
        void handle(TableLoadContext context);
    }

    private KineticLootEvents() {
    }

    /**
     * 注册战利品表加载监听器；同优先级的回调逐项执行，异常在本轮分发结束后统一抛出。
     */
    public static KineticEventSubscription onTableLoad(KineticEventPriority priority, TableLoadHandler handler) {
        return KineticLootEventRuntime.register(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
