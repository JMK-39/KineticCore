package dev.xyat.kineticcore.internal.runtime;

/** Implemented by every Mob through MobDespawnMixins; resets the cached despawn decision when equipment changes. */
public interface DespawnCacheAccess {
    void kineticcore$resetDespawnCache();
}
