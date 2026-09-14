package dev.xyat.kineticcore.api.hook;

@FunctionalInterface
public interface HookRegistration extends AutoCloseable {
    @Override
    void close();
}
