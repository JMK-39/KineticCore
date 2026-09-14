package dev.xyat.kineticcore.internal.mixin.networklimit;

import dev.xyat.kineticcore.api.network.KineticNetwork;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Varint21FrameDecoder;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

public class NetworkLimitMixins {

    private static String getErrorMsg() {
        return "Packet limit exceeded. Modified by kineticcore (packetSize).";
    }

    @Mixin(ClientboundCustomPayloadPacket.class)
    public static class ClientPayloadTweaks {
        @ModifyConstant(method = {"<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)V"}, constant = @Constant(intValue = 1048576), require = 0)
        private int kineticcore$newSize(int value) { return KineticNetwork.transportLimits().customPayloadBytes(); }

        @ModifyConstant(method = {"<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)V"}, constant = @Constant(stringValue = "Payload may not be larger than 1048576 bytes"), require = 0)
        private String kineticcore$newMessage(String value) { return getErrorMsg(); }
    }

    @Mixin(ServerboundCustomPayloadPacket.class)
    public static class ServerPayloadTweaks {
        @ModifyConstant(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", constant = @Constant(intValue = 32767), require = 0)
        private int kineticcore$newSize(int value) { return KineticNetwork.transportLimits().customPayloadBytes(); }
    }

    @Mixin(ClientboundLevelChunkPacketData.class)
    public static class ChunkPacketTweaks {
        @ModifyConstant(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;II)V", constant = @Constant(intValue = 2097152), require = 0)
        private int kineticcore$newSize(int value) { return KineticNetwork.transportLimits().chunkPacketBytes(); }
    }

    @Mixin(FriendlyByteBuf.class)
    public static abstract class FriendlyByteBufTweaks {
        @ModifyConstant(method = "readNbt()Lnet/minecraft/nbt/CompoundTag;", constant = @Constant(longValue = 2097152L), require = 0)
        private long kineticcore$readNbt$newSize(long value) { return KineticNetwork.transportLimits().nbtBytes(); }

        @ModifyConstant(method = "readUtf()Ljava/lang/String;", constant = @Constant(intValue = 32767), require = 0)
        private int kineticcore$readUtf$newSize(int value) { return KineticNetwork.transportLimits().stringChars(); }

        @ModifyConstant(method = "writeUtf(Ljava/lang/String;)Lnet/minecraft/network/FriendlyByteBuf;", constant = @Constant(intValue = 32767), require = 0)
        private int kineticcore$writeUtf$newSize(int value) { return KineticNetwork.transportLimits().stringChars(); }

        @ModifyConstant(method = "readVarInt", constant = @Constant(intValue = 5), require = 0)
        private int kineticcore$readVarInt$newSize(int value) { return KineticNetwork.transportLimits().varIntBytes(); }

        @ModifyConstant(method = "readVarLong", constant = @Constant(intValue = 10), require = 0)
        private int kineticcore$readVarLong$newSize(int value) { return KineticNetwork.transportLimits().varLongBytes(); }
    }

    @Mixin(NbtAccounter.class)
    public static class NbtAccounterTweaks {
        @Redirect(method = "accountBytes(J)V", at = @At(value = "FIELD", target = "Lnet/minecraft/nbt/NbtAccounter;quota:J", opcode = Opcodes.GETFIELD), require = 0)
        private long kineticcore$newSize(NbtAccounter instance) { return KineticNetwork.transportLimits().nbtBytes(); }
    }

    @Mixin(value = Varint21FrameDecoder.class, priority = 1001)
    public static class Varint21FrameDecoderTweaks {
        @ModifyConstant(method = "decode", constant = @Constant(intValue = 3), require = 0)
        private int kineticcore$newSize(int constant) { return KineticNetwork.transportLimits().varInt21Bytes(); }
    }

    @Mixin(targets = "net.minecraft.network.Connection$1")
    public static class ConnectionTweaks {
        @ModifyConstant(method = "initChannel", constant = @Constant(intValue = 30), require = 0)
        private int kineticcore$newTimeout(int value) { return KineticNetwork.transportLimits().timeoutSeconds(); }
    }
}
