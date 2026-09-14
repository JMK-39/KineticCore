package dev.xyat.kineticcore.api.hook;

import dev.xyat.kineticcore.internal.runtime.KineticCommonHookRuntime;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class CommonHooks {
    private CommonHooks() {
    }

    public static HookRegistration onCrawlPose(CrawlPoseHandler handler) {
        return KineticCommonHookRuntime.registerCrawlPose(
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onMobPersistence(MobPersistenceHandler handler) {
        return KineticCommonHookRuntime.registerMobPersistence(
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onRecipeBookRemoval(BooleanSupplier handler) {
        return KineticCommonHookRuntime.registerRecipeBookRemoval(
                Objects.requireNonNull(handler, "handler")
        );
    }

    @FunctionalInterface
    public interface CrawlPoseHandler {
        boolean handle(Player player);
    }

    public interface MobPersistenceHandler {
        boolean enabled();

        boolean shouldForceDespawn(Mob mob);

        void processPersistence(Mob mob, EquipmentSlot slot);

        void dropPickedEquipment(Mob mob);
    }
}
