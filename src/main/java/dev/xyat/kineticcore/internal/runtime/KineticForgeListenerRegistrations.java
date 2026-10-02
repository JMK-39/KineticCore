package dev.xyat.kineticcore.internal.runtime;

import java.util.BitSet;
import java.util.Objects;

/**
 * Keeps each installed Forge listener's success state across partial registration failures.
 * The owning runtime invokes attempts from its synchronized initialization method.
 */
public final class KineticForgeListenerRegistrations {
    private final BitSet installed = new BitSet();

    public Attempt begin() {
        return new Attempt();
    }

    public final class Attempt {
        private RuntimeException failure;
        private int nextSlot;

        /**
         * Installs the next listener of this attempt. The n-th call of every attempt uses slot n, so a retry skips
         * the listeners that an earlier attempt already installed. Do not mix with explicit slots on one registration.
         */
        public void install(Runnable registration) {
            install(nextSlot++, registration);
        }

        /** Installs the listener for an explicit slot, for registrations keyed by something other than call order. */
        public void install(int slot, Runnable registration) {
            if (slot < 0) throw new IllegalArgumentException("Listener slot cannot be negative");
            Objects.requireNonNull(registration, "registration");
            if (installed.get(slot)) return;
            try {
                registration.run();
                installed.set(slot);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }

        public void finish() {
            if (failure != null) throw failure;
        }
    }
}
