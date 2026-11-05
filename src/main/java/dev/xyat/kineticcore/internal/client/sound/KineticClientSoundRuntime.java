package dev.xyat.kineticcore.internal.client.sound;

import dev.xyat.kineticcore.api.client.sound.KineticClientSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Version bridge behind {@link KineticClientSounds}. */
public final class KineticClientSoundRuntime {
    private static final KineticClientSounds.Loop NONE = new KineticClientSounds.Loop() {
        @Override
        public boolean playing() {
            return false;
        }

        @Override
        public void end() {
        }
    };

    private KineticClientSoundRuntime() {
    }

    public static void play(Vec3 position, String soundId, float volume, float pitch) {
        Minecraft client = Minecraft.getInstance();
        SoundEvent sound = sound(soundId);
        if (client.level == null || sound == null || position == null) {
            return;
        }
        client.level.playLocalSound(position.x, position.y, position.z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    public static KineticClientSounds.Loop loop(String soundId, Supplier<KineticClientSounds.LoopState> state) {
        SoundEvent sound = sound(soundId);
        if (sound == null || state == null) {
            return NONE;
        }
        LoopingSound instance = new LoopingSound(sound, state);
        Minecraft.getInstance().getSoundManager().play(instance);
        return instance;
    }

    private static SoundEvent sound(String id) {
        ResourceLocation location = id == null ? null : ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        //? if >=26.1 {
        /*SoundEvent registered = BuiltInRegistries.SOUND_EVENT.getValue(location);
        *///?} else {
        SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(location);
        //?}
        // Sounds defined only in a resource pack's sounds.json play without a registered event.
        return registered != null ? registered : SoundEvent.createVariableRangeEvent(location);
    }

    private static final class LoopingSound extends AbstractTickableSoundInstance implements KineticClientSounds.Loop {
        private final Supplier<KineticClientSounds.LoopState> state;

        private LoopingSound(SoundEvent sound, Supplier<KineticClientSounds.LoopState> state) {
            super(sound, SoundSource.PLAYERS, RandomSource.create());
            this.state = state;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            apply(state.get());
        }

        @Override
        public void tick() {
            KineticClientSounds.LoopState current = state.get();
            if (current == null) {
                stop();
                return;
            }
            apply(current);
        }

        private void apply(KineticClientSounds.LoopState current) {
            if (current == null) {
                return;
            }
            x = current.position().x;
            y = current.position().y;
            z = current.position().z;
            volume = Math.max(0.0F, current.volume());
            pitch = Math.max(0.5F, Math.min(2.0F, current.pitch()));
        }

        @Override
        public boolean playing() {
            return !isStopped();
        }

        @Override
        public void end() {
            if (!isStopped()) {
                stop();
            }
        }
    }
}
