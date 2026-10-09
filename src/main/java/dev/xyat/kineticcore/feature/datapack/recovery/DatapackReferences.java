package dev.xyat.kineticcore.feature.datapack.recovery;

import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Records real codec lookups while the exact source resource is open, including forward references. */
public final class DatapackReferences {
    private static final Pattern UNBOUND = Pattern.compile("^Unbound (values|tags) in registry .*: \\[([^]]*)\\]$");
    private static final Map<Resource, Origin> SOURCES = new IdentityHashMap<>();
    private static final Map<Reference, Set<Origin>> REFERENCES = new LinkedHashMap<>();
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    private static long generation;
    private static boolean collecting;
    public record Origin(String packId, String resourceId) { }
    private record Reference(String registry, String identifier, boolean tag) { }
    private record Context(long generation, Origin origin) { }
    private DatapackReferences() { }

    public static synchronized void clear() { generation++; SOURCES.clear(); REFERENCES.clear(); collecting = true; }
    public static synchronized long generation() { return generation; }
    public static synchronized void finish(long attempt) {
        if (attempt == generation) { generation++; SOURCES.clear(); REFERENCES.clear(); collecting = false; }
    }
    public static synchronized void stop() { finish(generation); }

    public static synchronized void remember(Map<ResourceLocation, Resource> resources) {
        if (!collecting) return;
        resources.forEach((id, resource) -> SOURCES.put(resource, new Origin(resource.sourcePackId(), id.toString())));
    }

    public static BufferedReader reader(Resource resource, BufferedReader reader) {
        Context previous = CURRENT.get();
        Context current;
        synchronized (DatapackReferences.class) {
            var origin = SOURCES.get(resource);
            if (origin == null) return reader;
            current = new Context(generation, origin);
        }
        CURRENT.set(current);
        return new BufferedReader(reader) {
            @Override public void close() throws IOException {
                try { super.close(); }
                finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
            }
        };
    }

    public static void value(ResourceKey<?> key) { record(key.registry().toString(), key.location().toString(), false); }
    public static void value(ResourceKey<?> key, String identifier) { record(key.location().toString(), identifier, false); }
    public static void tag(ResourceKey<?> key, String identifier) { record(key.location().toString(), identifier, true); }
    private static synchronized void record(String registry, String identifier, boolean tag) {
        Context context = CURRENT.get();
        if (!collecting || context == null || context.generation() != generation) return;
        ResourceLocation parsed = KineticResourceIds.tryParse(identifier);
        if (parsed == null) return;
        REFERENCES.computeIfAbsent(new Reference(registry, parsed.toString(), tag), ignored -> new LinkedHashSet<>()).add(context.origin());
    }

    public static synchronized List<Origin> sources(ResourceKey<?> key, String reason) {
        var match = UNBOUND.matcher(reason);
        if (!match.matches()) return List.of();
        Set<Origin> result = new LinkedHashSet<>();
        for (String id : match.group(2).split(",")) {
            id = id.trim();
            if (id.startsWith("#")) id = id.substring(1);
            result.addAll(REFERENCES.getOrDefault(new Reference(key.location().toString(), id, match.group(1).equals("tags")), Set.of()));
        }
        return new ArrayList<>(result);
    }
}
