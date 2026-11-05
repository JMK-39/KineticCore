package dev.xyat.kineticcore.internal.client.editor.itemcreator;

import dev.xyat.kineticcore.api.itemcreator.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemCreatorDraft {
    public String entityId = "minecraft:zombie";
    public KineticItemCreator.PotionKind kind = KineticItemCreator.PotionKind.NORMAL;
    public String name = "";
    private final Map<String, AttributeSpec> attributes = new LinkedHashMap<>();
    private final Map<String, EffectSpec> effects = new LinkedHashMap<>();
    private final Map<EquipmentSpec.Slot, EquipmentSpec> equipment = new java.util.EnumMap<>(EquipmentSpec.Slot.class);
    public void setEquipment(EquipmentSpec.Slot slot, String item) {
        if (item == null || item.isBlank()) equipment.remove(slot);
        else equipment.put(slot, new EquipmentSpec(slot, item));
    }
    public List<EquipmentSpec> equipment() { return List.copyOf(equipment.values()); }
    private final Set<String> selectedAttributes = new LinkedHashSet<>();
    private final Set<String> selectedEffects = new LinkedHashSet<>();
    private final Set<String> pinnedAttributes = new LinkedHashSet<>(), pinnedEffects = new LinkedHashSet<>();
    public void saveAttribute(AttributeSpec spec) { ensureCapacity(selectedAttributes, spec.id()); attributes.put(spec.id(), spec); selectedAttributes.add(spec.id()); pinnedAttributes.add(spec.id()); }
    public void saveEffect(EffectSpec spec) { ensureCapacity(selectedEffects, spec.id()); effects.put(spec.id(), spec); selectedEffects.add(spec.id()); pinnedEffects.add(spec.id()); }
    /** Stores the values of an entry without changing whether it is selected or pinned (inline row edits). */
    public void putAttribute(AttributeSpec spec) { attributes.put(spec.id(), spec); }
    public void putEffect(EffectSpec spec) { effects.put(spec.id(), spec); }
    /** On save: the selected entries are the ones that sort to the top of the catalog from now on. */
    public void pinSelected() {
        pinnedAttributes.clear(); pinnedAttributes.addAll(selectedAttributes);
        pinnedEffects.clear(); pinnedEffects.addAll(selectedEffects);
    }
    /** Nothing chosen yet: default mob, no name, no attribute or effect on the item. */
    public boolean isBlank() {
        return name.isEmpty() && selectedAttributes.isEmpty() && selectedEffects.isEmpty() && equipment.isEmpty()
                && "minecraft:zombie".equals(entityId) && kind == KineticItemCreator.PotionKind.NORMAL;
    }
    public AttributeSpec attribute(String id) { return attributes.getOrDefault(id, new AttributeSpec(id, 0, AttributeSpec.Operation.ADDITION)); }
    public EffectSpec effect(String id) { return effects.getOrDefault(id, new EffectSpec(id, 0, 600, true, true)); }
    public void selectAttribute(String id, boolean selected) { if (selected) { ensureCapacity(selectedAttributes, id); attributes.putIfAbsent(id, attribute(id)); selectedAttributes.add(id); } else selectedAttributes.remove(id); }
    public void selectEffect(String id, boolean selected) { if (selected) { ensureCapacity(selectedEffects, id); effects.putIfAbsent(id, effect(id)); selectedEffects.add(id); } else selectedEffects.remove(id); }
    private static void ensureCapacity(Set<String> selected, String id) { if (!selected.contains(id) && selected.size() >= ItemCreatorValidation.MAX_ENTRIES) throw new IllegalStateException("Too many selected entries"); }
    public boolean isAttributeSelected(String id) { return selectedAttributes.contains(id); }
    public boolean isEffectSelected(String id) { return selectedEffects.contains(id); }
    public boolean isAttributePinned(String id) { return isAttributeSelected(id) && pinnedAttributes.contains(id); }
    public boolean isEffectPinned(String id) { return isEffectSelected(id) && pinnedEffects.contains(id); }
    public List<AttributeSpec> attributes() { return selectedAttributes.stream().map(attributes::get).toList(); }
    public List<EffectSpec> effects() { return selectedEffects.stream().map(effects::get).toList(); }
    public SpawnEggTemplate template() { return new SpawnEggTemplate(entityId, attributes(), effects(), name, equipment()); }
    public record Snapshot(String entityId, KineticItemCreator.PotionKind kind, String name, List<AttributeSpec> attributes,
                           List<EffectSpec> effects, List<String> selectedAttributes, List<String> selectedEffects,
                           List<String> pinnedAttributes, List<String> pinnedEffects, List<EquipmentSpec> equipment) {
        public Snapshot(String entityId, KineticItemCreator.PotionKind kind, String name, List<AttributeSpec> attributes, List<EffectSpec> effects,
                        List<String> selectedAttributes, List<String> selectedEffects, List<String> pinnedAttributes, List<String> pinnedEffects) {
            this(entityId, kind, name, attributes, effects, selectedAttributes, selectedEffects, pinnedAttributes, pinnedEffects, List.of());
        }
        public Snapshot {
            ItemCreatorValidation.validateId(entityId); java.util.Objects.requireNonNull(kind); name = ItemCreatorValidation.validateName(name);
            attributes = List.copyOf(attributes); effects = List.copyOf(effects);
            equipment = List.copyOf(equipment == null ? List.of() : equipment);ItemCreatorValidation.validateEquipment(equipment);
            if (attributes.stream().map(AttributeSpec::id).distinct().count() != attributes.size()
                    || effects.stream().map(EffectSpec::id).distinct().count() != effects.size()) throw new IllegalArgumentException("Duplicate cached entry");
            selectedAttributes = List.copyOf(selectedAttributes); selectedEffects = List.copyOf(selectedEffects);
            pinnedAttributes = List.copyOf(pinnedAttributes == null ? selectedAttributes : pinnedAttributes);
            pinnedEffects = List.copyOf(pinnedEffects == null ? selectedEffects : pinnedEffects);
            if (selectedAttributes.size() > ItemCreatorValidation.MAX_ENTRIES || selectedEffects.size() > ItemCreatorValidation.MAX_ENTRIES
                    || new java.util.HashSet<>(selectedAttributes).size() != selectedAttributes.size()
                    || new java.util.HashSet<>(selectedEffects).size() != selectedEffects.size()) throw new IllegalArgumentException("Invalid selected entries");
            if (!attributes.stream().map(AttributeSpec::id).toList().containsAll(selectedAttributes)
                    || !effects.stream().map(EffectSpec::id).toList().containsAll(selectedEffects)
                    || !attributes.stream().map(AttributeSpec::id).toList().containsAll(pinnedAttributes)
                    || !effects.stream().map(EffectSpec::id).toList().containsAll(pinnedEffects)) throw new IllegalArgumentException("Unknown selected entry");
        }
    }
    public Snapshot snapshot() { return new Snapshot(entityId, kind, name, List.copyOf(attributes.values()), List.copyOf(effects.values()), List.copyOf(selectedAttributes), List.copyOf(selectedEffects), List.copyOf(pinnedAttributes), List.copyOf(pinnedEffects), equipment()); }
    public static ItemCreatorDraft from(Snapshot snapshot) {
        var draft = new ItemCreatorDraft(); draft.entityId = snapshot.entityId(); draft.kind = snapshot.kind(); draft.name = snapshot.name();
        snapshot.attributes().forEach(spec -> draft.attributes.put(spec.id(), spec)); snapshot.effects().forEach(spec -> draft.effects.put(spec.id(), spec));
        snapshot.equipment().forEach(spec -> draft.equipment.put(spec.slot(), spec));
        draft.selectedAttributes.addAll(snapshot.selectedAttributes()); draft.selectedEffects.addAll(snapshot.selectedEffects());
        draft.pinnedAttributes.addAll(snapshot.pinnedAttributes()); draft.pinnedEffects.addAll(snapshot.pinnedEffects()); return draft;
    }
    public ItemCreatorDraft copy() { return from(snapshot()); }
}
