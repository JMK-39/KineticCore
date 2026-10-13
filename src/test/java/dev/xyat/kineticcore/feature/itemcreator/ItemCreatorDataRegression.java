package dev.xyat.kineticcore.feature.itemcreator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;

/** Structure and wire validation without Minecraft bootstrap. */
public final class ItemCreatorDataRegression {
    private static final String PREFIX = "dev.xyat.kineticcore.api.itemcreator.";
    public static void main(String[] args) throws Exception {
        Class<?> effect = required("EffectSpec");
        Class<?> attribute = required("AttributeSpec");
        Class<?> operation = required("AttributeSpec$Operation");
        Class<?> template = required("SpawnEggTemplate");
        Class<?> codec = required("ItemCreatorStackData");
        Object speed = effect.getConstructor(String.class, int.class, int.class, boolean.class, boolean.class)
                .newInstance("minecraft:speed", 1, 1200, false, true);
        Object permanent = effect.getConstructor(String.class, int.class, int.class, boolean.class, boolean.class)
                .newInstance("minecraft:strength", 0, -1, true, false);
        Object set = operation.getField("SET").get(null);
        Object health = attribute.getConstructor(String.class, double.class, operation)
                .newInstance("minecraft:generic.max_health", 80D, set);
        List<Object> attributes = new ArrayList<>(List.of(health));
        Object original = template.getConstructor(String.class, List.class, List.class)
                .newInstance("minecraft:zombie", attributes, List.of(speed, permanent));
        attributes.clear();
        require(((List<?>) template.getMethod("attributes").invoke(original)).size() == 1, "template aliases source list");
        CompoundTag tag = (CompoundTag) codec.getMethod("encode", template).invoke(null, original);
        Object restored = codec.getMethod("decode", CompoundTag.class).invoke(null, tag);
        require(original.equals(restored), "template round trip lost parameters");
        tag.putInt("version", 999);
        rejected(() -> codec.getMethod("decode", CompoundTag.class).invoke(null, tag), "unknown version");
        rejected(() -> effect.getConstructor(String.class, int.class, int.class, boolean.class, boolean.class)
                .newInstance("minecraft:speed", -1, 20, true, true), "negative amplifier");
        rejected(() -> effect.getConstructor(String.class, int.class, int.class, boolean.class, boolean.class)
                .newInstance("minecraft:speed", 256, 20, true, true), "amplifier overflow");
        rejected(() -> effect.getConstructor(String.class, int.class, int.class, boolean.class, boolean.class)
                .newInstance("minecraft:speed", 0, 0, true, true), "zero duration");
        rejected(() -> attribute.getConstructor(String.class, double.class, operation)
                .newInstance("minecraft:generic.max_health", Double.NaN, set), "NaN attribute");
        rejected(() -> attribute.getConstructor(String.class, double.class, operation)
                .newInstance("minecraft:generic.max_health", Double.POSITIVE_INFINITY, set), "infinite attribute");
        rejected(() -> template.getConstructor(String.class, List.class, List.class)
                .newInstance("minecraft:zombie", List.of(health, health), List.of(speed)), "duplicate attributes");
        rejected(() -> template.getConstructor(String.class, List.class, List.class)
                .newInstance("minecraft:zombie", List.of(), List.of(speed, speed)), "duplicate effects");
        Class<?> validation = required("ItemCreatorValidation");
        rejected(() -> validation.getMethod("secondsToTicks", double.class).invoke(null, Double.MAX_VALUE), "duration overflow");
        require((int) validation.getMethod("secondsToTicks", double.class).invoke(null, 0.05D) == 1, "one tick duration conversion");
        System.out.println("PASS: item creator data validation and template round trip");
    }
    private static Class<?> required(String name) {
        try { return Class.forName(PREFIX + name); }
        catch (ClassNotFoundException e) { throw new AssertionError("Missing item creator feature: " + name, e); }
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void rejected(Checked action, String name) throws Exception {
        try { action.run(); } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException) return;
            throw e;
        }
        throw new AssertionError("Accepted " + name);
    }
    private interface Checked { void run() throws Exception; }
}
