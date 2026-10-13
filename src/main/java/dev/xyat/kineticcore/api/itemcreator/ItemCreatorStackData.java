package dev.xyat.kineticcore.api.itemcreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/** Spawn-template storage across item NBT and data-component versions. */
public final class ItemCreatorStackData {
    public static final String KEY = "kineticcore_spawn_template";
    private ItemCreatorStackData() { }
    public static boolean hasTemplate(ItemStack stack) {
        CompoundTag tag = stackData(stack);
        return tag.contains(KEY, Tag.TAG_COMPOUND);
    }
    public static void writeTemplate(ItemStack stack, SpawnEggTemplate template) {
        //? if >=1.20.5 {
        /*CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(KEY, encode(template)));
        *///?} else {
        stack.getOrCreateTag().put(KEY, encode(template));
        //?}
    }
    public static Optional<SpawnEggTemplate> readTemplate(ItemStack stack) {
        CompoundTag tag = stackData(stack);
        if (!tag.contains(KEY, Tag.TAG_COMPOUND)) return Optional.empty();
        return Optional.of(decode(tag.getCompound(KEY)));
    }
    private static CompoundTag stackData(ItemStack stack) {
        //? if >=1.20.5 {
        /*CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
        *///?} else {
        return stack.hasTag() ? stack.getTag() : new CompoundTag();
        //?}
    }
    public static CompoundTag encode(SpawnEggTemplate template) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", 1);
        tag.putString("entity", template.entityId());
        tag.putString("name", template.name());
        ListTag attributes = new ListTag();
        for (AttributeSpec spec : template.attributes()) {
            CompoundTag data = new CompoundTag();
            data.putString("id", spec.id()); data.putDouble("amount", spec.amount());
            data.putString("operation", spec.operation().name()); attributes.add(data);
        }
        tag.put("attributes", attributes);
        ListTag effects = new ListTag();
        for (EffectSpec spec : template.effects()) {
            CompoundTag data = new CompoundTag();
            data.putString("id", spec.id()); data.putInt("amplifier", spec.amplifier());
            data.putInt("duration", spec.durationTicks()); data.putBoolean("particles", spec.particles());
            data.putBoolean("icon", spec.icon()); effects.add(data);
        }
        tag.put("effects", effects);
        return tag;
    }
    public static SpawnEggTemplate decode(CompoundTag tag) {
        if (tag.getInt("version") != 1) throw new IllegalArgumentException("Unsupported spawn template version");
        ListTag list = tag.getList("attributes", Tag.TAG_COMPOUND);
        if (list.size() > ItemCreatorValidation.MAX_ENTRIES) throw new IllegalArgumentException("Too many attributes");
        List<AttributeSpec> attributes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag data = list.getCompound(i);
            attributes.add(new AttributeSpec(data.getString("id"), data.getDouble("amount"), AttributeSpec.Operation.valueOf(data.getString("operation"))));
        }
        list = tag.getList("effects", Tag.TAG_COMPOUND);
        if (list.size() > ItemCreatorValidation.MAX_ENTRIES) throw new IllegalArgumentException("Too many effects");
        List<EffectSpec> effects = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag data = list.getCompound(i);
            effects.add(new EffectSpec(data.getString("id"), data.getInt("amplifier"), data.getInt("duration"), data.getBoolean("particles"), data.getBoolean("icon")));
        }
        return new SpawnEggTemplate(tag.getString("entity"), attributes, effects, tag.getString("name"));
    }
}
