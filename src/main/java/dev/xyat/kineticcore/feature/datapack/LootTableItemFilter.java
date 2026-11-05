package dev.xyat.kineticcore.feature.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Loot tables of Kinetic data packs load without the entries whose item is missing, for example after a mod was
 * removed, so the rest of the table keeps working; vanilla would drop the whole table. Each table that lost entries
 * is logged once per load.
 */
final class LootTableItemFilter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    //? if >=1.21 {
    /*private static final String FOLDER = "loot_table/";
    *///?} else {
    private static final String FOLDER = "loot_tables/";
    //?}

    private LootTableItemFilter() {
    }

    /** {@code original}, reading through the filter when {@code location} is a loot table file. */
    @Nullable
    static IoSupplier<InputStream> wrap(String pack, ResourceLocation location, @Nullable IoSupplier<InputStream> original) {
        if (original == null || !location.getPath().startsWith(FOLDER) || !location.getPath().endsWith(".json")) return original;
        return () -> filter(pack, location, original);
    }

    private static InputStream filter(String pack, ResourceLocation location, IoSupplier<InputStream> original) throws IOException {
        byte[] bytes;
        try (InputStream in = original.get()) {
            bytes = in.readAllBytes();
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            // Broken JSON is reported by the loot loader as usual.
            return new ByteArrayInputStream(bytes);
        }
        List<String> removed = new ArrayList<>();
        removeMissing(root, removed);
        if (removed.isEmpty()) return new ByteArrayInputStream(bytes);
        LOGGER.warn("[KineticCore] Loot table {} in data pack {}: {} entries left out because their items are missing: {}",
                location, pack, removed.size(), String.join(", ", removed));
        return new ByteArrayInputStream(GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
    }

    /** Removes the item entries with unknown items from every entry list below {@code element}. */
    private static void removeMissing(JsonElement element, List<String> removed) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) removeMissing(child, removed);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        // Pools list "entries"; alternatives, groups and sequences list "children".
        for (String key : List.of("entries", "children")) {
            if (!(object.get(key) instanceof JsonArray array)) continue;
            for (Iterator<JsonElement> iterator = array.iterator(); iterator.hasNext(); ) {
                String missing = missingItem(iterator.next());
                if (missing != null) {
                    removed.add(missing);
                    iterator.remove();
                }
            }
        }
        for (Map.Entry<String, JsonElement> child : object.entrySet()) removeMissing(child.getValue(), removed);
    }

    @Nullable
    private static String missingItem(JsonElement entry) {
        if (!entry.isJsonObject()) return null;
        JsonObject object = entry.getAsJsonObject();
        String type = object.get("type") != null && object.get("type").isJsonPrimitive() ? object.get("type").getAsString() : "";
        if (!type.equals("minecraft:item") && !type.equals("item")) return null;
        if (object.get("name") == null || !object.get("name").isJsonPrimitive()) return null;
        String name = object.get("name").getAsString();
        ResourceLocation id = KineticResourceIds.tryParse(name);
        return id != null && KineticRegistries.items().contains(id) ? null : name;
    }
}
