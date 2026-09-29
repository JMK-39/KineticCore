package dev.xyat.kineticcore.api.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Loader-independent view of a packet buffer used by {@link NetworkCodec}s.
 *
 * <p>Every value must be read back in the same order and with the same method it was written with. Methods without
 * a limit parameter use {@link NetworkProtocolLimits#DEFAULT}. Read methods that take a limit reject oversized
 * input before allocating it, so pass the smallest limit your protocol allows when decoding data from players.
 * String limits count UTF-16 characters. Instances are only valid inside the encode or decode call that received
 * them.
 */
public interface NetworkBuffer {
    /** Writes one byte: {@code 1} for {@code true}, {@code 0} for {@code false}. */
    void writeBoolean(boolean value);

    /** Reads a value written by {@link #writeBoolean(boolean)}. */
    boolean readBoolean();

    /** Writes the low 8 bits of {@code value}. */
    void writeByte(int value);

    /** Reads one signed byte. */
    byte readByte();

    /** Writes a fixed 4-byte big-endian integer. */
    void writeInt(int value);

    /** Reads a value written by {@link #writeInt(int)}. */
    int readInt();

    /** Writes an integer in 1 to 5 bytes; small non-negative values use fewer bytes than {@link #writeInt(int)}. */
    void writeVarInt(int value);

    /** Reads a value written by {@link #writeVarInt(int)}. */
    int readVarInt();

    /**
     * Writes an enum constant by ordinal. Both sides must declare the constants in the same order.
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    <E extends Enum<E>> void writeEnum(E value);

    /**
     * Reads a constant written by {@link #writeEnum(Enum)}.
     *
     * @throws IllegalArgumentException if {@code enumType} is {@code null}
     */
    <E extends Enum<E>> E readEnum(Class<E> enumType);

    /** Writes a fixed 8-byte big-endian long. */
    void writeLong(long value);

    /** Reads a value written by {@link #writeLong(long)}. */
    long readLong();

    /** Writes a long in 1 to 10 bytes; small non-negative values use fewer bytes. */
    void writeVarLong(long value);

    /** Reads a value written by {@link #writeVarLong(long)}. */
    long readVarLong();

    /** Writes a 4-byte IEEE 754 float. */
    void writeFloat(float value);

    /** Reads a value written by {@link #writeFloat(float)}. */
    float readFloat();

    /** Writes an 8-byte IEEE 754 double. */
    void writeDouble(double value);

    /** Reads a value written by {@link #writeDouble(double)}. */
    double readDouble();

    /**
     * Writes UTF-8 text limited to {@link NetworkProtocolLimits#DEFAULT} characters.
     *
     * @throws IllegalArgumentException if the text is {@code null}, too long or not valid UTF-16
     */
    void writeUtf(String value);

    /**
     * Writes UTF-8 text.
     *
     * @param value text to write
     * @param maxLength maximum length in UTF-16 characters; the reader should use the same limit
     * @throws IllegalArgumentException if the text is {@code null}, longer than {@code maxLength} or not valid
     *   UTF-16
     */
    void writeUtf(String value, int maxLength);

    /**
     * Reads text limited to {@link NetworkProtocolLimits#DEFAULT} characters.
     *
     * @throws IllegalArgumentException if the payload is too long or not valid UTF-8
     */
    String readUtf();

    /**
     * Reads text written by {@link #writeUtf(String, int)}.
     *
     * @param maxLength maximum accepted length in UTF-16 characters
     * @throws IllegalArgumentException if {@code maxLength} is negative, or the payload is too long or not valid
     *   UTF-8
     */
    String readUtf(int maxLength);

    /**
     * Writes a length-prefixed byte array limited to {@link NetworkProtocolLimits#DEFAULT} bytes. {@code null} is
     * written as an empty array.
     *
     * @throws IllegalArgumentException if the array is too long
     */
    void writeByteArray(byte[] value);

    /**
     * Writes a length-prefixed byte array. {@code null} is written as an empty array.
     *
     * @param maxLength maximum length in bytes
     * @throws IllegalArgumentException if {@code maxLength} is negative or the array is longer
     */
    void writeByteArray(byte[] value, int maxLength);

    /** Reads a byte array limited to {@link NetworkProtocolLimits#DEFAULT} bytes. */
    byte[] readByteArray();

    /**
     * Reads a byte array written by {@link #writeByteArray(byte[], int)}.
     *
     * @param maxLength maximum accepted length in bytes
     * @throws IllegalArgumentException if {@code maxLength} is negative
     */
    byte[] readByteArray(int maxLength);

    /**
     * Writes a length-prefixed VarInt array limited to {@link NetworkProtocolLimits#DEFAULT} entries. {@code null}
     * is written as an empty array.
     */
    void writeVarIntArray(int[] values);

    /**
     * Writes a length-prefixed VarInt array. {@code null} is written as an empty array.
     *
     * @param maxEntries maximum number of entries
     * @throws IllegalArgumentException if {@code maxEntries} is negative or the array has more entries
     */
    void writeVarIntArray(int[] values, int maxEntries);

    /** Reads a VarInt array limited to {@link NetworkProtocolLimits#DEFAULT} entries. */
    int[] readVarIntArray();

    /**
     * Reads a VarInt array written by {@link #writeVarIntArray(int[], int)}.
     *
     * @param maxEntries maximum accepted number of entries
     * @throws IllegalArgumentException if {@code maxEntries} is negative
     */
    int[] readVarIntArray(int maxEntries);

    /** Writes a text component as JSON; {@code null} is written as an empty component. */
    void writeComponent(Component value);

    /** Reads a component written by {@link #writeComponent(Component)}; never {@code null}. */
    Component readComponent();

    /** Writes a namespaced id as text. */
    void writeResourceLocation(ResourceLocation value);

    /** Reads a namespaced id; invalid ids fail the read. */
    ResourceLocation readResourceLocation();

    /** Writes a UUID as two longs. */
    void writeUuid(UUID value);

    /** Reads a UUID written by {@link #writeUuid(UUID)}. */
    UUID readUuid();

    /** Writes a block position packed into one long. */
    void writeBlockPos(BlockPos value);

    /** Reads a position written by {@link #writeBlockPos(BlockPos)}. */
    BlockPos readBlockPos();

    /** Writes an NBT compound; {@code null} is allowed and reads back as {@code null}. */
    void writeNbt(CompoundTag value);

    /**
     * Reads an NBT compound, limited by the game's NBT size accounting.
     *
     * @return the compound, or {@code null} when {@code null} was written
     */
    CompoundTag readNbt();

    /** Writes an item stack including count and tag; an empty stack is written as a single flag. */
    void writeItemStack(ItemStack value);

    /**
     * Reads a stack written by {@link #writeItemStack(ItemStack)}; returns {@link ItemStack#EMPTY} for empty
     * stacks.
     */
    ItemStack readItemStack();

    /**
     * Writes a recipe ingredient in the vanilla network format.
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    void writeIngredient(Ingredient value);

    /** Reads an ingredient written by {@link #writeIngredient(Ingredient)}. */
    Ingredient readIngredient();


    /** Writes a generic list by prefixing its size as a VarInt and delegating each element to the caller. */
    default <T> void writeList(List<T> values, BiConsumer<NetworkBuffer, T> writer) {
        List<T> safeValues = values == null ? List.of() : values;
        if (writer == null) throw new IllegalArgumentException("writer");
        writeVarInt(safeValues.size());
        for (T value : safeValues) {
            writer.accept(this, value);
        }
    }

    /** Reads a generic VarInt-sized list by delegating each element to the caller. */
    default <T> List<T> readList(Function<NetworkBuffer, T> reader) {
        if (reader == null) throw new IllegalArgumentException("reader");
        int size = readVarInt();
        if (size < 0) throw new IllegalArgumentException("Negative list size");
        List<T> values = new ArrayList<>(Math.min(size, 1024));
        for (int i = 0; i < size; i++) {
            values.add(reader.apply(this));
        }
        return values;
    }

    /**
     * Writes a string list using the default entry and length limits. {@code null} is written as an empty list.
     *
     * @throws IllegalArgumentException if an entry is {@code null} or a limit is exceeded; nothing is written in
     *   that case
     */
    void writeStringList(List<String> values);

    /**
     * Writes a string list with the default entry limit.
     *
     * @param maxStringLength maximum length of each entry in UTF-16 characters
     * @throws IllegalArgumentException if an entry is {@code null} or a limit is exceeded; nothing is written in
     *   that case
     */
    void writeStringList(List<String> values, int maxStringLength);

    /**
     * Writes a string list. The whole list is validated before anything is written, so a bad entry never leaves a
     * partial list in the buffer.
     *
     * @param values list to write; {@code null} is written as an empty list
     * @param maxEntries maximum number of entries
     * @param maxStringLength maximum length of each entry in UTF-16 characters
     * @throws IllegalArgumentException if a limit is negative or exceeded, or an entry is {@code null}
     */
    void writeStringList(List<String> values, int maxEntries, int maxStringLength);

    /** Reads a string list using the default entry and length limits. */
    List<String> readStringList();

    /**
     * Reads a string list with the default entry limit.
     *
     * @param maxStringLength maximum accepted length of each entry in UTF-16 characters
     */
    List<String> readStringList(int maxStringLength);

    /**
     * Reads a string list written by {@link #writeStringList(List, int, int)}.
     *
     * @param maxEntries maximum accepted number of entries
     * @param maxStringLength maximum accepted length of each entry in UTF-16 characters
     * @return a new mutable list
     * @throws IllegalArgumentException if a limit is negative or exceeded
     */
    List<String> readStringList(int maxEntries, int maxStringLength);
}
