package dev.xyat.kineticcore.api.loot.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.runtime.event.KineticLootEventRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Objects;

/** Public Kinetic API facade for loot events. */
public final class KineticLootEvents {
    /** A loot table was loaded from data packs. */
    public interface TableLoadContext {
        /** Returns the loot table id, for example {@code minecraft:chests/simple_dungeon}. */
        ResourceLocation id();

        /** Returns the table that will be used, including replacements by earlier handlers. */
        LootTable table();

        /** Replaces the table, for example with a modified copy. */
        void table(LootTable table);
    }

    /** Callback contract for table load notifications. */
    @FunctionalInterface
    public interface TableLoadHandler {
        /** Called once per table on every data-pack load. */
        void handle(TableLoadContext context);
    }

    private KineticLootEvents() {
    }

    /**
     * 注册战利品表加载监听器；同优先级的回调逐项执行，异常在本轮分发结束后统一抛出。
     *
     * <p>Subscribes to loot table loading ({@code LootTableLoadEvent}). Callbacks of one priority run one by one;
     * failures are rethrown together after dispatch.
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onTableLoad(KineticEventPriority priority, TableLoadHandler handler) {
        return KineticLootEventRuntime.register(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
