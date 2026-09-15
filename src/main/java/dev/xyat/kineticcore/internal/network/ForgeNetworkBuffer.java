package dev.xyat.kineticcore.internal.network;

import dev.xyat.kineticcore.api.network.NetworkBuffer;
import dev.xyat.kineticcore.api.network.NetworkProtocolLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ForgeNetworkBuffer implements NetworkBuffer {
    private final FriendlyByteBuf buffer;

    ForgeNetworkBuffer(FriendlyByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public void writeBoolean(boolean value) {
        buffer.writeBoolean(value);
    }

    @Override
    public boolean readBoolean() {
        return buffer.readBoolean();
    }

    @Override
    public void writeByte(int value) {
        buffer.writeByte(value);
    }

    @Override
    public byte readByte() {
        return buffer.readByte();
    }

    @Override
    public void writeInt(int value) {
        buffer.writeInt(value);
    }

    @Override
    public int readInt() {
        return buffer.readInt();
    }

    @Override
    public void writeVarInt(int value) {
        buffer.writeVarInt(value);
    }

    @Override
    public int readVarInt() {
        return buffer.readVarInt();
    }

    @Override
    public <E extends Enum<E>> void writeEnum(E value) {
        if (value == null) throw new IllegalArgumentException("value");
        buffer.writeEnum(value);
    }

    @Override
    public <E extends Enum<E>> E readEnum(Class<E> enumType) {
        if (enumType == null) throw new IllegalArgumentException("enumType");
        return buffer.readEnum(enumType);
    }

    @Override
    public void writeLong(long value) {
        buffer.writeLong(value);
    }

    @Override
    public long readLong() {
        return buffer.readLong();
    }

    @Override
    public void writeVarLong(long value) {
        buffer.writeVarLong(value);
    }

    @Override
    public long readVarLong() {
        return buffer.readVarLong();
    }

    @Override
    public void writeFloat(float value) {
        buffer.writeFloat(value);
    }

    @Override
    public float readFloat() {
        return buffer.readFloat();
    }

    @Override
    public void writeDouble(double value) {
        buffer.writeDouble(value);
    }

    @Override
    public double readDouble() {
        return buffer.readDouble();
    }

    @Override
    public void writeUtf(String value) {
        writeUtf(value, NetworkProtocolLimits.DEFAULT.maxUtfChars());
    }

    @Override
    public void writeUtf(String value, int maxLength) {
        buffer.writeUtf(value, maxLength);
    }

    @Override
    public String readUtf() {
        return readUtf(NetworkProtocolLimits.DEFAULT.maxUtfChars());
    }

    @Override
    public String readUtf(int maxLength) {
        return buffer.readUtf(maxLength);
    }

    @Override
    public void writeByteArray(byte[] value) {
        writeByteArray(value, NetworkProtocolLimits.DEFAULT.maxByteArrayBytes());
    }

    @Override
    public void writeByteArray(byte[] value, int maxLength) {
        if (maxLength < 0) throw new IllegalArgumentException("maxLength must be non-negative");
        byte[] safe = value == null ? new byte[0] : value;
        if (safe.length > maxLength) {
            throw new IllegalArgumentException("Network byte array exceeds length limit");
        }
        buffer.writeByteArray(safe);
    }

    @Override
    public byte[] readByteArray() {
        return readByteArray(NetworkProtocolLimits.DEFAULT.maxByteArrayBytes());
    }

    @Override
    public byte[] readByteArray(int maxLength) {
        if (maxLength < 0) throw new IllegalArgumentException("maxLength must be non-negative");
        return buffer.readByteArray(maxLength);
    }

    @Override
    public void writeVarIntArray(int[] values) {
        writeVarIntArray(values, NetworkProtocolLimits.DEFAULT.maxCollectionEntries());
    }

    @Override
    public void writeVarIntArray(int[] values, int maxEntries) {
        if (maxEntries < 0) throw new IllegalArgumentException("maxEntries must be non-negative");
        int[] safe = values == null ? new int[0] : values;
        if (safe.length > maxEntries) {
            throw new IllegalArgumentException("Network int array exceeds entry limit");
        }
        buffer.writeVarIntArray(safe);
    }

    @Override
    public int[] readVarIntArray() {
        return readVarIntArray(NetworkProtocolLimits.DEFAULT.maxCollectionEntries());
    }

    @Override
    public int[] readVarIntArray(int maxEntries) {
        if (maxEntries < 0) throw new IllegalArgumentException("maxEntries must be non-negative");
        return buffer.readVarIntArray(maxEntries);
    }

    @Override
    public void writeComponent(Component value) {
        buffer.writeComponent(value == null ? Component.empty() : value);
    }

    @Override
    public Component readComponent() {
        return buffer.readComponent();
    }

    @Override
    public void writeResourceLocation(ResourceLocation value) {
        buffer.writeResourceLocation(value);
    }

    @Override
    public ResourceLocation readResourceLocation() {
        return buffer.readResourceLocation();
    }

    @Override
    public void writeUuid(UUID value) {
        buffer.writeUUID(value);
    }

    @Override
    public UUID readUuid() {
        return buffer.readUUID();
    }

    @Override
    public void writeBlockPos(BlockPos value) {
        buffer.writeBlockPos(value);
    }

    @Override
    public BlockPos readBlockPos() {
        return buffer.readBlockPos();
    }

    @Override
    public void writeNbt(CompoundTag value) {
        buffer.writeNbt(value);
    }

    @Override
    public CompoundTag readNbt() {
        return buffer.readNbt();
    }

    @Override
    public void writeItemStack(ItemStack value) {
        buffer.writeItem(value);
    }

    @Override
    public ItemStack readItemStack() {
        return buffer.readItem();
    }

    @Override
    public void writeIngredient(Ingredient value) {
        if (value == null) throw new IllegalArgumentException("value");
        value.toNetwork(buffer);
    }

    @Override
    public Ingredient readIngredient() {
        return Ingredient.fromNetwork(buffer);
    }

    @Override
    public void writeStringList(List<String> values) {
        writeStringList(
                values,
                NetworkProtocolLimits.DEFAULT.maxCollectionEntries(),
                NetworkProtocolLimits.DEFAULT.maxUtfChars()
        );
    }

    @Override
    public void writeStringList(List<String> values, int maxStringLength) {
        writeStringList(values, NetworkProtocolLimits.DEFAULT.maxCollectionEntries(), maxStringLength);
    }

    @Override
    public void writeStringList(List<String> values, int maxEntries, int maxStringLength) {
        if (maxEntries < 0) throw new IllegalArgumentException("maxEntries must be non-negative");
        if (maxStringLength < 0) throw new IllegalArgumentException("maxStringLength must be non-negative");
        List<String> safe = values == null ? List.of() : values;
        if (safe.size() > maxEntries) {
            throw new IllegalArgumentException("Network string list exceeds entry limit");
        }
        buffer.writeVarInt(safe.size());
        for (String value : safe) {
            buffer.writeUtf(value == null ? "" : value, maxStringLength);
        }
    }

    @Override
    public List<String> readStringList() {
        return readStringList(
                NetworkProtocolLimits.DEFAULT.maxCollectionEntries(),
                NetworkProtocolLimits.DEFAULT.maxUtfChars()
        );
    }

    @Override
    public List<String> readStringList(int maxStringLength) {
        return readStringList(NetworkProtocolLimits.DEFAULT.maxCollectionEntries(), maxStringLength);
    }

    @Override
    public List<String> readStringList(int maxEntries, int maxStringLength) {
        if (maxEntries < 0) throw new IllegalArgumentException("maxEntries must be non-negative");
        if (maxStringLength < 0) throw new IllegalArgumentException("maxStringLength must be non-negative");
        int size = buffer.readVarInt();
        if (size < 0 || size > maxEntries) {
            throw new IllegalArgumentException("Network string list exceeds entry limit");
        }
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(buffer.readUtf(maxStringLength));
        }
        return result;
    }
}
