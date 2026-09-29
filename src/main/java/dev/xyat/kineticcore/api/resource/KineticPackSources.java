package dev.xyat.kineticcore.api.resource;

import dev.xyat.kineticcore.internal.resource.KineticPackSourceRuntime;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for pack sources. */
public final class KineticPackSources {
    private KineticPackSources() {
    }

    /**
     * 注册可复用的资源包来源工厂。Forge 监听器安装成功后才保存工厂； 安装失败不残留来源，可在下一次调用时重试。同类型的多个来源会逐个注册， 单个工厂的 RuntimeException
     * 不会阻止后续工厂，处理结束后汇总报告异常。 对应 PackType 的事件开始后再登记会抛出 IllegalStateException。
     *
     * <p>Registers a factory for a built-in resource or data pack source. The factory is kept only after the Forge
     * listener is installed, so a failed call can be retried. Factories of one pack type run one by one; failures
     * are reported together.
     *
     * @throws IllegalStateException if the pack-finder event for {@code packType} has already started
     */
    public static void register(PackType packType, Supplier<? extends RepositorySource> sourceFactory) {
        KineticPackSourceRuntime.register(
                Objects.requireNonNull(packType, "packType"),
                Objects.requireNonNull(sourceFactory, "sourceFactory")
        );
    }
}
