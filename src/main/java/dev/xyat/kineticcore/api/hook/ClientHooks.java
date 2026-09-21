package dev.xyat.kineticcore.api.hook;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

/** Public API type for client hooks. */
public final class ClientHooks {
    private ClientHooks() {
    }

    /**
     * 注册原版或 Forge 配置加载前的通知。调用者可在此更新配置文件或按键默认值。
     * 返回的句柄可取消本次注册；相同监听器分别注册会得到独立句柄。
     * 回调逐项执行；单个监听器失败不阻断同批其他监听器，错误在分发结束后报告。
     */
    public static HookRegistration onOptionsLoading(OptionsLoadHandler handler) {
        return KineticClientHookRuntime.registerOptionsLoading(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * 注册资源重载 UI 监听器；关闭状态和绘制通知逐项执行，异常在通知结束后报告。
     */
    public static HookRegistration onResourceReloadUi(ResourceReloadUi handler) {
        return KineticClientHookRuntime.registerResourceReloadUi(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /** Callback contract for options load notifications. */
    @FunctionalInterface
    public interface OptionsLoadHandler {
        void beforeLoad(Options options);
    }

    /** Public API contract for resource reload ui. */
    public interface ResourceReloadUi {
        void setPackScreenClosing(boolean closing);

        boolean interceptReloadStart();

        void render(GuiGraphics graphics, int width, int height);
    }
}
