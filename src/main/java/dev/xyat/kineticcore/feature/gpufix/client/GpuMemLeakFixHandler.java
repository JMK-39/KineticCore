package dev.xyat.kineticcore.feature.gpufix.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
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

    /**
     * 你的自动扫描系统会反射调用此方法
     */
    public static void register() {
        // 自动挂载到 Forge 的客户端 Tick 事件
        MinecraftForge.EVENT_BUS.addListener(GpuMemLeakFixHandler::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processPendingDeletions();
        }
    }

    public static class RenderTargetState implements Runnable {
        public int colorTextureId = -1;
        public int depthBufferId = -1;
        public int frameBufferId = -1;

        @Override
        public void run() {
            if (colorTextureId > -1 || depthBufferId > -1 || frameBufferId > -1) {
                PENDING_DELETIONS.add(new Vec3i(colorTextureId, depthBufferId, frameBufferId));
            }
        }
    }

    public static void track(RenderTarget target, RenderTargetState state) {
        CLEANER.register(target, state);
    }

    public static void processPendingDeletions() {
        if (PENDING_DELETIONS.isEmpty()) return;

        Vec3i ids;
        while ((ids = PENDING_DELETIONS.poll()) != null) {
            int color = ids.getX();
            int depth = ids.getY();
            int frame = ids.getZ();

            if (color > -1) {
                TextureUtil.releaseTextureId(color);
            }
            if (depth > -1) {
                TextureUtil.releaseTextureId(depth);
            }
            if (frame > -1) {
                GlStateManager._glDeleteFramebuffers(frame);
            }
        }
    }
}