package dev.xyat.kineticcore.feature.gpufix.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import net.minecraft.core.Vec3i;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.lang.ref.Cleaner;
import java.util.concurrent.ConcurrentLinkedQueue;

@KTClientModule
public class GpuMemLeakFixHandler {

    private static final Cleaner CLEANER = Cleaner.create();
    public static final ConcurrentLinkedQueue<Vec3i> PENDING_DELETIONS = new ConcurrentLinkedQueue<>();

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(GpuMemLeakFixHandler::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processPendingDeletions();
        }
    }

    public static class RenderTargetState implements Runnable {
        private int colorTextureId = -1;
        private int depthBufferId = -1;
        private int frameBufferId = -1;

        public synchronized void update(int colorTextureId, int depthBufferId, int frameBufferId) {
            this.colorTextureId = colorTextureId;
            this.depthBufferId = depthBufferId;
            this.frameBufferId = frameBufferId;
        }

        public synchronized void clear() {
            this.colorTextureId = -1;
            this.depthBufferId = -1;
            this.frameBufferId = -1;
        }

        @Override
        public void run() {
            Vec3i leakedIds;
            synchronized (this) {
                if (colorTextureId < 0 && depthBufferId < 0 && frameBufferId < 0) {
                    return;
                }
                leakedIds = new Vec3i(colorTextureId, depthBufferId, frameBufferId);
                colorTextureId = -1;
                depthBufferId = -1;
                frameBufferId = -1;
            }
            PENDING_DELETIONS.add(leakedIds);
        }
    }

    public static void track(RenderTarget target, RenderTargetState state) {
        CLEANER.register(target, state);
    }

    public static void processPendingDeletions() {
        Vec3i ids;
        while ((ids = PENDING_DELETIONS.poll()) != null) {
            releaseTexture(ids.getX(), "color texture");
            releaseTexture(ids.getY(), "depth texture");
            releaseFramebuffer(ids.getZ());
        }
    }

    private static void releaseTexture(int textureId, String type) {
        if (textureId < 0) return;
        try {
            TextureUtil.releaseTextureId(textureId);
        } catch (Throwable throwable) {
            KineticCore.LOGGER.error("Failed to release leaked GPU {} id {}", type, textureId, throwable);
        }
    }

    private static void releaseFramebuffer(int framebufferId) {
        if (framebufferId < 0) return;
        try {
            GlStateManager._glDeleteFramebuffers(framebufferId);
        } catch (Throwable throwable) {
            KineticCore.LOGGER.error("Failed to release leaked GPU framebuffer id {}", framebufferId, throwable);
        }
    }
}
