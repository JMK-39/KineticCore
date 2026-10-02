package dev.xyat.kineticcore.feature.flight;

import dev.xyat.kineticcore.api.flight.KineticFlightSources;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps flight that a flight source grants when an outbound ability packet would take it away. Called by the packet
 * listener mixins; it lives outside the mixin package because Mixin does not let code load classes from there.
 */
public final class FlightAbilityPackets {
    private FlightAbilityPackets() {
    }

    public static void interceptOutbound(ServerPlayer player, Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ClientboundPlayerAbilitiesPacket)) return;
        if (FlightState.isInternalUpdate || KineticFlightSources.abilityRefreshInProgress() || FlightState.isProcessingExplicitCancel) return;

        boolean outgoingMayfly = player.getAbilities().mayfly;
        boolean wasFlying = FlightState.lastKnownFlying(player);

        if (outgoingMayfly && !player.getAbilities().flying && wasFlying) {
            ci.cancel();
            player.getAbilities().flying = true;
            FlightState.isInternalUpdate = true;
            player.onUpdateAbilities();
            FlightState.isInternalUpdate = false;
            return;
        }
        if (!outgoingMayfly && KineticFlightSources.allowsFlight(player)) {
            ci.cancel();
            player.getAbilities().mayfly = true;
            if (wasFlying) player.getAbilities().flying = true;
            FlightState.isInternalUpdate = true;
            player.onUpdateAbilities();
            FlightState.isInternalUpdate = false;
        }
    }
}
