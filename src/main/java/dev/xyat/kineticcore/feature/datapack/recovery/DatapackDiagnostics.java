package dev.xyat.kineticcore.feature.datapack.recovery;

import dev.xyat.kineticcore.feature.datapack.PackModule;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class DatapackDiagnostics {
    private static final Logger LOG = LoggerFactory.getLogger(DatapackDiagnostics.class);
    private static final Pattern SOURCE = Pattern.compile("^Failed to parse (\\S+) from pack (.+)$", Pattern.DOTALL);
    private static final List<DatapackProblem> PROBLEMS = new ArrayList<>();
    private DatapackDiagnostics() { }

    public static DatapackProblem parse(Throwable failure, String registryPath, String elementId) {
        String pack = "", resource = "", reason = "";
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable error = failure; error != null && visited.add(error); error = error.getCause()) {
            String message = error.getMessage();
            if (message == null || message.isBlank()) continue;
            var matcher = SOURCE.matcher(message);
            if (matcher.matches() && pack.isEmpty()) {
                pack = matcher.group(2); resource = matcher.group(1);
                // 26 reports an element identifier, while earlier versions report its JSON resource identifier.
                if (resource.equals(elementId)) {
                    int colon = resource.indexOf(':');
                    if (colon > 0) resource = resource.substring(0, colon + 1) + registryPath + "/" + resource.substring(colon + 1) + ".json";
                }
            }
            reason = message;
        }
        if (reason.isEmpty()) reason = failure.getClass().getSimpleName();
        return new DatapackProblem(pack, resource, reason, null);
    }

    public static synchronized void clearAttempt() { PROBLEMS.clear(); DatapackReferences.stop(); }
    public static synchronized List<DatapackProblem> problems() { return List.copyOf(PROBLEMS); }

    public static void recordRegistryErrors(Map<ResourceKey<?>, Exception> errors) {
        var world = DatapackRecovery.attemptWorld();
        errors.forEach((key, error) -> {
            var registry = key.registry();
            String directory = registry.getNamespace().equals("minecraft") ? registry.getPath() : registry.getNamespace() + "/" + registry.getPath();
            DatapackProblem problem = parse(error, directory, key.location().toString());
            var sources = problem.packId().isEmpty() ? DatapackReferences.sources(key, problem.reason()) : List.<DatapackReferences.Origin>of();
            if (sources.isEmpty()) add(problem, world);
            else for (var source : sources) add(new DatapackProblem(source.packId(), source.resourceId(), problem.reason(), null), world);
        });
        DatapackReferences.stop();
    }

    private static void add(DatapackProblem problem, java.nio.file.Path world) {
        var location = DatapackFiles.locate(problem.packId(), problem.resourceId(), PackModule.DATA_PACK_DIR, world);
        problem = problem.withLocation(location);
        synchronized (DatapackDiagnostics.class) { if (!PROBLEMS.contains(problem)) PROBLEMS.add(problem); }
        LOG.error("Data pack loading failure: pack={}, file={}, reason={}", problem.packId().isEmpty() ? "unresolved" : problem.packId(),
                location == null ? problem.resourceId() : location.displayPath(), problem.reason());
    }

    public static void recordWorldFailure(Throwable error) {
        DatapackReferences.stop();
        var world = DatapackRecovery.attemptWorld();
        DatapackProblem problem = parse(error, "", "");
        synchronized (DatapackDiagnostics.class) {
            if (!PROBLEMS.isEmpty()) return;
            PROBLEMS.add(problem);
        }
        LOG.error("World data loading failure: world={}, reason={}", world, problem.reason());
    }
}
