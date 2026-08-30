package dev.xyat.kineticcore.api.client.entity;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class EntityPreviewRenderer {
    public static final float DEFAULT_ROTATION = 20f;
    public static final float DEFAULT_BASE_ROTATION_SPEED = 90f;
    public static final int DEFAULT_ZOOM_PERCENT = 100;
    public static final int MIN_ZOOM_PERCENT = 10;
    public static final int MAX_ZOOM_PERCENT = 500;
    public static final int ZOOM_STEP_PERCENT = 10;
    public static final float DEFAULT_FILL_RATIO = 0.56f;
    public static final float DEFAULT_MAX_AUTO_SCALE_FACTOR = 1.10f;
    public static final long DEFAULT_FRAME_LIMIT_NANOS = 100_000_000L;
    public static final int DEFAULT_CACHE_SIZE = 48;

    private static final int CHECKER_SIZE = 6;
    private static final int CHECKER_LIGHT = 0xFFF0F0F0;
    private static final int CHECKER_DARK = 0xFFD2D2D2;

    private final int maxCacheSize;
    private final float fillRatio;
    private final float maxAutoScaleFactor;
    private final Map<String, Entity> renderCache;
    private final Set<String> renderFailed = new HashSet<>();
    private final Map<String, EntityPreviewState> states = new LinkedHashMap<>();

    private int rotationSpeedPercent = 100;
    private boolean clockwise = true;

    public EntityPreviewRenderer() {
        this(
                DEFAULT_CACHE_SIZE,
                DEFAULT_FILL_RATIO,
                DEFAULT_MAX_AUTO_SCALE_FACTOR
        );
    }

    public EntityPreviewRenderer(
            int maxCacheSize,
            float fillRatio,
            float maxAutoScaleFactor
    ) {
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

    public void setRotationSpeedPercent(int rotationSpeedPercent) {
        this.rotationSpeedPercent = Math.max(
                0,
                Math.min(500, rotationSpeedPercent)
        );
    }

    public void setClockwise(boolean clockwise) {
        this.clockwise = clockwise;
    }

    public int getZoomPercent(String stateKey) {
        return getState(stateKey).getZoomPercent();
    }

    public void setZoomPercent(String stateKey, int zoomPercent) {
        getState(stateKey).setZoomPercent(
                Math.max(
                        MIN_ZOOM_PERCENT,
                        Math.min(MAX_ZOOM_PERCENT, zoomPercent)
                )
        );
    }

    public void adjustZoom(String stateKey, double delta) {
        if (stateKey == null || delta == 0D) {
            return;
        }

        int current = getZoomPercent(stateKey);
        int next = current + (
                delta > 0D
                        ? ZOOM_STEP_PERCENT
                        : -ZOOM_STEP_PERCENT
        );

        setZoomPercent(stateKey, next);
    }

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
        ResourceLocation id = ResourceLocation.tryParse(entityId);

        if (id == null) {
            return false;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);

        return render(
                graphics,
                type,
                id,
                stateKey,
                boxX,
                boxY,
                boxW,
                boxH,
                guiScale,
                offsetX,
                offsetY,
                hovered
        );
    }

    public boolean render(
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
            boolean hovered
    ) {
        if (type == null || id == null || stateKey == null) {
            return false;
        }

        Entity entity = getOrCreateEntity(type, id);

        if (entity == null) {
            return false;
        }

        float entityWidth = Math.max(entity.getBbWidth(), 0.1f);
        float entityHeight = Math.max(entity.getBbHeight(), 0.1f);
        float safeWidth = boxW * fillRatio;
        float safeHeight = boxH * fillRatio;

        float autoScale = Math.min(
                safeWidth / entityWidth,
                safeHeight / entityHeight
        );

        autoScale = Math.min(
                autoScale,
                boxH * maxAutoScaleFactor
        );

        EntityPreviewState state = getState(stateKey);

        float scale = autoScale
                * state.getZoomPercent()
                / 100f;

        int renderX = Math.round(
                boxX + boxW / 2f
        );

        int renderY = Math.round(
                boxY
                        + boxH / 2f
                        + entityHeight * scale / 2f
        );

        float angle = state.updateRotation(
                hovered,
                DEFAULT_BASE_ROTATION_SPEED,
                rotationSpeedPercent,
                clockwise,
                DEFAULT_FRAME_LIMIT_NANOS
        );

        int scissorX1 = (int) (boxX * guiScale) + offsetX;
        int scissorY1 = (int) (boxY * guiScale) + offsetY;
        int scissorX2 = (int) ((boxX + boxW) * guiScale) + offsetX;
        int scissorY2 = (int) ((boxY + boxH) * guiScale) + offsetY;

        graphics.enableScissor(
                scissorX1,
                scissorY1,
                scissorX2,
                scissorY2
        );

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
            graphics.pose().translate(
                    renderX,
                    renderY,
                    50D
            );

            graphics.pose().scale(
                    scale,
                    scale,
                    -scale
            );

            graphics.pose().mulPose(
                    com.mojang.math.Axis.ZP.rotationDegrees(180f)
            );

            graphics.pose().mulPose(
                    com.mojang.math.Axis.XP.rotationDegrees(-10f)
            );

            graphics.pose().mulPose(
                    com.mojang.math.Axis.YP.rotationDegrees(
                            180f - angle
                    )
            );

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

            var dispatcher = Minecraft.getInstance()
                    .getEntityRenderDispatcher();

            dispatcher.setRenderShadow(false);

            RenderSystem.setShaderColor(
                    1f,
                    1f,
                    1f,
                    1f
            );

            Lighting.setupFor3DItems();

            RenderSystem.runAsFancy(() ->
                    dispatcher.render(
                            entity,
                            0D,
                            0D,
                            0D,
                            0f,
                            1f,
                            graphics.pose(),
                            graphics.bufferSource(),
                            15728880
                    )
            );

            graphics.bufferSource().endBatch();
            return true;
        } catch (Throwable ignored) {
            renderCache.remove(id.toString());
            renderFailed.add(id.toString());
            return false;
        } finally {
            Minecraft.getInstance()
                    .getEntityRenderDispatcher()
                    .setRenderShadow(true);

            Lighting.setupFor3DItems();

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
            graphics.disableScissor();
        }
    }

    public void clear() {
        renderCache.clear();
        renderFailed.clear();
        states.clear();
    }

    public static void drawCheckerboard(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                CHECKER_LIGHT
        );

        int rows = (height + CHECKER_SIZE - 1) / CHECKER_SIZE;
        int cols = (width + CHECKER_SIZE - 1) / CHECKER_SIZE;

        for (int row = 0; row < rows; row++) {
            for (int col = row & 1; col < cols; col += 2) {
                int x1 = x + col * CHECKER_SIZE;
                int y1 = y + row * CHECKER_SIZE;

                graphics.fill(
                        x1,
                        y1,
                        Math.min(x1 + CHECKER_SIZE, x + width),
                        Math.min(y1 + CHECKER_SIZE, y + height),
                        CHECKER_DARK
                );
            }
        }
    }

    private EntityPreviewState getState(String stateKey) {
        return states.computeIfAbsent(
                stateKey,
                ignored -> new EntityPreviewState(
                        DEFAULT_ROTATION,
                        DEFAULT_ZOOM_PERCENT
                )
        );
    }

    private Entity getOrCreateEntity(
            EntityType<?> type,
            ResourceLocation id
    ) {
        String key = id.toString();

        if (renderFailed.contains(key)) {
            return null;
        }

        Entity cached = renderCache.get(key);

        if (cached != null) {
            return cached;
        }

        if (Minecraft.getInstance().level == null) {
            return null;
        }

        try {
            Entity entity = type.create(
                    Minecraft.getInstance().level
            );

            if (entity == null) {
                renderFailed.add(key);
                return null;
            }

            renderCache.put(key, entity);
            return entity;
        } catch (Throwable ignored) {
            renderFailed.add(key);
            return null;
        }
    }
}
