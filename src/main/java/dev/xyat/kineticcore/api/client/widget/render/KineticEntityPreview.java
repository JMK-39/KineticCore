package dev.xyat.kineticcore.api.client.widget.render;

import dev.xyat.kineticcore.api.client.widget.KineticWidgets.FactoryAccess;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.render.KineticEntityPreviewRuntime;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 控件实现分组；附属统一从 KineticWidgets 工厂进入。 */
public final class KineticEntityPreview {
    private KineticEntityPreview() {}

    /** Public API type for entity preview renderer. */
    public static class EntityPreviewRenderer {
        /**
         * Exposes the default rotation API value.
         */
        public static final float DEFAULT_ROTATION = 20f;
        /**
         * Exposes the default base rotation speed API value.
         */
        public static final float DEFAULT_BASE_ROTATION_SPEED = 90f;
        /**
         * Exposes the default zoom percent API value.
         */
        public static final int DEFAULT_ZOOM_PERCENT = 100;
        /**
         * Exposes the min zoom percent API value.
         */
        public static final int MIN_ZOOM_PERCENT = 10;
        /**
         * Exposes the max zoom percent API value.
         */
        public static final int MAX_ZOOM_PERCENT = 500;
        /**
         * Exposes the zoom step percent API value.
         */
        public static final int ZOOM_STEP_PERCENT = 10;
        /**
         * Exposes the default fill ratio API value.
         */
        public static final float DEFAULT_FILL_RATIO = 0.56f;
        /**
         * Exposes the default max auto scale factor API value.
         */
        public static final float DEFAULT_MAX_AUTO_SCALE_FACTOR = 0.55f;
        /**
         * Exposes the default frame limit nanos API value.
         */
        public static final long DEFAULT_FRAME_LIMIT_NANOS = 100_000_000L;
        /**
         * Exposes the default cache size API value.
         */
        public static final int DEFAULT_CACHE_SIZE = 48;
        /** Retry delay after a transient preview construction/render failure. */
        public static final long DEFAULT_FAILURE_RETRY_NANOS = 2_000_000_000L;
        /** Minimum visual width used for auto-fit so small collision boxes cannot over-zoom large custom models. */
        public static final float DEFAULT_VISUAL_WIDTH_FLOOR = 0.90f;
        /** Minimum visual height used for auto-fit so small collision boxes cannot over-zoom large custom models. */
        public static final float DEFAULT_VISUAL_HEIGHT_FLOOR = 1.35f;

        private static final int CHECKER_SIZE = 6;
        private static final int CHECKER_LIGHT = 0xFFF0F0F0;
        private static final int CHECKER_DARK = 0xFFD2D2D2;

        private final int maxCacheSize;
        private final float fillRatio;
        private final float maxAutoScaleFactor;
        private final Map<String, Entity> renderCache;
        private final Map<String, Long> renderFailedUntil = new LinkedHashMap<>();
        private Object renderLevelToken;
        private final Map<String, EntityPreviewState> states = new LinkedHashMap<>();
        private int rotationSpeedPercent = 100;
        private boolean clockwise = true;

        /** Creates a renderer through the Kinetic widget factory token. */
        public EntityPreviewRenderer(FactoryAccess access, int maxCacheSize, float fillRatio, float maxAutoScaleFactor) {
            Objects.requireNonNull(access, "factory access");
            this.maxCacheSize = Math.max(1, maxCacheSize);
            this.fillRatio = Math.max(0.05f, fillRatio);
            this.maxAutoScaleFactor = Math.max(0.05f, maxAutoScaleFactor);
            this.renderCache = new LinkedHashMap<>(this.maxCacheSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Entity> eldest) {
                    return size() > EntityPreviewRenderer.this.maxCacheSize;
                }
            };
        }

        /**
         * Updates rotation speed percent.
         */
        public void setRotationSpeedPercent(int rotationSpeedPercent) {
            this.rotationSpeedPercent = Math.max(0, Math.min(500, rotationSpeedPercent));
        }

        /**
         * Updates clockwise.
         */
        public void setClockwise(boolean clockwise) {
            this.clockwise = clockwise;
        }

