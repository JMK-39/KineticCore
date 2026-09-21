package dev.xyat.kineticcore.api.minecraft;

import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Collection;
import java.util.UUID;

/**
 * Public Kinetic API for client-known player information.
 */
public final class MinecraftPlayers {
    private MinecraftPlayers() {
    }

    /** Returns the player entries currently known by the active client connection. */
    public static Collection<PlayerInfo> onlinePlayers() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.onlinePlayers();
    }

    /** Returns the player-info entry for the supplied UUID, when available. */
    public static PlayerInfo playerInfo(UUID uuid) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.playerInfo(uuid);
    }
}
