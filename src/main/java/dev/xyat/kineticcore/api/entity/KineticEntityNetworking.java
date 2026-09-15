package dev.xyat.kineticcore.api.entity;

import dev.xyat.kineticcore.internal.entity.KineticEntityNetworkingRuntime;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

public final class KineticEntityNetworking {
    private KineticEntityNetworking() {
    }

    public static Packet<ClientGamePacketListener> spawningPacket(Entity entity) {
        return KineticEntityNetworkingRuntime.spawningPacket(Objects.requireNonNull(entity, "entity"));
    }
}
