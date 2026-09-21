package dev.xyat.kineticcore.feature.flight.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.flight.KineticFlightSources;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.feature.flight.FlightState;
import dev.xyat.kineticcore.feature.flight.network.FlightNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class FlightEvents {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticServerEvents.onPlayerGameModeChange(KineticEventPriority.NORMAL, FlightEvents::onGameModeChange),
                () -> KineticServerEvents.onPlayerLogin(KineticEventPriority.NORMAL, FlightEvents::onPlayerLogin),
                () -> KineticServerEvents.onPlayerChangedDimension(KineticEventPriority.NORMAL, (player, from, to) -> onDimensionChange(player)),
                () -> KineticServerEvents.onPlayerRespawn(KineticEventPriority.NORMAL, (player, endConquered) -> onPlayerRespawn(player)),
                () -> KineticServerEvents.onPlayerTick(KineticEventPriority.NORMAL, KineticServerEvents.TickPhase.END, KineticSuperFlight::tick)
        );
    }

    public static void onGameModeChange(KineticServerEvents.PlayerGameModeChangeContext context) {
        ServerPlayer player = context.player();
        GameType newMode = context.newGameMode();
        if (newMode != GameType.CREATIVE) {
            player.server.execute(() -> FlightNetwork.applyServerNoclip(player, false));
        }
        if (newMode == GameType.CREATIVE) {
            boolean noclip = player.getPersistentData().getBoolean("kt_noclip");
            MutableComponent statusText = KineticI18n.translatable(
                    noclip ? "msg.kineticcore.flying.on" : "msg.kineticcore.flying.off"
            );

            MutableComponent speedKey = Component.keybind("key.kineticcore.flying.speed.modifier");
            MutableComponent noclipKey = Component.keybind("key.kineticcore.flying.noclip");

            player.displayClientMessage(
                    KineticI18n.translatable("msg.kineticcore.flying.fine.tune", speedKey),
                    false
            );
            player.displayClientMessage(
                    KineticI18n.translatable("msg.kineticcore.flying.fast.tune", speedKey),
                    false
            );
            player.displayClientMessage(
                    KineticI18n.translatable("msg.kineticcore.flying.noclip.status", statusText, noclipKey),
                    false
            );
        }
    }

    public static void onPlayerLogin(ServerPlayer player) {
        player.server.execute(() -> {
            FlightNetwork.applyServerNoclip(player, false);
            KineticSuperFlight.resetTransientState(player);
            resyncFlightAbilities(player);
            KineticSuperFlight.sync(player);
        });
    }

    public static void onDimensionChange(ServerPlayer player) {
        player.server.execute(() -> {
            KineticSuperFlight.resetTransientState(player);
            resyncFlightAbilities(player);
            FlightNetwork.syncNoclipState(player);
            KineticSuperFlight.sync(player);
        });
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        player.server.execute(() -> {
            KineticSuperFlight.resetTransientState(player);
            KineticSuperFlight.setActive(player, false);
            resyncFlightAbilities(player);
            FlightNetwork.syncNoclipState(player);
        });
    }

    private static void resyncFlightAbilities(ServerPlayer player) {
        boolean forceFly = KineticFlightSources.allowsFlight(player);
        boolean wasFlying = FlightState.lastKnownFlying(player);
        if (forceFly || player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            if (wasFlying) player.getAbilities().flying = true;
        }
        FlightState.isInternalUpdate = true;
        player.onUpdateAbilities();
        FlightState.isInternalUpdate = false;
    }
}
