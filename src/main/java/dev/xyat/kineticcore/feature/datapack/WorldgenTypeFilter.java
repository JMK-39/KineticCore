package dev.xyat.kineticcore.feature.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.registry.KineticRegistryView;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structures and structure sets of Kinetic data packs whose type no installed mod registers, for example after the
 * mod that adds the type was removed, are left out of the pack so the world still loads: vanilla would refuse to
 * load the world's data packs at all. A structure set is left out with its unknown placement type, so the structure
 * it names (left out the same way) is never asked for. Each file left out is logged once.
 */
final class WorldgenTypeFilter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STRUCTURES = "worldgen/structure/", SETS = "worldgen/structure_set/";
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private WorldgenTypeFilter() {
    }

    /** Whether the file is a structure or structure set of a type nothing registers. */
    static boolean hidden(String pack, ResourceLocation location, @Nullable IoSupplier<InputStream> file) {
        String path = location.getPath();
        boolean structure = path.startsWith(STRUCTURES);
        if (file == null || !path.endsWith(".json") || !structure && !path.startsWith(SETS)) return false;
        String type = type(file, structure);
        if (type == null) return false;
        String registry = structure ? "worldgen/structure_type" : "worldgen/structure_placement";
        Optional<KineticRegistryView<Object>> types = KineticRegistries.custom(KineticResourceIds.of("minecraft", registry));
        ResourceLocation id = KineticResourceIds.tryParse(type);
        if (types.isEmpty() || id == null || types.get().contains(id)) return false;
        if (REPORTED.add(pack + "/" + location)) {
            LOGGER.warn("[KineticCore] {} {} in data pack {} is left out: its type {} is not registered (is the mod that adds it installed?)",
                    structure ? "Structure" : "Structure set", location, pack, type);
        }
        return true;
    }

    /** The structure's {@code type}, or the set's {@code placement.type}; {@code null} if the file does not say. */
    @Nullable
    private static String type(IoSupplier<InputStream> file, boolean structure) {
        try (InputStream in = file.get(); Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return null;
            JsonObject json = root.getAsJsonObject();
            if (!structure) {
                if (!json.has("placement") || !json.get("placement").isJsonObject()) return null;
                json = json.getAsJsonObject("placement");
            }
            return json.has("type") && json.get("type").isJsonPrimitive() ? json.get("type").getAsString() : null;
        } catch (Exception e) {
            // Unreadable files are left to vanilla, which reports them.
            return null;
        }
    }
}
