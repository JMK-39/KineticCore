package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.xyat.kineticcore.api.runtime.KineticPaths;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import java.nio.file.*;
import java.util.*;

/**
 * Saved potion and spawn-egg records, kept on this client in config/kineticcore/core/item_creator_presets.json. The editor
 * lists them on its left, newest first; only Save writes a record, so leaving the editor never stores half edits.
 */
public final class ItemCreatorPresets {
    public record Entry(String id, boolean potion, ItemCreatorDraft.Snapshot draft) { }
    private record Document(int version, String egg, String potion, List<Entry> entries) { }
    private static final int VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ItemCreatorPresets instance;
    private final Path path;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
    /** The record each editor showed last, or null for a new unsaved draft. */
    private String eggId, potionId;
    private boolean saveFailed;

    public static ItemCreatorPresets local() {
        if (instance == null) {
            KineticPaths.migrateConfigFile("kineticcore/item_creator_presets.json", "kineticcore/core/item_creator_presets.json");
            instance = new ItemCreatorPresets(KineticPaths.configFile("kineticcore/core/item_creator_presets.json"));
        }
        return instance;
    }

    public ItemCreatorPresets(Path path) {
        this.path = path;
        if (!Files.exists(path)) return;
        try {
            if (Files.size(path) > 8L * 1024 * 1024) throw new IllegalArgumentException("Preset file too large");
            var document = GSON.fromJson(Files.readString(path), Document.class);
            if (document == null || document.version() < 1 || document.version() > VERSION || document.entries() == null)
                throw new IllegalArgumentException("Unsupported preset document");
            for (var entry : document.entries()) {
                UUID.fromString(entry.id());
                var draft = ItemCreatorDraft.from(entry.draft());
                // Version 1 stored an empty draft for every opened editor; those are not records anyone saved.
                if (document.version() == 1 && draft.isBlank()) continue;
                if (entries.putIfAbsent(entry.id(), entry) != null) throw new IllegalArgumentException("Duplicate preset");
            }
            eggId = valid(document.egg(), false) ? document.egg() : null;
            potionId = valid(document.potion(), true) ? document.potion() : null;
        } catch (Exception error) { throw new IllegalStateException("Cannot read item creator presets: " + path, error); }
    }

    private boolean valid(String id, boolean potion) { var entry = id == null ? null : entries.get(id); return entry != null && entry.potion() == potion; }

    /** The record the editor showed last, or null. */
    public String activeId(boolean potion) { return potion ? potionId : eggId; }

    public Entry entry(String id) { return id == null ? null : entries.get(id); }

    /** Records of one editor, newest first. */
    public List<Entry> entries(boolean potion) { return entries.values().stream().filter(e -> e.potion() == potion).toList(); }

    /** Remembers which record the editor shows, so it opens there next time; null for a new draft. */
    public void setActive(boolean potion, String id) {
        if (id != null && !valid(id, potion)) throw new IllegalArgumentException("Wrong editor record");
        if (potion) potionId = id; else eggId = id;
        write();
    }

    /** Saves the draft as a record (a new one when id is null or unknown), moves it to the top and returns its id. */
    public String save(boolean potion, String id, ItemCreatorDraft draft) {
        String key = valid(id, potion) ? id : UUID.randomUUID().toString();
        var updated = new Entry(key, potion, draft.snapshot());
        var reordered = new LinkedHashMap<String, Entry>();
        reordered.put(key, updated);
        entries.forEach((existing, entry) -> { if (!existing.equals(key)) reordered.put(existing, entry); });
        entries.clear();
        entries.putAll(reordered);
        if (potion) potionId = key; else eggId = key;
        write();
        return key;
    }

    /** Copies a record to a new one at the top and returns the new id. */
    public String duplicate(String id) {
        var entry = entries.get(id);
        if (entry == null) throw new IllegalArgumentException("Unknown record");
        return save(entry.potion(), null, ItemCreatorDraft.from(entry.draft()));
    }

    public void delete(String id) {
        if (entries.remove(id) == null) return;
        if (id.equals(potionId)) potionId = null;
        if (id.equals(eggId)) eggId = null;
        write();
    }

    public boolean saveFailed() { return saveFailed; }

    private void write() {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(new Document(VERSION, eggId, potionId, List.copyOf(entries.values()))));
            try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            saveFailed = false;
        } catch (java.io.IOException error) { if (!saveFailed) KineticRuntime.logger().error("Cannot save item creator presets", error); saveFailed = true; }
    }
}
