package dev.xyat.kineticcore.api.itemcreator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** World-independent structural checks for editor drafts and bounded protocol data. */
public final class ItemCreatorValidation {
    public static final int MAX_ENTRIES = 128;
    public static final int MAX_ID_LENGTH = 256;
    public static final int MAX_NAME_LENGTH = 128;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private ItemCreatorValidation() { }
    public static String validateName(String name) {
        if (name == null || name.length() > MAX_NAME_LENGTH || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid item name");
        return name.trim();
    }
    public static void validateId(String id) {
        if (id == null || id.length() > MAX_ID_LENGTH || !ID.matcher(id).matches())
            throw new IllegalArgumentException("Invalid registry ID");
    }
    public static void validateTemplate(SpawnEggTemplate template) {
        validateId(template.entityId());
        validateAttributes(template.attributes());
        validateEffects(template.effects());
        validateEquipment(template.equipment());
    }
    public static void validateEquipment(List<EquipmentSpec> entries) {
        if (entries.size() > EquipmentSpec.Slot.values().length) throw new IllegalArgumentException("Too many equipment slots");
        Set<EquipmentSpec.Slot> slots = new HashSet<>();
        for (var entry : entries) if (!slots.add(entry.slot())) throw new IllegalArgumentException("Duplicate equipment slot");
    }
    public static void validateAttributes(List<AttributeSpec> entries) {
        unique(entries.stream().map(AttributeSpec::id).toList());
    }
    public static void validateEffects(List<EffectSpec> entries) {
        unique(entries.stream().map(EffectSpec::id).toList());
    }
    private static void unique(List<String> ids) {
        if (ids.size() > MAX_ENTRIES) throw new IllegalArgumentException("Too many entries");
        Set<String> seen = new HashSet<>();
        for (String id : ids) if (!seen.add(id)) throw new IllegalArgumentException("Duplicate ID: " + id);
    }
    public static int secondsToTicks(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds * 20 > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Duration is out of range");
        return (int) Math.max(1, Math.round(seconds * 20));
    }
}
