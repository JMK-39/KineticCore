package dev.xyat.kineticcore.internal.runtime;

import dev.xyat.kineticcore.api.hook.CommonHooks;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

public final class KineticCommonHookRuntime {
    private static final CopyOnWriteArrayList<CommonHooks.CrawlPoseHandler> CRAWL_POSE =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CommonHooks.MobPersistenceHandler> MOB_PERSISTENCE =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<BooleanSupplier> RECIPE_BOOK_REMOVAL =
            new CopyOnWriteArrayList<>();

    private KineticCommonHookRuntime() {
    }

    public static HookRegistration registerCrawlPose(CommonHooks.CrawlPoseHandler handler) {
        CRAWL_POSE.add(handler);
        return HookRegistration.once(() -> CRAWL_POSE.remove(handler));
    }

    public static HookRegistration registerMobPersistence(CommonHooks.MobPersistenceHandler handler) {
        MOB_PERSISTENCE.add(handler);
        return HookRegistration.once(() -> MOB_PERSISTENCE.remove(handler));
    }

    public static HookRegistration registerRecipeBookRemoval(BooleanSupplier handler) {
        RECIPE_BOOK_REMOVAL.add(handler);
        return HookRegistration.once(() -> RECIPE_BOOK_REMOVAL.remove(handler));
    }

    public static boolean handleCrawlPose(Player player) {
        return KineticCallbackQueries.anyMatch(CRAWL_POSE, handler -> handler.handle(player));
    }

    public static boolean mobPersistenceEnabled() {
        return KineticCallbackQueries.anyMatch(MOB_PERSISTENCE, CommonHooks.MobPersistenceHandler::enabled);
    }

    public static boolean shouldForceDespawn(Mob mob) {
        return KineticCallbackQueries.anyMatch(MOB_PERSISTENCE,
                handler -> handler.enabled() && handler.shouldForceDespawn(mob));
    }

    public static void processPersistence(Mob mob, EquipmentSlot slot) {
        KineticCallbackBatch.runAll(MOB_PERSISTENCE, handler -> {
            if (handler.enabled()) handler.processPersistence(mob, slot);
        });
    }

    public static void dropPickedEquipment(Mob mob) {
        KineticCallbackBatch.runAll(MOB_PERSISTENCE, handler -> {
            if (handler.enabled()) handler.dropPickedEquipment(mob);
        });
    }

    public static boolean recipeBookRemovalEnabled() {
        return KineticCallbackQueries.anyMatch(RECIPE_BOOK_REMOVAL, BooleanSupplier::getAsBoolean);
    }
}
