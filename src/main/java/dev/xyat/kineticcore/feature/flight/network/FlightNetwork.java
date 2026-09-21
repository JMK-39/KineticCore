package dev.xyat.kineticcore.feature.flight.network;

import dev.xyat.kineticcore.api.flight.KineticFlight;
import dev.xyat.kineticcore.api.flight.KineticFlightClient;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.PacketRegistrations;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.UUID;

public final class FlightNetwork {
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(KineticRuntime.id("flight_channel"), "5", NetworkVersionPolicy.EXACT);

    private static ServerboundSender<PacketNoclip> noclipRequestSender;
    private static ClientboundSender<PacketNoclipState> noclipStateSender;
    private static ServerboundSender<PacketSuperFlightRequest> superFlightRequestSender;
    private static ClientboundSender<PacketSuperFlightState> superFlightStateSender;
    private static ServerboundSender<PacketSuperFlightFallFlying> superFlightFallFlyingSender;
    private static ServerboundSender<PacketSuperFlightRollRequest> superFlightRollRequestSender;
    private static ClientboundSender<PacketSuperFlightRollState> superFlightRollStateSender;
    private static ClientboundSender<PacketSuperFlightTransientReset> superFlightTransientResetSender;
    private static boolean syncSenderInstalled;

    private FlightNetwork() {
    }

