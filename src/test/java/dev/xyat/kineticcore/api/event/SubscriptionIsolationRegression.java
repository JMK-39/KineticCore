package dev.xyat.kineticcore.api.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Closing a handle twice must not accidentally cancel a second identical registration. */
public final class SubscriptionIsolationRegression {
    private static int checks;
    private static void check(boolean result, String description) {
        checks++;
        if (!result) throw new AssertionError(description);
    }
    public static void main(String[] args) {
        var eventListeners = new CopyOnWriteArrayList<String>();
        eventListeners.add("shared");
        KineticEventSubscription first = KineticEventSubscription.once(() -> eventListeners.remove("shared"));
        eventListeners.add("shared");
        KineticEventSubscription second = KineticEventSubscription.once(() -> eventListeners.remove("shared"));
        first.close();
        first.close();
        check(eventListeners.size() == 1, "closing first subscription twice cannot remove second registration");
        second.close();
        second.close();
        check(eventListeners.isEmpty(), "second subscription must be individually removable");
        AtomicInteger eventCalls = new AtomicInteger();
        KineticEventSubscription event = KineticEventSubscription.once(eventCalls::incrementAndGet);
        event.close(); event.close();
        check(eventCalls.get() == 1, "event cancellation is invoked once");
        AtomicInteger hookCalls = new AtomicInteger();
        HookRegistration hook = HookRegistration.once(hookCalls::incrementAndGet);
        hook.close(); hook.close();
        check(hookCalls.get() == 1, "hook cancellation is invoked once");
        System.out.println("PASS: " + checks + " subscription isolation checks");
    }
}
