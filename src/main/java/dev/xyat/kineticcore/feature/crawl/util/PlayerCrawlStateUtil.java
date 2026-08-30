package dev.xyat.kineticcore.feature.crawl.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public final class PlayerCrawlStateUtil {
    public static final String CRAWLING_TAG = "IsCrawling";

    private PlayerCrawlStateUtil() {
    }

    public static boolean hasManualCrawlFlag(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(CRAWLING_TAG);
    }

    public static boolean isCrawling(LivingEntity entity) {
        return hasManualCrawlFlag(entity) && !shouldReleaseToVanilla(entity);
    }

    public static void setCrawling(LivingEntity entity, boolean crawling) {
        if (entity == null) return;

        if (crawling) {
            startManualCrawling(entity);
        } else {
            stopManualCrawling(entity);
        }
    }

    public static void clearCrawling(LivingEntity entity) {
        stopManualCrawling(entity);
    }

    public static boolean shouldReleaseToVanilla(LivingEntity entity) {
        if (entity == null) return true;
        if (entity.isSleeping()) return true;
        if (entity.isFallFlying()) return true;
        if (entity.isInWaterOrBubble()) return true;
        if (entity.isPassenger()) return true;
        return entity.getPose() == Pose.SPIN_ATTACK;
    }

    public static void startManualCrawling(LivingEntity entity) {
        if (entity == null) return;

        if (shouldReleaseToVanilla(entity)) {
            releaseToVanilla(entity);
            return;
        }

        entity.getPersistentData().putBoolean(CRAWLING_TAG, true);
        applyManualCrawlPose(entity);
    }

    public static void stopManualCrawling(LivingEntity entity) {
        if (entity == null) return;

        entity.getPersistentData().putBoolean(CRAWLING_TAG, false);

        if (!shouldReleaseToVanilla(entity) && entity.getPose() == Pose.SWIMMING) {
            entity.setPose(entity.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);
        }

        entity.refreshDimensions();
    }

    public static void releaseToVanilla(LivingEntity entity) {
        if (entity == null) return;

        entity.getPersistentData().putBoolean(CRAWLING_TAG, false);
        entity.refreshDimensions();
    }

    public static void applyManualCrawlPose(LivingEntity entity) {
        if (entity == null) return;

        if (shouldReleaseToVanilla(entity)) {
            releaseToVanilla(entity);
            return;
        }

        entity.getPersistentData().putBoolean(CRAWLING_TAG, true);

        if (entity.getPose() != Pose.SWIMMING) {
            entity.setPose(Pose.SWIMMING);
        }

        entity.refreshDimensions();
    }
}