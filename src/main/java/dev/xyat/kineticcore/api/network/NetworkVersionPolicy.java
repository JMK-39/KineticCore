package dev.xyat.kineticcore.api.network;

/** How a channel compares its protocol version with the other side during login. */
public enum NetworkVersionPolicy {
    /** Both sides must have the channel with exactly the same version; otherwise the connection is refused. */
    EXACT,
    /** Any version is accepted, including a missing channel. Use it for optional features. */
    ANY
}
