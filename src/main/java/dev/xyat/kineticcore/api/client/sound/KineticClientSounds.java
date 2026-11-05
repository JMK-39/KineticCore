package dev.xyat.kineticcore.api.client.sound;

import dev.xyat.kineticcore.internal.client.sound.KineticClientSoundRuntime;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Sounds heard only on this client, addressed by sound event id such as {@code minecraft:entity.warden.sonic_boom}.
 * Unknown ids are ignored. Call from the client thread.
 */
public final class KineticClientSounds {
    private KineticClientSounds() {
    }

    /** Plays one sound at a world position in the player sound category. */
    public static void play(Vec3 position, String soundId, float volume, float pitch) {
        KineticClientSoundRuntime.play(position, soundId, volume, pitch);
    }

    /**
     * Starts a looping sound. Every client tick it asks {@code state} for its position, volume and pitch; it stops
     * when {@code state} returns {@code null} or when the returned handle is ended.
     */
    public static Loop loop(String soundId, Supplier<LoopState> state) {
        return KineticClientSoundRuntime.loop(soundId, state);
    }

    /** Where a looping sound is and how loud and high it plays this tick. */
    public record LoopState(Vec3 position, float volume, float pitch) {
    }

    /** A running looping sound. */
    public interface Loop {
        /** Returns whether the sound is still playing. */
        boolean playing();

        /** Stops the sound; ending it twice is harmless. */
        void end();
    }
}
