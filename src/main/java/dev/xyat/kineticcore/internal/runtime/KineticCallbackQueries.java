package dev.xyat.kineticcore.internal.runtime;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Dispatches ordered short-circuit queries without letting a broken provider block
 * later providers or force the caller to lose its vanilla fallback. Errors are logged.
 */
public final class KineticCallbackQueries {
    private static final Logger LOGGER = LogUtils.getLogger();

    private KineticCallbackQueries() {
    }

    public static <T> boolean anyMatch(Iterable<? extends T> providers, Predicate<? super T> predicate) {
        return firstMatch(providers, predicate) != null;
    }

    /** First successful match wins, otherwise null. A failing provider is skipped and reported. */
    public static <T> T firstMatch(Iterable<? extends T> providers, Predicate<? super T> predicate) {
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(predicate, "predicate");
        for (T provider : providers) {
            try {
                if (predicate.test(provider)) return provider;
            } catch (RuntimeException failure) {
                reportFailure(failure);
            }
        }
        return null;
    }

    /** The first nonempty Optional wins. A broken or null-returning provider does not block later providers. */
    public static <T, R> Optional<R> firstPresent(
            Iterable<? extends T> providers,
            Function<? super T, Optional<R>> query
    ) {
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(query, "query");
        for (T provider : providers) {
            try {
                Optional<R> result = query.apply(provider);
                if (result != null && result.isPresent()) return result;
            } catch (RuntimeException failure) {
                reportFailure(failure);
            }
        }
        return Optional.empty();
    }

    public static void reportFailure(RuntimeException failure) {
        LOGGER.error("Kinetic hook provider failed; continuing with the next provider", failure);
    }
}