        /**
         * Returns zoom percent.
         */
        public int getZoomPercent(String stateKey) {
            return getState(stateKey).getZoomPercent();
        }

        /**
         * Updates zoom percent.
         */
        public void setZoomPercent(String stateKey, int zoomPercent) {
            getState(stateKey).setZoomPercent(Math.max(MIN_ZOOM_PERCENT, Math.min(MAX_ZOOM_PERCENT, zoomPercent)));
        }

        /**
         * Performs the adjust zoom API operation.
         */
        public void adjustZoom(String stateKey, double delta) {
            if (stateKey == null || delta == 0D) return;
            int current = getZoomPercent(stateKey);
            setZoomPercent(stateKey, current + (delta > 0D ? ZOOM_STEP_PERCENT : -ZOOM_STEP_PERCENT));
        }

        /** Handles Ctrl + mouse wheel over one preview without allowing the grid to scroll. */
        public boolean handleControlWheel(String stateKey, boolean hovered, double delta) {
            if (!hovered || stateKey == null || delta == 0D || !KineticClientRuntime.controlModifierDown()) {
                return false;
            }
            adjustZoom(stateKey, delta);
            return true;
        }

        /**
         * Performs the render API operation.
         */
        public boolean render(
                GuiGraphics graphics,
                String entityId,
                String stateKey,
                int boxX,
                int boxY,
                int boxW,
                int boxH,
                float guiScale,
                int offsetX,
                int offsetY,
                boolean hovered
        ) {
            ResourceLocation id = KineticResourceIds.tryParse(entityId);
            if (id == null) return false;
            EntityType<?> type = KineticRegistries.entityTypes().get(id);
            return render(graphics, type, id, stateKey, boxX, boxY, boxW, boxH, guiScale, offsetX, offsetY, hovered, false);
        }

        /**
         * Renders an entity preview inside a {@code KineticScreen} logical canvas.
         * Canvas scaling and screen offsets remain owned by the screen instead of being supplied by addon code.
         */
        public boolean renderCanvas(
                GuiGraphics graphics,
                String entityId,
                String stateKey,
                int boxX,
                int boxY,
                int boxW,
                int boxH,
                boolean hovered
        ) {
            ResourceLocation id = KineticResourceIds.tryParse(entityId);
            if (id == null) return false;
            EntityType<?> type = KineticRegistries.entityTypes().get(id);
            return render(graphics, type, id, stateKey, boxX, boxY, boxW, boxH, 1.0F, 0, 0, hovered, true);
        }

        /**
         * Renders an entity instance that has already been prepared by the caller.
         * This avoids constructing the same modded entity a second time when the caller already owns a valid preview instance.
         */
        public boolean renderCanvas(
                GuiGraphics graphics,
                Entity entity,
                String stateKey,
                int boxX,
                int boxY,
                int boxW,
                int boxH,
                boolean hovered
        ) {
            if (entity == null || stateKey == null) return false;
            return renderPrepared(graphics, entity, stateKey, boxX, boxY, boxW, boxH, 1.0F, 0, 0, hovered, true);
        }

        private boolean render(
                GuiGraphics graphics,
                EntityType<?> type,
                ResourceLocation id,
                String stateKey,
                int boxX,
                int boxY,
                int boxW,
                int boxH,
                float guiScale,
                int offsetX,
                int offsetY,
                boolean hovered,
                boolean canvasCoordinates
        ) {
            if (type == null || id == null || stateKey == null) return false;
            Entity entity = getOrCreateEntity(type, id);
            if (entity == null) return false;
            return renderPrepared(graphics, entity, stateKey, boxX, boxY, boxW, boxH, guiScale, offsetX, offsetY, hovered, canvasCoordinates);
        }

