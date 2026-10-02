package dev.xyat.kineticcore.internal.inventory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
/*import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import dev.xyat.kineticcore.internal.registry.KineticRegistryAccessRuntime;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
*///?} else {
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
//?}

/**
 * Item stacks as text, in the syntax of the running Minecraft version's commands: {@code id{nbt}} before 1.20.5,
 * {@code id[components]} since. The count is not part of the text. Since 1.20.5 the 1.20.1 form {@code id{nbt}} is still
 * read, and upgraded to components.
 */
public final class KineticItemTextRuntime {
    private KineticItemTextRuntime() {
    }

    //? if >=1.20.5 {
    /*// Data version of 1.20.1, whose id{nbt} text configs written before the upgrade still use.
    private static final int LEGACY_DATA_VERSION = 3465;

    public static ItemStack parse(String text) {
        String trimmed = text.trim();
        int components = trimmed.indexOf('[');
        int legacyNbt = trimmed.indexOf('{');
        if (legacyNbt >= 0 && (components < 0 || legacyNbt < components)) {
            return parseLegacy(trimmed.substring(0, legacyNbt), trimmed.substring(legacyNbt));
        }
        try {
            StringReader reader = new StringReader(trimmed);
            ItemParser.ItemResult result = new ItemParser(KineticRegistryAccessRuntime.currentOrBuiltIn()).parse(reader);
            if (reader.canRead()) {
                throw new IllegalArgumentException("Unexpected text after the item: " + reader.getRemaining());
            }
            return new ItemStack(result.item(), 1, result.components());
        } catch (CommandSyntaxException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    // id{nbt} from 1.20.1 is upgraded the way the game upgrades old worlds, so its item data turns into components.
    private static ItemStack parseLegacy(String id, String nbt) {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", id.trim());
        stack.putByte("Count", (byte) 1);
        try {
            stack.put("tag", TagParser.parseTag(nbt));
        } catch (CommandSyntaxException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
        Tag upgraded = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stack),
                LEGACY_DATA_VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        return ItemStack.parse(KineticRegistryAccessRuntime.currentOrBuiltIn(), upgraded)
                .orElseThrow(() -> new IllegalArgumentException("Cannot read item " + id + nbt));
    }

    public static String format(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        StringBuilder text = new StringBuilder(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return text.toString();

        RegistryOps<Tag> ops = KineticRegistryAccessRuntime.currentOrBuiltIn().createSerializationContext(NbtOps.INSTANCE);
        List<String> components = new ArrayList<>();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            ResourceLocation typeId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            if (typeId == null) continue;
            String name = "minecraft".equals(typeId.getNamespace()) ? typeId.getPath() : typeId.toString();
            if (entry.getValue().isEmpty()) {
                // A removed default component, written as in commands: [!food]
                components.add("!" + name);
                continue;
            }
            @SuppressWarnings("unchecked")
            Codec<Object> codec = (Codec<Object>) entry.getKey().codec();
            // Components without a codec only exist at run time and are never written.
            if (codec == null) continue;
            codec.encodeStart(ops, entry.getValue().get()).result()
                    .ifPresent(value -> components.add(name + "=" + value));
        }
        if (!components.isEmpty()) text.append('[').append(String.join(",", components)).append(']');
        return text.toString();
    }
    *///?} else {
    public static ItemStack parse(String text) {
        String trimmed = text.trim();
        int nbtStart = trimmed.indexOf('{');
        String id = nbtStart < 0 ? trimmed : trimmed.substring(0, nbtStart).trim();
        ResourceLocation itemId = ResourceLocation.tryParse(id);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            throw new IllegalArgumentException("Unknown item: " + id);
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        if (nbtStart >= 0) {
            try {
                stack.setTag(TagParser.parseTag(trimmed.substring(nbtStart)));
            } catch (CommandSyntaxException exception) {
                throw new IllegalArgumentException(exception.getMessage(), exception);
            }
        }
        return stack;
    }

    public static String format(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return stack.hasTag() && stack.getTag() != null ? id + stack.getTag() : id;
    }
    //?}
}
