package dev.xyat.kineticcore.api.client.registry;

import dev.xyat.kineticcore.internal.client.registry.KineticClientRendererRuntime;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for client renderers. */
public final class KineticClientRenderers {
    private KineticClientRenderers() {
    }

    /**
     * 在实体渲染器注册事件前登记工厂。监听器安装失败不会留下无效登记；
     * 注册事件中某项失败仍会继续处理其他项，并在结束后报告错误。事件开始后再登记会抛出 IllegalStateException。
     */
    public static <T extends Entity> void registerEntityRenderer(
            Supplier<? extends EntityType<T>> entityType,
            EntityRendererProvider<T> provider
    ) {
        KineticClientRendererRuntime.registerEntityRenderer(
                Objects.requireNonNull(entityType, "entityType"),
                Objects.requireNonNull(provider, "provider")
        );
    }
}