    public static synchronized void register() {
        PacketRegistrations.runIndependent(
        () -> {
            if (noclipRequestSender == null) {
                noclipRequestSender = CHANNEL.registerServerbound(0,
                                PacketNoclip.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.enabled()),
                                buffer -> new PacketNoclip(buffer.readBoolean())
                        ),
                        (message, context) -> KineticFlight.applyServerNoclip(context.sender(), message.enabled())
                );

            }
        },
        () -> {
            if (noclipStateSender == null) {
                noclipStateSender = CHANNEL.registerClientbound(1,
                                PacketNoclipState.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.enabled()),
                                buffer -> new PacketNoclipState(buffer.readBoolean())
                        ),
                        message -> KineticFlightClient.applyServerNoclip(message.enabled())
                );

            }
        },
        () -> {
            if (superFlightRequestSender == null) {
                superFlightRequestSender = CHANNEL.registerServerbound(2,
                        PacketSuperFlightRequest.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.enabled()),
                                buffer -> new PacketSuperFlightRequest(buffer.readBoolean())
                        ),
                        (message, context) -> KineticSuperFlight.setActive(context.sender(), message.enabled())
                );
            }
        },
        () -> {
            if (superFlightStateSender == null) {
                superFlightStateSender = CHANNEL.registerClientbound(3,
                        PacketSuperFlightState.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.enabled()),
                                buffer -> new PacketSuperFlightState(buffer.readBoolean())
                        ),
                        message -> KineticFlightClient.applySuperFlightState(message.enabled())
                );
            }
        },
        () -> {
            if (superFlightFallFlyingSender == null) {
                superFlightFallFlyingSender = CHANNEL.registerServerbound(4,
                        PacketSuperFlightFallFlying.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.enabled()),
                                buffer -> new PacketSuperFlightFallFlying(buffer.readBoolean())
                        ),
                        (message, context) -> KineticSuperFlight.setFallFlyingPose(context.sender(), message.enabled())
                );
            }
        },
        () -> {
            if (superFlightRollRequestSender == null) {
                superFlightRollRequestSender = CHANNEL.registerServerbound(5,
                        PacketSuperFlightRollRequest.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeFloat(message.roll()),
                                buffer -> new PacketSuperFlightRollRequest(buffer.readFloat())
                        ),
                        (message, context) -> {
                            ServerPlayer player = context.sender();
                            if (player == null || !KineticSuperFlight.active(player)) {
                                broadcastSuperFlightRoll(player, 0.0F);
                                return;
                            }
                            float roll = Float.isFinite(message.roll()) ? Mth.wrapDegrees(message.roll()) : 0.0F;
                            broadcastSuperFlightRoll(player, roll);
                        }
                );
            }
        },
        () -> {
            if (superFlightRollStateSender == null) {
                superFlightRollStateSender = CHANNEL.registerClientbound(6,
                        PacketSuperFlightRollState.class,
                        NetworkCodec.of(
                                (buffer, message) -> {
                                    buffer.writeUuid(message.playerId());
                                    buffer.writeFloat(message.roll());
                                },
                                buffer -> new PacketSuperFlightRollState(buffer.readUuid(), buffer.readFloat())
                        ),
                        message -> KineticFlightClient.applySuperFlightRemoteRoll(message.playerId(), message.roll())
                );
            }
        },
        () -> {
            if (superFlightTransientResetSender == null) {
                superFlightTransientResetSender = CHANNEL.registerClientbound(7,
                        PacketSuperFlightTransientReset.class,
                        NetworkCodec.of((buffer, message) -> { }, buffer -> new PacketSuperFlightTransientReset()),
                        message -> KineticFlightClient.resetSuperFlightTransientState()
                );
            }
        },
        () -> {
            if (!syncSenderInstalled && noclipStateSender != null && superFlightStateSender != null && superFlightRollStateSender != null && superFlightTransientResetSender != null) {
                KineticFlight.installNoclipSyncSender((player, enabled) -> {
                    if (noclipStateSender != null) {
                        noclipStateSender.send(player, new PacketNoclipState(enabled));
                    }
                });
                KineticSuperFlight.installStateSyncSender((player, enabled) -> {
                    if (superFlightStateSender != null) {
                        superFlightStateSender.send(player, new PacketSuperFlightState(enabled));
                    }
                });
                KineticSuperFlight.installRollSyncSender(FlightNetwork::broadcastSuperFlightRoll);
                KineticSuperFlight.installTransientResetSender(player -> {
                    if (superFlightTransientResetSender != null) {
                        superFlightTransientResetSender.send(player, new PacketSuperFlightTransientReset());
                    }
                });
                syncSenderInstalled = true;
            }
        }
        );
    }

    public static void requestNoclip(boolean enabled) {
        if (noclipRequestSender != null) {
            noclipRequestSender.send(new PacketNoclip(enabled));
        }
    }

    public static void requestSuperFlight(boolean enabled) {
        if (superFlightRequestSender != null) {
            superFlightRequestSender.send(new PacketSuperFlightRequest(enabled));
        }
    }

    public static void requestSuperFlightFallFlying(boolean enabled) {
        if (superFlightFallFlyingSender != null) {
            superFlightFallFlyingSender.send(new PacketSuperFlightFallFlying(enabled));
        }
    }

    public static void requestSuperFlightRoll(float roll) {
        if (superFlightRollRequestSender != null && Float.isFinite(roll)) {
            superFlightRollRequestSender.send(new PacketSuperFlightRollRequest(Mth.wrapDegrees(roll)));
        }
    }

    private static void broadcastSuperFlightRoll(ServerPlayer player, float roll) {
        if (player == null || superFlightRollStateSender == null) return;
        superFlightRollStateSender.sendToTrackingAndSelf(
                player,
                new PacketSuperFlightRollState(player.getUUID(), Mth.wrapDegrees(roll))
        );
    }

    public static void applyServerNoclip(ServerPlayer player, boolean requestedState) {
        KineticFlight.applyServerNoclip(player, requestedState);
    }

    public static void syncNoclipState(ServerPlayer player) {
        KineticFlight.syncServerNoclip(player);
    }

    public record PacketNoclip(boolean enabled) {
    }

    public record PacketNoclipState(boolean enabled) {
    }

    public record PacketSuperFlightRequest(boolean enabled) {
    }

    public record PacketSuperFlightState(boolean enabled) {
    }

    public record PacketSuperFlightFallFlying(boolean enabled) {
    }

    public record PacketSuperFlightRollRequest(float roll) {
    }

    public record PacketSuperFlightRollState(UUID playerId, float roll) {
    }

    public record PacketSuperFlightTransientReset() {
    }
}
