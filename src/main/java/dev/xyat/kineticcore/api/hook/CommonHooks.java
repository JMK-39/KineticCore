package dev.xyat.kineticcore.api.hook;

import dev.xyat.kineticcore.internal.runtime.KineticCommonHookRuntime;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Hooks into shared client and server behavior that Forge has no event for. Handlers run in registration order;
 * close the returned {@link HookRegistration} to unregister.
 */
public final class CommonHooks {
    private CommonHooks() {
    }

    /** Registers a listener that may take ownership of the current player pose update. */
    public static HookRegistration onPlayerPoseUpdate(PlayerPoseUpdateHandler handler) {
        return KineticCommonHookRuntime.registerPlayerPoseUpdate(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * 注册生物持久化处理器。processPersistence 与 dropPickedEquipment 按顺序调用全部
     * 处理器；单项 RuntimeException 不阻断后续项，最终汇总抛出异常。
     * enabled 与 shouldForceDespawn 等布尔查询仍保持原有短路语义。
     */
    public static HookRegistration onMobPersistence(MobPersistenceHandler handler) {
        return KineticCommonHookRuntime.registerMobPersistence(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a switch that removes the recipe book: its button, unlock syncing and saved data. The recipe book
     * is removed while any registered supplier returns {@code true}.
     *
     * @param handler returns whether the recipe book should currently be removed
     * @return the registration handle
     * @throws NullPointerException if {@code handler} is {@code null}
     */
    public static HookRegistration onRecipeBookRemoval(BooleanSupplier handler) {
        return KineticCommonHookRuntime.registerRecipeBookRemoval(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /** Callback contract for player pose update ownership. */
    @FunctionalInterface
    public interface PlayerPoseUpdateHandler {
        /**
         * Returns {@code true} when this handler has set the player's pose for this tick, which skips vanilla's
         * pose update.
         */
        boolean handle(Player player);
    }

    /** Overrides when mobs despawn and what happens to equipment they picked up. */
    public interface MobPersistenceHandler {
        /** Returns whether this handler is active; disabled handlers receive no other calls. */
        boolean enabled();

        /**
         * Returns whether the mob may despawn even though vanilla would keep it, for example because it picked up
         * an item. The answer is cached until its equipment changes.
         */
        boolean shouldForceDespawn(Mob mob);

        /** Called after the mob equipped a picked-up item in {@code slot}. */
        void processPersistence(Mob mob, EquipmentSlot slot);

        /** Called just before the mob despawns or is removed, so picked-up items can be dropped instead of lost. */
        void dropPickedEquipment(Mob mob);
    }
}
