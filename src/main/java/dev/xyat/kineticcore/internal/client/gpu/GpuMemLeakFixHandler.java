package dev.xyat.kineticcore.internal.client.gpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
//? if >=26.1 {
/*import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
*///?} else {
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
//?}
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
//? if <26.1
import net.minecraft.core.Vec3i;
import net.minecraftforge.common.MinecraftForge;
//? if forge {
import net.minecraftforge.event.TickEvent;
//?} else {
/*import net.neoforged.neoforge.client.event.ClientTickEvent;
*///?}

import java.lang.ref.Cleaner;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

public class GpuMemLeakFixHandler {
    private static boolean registered;

    private static final Cleaner CLEANER = Cleaner.create();
    //? if >=26.1 {
    /*public static final ConcurrentLinkedQueue<AutoCloseable[]> PENDING_DELETIONS = new ConcurrentLinkedQueue<>();
    *///?} else {
    public static final ConcurrentLinkedQueue<Vec3i> PENDING_DELETIONS = new ConcurrentLinkedQueue<>();
    //?}

    public static synchronized void register() {
        if (registered) return;
        MinecraftForge.EVENT_BUS.addListener(GpuMemLeakFixHandler::onClientTick);
        registered = true;
    }

    //? if forge {
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processPendingDeletions();
        }
    }
    //?} else {
    /*private static void onClientTick(ClientTickEvent.Post event) {
        processPendingDeletions();
    }
    *///?}

    //? if >=26.1 {
    /*// 26.1 render targets own GpuTexture objects; a collected target's textures and views are closed on the render
    // thread. The state keeps the textures, never the target itself.
    public static class RenderTargetState implements Runnable {
        private AutoCloseable[] resources = new AutoCloseable[0];

        public synchronized void update(@Nullable GpuTexture colorTexture, @Nullable GpuTextureView colorTextureView,
                                        @Nullable GpuTexture depthTexture, @Nullable GpuTextureView depthTextureView) {
            // Views go first, as RenderTarget.destroyBuffers would close them.
            this.resources = java.util.stream.Stream.of(colorTextureView, depthTextureView, colorTexture, depthTexture)
                    .filter(java.util.Objects::nonNull)
                    .toArray(AutoCloseable[]::new);
        }

        public synchronized void clear() {
            this.resources = new AutoCloseable[0];
        }

        @Override
        public void run() {
            AutoCloseable[] leaked;
            synchronized (this) {
                if (resources.length == 0) return;
                leaked = resources;
                resources = new AutoCloseable[0];
            }
            PENDING_DELETIONS.add(leaked);
        }
    }

    public static void track(RenderTarget target, RenderTargetState state) {
        CLEANER.register(target, state);
    }

    public static void processPendingDeletions() {
        AutoCloseable[] resources;
        while ((resources = PENDING_DELETIONS.poll()) != null) {
            for (AutoCloseable resource : resources) {
                release(resource);
            }
        }
    }

    private static void release(AutoCloseable resource) {
        try {
            boolean closed = resource instanceof GpuTexture texture ? texture.isClosed()
                    : resource instanceof GpuTextureView view && view.isClosed();
            if (!closed) resource.close();
        } catch (Throwable throwable) {
            KineticRuntime.logger().error("Failed to release leaked GPU resource {}", resource, throwable);
        }
    }
    *///?} else {
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
            KineticRuntime.logger().error("Failed to release leaked GPU {} id {}", type, textureId, throwable);
        }
    }

    private static void releaseFramebuffer(int framebufferId) {
        if (framebufferId < 0) return;
        try {
            GlStateManager._glDeleteFramebuffers(framebufferId);
        } catch (Throwable throwable) {
            KineticRuntime.logger().error("Failed to release leaked GPU framebuffer id {}", framebufferId, throwable);
        }
    }
    //?}
}
