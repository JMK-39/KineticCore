package dev.xyat.kineticcore.api.hook;

import dev.xyat.kineticcore.internal.runtime.KineticCommonHookRuntime;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Public API type for common hooks. */
public final class CommonHooks {
    private CommonHooks() {
    }

    /** Registers a listener that may take ownership of the current player pose update. */
    public static HookRegistration onPlayerPoseUpdate(PlayerPoseUpdateHandler handler) {
        return KineticCommonHookRuntime.registerPlayerPoseUpdate(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /** Compatibility alias for existing crawl integrations. */
    public static HookRegistration onCrawlPose(CrawlPoseHandler handler) {
        return onPlayerPoseUpdate(Objects.requireNonNull(handler, "handler"));
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
     * Registers a listener for recipe book removal.
     */
    public static HookRegistration onRecipeBookRemoval(BooleanSupplier handler) {
        return KineticCommonHookRuntime.registerRecipeBookRemoval(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /** Callback contract for player pose update ownership. */
    @FunctionalInterface
    public interface PlayerPoseUpdateHandler {
        boolean handle(Player player);
    }

    /** Compatibility callback type for existing crawl integrations. */
    @FunctionalInterface
    public interface CrawlPoseHandler extends PlayerPoseUpdateHandler {
    }

    /** Callback contract for mob persistence notifications. */
    public interface MobPersistenceHandler {
        boolean enabled();

        boolean shouldForceDespawn(Mob mob);

        void processPersistence(Mob mob, EquipmentSlot slot);

        void dropPickedEquipment(Mob mob);
    }
}
