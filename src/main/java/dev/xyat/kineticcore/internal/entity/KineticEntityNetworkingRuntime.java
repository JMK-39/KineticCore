package dev.xyat.kineticcore.internal.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkHooks;

public final class KineticEntityNetworkingRuntime {
    private KineticEntityNetworkingRuntime() {
    }

    public static Packet<ClientGamePacketListener> spawningPacket(Entity entity) {
        return NetworkHooks.getEntitySpawningPacket(entity);
    }
}
