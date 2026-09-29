/**
 * Versioned network channels without Forge networking types. Create a channel with {@link KineticNetwork} or
 * {@link PacketChannel}, describe messages with a {@link NetworkCodec} over {@link NetworkBuffer}, and send them
 * with the returned senders. Decoding runs on the network thread; handlers run on the main thread of the receiving
 * side. All read methods accept explicit size limits for untrusted input.
 */
package dev.xyat.kineticcore.api.network;
