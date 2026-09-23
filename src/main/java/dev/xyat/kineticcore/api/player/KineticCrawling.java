package dev.xyat.kineticcore.api.player;

import dev.xyat.kineticcore.internal.player.KineticCrawlingRuntime;
import net.minecraft.world.entity.LivingEntity;

/** Public common API for Kinetic manual crawling state and pose ownership. */
public final class KineticCrawling {
    private KineticCrawling() {
    }

    /** Returns whether the entity has an explicit manual crawling flag. */
    public static boolean hasManualFlag(LivingEntity entity) {
        return KineticCrawlingRuntime.hasManualCrawlFlag(entity);
    }

    /** Returns whether the entity is currently treated as crawling. */
    public static boolean isCrawling(LivingEntity entity) {
        return KineticCrawlingRuntime.isCrawling(entity);
    }

    /** Sets the explicit crawling state for the entity. */
    public static void set(LivingEntity entity, boolean crawling) {
        KineticCrawlingRuntime.setCrawling(entity, crawling);
    }

    /** Clears the explicit crawling state for the entity. */
    public static void clear(LivingEntity entity) {
        KineticCrawlingRuntime.clearCrawling(entity);
    }

    /** Returns whether crawling control should be released back to vanilla behavior. */
    public static boolean shouldReleaseToVanilla(LivingEntity entity) {
        return KineticCrawlingRuntime.shouldReleaseToVanilla(entity);
    }

    /** Starts API-controlled crawling for the entity. */
    public static void start(LivingEntity entity) {
        KineticCrawlingRuntime.startManualCrawling(entity);
    }

    /** Stops API-controlled crawling for the entity. */
    public static void stop(LivingEntity entity) {
        KineticCrawlingRuntime.stopManualCrawling(entity);
    }

    /** Releases API-controlled crawling back to vanilla behavior. */
    public static void releaseToVanilla(LivingEntity entity) {
        KineticCrawlingRuntime.releaseToVanilla(entity);
    }

    /** Applies the crawling pose required by the current API-controlled state. */
    public static void applyPose(LivingEntity entity) {
        KineticCrawlingRuntime.applyManualCrawlPose(entity);
    }
}
