package dev.xyat.kineticcore.feature.spawnegg;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Native SpawnData replacement retains spawner tuning and extension state without aliasing either input. */
public final class NativeSpawnEggDataRegression {
    public static void main(String[] args) {
        CompoundTag original = new CompoundTag();
        for (String key : new String[] {"Delay", "MinSpawnDelay", "MaxSpawnDelay", "SpawnCount", "MaxNearbyEntities", "RequiredPlayerRange", "SpawnRange"}) {
            original.putShort(key, (short) 37);
        }
        original.putLong("entitycontrolCooldownEnd", 12_345_678_901L);
        original.putInt("entitycontrolSpawnCount", 7);
        CompoundTag data = new CompoundTag();
        CompoundTag rules = new CompoundTag();
        rules.putString("fixture", "keep");
        data.put("custom_spawn_rules", rules);
        CompoundTag previousEntity = new CompoundTag();
        previousEntity.putString("id", "minecraft:pig");
        data.put("entity", previousEntity);
        original.put("SpawnData", data);
        original.put("SpawnPotentials", new ListTag());
        CompoundTag originalCopy = original.copy();
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:cow");
        entity.putFloat("Health", 60);
        CompoundTag result = NativeSpawnEggData.withEntityData(original, entity);
        require(original.equals(originalCopy), "input spawner state was mutated");
        //? if >=26.1 {
        /*for (String key : original.keySet()) {
        *///?} else {
        for (String key : original.getAllKeys()) {
        //?}
            if (key.equals("SpawnData") || key.equals("SpawnPotentials")) continue;
            require(original.get(key).equals(result.get(key)), "spawner field changed: " + key);
        }
        require(compound(compound(result, "SpawnData"), "custom_spawn_rules").equals(rules), "custom spawn rules were lost");
        require(compound(compound(result, "SpawnData"), "entity").equals(entity), "new entity snapshot was not installed");
        require(!result.contains("SpawnPotentials"), "old weighted entries can replace the new template after one wave");
        entity.putString("id", "minecraft:zombie");
        compound(compound(result, "SpawnData"), "custom_spawn_rules").putString("fixture", "changed");
        //? if >=26.1 {
        /*require(compound(compound(result, "SpawnData"), "entity").getStringOr("id", "").equals("minecraft:cow"), "new snapshot aliases its source");
        *///?} else {
        require(compound(compound(result, "SpawnData"), "entity").getString("id").equals("minecraft:cow"), "new snapshot aliases its source");
        //?}
        require(original.equals(originalCopy), "new spawn rules alias the original data");
        System.out.println("PASS: native spawn egg data retains spawner settings, extension state and independent snapshots");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static CompoundTag compound(CompoundTag tag, String key) {
        //? if >=26.1 {
        /*return tag.getCompoundOrEmpty(key);
        *///?} else {
        return tag.getCompound(key);
        //?}
    }
}
