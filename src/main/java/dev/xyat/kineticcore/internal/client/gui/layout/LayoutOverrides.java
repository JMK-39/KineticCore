package dev.xyat.kineticcore.internal.client.gui.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xyat.kineticcore.api.runtime.KineticPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Player-edited GUI layouts: {@code config/kineticcore/gui-layouts/<modid>.json}, one file per mod, grouped by page:
 * {@code {"<page class>": {"<key>": [x, y, width, height, defaultX, defaultY, defaultWidth, defaultHeight]}}}. Only
 * controls and regions moved or resized away from their defaults are stored; deleting a page entry or the file restores
 * the defaults. The stored defaults detect layouts made stale by a mod update: when the code's default changed, the
 * entry is ignored (and dropped on the next save) instead of misplacing the control. Edits stay in memory until saved.
 */
public final class LayoutOverrides {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    // modid -> page -> key -> rect
    private static final Map<String, Map<String, Map<String, int[]>>> LOADED = new HashMap<>();
    private static final Set<String> DIRTY = new HashSet<>();
    // Mods with stale entries removed in memory: written on the next save without counting as an unsaved edit.
    private static final Set<String> PRUNED = new HashSet<>();
    private static final Set<String> STALE = new HashSet<>();

    private LayoutOverrides() {
    }

    /** The mod a page class belongs to: dev.xyat.(modid).… for Kinetic mods, otherwise the second package segment. */
    static String modId(String pageClass) {
        String[] parts = pageClass.split("\\.");
        if (parts.length > 3 && parts[0].equals("dev") && parts[1].equals("xyat")) return parts[2];
        return parts.length > 1 ? parts[1] : parts[0];
    }

    private static Path file(String modId) {
        return KineticPaths.configFile("kineticcore/gui-layouts/" + modId + ".json");
    }

    private static Map<String, Map<String, int[]>> mod(String modId) {
        return LOADED.computeIfAbsent(modId, LayoutOverrides::read);
    }

    private static Map<String, Map<String, int[]>> read(String modId) {
        Map<String, Map<String, int[]>> pages = new TreeMap<>();
        Path path = file(modId);
        if (!Files.isRegularFile(path)) return pages;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> page : root.entrySet()) {
                Map<String, int[]> controls = new TreeMap<>();
                for (Map.Entry<String, JsonElement> control : page.getValue().getAsJsonObject().entrySet()) {
                    JsonArray rect = control.getValue().getAsJsonArray();
                    if (rect.size() != 4 && rect.size() != 8) continue;
                    int[] values = new int[rect.size()];
                    for (int index = 0; index < values.length; index++) values[index] = rect.get(index).getAsInt();
                    controls.put(control.getKey(), values);
                }
                if (!controls.isEmpty()) pages.put(page.getKey(), controls);
            }
        } catch (RuntimeException | IOException invalid) {
            // A damaged file must not break the GUI: its pages fall back to the code defaults.
        }
        return pages;
    }

    /**
     * The stored rect for a control or region, or null when it uses its default. An entry saved against different
     * defaults (the mod changed its layout since) is stale: it is ignored and removed.
     */
    public static int[] get(String page, String key, int[] defaults) {
        String modId = modId(page);
        Map<String, int[]> controls = mod(modId).get(page);
        int[] rect = controls == null ? null : controls.get(key);
        if (rect == null) return null;
        if (rect.length == 8 && defaults != null && !java.util.Arrays.equals(java.util.Arrays.copyOfRange(rect, 4, 8), defaults)) {
            controls.remove(key);
            if (controls.isEmpty()) mod(modId).remove(page);
            PRUNED.add(modId);
            STALE.add(page + "|" + key);
            return null;
        }
        return java.util.Arrays.copyOf(rect, 4);
    }

    /** Whether a stored rect exists for the key (for edit-mode colouring). */
    public static boolean has(String page, String key) {
        Map<String, int[]> controls = mod(modId(page)).get(page);
        return controls != null && controls.containsKey(key);
    }

    /** Stale entries found since the last call (for one notice when edit mode starts). */
    public static int takeStaleCount() {
        int count = STALE.size();
        STALE.clear();
        return count;
    }

    /** Stores a rect, or forgets it when it equals the default. Not written until {@link #save()}. */
    public static void put(String page, String key, int[] rect, int[] defaults) {
        String modId = modId(page);
        Map<String, Map<String, int[]>> pages = mod(modId);
        if (java.util.Arrays.equals(rect, defaults)) {
            Map<String, int[]> controls = pages.get(page);
            if (controls != null && controls.remove(key) != null) {
                if (controls.isEmpty()) pages.remove(page);
                DIRTY.add(modId);
            }
            return;
        }
        int[] stored = java.util.Arrays.copyOf(rect, 8);
        System.arraycopy(defaults, 0, stored, 4, 4);
        pages.computeIfAbsent(page, ignored -> new TreeMap<>()).put(key, stored);
        DIRTY.add(modId);
    }

    /** Whether there are unsaved edits. */
    public static boolean dirty() {
        return !DIRTY.isEmpty();
    }

    /** Writes every edited mod file; mods without overrides left have their file removed. */
    public static void save() throws IOException {
        Set<String> mods = new HashSet<>(DIRTY);
        mods.addAll(PRUNED);
        for (String modId : mods) {
            Map<String, Map<String, int[]>> pages = mod(modId);
            Path path = file(modId);
            if (pages.isEmpty()) {
                Files.deleteIfExists(path);
                continue;
            }
            JsonObject root = new JsonObject();
            for (Map.Entry<String, Map<String, int[]>> page : pages.entrySet()) {
                JsonObject controls = new JsonObject();
                for (Map.Entry<String, int[]> control : page.getValue().entrySet()) {
                    JsonArray rect = new JsonArray();
                    for (int value : control.getValue()) rect.add(value);
                    controls.add(control.getKey(), rect);
                }
                root.add(page.getKey(), controls);
            }
            KineticPaths.writeConfigText("kineticcore/gui-layouts/" + modId + ".json", GSON.toJson(root));
        }
        DIRTY.clear();
        PRUNED.clear();
    }

    /** Drops unsaved edits; the next lookup reads the files again. */
    public static void discard() {
        LOADED.clear();
        DIRTY.clear();
        PRUNED.clear();
    }

    /** Pages of a mod with stored overrides (for tests and diagnostics). */
    static Map<String, Map<String, int[]>> snapshot(String modId) {
        return new LinkedHashMap<>(mod(modId));
    }
}
