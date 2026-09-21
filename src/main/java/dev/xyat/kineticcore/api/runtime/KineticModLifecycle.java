package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticModLifecycleRuntime;

import java.util.Objects;

/** Public Kinetic API facade for mod lifecycle. */
public final class KineticModLifecycle {
    private KineticModLifecycle() {
    }

    /**
     * 在 Forge 通用初始化阶段分别提交回调。某个回调失败不会阻止其余回调被提交。
     * 必须在通用初始化事件开始前注册；开始后再注册会抛出 IllegalStateException。
     */
    public static void onCommonSetup(Runnable action) {
        KineticModLifecycleRuntime.onCommonSetup(Objects.requireNonNull(action, "action"));
    }

    /**
     * 在 Forge 客户端初始化阶段分别提交回调。某个回调失败不会阻止其余回调被提交。
     * 必须在客户端初始化事件开始前注册；开始后再注册会抛出 IllegalStateException。
     */
    public static void onClientSetup(Runnable action) {
        KineticModLifecycleRuntime.onClientSetup(Objects.requireNonNull(action, "action"));
    }

    /**
     * 在 Forge 加载完成事件中分别提交回调，前一个回调失败不会阻止后续回调被提交。
     * 加载完成事件触发但排队任务尚未执行时，新回调会在原任务结束后执行；真正完成后新注册的回调立即执行。
     * 同一事件重复触发不会重复提交。任务提交失败时仍尝试提交其他任务，并报告异常。
     * 已完成后的回调不会在内部注册锁中执行，允许回调进一步调用生命周期 API。
     */
    public static void onLoadComplete(Runnable action) {
        KineticModLifecycleRuntime.onLoadComplete(Objects.requireNonNull(action, "action"));
    }
}
