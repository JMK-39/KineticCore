package dev.xyat.kineticcore.api.itemcreator;

/**
 * One effect edit, independent of its registry holder and game version.
 * @param id exact mob-effect registry ID
 * @param amplifier zero-based level, 0–255
 * @param durationTicks positive duration, or -1 for permanent
 * @param particles whether particles appear
 * @param icon whether the active-effect icon appears
 */
public record EffectSpec(String id, int amplifier, int durationTicks, boolean particles, boolean icon) {
    public EffectSpec {
        ItemCreatorValidation.validateId(id);
        if (amplifier < 0 || amplifier > 255) throw new IllegalArgumentException("Effect level must be 1–256");
        if (durationTicks != -1 && durationTicks <= 0) throw new IllegalArgumentException("Duration must be positive or permanent");
    }
}
