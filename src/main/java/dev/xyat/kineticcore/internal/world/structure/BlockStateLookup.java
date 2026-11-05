package dev.xyat.kineticcore.internal.world.structure;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Reads block state strings such as {@code minecraft:oak_stairs[facing=east]} against this side's block registry. */
public final class BlockStateLookup {
    private BlockStateLookup() {
    }

    /** The block state a string names, or {@code null} if the block is not registered here or the string is bad. */
    @Nullable
    public static BlockState parse(String name) {
        try {
            return BlockStateParser.parseForBlock(blocks(), name, false).blockState();
        } catch (CommandSyntaxException | RuntimeException e) {
            return null;
        }
    }

    /** The string form of a block state, readable by {@link #parse(String)}. */
    public static String serialize(BlockState state) {
        return BlockStateParser.serialize(state);
    }

    private static HolderLookup<Block> blocks() {
        //? if >=26.1 {
        /*return BuiltInRegistries.BLOCK;
        *///?} else {
        return BuiltInRegistries.BLOCK.asLookup();
        //?}
    }
}
