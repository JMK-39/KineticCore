package dev.xyat.kineticcore.api.itemcreator;

/** One equipped item, using the running Minecraft version's native item NBT/component text. */
public record EquipmentSpec(Slot slot, String item) {
    public static final int MAX_ITEM_LENGTH = 32767;
    public enum Slot { MAINHAND, OFFHAND, HEAD, CHEST, LEGS, FEET }
    public EquipmentSpec {
        if (slot == null || item == null || item.isBlank() || item.length() > MAX_ITEM_LENGTH)
            throw new IllegalArgumentException("Invalid equipment item");
        item = item.trim();
        int end = item.length();
        for (char marker : new char[] {'{', '['}) { int at = item.indexOf(marker); if (at >= 0) end = Math.min(end, at); }
        ItemCreatorValidation.validateId(item.substring(0, end));
    }
}
