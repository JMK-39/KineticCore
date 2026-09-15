package dev.xyat.kineticcore.api.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.UUID;

public interface NetworkBuffer {
    void writeBoolean(boolean value);

    boolean readBoolean();

    void writeByte(int value);

    byte readByte();

    void writeInt(int value);

    int readInt();

    void writeVarInt(int value);

    int readVarInt();

    <E extends Enum<E>> void writeEnum(E value);

    <E extends Enum<E>> E readEnum(Class<E> enumType);

    void writeLong(long value);

    long readLong();

    void writeVarLong(long value);

    long readVarLong();

    void writeFloat(float value);

    float readFloat();

    void writeDouble(double value);

    double readDouble();

    void writeUtf(String value);

    void writeUtf(String value, int maxLength);

    String readUtf();

    String readUtf(int maxLength);

    void writeByteArray(byte[] value);

    void writeByteArray(byte[] value, int maxLength);

    byte[] readByteArray();

    byte[] readByteArray(int maxLength);

    void writeVarIntArray(int[] values);

    void writeVarIntArray(int[] values, int maxEntries);

    int[] readVarIntArray();

    int[] readVarIntArray(int maxEntries);

    void writeComponent(Component value);

    Component readComponent();

    void writeResourceLocation(ResourceLocation value);

    ResourceLocation readResourceLocation();

    void writeUuid(UUID value);

    UUID readUuid();

    void writeBlockPos(BlockPos value);

    BlockPos readBlockPos();

    void writeNbt(CompoundTag value);

    CompoundTag readNbt();

    void writeItemStack(ItemStack value);

    ItemStack readItemStack();

    void writeIngredient(Ingredient value);

    Ingredient readIngredient();

    void writeStringList(List<String> values);

    void writeStringList(List<String> values, int maxStringLength);

    void writeStringList(List<String> values, int maxEntries, int maxStringLength);

    List<String> readStringList();

    List<String> readStringList(int maxStringLength);

    List<String> readStringList(int maxEntries, int maxStringLength);
}
