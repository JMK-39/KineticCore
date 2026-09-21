package dev.xyat.kineticcore.api.network;

import java.util.Objects;

/**
 * 在同一个频道上独立尝试注册每种数据包。
 * 单项失败不跳过后续项目，首次异常保留原样，后续异常附加为 suppressed；
 * 调用方应当对每个已成功的 Sender 独立记录状态，重试时跳过它。
 */
public final class PacketRegistrations {
    private PacketRegistrations() {
    }

    /** Runs packet registrations independently, preserving the first failure and suppressing later failures. */
    public static void runIndependent(Runnable... registrations) {
        Objects.requireNonNull(registrations, "registrations");
        Throwable failure = null;
        for (Runnable registration : registrations) {
            try {
                Objects.requireNonNull(registration, "registration").run();
            } catch (RuntimeException | Error exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (failure instanceof Error errorFailure) throw errorFailure;
    }
}
