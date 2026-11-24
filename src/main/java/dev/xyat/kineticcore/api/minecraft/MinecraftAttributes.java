package dev.xyat.kineticcore.api.minecraft;

import dev.xyat.kineticcore.internal.registry.AttributeRangeRuntime;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/** Helpers for vanilla attribute definitions. */
public final class MinecraftAttributes {
    private MinecraftAttributes() {
    }

    /** Inclusive range bounds; infinite bounds are supported, but NaN and reversed bounds are invalid. */
    public record Range(double minimum, double maximum) {
        /** Validates bounds without restricting existing infinite ranges. */
        public Range {
            if (Double.isNaN(minimum) || Double.isNaN(maximum) || minimum > maximum) {
                throw new IllegalArgumentException("Invalid attribute range: " + minimum + ".." + maximum);
            }
        }
    }

    /** Returns the current base range, independently of any owner override. */
    public static Range baseRange(RangedAttribute attribute) {
        return AttributeRangeRuntime.baseRange(attribute);
    }

    /**
     * Validates and replaces one owner's complete layer atomically. An empty map removes that layer.
     * Highest numeric priority wins; ties choose the lexicographically smallest full owner ID.
     * Returns the attributes whose effective bounds changed. Values are copied, so subsequent changes to
     * the input map have no effect. Existing entity caches require {@code KineticEntityAttributes.invalidate}.
     */
    public static Set<RangedAttribute> replaceRangeOverrides(ResourceLocation owner, int priority, Map<RangedAttribute, Range> ranges) {
        return AttributeRangeRuntime.replace(owner, priority, ranges);
    }

    /**
     * Writable range of a {@code RangedAttribute}. Implemented by a KineticCore mixin; add-ons call the static
     * helpers instead.
     */
    public interface RangeAccess {
        /** Replaces the attribute's minimum value. */
        void kineticcore$setMinValue(double minValue);

        /** Replaces the attribute's maximum value. */
        void kineticcore$setMaxValue(double maxValue);
    }

    /**
     * Updates the base range of an attribute, for example to lift vanilla's armor cap. Active owner layers
     * retain priority; removing them restores the newest base range. Affects every entity definition;
     * existing entity caches require {@code KineticEntityAttributes.invalidate}.
     *
     * @param attribute attribute to change
     * @param minimum new minimum value
     * @param maximum new maximum value
     */
    public static void setRange(RangedAttribute attribute, double minimum, double maximum) {
        AttributeRangeRuntime.setBase(attribute, new Range(minimum, maximum));
    }
}
