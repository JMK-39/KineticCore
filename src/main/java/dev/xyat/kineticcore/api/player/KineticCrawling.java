package dev.xyat.kineticcore.api.player;

import dev.xyat.kineticcore.internal.player.KineticCrawlingRuntime;
import net.minecraft.world.entity.LivingEntity;

/** Public common API for Kinetic manual crawling state and pose ownership. */
public final class KineticCrawling {
    private KineticCrawling() {
    }

    public static boolean hasManualFlag(LivingEntity entity) {
        return KineticCrawlingRuntime.hasManualCrawlFlag(entity);
    }

    public static boolean isCrawling(LivingEntity entity) {
        return KineticCrawlingRuntime.isCrawling(entity);
    }

    public static void set(LivingEntity entity, boolean crawling) {
        KineticCrawlingRuntime.setCrawling(entity, crawling);
    }

    public static void clear(LivingEntity entity) {
        KineticCrawlingRuntime.clearCrawling(entity);
    }

    public static boolean shouldReleaseToVanilla(LivingEntity entity) {
        return KineticCrawlingRuntime.shouldReleaseToVanilla(entity);
    }

    public static void start(LivingEntity entity) {
        KineticCrawlingRuntime.startManualCrawling(entity);
    }

    public static void stop(LivingEntity entity) {
        KineticCrawlingRuntime.stopManualCrawling(entity);
    }

    public static void releaseToVanilla(LivingEntity entity) {
        KineticCrawlingRuntime.releaseToVanilla(entity);
    }

    public static void applyPose(LivingEntity entity) {
        KineticCrawlingRuntime.applyManualCrawlPose(entity);
    }
}
