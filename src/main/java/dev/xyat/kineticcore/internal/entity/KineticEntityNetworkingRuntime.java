package dev.xyat.kineticcore.internal.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?} else {
/*import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
*///?}

public final class KineticEntityNetworkingRuntime {
    private KineticEntityNetworkingRuntime() {
    }

    public static Packet<ClientGamePacketListener> spawningPacket(Entity entity) {
        //? if forge {
        return NetworkHooks.getEntitySpawningPacket(entity);
        //?} else {
        /*// NeoForge spawns modded entities with the vanilla packet; extra spawn data goes through IEntityWithComplexSpawn.
        return new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), entity.getX(), entity.getY(), entity.getZ(),
                entity.getXRot(), entity.getYRot(), entity.getType(), 0, entity.getDeltaMovement(), entity.getYHeadRot());
        *///?}
    }
}
