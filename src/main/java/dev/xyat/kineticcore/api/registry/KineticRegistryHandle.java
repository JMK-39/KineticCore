package dev.xyat.kineticcore.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Deferred reference to an object registered through a Kinetic registration helper.
 *
 * <p>Handles are returned during mod construction, before the object exists. Keep them in static fields and call
 * {@link #get()} only after registration has finished.
 *
 * @param <T> registered object type
 */
public interface KineticRegistryHandle<T> extends Supplier<T> {
    /** Returns the id the object is registered under; available immediately. */
    ResourceLocation id();

    /** Returns whether the backing registry object has completed registration and can be read safely. */
    boolean isPresent();

    /**
     * Returns the registered object.
     *
     * @return the object
     * @throws NullPointerException if registration has not completed yet; check {@link #isPresent()} when unsure
     */
    @Override
    T get();
}
