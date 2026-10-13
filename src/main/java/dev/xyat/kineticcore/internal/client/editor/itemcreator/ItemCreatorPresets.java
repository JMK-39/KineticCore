package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import java.nio.file.*;
import java.util.*;

/** Local named drafts. Each editor owns one active draft; switching never merges drafts. */
public final class ItemCreatorPresets {
    public record Entry(String id, boolean potion, ItemCreatorDraft.Snapshot draft) { }
    private record Document(int version, String egg, String potion, List<Entry> entries) { }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ItemCreatorPresets instance;
    private final Path path;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
    private String eggId, potionId;
    private boolean dirty;
    private boolean saveFailed;
    public static ItemCreatorPresets local() {
        if (instance == null) instance = new ItemCreatorPresets(KineticPlatform.configDirectory().resolve("kineticcore/item_creator_presets.json"));
        return instance;
    }
    public ItemCreatorPresets(Path path) {
        this.path = path;
        if (Files.exists(path)) try {
            if (Files.size(path) > 8L * 1024 * 1024) throw new IllegalArgumentException("Preset file too large");
            var document = GSON.fromJson(Files.readString(path), Document.class);
            if (document == null || document.version() != 1 || document.entries() == null) throw new IllegalArgumentException("Unsupported preset document");
            for (var entry : document.entries()) {
                UUID.fromString(entry.id()); ItemCreatorDraft.from(entry.draft());
                if (entries.putIfAbsent(entry.id(), entry) != null) throw new IllegalArgumentException("Duplicate preset");
            }
            eggId = document.egg(); potionId = document.potion();
        } catch (Exception error) { throw new IllegalStateException("Cannot read item creator presets: " + path, error); }
        if (!validActive(eggId, false)) eggId = add(false);
        if (!validActive(potionId, true)) potionId = add(true);
    }
    private boolean validActive(String id, boolean potion) { var entry = entries.get(id); return entry != null && entry.potion() == potion; }
    public String activeId(boolean potion) { return potion ? potionId : eggId; }
    public ItemCreatorDraft active(boolean potion) { return ItemCreatorDraft.from(entries.get(activeId(potion)).draft()); }
    public List<Entry> entries(boolean potion) { return entries.values().stream().filter(e -> e.potion() == potion).toList(); }
    private String add(boolean potion) {
        String id = UUID.randomUUID().toString(); entries.put(id, new Entry(id, potion, new ItemCreatorDraft().snapshot())); return id;
    }
    public void create(boolean potion) { activate(potion, add(potion)); }
    public void activate(boolean potion, String id) {
        if (!validActive(id, potion)) throw new IllegalArgumentException("Wrong editor preset");
        if (potion) potionId = id; else eggId = id; dirty = true; save();
    }
    public void retain(boolean potion, ItemCreatorDraft draft) {
        var id = activeId(potion); var next = new Entry(id, potion, draft.snapshot());
        if (!next.equals(entries.get(id))) { entries.put(id, next); dirty = true; }
        if (dirty) save();
    }
    public boolean saveFailed() { return saveFailed; }
    public void save() {
        dirty = true;
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(new Document(1, eggId, potionId, List.copyOf(entries.values()))));
            try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            dirty = false; saveFailed = false;
        } catch (java.io.IOException error) { if (!saveFailed) KineticRuntime.logger().error("Cannot save item creator presets", error); saveFailed = true; }
    }
}