        private boolean renderPrepared(
                GuiGraphics graphics,
                Entity entity,
                String stateKey,
                int boxX,
                int boxY,
                int boxW,
                int boxH,
                float guiScale,
                int offsetX,
                int offsetY,
                boolean hovered,
                boolean canvasCoordinates
        ) {
            if (entity == null || stateKey == null) return false;

            float entityWidth = Math.max(entity.getBbWidth(), 0.1f);
            float entityHeight = Math.max(entity.getBbHeight(), 0.1f);

            // Collision boxes are not reliable visual bounds for modded entities. Some renderers draw models
            // several times larger than their hit box; fitting from the raw hit box then scales the model up
            // until most of it is clipped by the preview cell. Use conservative visual floors for small hit
            // boxes while leaving normal/large entities governed by their real dimensions.
            float fitWidth = Math.max(entityWidth, DEFAULT_VISUAL_WIDTH_FLOOR);
            float fitHeight = Math.max(entityHeight, DEFAULT_VISUAL_HEIGHT_FLOOR);
            float safeWidth = Math.max(1f, boxW * fillRatio);
            float safeHeight = Math.max(1f, boxH * fillRatio);
            float autoScale = Math.min(safeWidth / fitWidth, safeHeight / fitHeight);
            float boxScaleCap = Math.min(boxW, boxH) * maxAutoScaleFactor;
            autoScale = Math.min(autoScale, boxScaleCap);
            EntityPreviewState state = getState(stateKey);
            float scale = autoScale * state.getZoomPercent() / 100f;
            int renderX = Math.round(boxX + boxW / 2f);
            int renderY = Math.round(boxY + boxH / 2f + fitHeight * scale / 2f);
            float angle = state.updateRotation(hovered, DEFAULT_BASE_ROTATION_SPEED, rotationSpeedPercent, clockwise, DEFAULT_FRAME_LIMIT_NANOS);

            int scissorX1 = canvasCoordinates
                    ? (int) Math.ceil(boxX + 0.5f)
                    : offsetX + (int) Math.ceil((boxX + 0.5f) * guiScale);
            int scissorY1 = canvasCoordinates
                    ? (int) Math.ceil(boxY + 0.5f)
                    : offsetY + (int) Math.ceil((boxY + 0.5f) * guiScale);
            int scissorX2 = canvasCoordinates
                    ? (int) Math.floor(boxX + boxW - 0.5f)
                    : offsetX + (int) Math.floor((boxX + boxW - 0.5f) * guiScale);
            int scissorY2 = canvasCoordinates
                    ? (int) Math.floor(boxY + boxH - 0.5f)
                    : offsetY + (int) Math.floor((boxY + boxH - 0.5f) * guiScale);
            if (scissorX2 <= scissorX1 || scissorY2 <= scissorY1) return false;
            KineticRenderRuntime.enableScissor(graphics, scissorX1, scissorY1, scissorX2, scissorY2);
            graphics.pose().pushPose();

            float oldYRot = entity.getYRot();
            float oldYRotO = entity.yRotO;
            float oldXRot = entity.getXRot();
            float oldXRotO = entity.xRotO;
            float oldBody = 0f;
            float oldBodyO = 0f;
            float oldHead = 0f;
            float oldHeadO = 0f;

            try {
                graphics.pose().translate(renderX, renderY, 50D);
                graphics.pose().scale(scale, scale, -scale);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f));
                graphics.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(-10f));
                graphics.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f - angle));

                if (entity instanceof LivingEntity living) {
                    oldBody = living.yBodyRot;
                    oldBodyO = living.yBodyRotO;
                    oldHead = living.yHeadRot;
                    oldHeadO = living.yHeadRotO;
                    living.yBodyRot = 0f;
                    living.yBodyRotO = 0f;
                    living.yHeadRot = 0f;
                    living.yHeadRotO = 0f;
                }

                entity.setYRot(0f);
                entity.yRotO = 0f;
                entity.setXRot(0f);
                entity.xRotO = 0f;

                KineticEntityPreviewRuntime.render(entity, graphics);
                return true;
            } catch (Throwable ignored) {
                ResourceLocation failedId = KineticRegistries.entityTypes().id(entity.getType());
                if (failedId != null) {
                    renderCache.remove(failedId.toString());
                    markRenderFailure(failedId.toString());
                }
                return false;
            } finally {
                KineticEntityPreviewRuntime.restore();
                entity.setYRot(oldYRot);
                entity.yRotO = oldYRotO;
                entity.setXRot(oldXRot);
                entity.xRotO = oldXRotO;
                if (entity instanceof LivingEntity living) {
                    living.yBodyRot = oldBody;
                    living.yBodyRotO = oldBodyO;
                    living.yHeadRot = oldHead;
                    living.yHeadRotO = oldHeadO;
                }
                graphics.pose().popPose();
                KineticRenderRuntime.disableScissor(graphics);
            }
        }

        /**
         * Clears the current API state.
         */
        public void clear() {
            renderCache.clear();
            renderFailedUntil.clear();
            states.clear();
            renderLevelToken = null;
        }

        /**
         * Draws checkerboard.
         */
        public static void drawCheckerboard(GuiGraphics graphics, int x, int y, int width, int height) {
            graphics.fill(x, y, x + width, y + height, CHECKER_LIGHT);
            int rows = (height + CHECKER_SIZE - 1) / CHECKER_SIZE;
            int cols = (width + CHECKER_SIZE - 1) / CHECKER_SIZE;
            for (int row = 0; row < rows; row++) {
                for (int col = row & 1; col < cols; col += 2) {
                    int x1 = x + col * CHECKER_SIZE;
                    int y1 = y + row * CHECKER_SIZE;
                    graphics.fill(x1, y1, Math.min(x1 + CHECKER_SIZE, x + width), Math.min(y1 + CHECKER_SIZE, y + height), CHECKER_DARK);
                }
            }
        }

        private EntityPreviewState getState(String stateKey) {
            return states.computeIfAbsent(stateKey, ignored -> new EntityPreviewState(DEFAULT_ROTATION, DEFAULT_ZOOM_PERCENT));
        }


        private void synchronizeLevelContext(Object currentLevel) {
            if (renderLevelToken == currentLevel) return;
            renderCache.clear();
            renderFailedUntil.clear();
            renderLevelToken = currentLevel;
        }

        private Entity getOrCreateEntity(EntityType<?> type, ResourceLocation id) {
            String key = id.toString();
            var level = KineticClientRuntime.currentLevel();
            synchronizeLevelContext(level);
            if (level == null) return null;
            if (isRenderFailureCoolingDown(key)) return null;
            Entity cached = renderCache.get(key);
            if (cached != null) return cached;
            try {
                Entity entity = type.create(level);
                if (entity == null) {
                    markRenderFailure(key);
                    return null;
                }
                renderCache.put(key, entity);
                renderFailedUntil.remove(key);
                return entity;
            } catch (Throwable ignored) {
                markRenderFailure(key);
                return null;
            }
        }

        private boolean isRenderFailureCoolingDown(String key) {
            Long retryAt = renderFailedUntil.get(key);
            if (retryAt == null) return false;
            if (System.nanoTime() < retryAt) return true;
            renderFailedUntil.remove(key);
            return false;
        }

        private void markRenderFailure(String key) {
            if (key == null || key.isBlank()) return;
            renderFailedUntil.put(key, System.nanoTime() + DEFAULT_FAILURE_RETRY_NANOS);
        }
    }

    private static final class EntityPreviewState {
        private float angle;
        private int zoomPercent;
        private long lastNanos;
        private boolean hovered;

        private EntityPreviewState(float defaultAngle, int defaultZoomPercent) {
            angle = defaultAngle;
            zoomPercent = defaultZoomPercent;
            lastNanos = System.nanoTime();
        }

        private float updateRotation(boolean hoveredNow, float baseSpeed, int speedPercent, boolean clockwise, long frameLimitNanos) {
            long now = System.nanoTime();
            if (hoveredNow && hovered) {
                long elapsedNanos = now - lastNanos;
                if (elapsedNanos > 0L && elapsedNanos <= frameLimitNanos) {
                    float elapsedSeconds = elapsedNanos / 1_000_000_000f;
                    float direction = clockwise ? 1f : -1f;
                    angle = (angle + elapsedSeconds * baseSpeed * speedPercent * direction / 100f) % 360f;
                }
            }
            hovered = hoveredNow;
            lastNanos = now;
            return angle;
        }

        private int getZoomPercent() {
            return zoomPercent;
        }

        private void setZoomPercent(int zoomPercent) {
            this.zoomPercent = zoomPercent;
        }
    }
}
