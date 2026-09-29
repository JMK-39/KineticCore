package dev.xyat.kineticcore.api.hook;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.Options;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;

import java.util.Objects;

/**
 * Hooks into client behavior that Forge has no event for. Handlers run on the client thread in registration order;
 * close the returned {@link HookRegistration} to unregister.
 */
public final class ClientHooks {
    private ClientHooks() {
    }

    /**
     * 注册原版或 Forge 配置加载前的通知。调用者可在此更新配置文件或按键默认值。 返回的句柄可取消本次注册；相同监听器分别注册会得到独立句柄。 回调逐项执行；单个监听器失败不阻断同批其他监听器，错误在分发结束后报告。
     *
     * <p>Registers a callback that runs before the vanilla or Forge options file is read, to update the file or key
     * binding defaults. Registering the same handler twice gives two independent handles.
     */
    public static HookRegistration onOptionsLoading(OptionsLoadHandler handler) {
        return KineticClientHookRuntime.registerOptionsLoading(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * 注册资源重载 UI 监听器；关闭状态和绘制通知逐项执行，异常在通知结束后报告。
     *
     * <p>Registers a replacement for the resource reload overlay. Notifications run one by one; failures are
     * reported afterwards.
     */
    public static HookRegistration onResourceReloadUi(ResourceReloadUi handler) {
        return KineticClientHookRuntime.registerResourceReloadUi(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /** Callback contract for options load notifications. */
    @FunctionalInterface
    public interface OptionsLoadHandler {
        /** Called before the options file is read, so defaults such as key bindings can be changed first. */
        void beforeLoad(Options options);
    }

    /** Replaces the full-screen resource reload overlay with a custom indicator. */
    public interface ResourceReloadUi {
        /** Told {@code true} when the pack selection screen starts closing and {@code false} when it has closed. */
        void setPackScreenClosing(boolean closing);

        /**
         * Returns {@code true} to take over a reload request; the vanilla reload with its overlay is then skipped.
         */
        boolean interceptReloadStart();

        /**
         * Draws the custom reload indicator on top of the HUD every frame; {@code width} and {@code height} are the
         * GUI size.
         */
        void render(KineticGraphics graphics, int width, int height);
    }
}
