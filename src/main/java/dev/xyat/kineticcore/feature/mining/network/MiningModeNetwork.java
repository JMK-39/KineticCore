package dev.xyat.kineticcore.feature.mining.network;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = KineticCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MiningModeNetwork {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KineticCore.MODID, "mining_mode"),
            () -> PROTOCOL_VERSION,
            KTNetworkProtocol::acceptsAnyVersion,
            KTNetworkProtocol::acceptsAnyVersion
    );

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            INSTANCE.registerMessage(
                    0,
                    ToggleMiningModePacket.class,
                    ToggleMiningModePacket::toBytes,
                    ToggleMiningModePacket::new,
                    ToggleMiningModePacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_SERVER)
            );
        });
    }

    public static class ToggleMiningModePacket {
        public ToggleMiningModePacket() {}

        public ToggleMiningModePacket(FriendlyByteBuf buf) {}

        public void toBytes(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context ctx = supplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    // 获取或创建玩家的持久化 NBT 数据 (死亡不掉落)
                    CompoundTag persistentData = player.getPersistentData();
                    CompoundTag forgeData;
                    if (persistentData.contains(ServerPlayer.PERSISTED_NBT_TAG)) {
                        forgeData = persistentData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
                    } else {
                        forgeData = new CompoundTag();
                        persistentData.put(ServerPlayer.PERSISTED_NBT_TAG, forgeData);
                    }

                    // 切换全局单次/连锁挖掘模式
                    boolean current = forgeData.getBoolean("SingleMiningMode");
                    forgeData.putBoolean("SingleMiningMode", !current);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
