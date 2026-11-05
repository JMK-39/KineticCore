package dev.xyat.kineticcore.feature.itemcreator;

import dev.xyat.kineticcore.api.itemcreator.EffectSpec;
import java.util.List;

public final class ItemCreatorDraftRegression {
    public static void main(String[] args) throws Exception {
        Class<?> type;
        try { type = Class.forName("dev.xyat.kineticcore.internal.client.editor.itemcreator.ItemCreatorDraft"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Missing transactional item creator draft", e); }
        Object draft = type.getConstructor().newInstance();
        type.getMethod("selectEffect", String.class, boolean.class).invoke(draft, "test:unsaved", true);
        if ((boolean)type.getMethod("isEffectPinned", String.class).invoke(draft, "test:unsaved")) throw new AssertionError("Selection pinned before Save");
        type.getMethod("selectEffect", String.class, boolean.class).invoke(draft, "test:unsaved", false);
        EffectSpec effect = new EffectSpec("minecraft:speed", 2, 600, false, true);
        type.getMethod("saveEffect", EffectSpec.class).invoke(draft, effect);
        if (!(boolean)type.getMethod("isEffectPinned", String.class).invoke(draft, effect.id())) throw new AssertionError("Save did not pin selected effect");
        if (!type.getMethod("effects").invoke(draft).equals(List.of(effect))) throw new AssertionError("Save did not select effect");
        type.getMethod("selectEffect", String.class, boolean.class).invoke(draft, effect.id(), false);
        if (!type.getMethod("effects").invoke(draft).equals(List.of())) throw new AssertionError("Deselection left effect enabled");
        if (!type.getMethod("effect", String.class).invoke(draft, effect.id()).equals(effect)) throw new AssertionError("Deselection lost edited parameters");
        type.getMethod("selectEffect", String.class, boolean.class).invoke(draft, effect.id(), true);
        if (!type.getMethod("effects").invoke(draft).equals(List.of(effect))) throw new AssertionError("Reselection lost parameters");
        type.getField("name").set(draft, "测试药水");
        var copy = type.getMethod("copy").invoke(draft);
        type.getMethod("selectEffect", String.class, boolean.class).invoke(copy, effect.id(), false);
        if (!type.getMethod("effects").invoke(draft).equals(List.of(effect))) throw new AssertionError("Draft copies alias selection");
        if (!type.getField("name").get(copy).equals("测试药水")) throw new AssertionError("Copy lost name");
        for (int i = 0; i < 129; i++) {
            String id = "test:effect_" + i;
            type.getMethod("saveEffect", EffectSpec.class).invoke(draft, new EffectSpec(id, 0, 600, true, true));
            type.getMethod("selectEffect", String.class, boolean.class).invoke(draft, id, false);
        }
        type.getMethod("copy").invoke(draft);
        var file = java.nio.file.Files.createTempFile("kinetic-itemcreator-presets-", ".json"); java.nio.file.Files.delete(file);
        var storeType = Class.forName("dev.xyat.kineticcore.internal.client.editor.itemcreator.ItemCreatorPresets");
        var store = storeType.getConstructor(java.nio.file.Path.class).newInstance(file);
        var save = storeType.getMethod("save", boolean.class, String.class, type);
        var entries = storeType.getMethod("entries", boolean.class);
        if (storeType.getMethod("activeId", boolean.class).invoke(store, true) != null) throw new AssertionError("Empty store opened a record");
        String first = (String)save.invoke(store, true, null, draft);
        String second = (String)save.invoke(store, true, null, copy);
        if (first.equals(second)) throw new AssertionError("Saving a new draft replaced the original record");
        save.invoke(store, true, first, draft);
        var reopened = storeType.getConstructor(java.nio.file.Path.class).newInstance(file);
        List<?> listed = (List<?>)entries.invoke(reopened, true);
        if (listed.size() != 2) throw new AssertionError("Records did not survive reopening");
        var top = listed.get(0);
        if (!top.getClass().getMethod("id").invoke(top).equals(first)) throw new AssertionError("Saved record not moved to the top");
        if (!first.equals(storeType.getMethod("activeId", boolean.class).invoke(reopened, true))) throw new AssertionError("Last saved record not reopened");
        var snapshot = top.getClass().getMethod("draft").invoke(top);
        var restored = type.getMethod("from", snapshot.getClass()).invoke(null, snapshot);
        if (!type.getMethod("snapshot").invoke(restored).equals(type.getMethod("snapshot").invoke(draft))) throw new AssertionError("Record lost named draft or selection");
        if (!((List<?>)entries.invoke(reopened, false)).isEmpty()) throw new AssertionError("Potion records leaked into the egg editor");
        storeType.getMethod("delete", String.class).invoke(reopened, first);
        if (storeType.getMethod("activeId", boolean.class).invoke(reopened, true) != null || ((List<?>)entries.invoke(reopened, true)).size() != 1) throw new AssertionError("Delete left the record");
        java.nio.file.Files.delete(file);
        var retryDirectory = java.nio.file.Files.createTempDirectory("kinetic-presets-retry-");
        var blocker = retryDirectory.resolve("blocked"); java.nio.file.Files.writeString(blocker, "not a directory");
        var retryFile = blocker.resolve("presets.json"); var retryStore = storeType.getConstructor(java.nio.file.Path.class).newInstance(retryFile);
        String retryId = (String)save.invoke(retryStore, true, null, draft);
        if (!(boolean)storeType.getMethod("saveFailed").invoke(retryStore)) throw new AssertionError("Write failure not reported");
        java.nio.file.Files.delete(blocker); java.nio.file.Files.createDirectory(blocker); save.invoke(retryStore, true, retryId, draft);
        var retried = storeType.getConstructor(java.nio.file.Path.class).newInstance(retryFile);
        if (((List<?>)entries.invoke(retried, true)).size() != 1) throw new AssertionError("Record not written after storage recovered");
        java.nio.file.Files.delete(retryFile); java.nio.file.Files.delete(blocker); java.nio.file.Files.delete(retryDirectory);
        System.out.println("PASS: item creator draft retains parameters independently of selection");
    }
}
