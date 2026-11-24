package dev.xyat.kineticcore.api.minecraft;

import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Exercises the public layering contract; only the loader's field accessor is supplied by the fixture. */
public final class AttributeRangeRegression {
    public static void main(String[] args) {
        loadOrderAndNewestBase();
        priorityAndOwnerTies();
        atomicReplacementAndValidation();
        System.out.println("Attribute range regression passed.");
    }

    private static void loadOrderAndNewestBase() {
        var first = attribute();
        var second = attribute();
        var owner = id("load_order");
        MinecraftAttributes.setRange(first, -10, 100);
        changed(Set.of(first, second), MinecraftAttributes.replaceRangeOverrides(owner, 1000,
                Map.of(first, range(-2, 40), second, range(-2, 40))));
        MinecraftAttributes.setRange(second, -10, 100);
        bounds(first, -2, 40);
        bounds(second, -2, 40);
        if (!MinecraftAttributes.baseRange(second).equals(range(-10, 100))) throw new AssertionError("override leaked into base");
        MinecraftAttributes.setRange(first, -20, 200);
        bounds(first, -2, 40);
        changed(Set.of(first, second), MinecraftAttributes.replaceRangeOverrides(owner, 1000, Map.of()));
        bounds(first, -20, 200);
        bounds(second, -10, 100);
    }

    private static void priorityAndOwnerTies() {
        var attribute = attribute();
        var a = id("a");
        var z = id("z");
        MinecraftAttributes.replaceRangeOverrides(z, 10, Map.of(attribute, range(0, 80)));
        MinecraftAttributes.replaceRangeOverrides(a, 10, Map.of(attribute, range(0, 70)));
        bounds(attribute, 0, 70);
        changed(Set.of(), MinecraftAttributes.replaceRangeOverrides(z, 9, Map.of(attribute, range(0, 90))));
        MinecraftAttributes.replaceRangeOverrides(z, 11, Map.of(attribute, range(0, 90)));
        bounds(attribute, 0, 90);
        MinecraftAttributes.replaceRangeOverrides(z, 11, Map.of());
        bounds(attribute, 0, 70);
        MinecraftAttributes.replaceRangeOverrides(a, 10, Map.of());
        // Reverse insertion order must choose the same lexicographic winner.
        MinecraftAttributes.replaceRangeOverrides(a, 10, Map.of(attribute, range(0, 70)));
        MinecraftAttributes.replaceRangeOverrides(z, 10, Map.of(attribute, range(0, 80)));
        bounds(attribute, 0, 70);
        MinecraftAttributes.replaceRangeOverrides(a, 10, Map.of());
        MinecraftAttributes.replaceRangeOverrides(z, 10, Map.of());
        bounds(attribute, 0, 20);
    }

    private static void atomicReplacementAndValidation() {
        expect(IllegalArgumentException.class, () -> range(Double.NaN, 1));
        expect(IllegalArgumentException.class, () -> range(0, Double.NaN));
        expect(IllegalArgumentException.class, () -> range(2, 1));
        range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        var first = attribute();
        var second = attribute();
        var owner = id("atomic");
        Map<RangedAttribute, MinecraftAttributes.Range> original = new LinkedHashMap<>();
        original.put(first, range(0, 50));
        MinecraftAttributes.replaceRangeOverrides(owner, 5, original);
        original.clear();
        bounds(first, 0, 50);
        Map<RangedAttribute, MinecraftAttributes.Range> invalid = new LinkedHashMap<>();
        invalid.put(first, range(0, 60));
        invalid.put(second, null);
        expect(NullPointerException.class, () -> MinecraftAttributes.replaceRangeOverrides(owner, 99, invalid));
        bounds(first, 0, 50);
        bounds(second, 0, 20);
        invalid.clear();
        invalid.put(null, range(0, 60));
        expect(NullPointerException.class, () -> MinecraftAttributes.replaceRangeOverrides(owner, 99, invalid));
        expect(NullPointerException.class, () -> MinecraftAttributes.replaceRangeOverrides(null, 1, Map.of()));
        expect(NullPointerException.class, () -> MinecraftAttributes.replaceRangeOverrides(owner, 1, null));
        expect(IllegalArgumentException.class, () -> MinecraftAttributes.setRange(first, 80, 10));
        bounds(first, 0, 50);
        changed(Set.of(first, second), MinecraftAttributes.replaceRangeOverrides(owner, 5, Map.of(second, range(0, 60))));
        bounds(first, 0, 20);
        bounds(second, 0, 60);
        MinecraftAttributes.replaceRangeOverrides(owner, 5, Map.of());
    }

    static TestAttribute attribute() {
        //? if >=1.20.5 {
        /*// Attribute's codec needs empty registry metadata on newer Minecraft. These checks run in their
        // own JVM and do not need bootstrap contents or a mod loading context.
        try {
            Field bootstrapped = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
            bootstrapped.setAccessible(true);
            bootstrapped.setBoolean(null, true);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        *///?}
        return new TestAttribute();
    }
    private static ResourceLocation id(String path) { return KineticResourceIds.of("range_regression", path); }
    private static MinecraftAttributes.Range range(double min, double max) { return new MinecraftAttributes.Range(min, max); }
    static void bounds(RangedAttribute attribute, double min, double max) {
        if (attribute.getMinValue() != min || attribute.getMaxValue() != max) throw new AssertionError("bounds: " + attribute.getMinValue() + ".." + attribute.getMaxValue() + ", expected " + min + ".." + max);
    }
    private static void changed(Set<RangedAttribute> expected, Set<RangedAttribute> actual) {
        if (!expected.equals(actual)) throw new AssertionError("changed attributes differ");
    }
    static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong failure", failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    static final class TestAttribute extends RangedAttribute implements MinecraftAttributes.RangeAccess {
        TestAttribute() { super("range.regression", 10, 0, 20); }
        public void kineticcore$setMinValue(double value) { write("minValue", value); }
        public void kineticcore$setMaxValue(double value) { write("maxValue", value); }
        private void write(String name, double value) {
            try { Field field = RangedAttribute.class.getDeclaredField(name); field.setAccessible(true); field.setDouble(this, value); }
            catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }
    }
}
