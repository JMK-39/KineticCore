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
        return () -> CRAWL_POSE.remove(handler);
    }

    public static HookRegistration registerMobPersistence(CommonHooks.MobPersistenceHandler handler) {
        MOB_PERSISTENCE.add(handler);
        return () -> MOB_PERSISTENCE.remove(handler);
    }

    public static HookRegistration registerRecipeBookRemoval(BooleanSupplier handler) {
        RECIPE_BOOK_REMOVAL.add(handler);
        return () -> RECIPE_BOOK_REMOVAL.remove(handler);
    }

    public static boolean handleCrawlPose(Player player) {
        for (CommonHooks.CrawlPoseHandler handler : CRAWL_POSE) {
            if (handler.handle(player)) return true;
        }
        return false;
    }

    public static boolean mobPersistenceEnabled() {
        for (CommonHooks.MobPersistenceHandler handler : MOB_PERSISTENCE) {
            if (handler.enabled()) return true;
        }
        return false;
    }

    public static boolean shouldForceDespawn(Mob mob) {
        for (CommonHooks.MobPersistenceHandler handler : MOB_PERSISTENCE) {
            if (handler.enabled() && handler.shouldForceDespawn(mob)) return true;
        }
        return false;
    }

    public static void processPersistence(Mob mob, EquipmentSlot slot) {
        for (CommonHooks.MobPersistenceHandler handler : MOB_PERSISTENCE) {
            if (handler.enabled()) handler.processPersistence(mob, slot);
        }
    }

    public static void dropPickedEquipment(Mob mob) {
        for (CommonHooks.MobPersistenceHandler handler : MOB_PERSISTENCE) {
            if (handler.enabled()) handler.dropPickedEquipment(mob);
        }
    }

    public static boolean recipeBookRemovalEnabled() {
        for (BooleanSupplier handler : RECIPE_BOOK_REMOVAL) {
            if (handler.getAsBoolean()) return true;
        }
        return false;
    }
}
