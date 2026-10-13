package dev.xyat.kineticcore.api.spawnegg;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/** Changing callback priority or sharing subscriptions must fail this test. */
public final class SpawnEggCallbackRegression {
    public static void main(String[] args) throws Exception {
        Class<?> api;
        try { api = Class.forName("dev.xyat.kineticcore.api.spawnegg.KineticSpawnEggs"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Missing spawn completion API", e); }
        Class<?> handler = Class.forName(api.getName() + "$SpawnedHandler");
        Class<?> context = Class.forName(api.getName() + "$SpawnedContext");
        Class<?> runtime = Class.forName("dev.xyat.kineticcore.internal.spawnegg.KineticSpawnEggRuntime");
        List<String> calls = new ArrayList<>();
        var on = api.getMethod("onSpawned", KineticEventPriority.class, handler);
        List<AutoCloseable> subscriptions = new ArrayList<>();
        try {
            subscriptions.add((AutoCloseable) on.invoke(null, KineticEventPriority.LOWEST, callback(handler, () -> calls.add("lowest"))));
            subscriptions.add((AutoCloseable) on.invoke(null, KineticEventPriority.HIGH, callback(handler, () -> calls.add("high1"))));
            var removed = (AutoCloseable) on.invoke(null, KineticEventPriority.HIGH, callback(handler, () -> calls.add("removed")));
            removed.close(); removed.close();
            subscriptions.add((AutoCloseable) on.invoke(null, KineticEventPriority.HIGH, callback(handler, () -> calls.add("high2"))));
            Object ctx = Proxy.newProxyInstance(context.getClassLoader(), new Class<?>[]{context}, (p,m,a) -> null);
            runtime.getMethod("dispatch", context).invoke(null, ctx);
            if (!calls.equals(List.of("high1", "high2", "lowest"))) throw new AssertionError("Incorrect callback order: " + calls);
            calls.clear();
            subscriptions.add((AutoCloseable) on.invoke(null, KineticEventPriority.HIGHEST, callback(handler, () -> { throw new IllegalStateException("expected failure"); })));
            try { runtime.getMethod("dispatch", context).invoke(null, ctx); throw new AssertionError("Callback exception swallowed"); }
            catch (InvocationTargetException e) { if (!(e.getCause() instanceof IllegalStateException)) throw e; }
            if (!calls.equals(List.of("high1", "high2", "lowest"))) throw new AssertionError("Failure skipped later priorities: " + calls);
        } finally { for (var subscription : subscriptions) subscription.close(); }
        System.out.println("PASS: spawn completion order, subscription removal and exception isolation");
    }
    private static Object callback(Class<?> type, Runnable action) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,a) -> {
            if (m.getName().equals("handle")) { action.run(); return null; }
            if (m.getName().equals("equals")) return p == a[0];
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            return "test handler";
        });
    }
}
