package dev.xyat.kineticcore.feature.datapack.recovery;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.storage.PrimaryLevelData;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Common-side state. Dedicated servers never create a client recovery session. */
public final class DatapackRecovery {
    private static final Map<PackRepository, Path> WORLD_PATHS = new WeakHashMap<>();
    private static final Map<PrimaryLevelData, RestoreChoices> SAVE_CHOICES = new WeakHashMap<>();
    private static final Set<PackRepository> CLIENT_REPOSITORIES = java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<String> SKIPPED = new HashSet<>();
    private static Path clientWorld, attemptWorld;
    private static boolean retry;
    private static List<String> originalSelection = List.of();
    private record RestoreChoices(List<String> original, Set<String> skipped) { }
    private DatapackRecovery() { }

    public static synchronized void beginWorld(Path world) {
        clientWorld = world.toAbsolutePath().normalize();
        SKIPPED.clear(); CLIENT_REPOSITORIES.clear(); originalSelection = List.of(); retry = false;
        DatapackDiagnostics.clearAttempt();
    }

    public static synchronized void bindWorldRepository(PackRepository repository, Path world) {
        WORLD_PATHS.put(repository, world.toAbsolutePath().normalize());
    }

    public static synchronized void beginAttempt(PackRepository repository) {
        attemptWorld = WORLD_PATHS.get(repository);
        if (clientWorld != null && clientWorld.equals(attemptWorld)) CLIENT_REPOSITORIES.add(repository);
        DatapackDiagnostics.clearAttempt();
        DatapackReferences.clear();
    }

    public static synchronized Path attemptWorld() { return attemptWorld; }
    public static synchronized Path clientWorld() { return clientWorld; }
    public static synchronized boolean normalRetry(boolean safeMode) { return retry && clientWorld != null ? false : safeMode; }
    public static synchronized Set<String> skippedPacks() { return Set.copyOf(SKIPPED); }

    public static synchronized List<Pack> filter(PackRepository repository, List<Pack> selected) {
        if (!CLIENT_REPOSITORIES.contains(repository)) return selected;
        if (SKIPPED.isEmpty()) originalSelection = selected.stream().map(Pack::getId).toList();
        if (SKIPPED.isEmpty()) return selected;
        return selected.stream().filter(pack -> !SKIPPED.contains(pack.getId())).toList();
    }

    /** Required packs may be reopened during ordinary server reloads; guard the actual resource manager too. */
    public static List<PackResources> filterResources(PackType type, List<PackResources> packs) {
        Set<String> skipped;
        synchronized (DatapackRecovery.class) {
            if (type != PackType.SERVER_DATA || clientWorld == null || SKIPPED.isEmpty()) return packs;
            skipped = Set.copyOf(SKIPPED);
        }
        List<PackResources> kept = new ArrayList<>();
        for (PackResources pack : packs) {
            if (!skipped.contains(pack.packId())) kept.add(pack);
            else pack.close();
        }
        return kept;
    }

    public static synchronized boolean canSkipProblems() {
        return clientWorld != null && DatapackDiagnostics.problems().stream().anyMatch(DatapackRecovery::canSkip);
    }
    private static boolean canSkip(DatapackProblem problem) {
        return problem.location() != null && !problem.packId().isEmpty() && !SKIPPED.contains(problem.packId());
    }
    public static synchronized boolean skipProblems() {
        boolean changed = false;
        for (DatapackProblem problem : DatapackDiagnostics.problems()) {
            if (canSkip(problem)) changed |= SKIPPED.add(problem.packId());
        }
        if (changed) retry = true;
        return changed;
    }

    public static synchronized void rememberWorldData(PrimaryLevelData data) {
        if (clientWorld == null || SKIPPED.isEmpty()) return;
        SAVE_CHOICES.put(data, new RestoreChoices(List.copyOf(originalSelection), Set.copyOf(SKIPPED)));
    }

    public static synchronized Object savedConfiguration(PrimaryLevelData data, Object actual) {
        RestoreChoices restore = SAVE_CHOICES.get(data);
        if (!(actual instanceof WorldDataConfiguration current) || restore == null) return actual;
        List<String> enabled = new ArrayList<>(current.dataPacks().getEnabled());
        Set<String> restored = new HashSet<>();
        for (int i = 0; i < restore.original().size(); i++) {
            String id = restore.original().get(i);
            if (!restore.skipped().contains(id)) continue;
            restored.add(id);
            if (enabled.contains(id)) continue;
            int position = enabled.size();
            // Keep the current order of all unrelated packs, using the next surviving original pack as an anchor.
            for (int next = i + 1; next < restore.original().size(); next++) {
                int anchor = enabled.indexOf(restore.original().get(next));
                if (anchor >= 0) { position = anchor; break; }
            }
            enabled.add(position, id);
        }
        List<String> disabled = new ArrayList<>(current.dataPacks().getDisabled());
        disabled.removeAll(restored);
        return new WorldDataConfiguration(new DataPackConfig(enabled, disabled), current.enabledFeatures());
    }

    public static synchronized void cancel() {
        clientWorld = null; retry = false; SKIPPED.clear(); CLIENT_REPOSITORIES.clear(); originalSelection = List.of();
        DatapackDiagnostics.clearAttempt();
    }
}
