/**
 * Versioned network channels without Forge networking types. Create a channel with {@link dev.xyat.kineticcore.api.network.KineticNetwork KineticNetwork} or
 * {@link dev.xyat.kineticcore.api.network.PacketChannel PacketChannel}, describe messages with a {@link dev.xyat.kineticcore.api.network.NetworkCodec NetworkCodec} over {@link dev.xyat.kineticcore.api.network.NetworkBuffer NetworkBuffer}, and send them
 * with the returned senders. Decoding runs on the network thread; handlers run on the main thread of the receiving
 * side. All read methods accept explicit size limits for untrusted input.
 */
package dev.xyat.kineticcore.api.network;
