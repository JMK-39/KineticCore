package dev.xyat.kineticcore.feature.mining.network;


import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class MiningModeNetwork {
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            KineticResourceIds.of(KineticRuntime.MOD_ID, "mining_mode")
    , "1", NetworkVersionPolicy.EXACT);

    private static ServerboundSender<ToggleMiningModePacket> toggleSender;

    private MiningModeNetwork() {
    }

    public static synchronized void register() {
        if (toggleSender == null) {
            toggleSender = CHANNEL.registerServerbound(0,
                            ToggleMiningModePacket.class,
                    NetworkCodec.of((buffer, message) -> { }, buffer -> new ToggleMiningModePacket()),
                    (message, context) -> toggle(context.sender())
            );
        }
    }

    public static void sendToggleToServer() {
        if (toggleSender != null) {
            toggleSender.send(new ToggleMiningModePacket());
        }
    }

    private static void toggle(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag forgeData;
        if (persistentData.contains(ServerPlayer.PERSISTED_NBT_TAG)) {
            forgeData = persistentData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        } else {
            forgeData = new CompoundTag();
            persistentData.put(ServerPlayer.PERSISTED_NBT_TAG, forgeData);
        }

        boolean current = forgeData.getBoolean("SingleMiningMode");
        forgeData.putBoolean("SingleMiningMode", !current);
    }

    public record ToggleMiningModePacket() {
    }
}
