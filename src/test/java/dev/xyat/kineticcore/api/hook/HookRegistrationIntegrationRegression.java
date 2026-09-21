package dev.xyat.kineticcore.api.hook;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import java.util.concurrent.atomic.AtomicInteger;

/** Verify the public hook registrar and its real runtime listener container. */
public final class HookRegistrationIntegrationRegression {
    public static void main(String[] args) {
        int checks = 0;
        AtomicInteger received = new AtomicInteger();
        ClientHooks.OptionsLoadHandler shared = options -> received.incrementAndGet();
        HookRegistration first = ClientHooks.onOptionsLoading(shared);
        HookRegistration second = ClientHooks.onOptionsLoading(shared);
        first.close();
        first.close();
        KineticClientHookRuntime.fireOptionsLoading(null);
        if (received.get() != 1) throw new AssertionError("closing one hook twice must preserve the second registration");
        checks++;
        second.close();
        second.close();
        KineticClientHookRuntime.fireOptionsLoading(null);
        if (received.get() != 1) throw new AssertionError("the second hook must be fully unsubscribed");
        checks++;
        System.out.println("PASS: " + checks + " real hook runtime integration checks");
    }
}
