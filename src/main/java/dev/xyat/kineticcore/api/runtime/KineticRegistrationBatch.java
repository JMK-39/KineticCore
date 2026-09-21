package dev.xyat.kineticcore.api.runtime;

import java.util.BitSet;
import java.util.Objects;

/**
 * Runs a fixed ordered set of one-time registration steps while retaining the success state of each step.
 * Use {@link #run(Runnable...)} for independent steps that should all be attempted in the same invocation, or
 * {@link #runSequential(Runnable...)} when a later step depends on every earlier step having succeeded.
 */
public final class KineticRegistrationBatch {
    private final BitSet completed = new BitSet();
    private int expectedSize = -1;

    /**
     * Executes dependent one-time registration steps in order. A later step is not attempted until every earlier
     * step has completed successfully. Successful steps remain completed across retries, so a later failure resumes
     * from the failed slot instead of repeating earlier side effects.
     * The same batch instance must always receive the same number of steps in the same order.
     */
    public synchronized void runSequential(Runnable... registrations) {
        Objects.requireNonNull(registrations, "registrations");
        if (expectedSize < 0) {
            expectedSize = registrations.length;
        } else if (expectedSize != registrations.length) {
            throw new IllegalStateException("Registration batch size changed after initialization");
        }

        for (int slot = 0; slot < registrations.length; slot++) {
            if (completed.get(slot)) continue;
            Objects.requireNonNull(registrations[slot], "registration").run();
            completed.set(slot);
        }
    }

    /**
     * Executes the supplied fixed-order registration set.
     * The same batch instance must always receive the same number of steps in the same order.
     */
    public synchronized void run(Runnable... registrations) {
        Objects.requireNonNull(registrations, "registrations");
        if (expectedSize < 0) {
            expectedSize = registrations.length;
        } else if (expectedSize != registrations.length) {
            throw new IllegalStateException("Registration batch size changed after initialization");
        }

        Throwable failure = null;
        for (int slot = 0; slot < registrations.length; slot++) {
            if (completed.get(slot)) continue;
            try {
                Objects.requireNonNull(registrations[slot], "registration").run();
                completed.set(slot);
            } catch (RuntimeException | Error exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }

        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (failure instanceof Error errorFailure) throw errorFailure;
    }
}
