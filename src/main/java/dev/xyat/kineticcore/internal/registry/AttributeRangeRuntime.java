package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.minecraft.MinecraftAttributes.Range;
import dev.xyat.kineticcore.api.minecraft.MinecraftAttributes.RangeAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Serializes base and owner-layer updates, including the shared definitions in an integrated server. */
public final class AttributeRangeRuntime {
    private static final Map<RangedAttribute, Range> BASES = new IdentityHashMap<>();
    private static final Map<ResourceLocation, Layer> LAYERS = new HashMap<>();

    private AttributeRangeRuntime() {
    }

    public static synchronized Range baseRange(RangedAttribute attribute) {
        Objects.requireNonNull(attribute, "attribute");
        Range base = BASES.get(attribute);
        return base != null ? base : current(attribute);
    }

    public static synchronized void setBase(RangedAttribute attribute, Range range) {
        requireWritable(attribute);
        BASES.put(attribute, range);
        write(attribute, effective(attribute));
    }

    public static synchronized Set<RangedAttribute> replace(ResourceLocation owner, int priority, Map<RangedAttribute, Range> ranges) {
        Objects.requireNonNull(owner, "owner");
        // Finish validation and defensive copying before touching a layer, baseline or vanilla field.
        Map<RangedAttribute, Range> replacement = Map.copyOf(Objects.requireNonNull(ranges, "ranges"));
        replacement.keySet().forEach(AttributeRangeRuntime::requireWritable);
        Set<RangedAttribute> affected = new HashSet<>(replacement.keySet());
        Layer previous = LAYERS.get(owner);
        if (previous != null) affected.addAll(previous.ranges().keySet());
        Map<RangedAttribute, Range> before = new IdentityHashMap<>();
        for (RangedAttribute attribute : affected) {
            Range current = current(attribute);
            before.put(attribute, current);
            BASES.putIfAbsent(attribute, current);
        }
        if (replacement.isEmpty()) LAYERS.remove(owner);
        else LAYERS.put(owner, new Layer(priority, replacement));
        Set<RangedAttribute> changed = new HashSet<>();
        for (RangedAttribute attribute : affected) {
            Range next = effective(attribute);
            if (!next.equals(before.get(attribute))) {
                write(attribute, next);
                changed.add(attribute);
            }
        }
        return Set.copyOf(changed);
    }

    private static Range effective(RangedAttribute attribute) {
        ResourceLocation winner = null;
        Layer winningLayer = null;
        for (var entry : LAYERS.entrySet()) {
            Layer candidate = entry.getValue();
            if (!candidate.ranges().containsKey(attribute)) continue;
            if (winningLayer == null || candidate.priority() > winningLayer.priority()
                    || (candidate.priority() == winningLayer.priority()
                    && entry.getKey().toString().compareTo(winner.toString()) < 0)) {
                winner = entry.getKey();
                winningLayer = candidate;
            }
        }
        return winningLayer == null ? BASES.get(attribute) : winningLayer.ranges().get(attribute);
    }

    private static Range current(RangedAttribute attribute) {
        return new Range(attribute.getMinValue(), attribute.getMaxValue());
    }

    private static void requireWritable(RangedAttribute attribute) {
        Objects.requireNonNull(attribute, "attribute");
        if (!(attribute instanceof RangeAccess)) throw new IllegalArgumentException("Attribute range accessor is unavailable");
    }

    private static void write(RangedAttribute attribute, Range range) {
        RangeAccess access = (RangeAccess) attribute;
        access.kineticcore$setMinValue(range.minimum());
        access.kineticcore$setMaxValue(range.maximum());
    }

    private record Layer(int priority, Map<RangedAttribute, Range> ranges) {
    }
}
