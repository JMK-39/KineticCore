package dev.xyat.kineticcore.feature.crawl.util;

import dev.xyat.kineticcore.api.player.KineticCrawling;
import net.minecraft.world.entity.LivingEntity;

public final class PlayerCrawlStateUtil {
    public static final String CRAWLING_TAG = "IsCrawling";

    private PlayerCrawlStateUtil() {
    }

    public static boolean hasManualCrawlFlag(LivingEntity entity) {
        return KineticCrawling.hasManualFlag(entity);
    }

    public static boolean isCrawling(LivingEntity entity) {
        return KineticCrawling.isCrawling(entity);
    }

    public static void setCrawling(LivingEntity entity, boolean crawling) {
        KineticCrawling.set(entity, crawling);
    }

    public static void clearCrawling(LivingEntity entity) {
        KineticCrawling.clear(entity);
    }

    public static boolean shouldReleaseToVanilla(LivingEntity entity) {
        return KineticCrawling.shouldReleaseToVanilla(entity);
    }

    public static void startManualCrawling(LivingEntity entity) {
        KineticCrawling.start(entity);
    }

    public static void stopManualCrawling(LivingEntity entity) {
        KineticCrawling.stop(entity);
    }

    public static void releaseToVanilla(LivingEntity entity) {
        KineticCrawling.releaseToVanilla(entity);
    }

    public static void applyManualCrawlPose(LivingEntity entity) {
        KineticCrawling.applyPose(entity);
    }
}
