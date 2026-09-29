package dev.xyat.kineticcore.api.registry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only view of one game registry.
 *
 * <p>Obtain instances from {@link KineticRegistries}. Collection results are immutable snapshots.
 *
 * @param <T> registry element type
 */
public interface KineticRegistryView<T> {
    /**
     * Looks up an element by id.
     *
     * @param id element id
     * @return the element, or the registry's default element (for example air) or {@code null} when the id is not
     *   registered, depending on the registry
     */
    T get(ResourceLocation id);

    /**
     * Returns the id of a registered element.
     *
     * @param value element to look up
     * @return the id, or {@code null} when the element is not registered
     */
    ResourceLocation id(T value);

    /** Returns an immutable snapshot of every registered element in registry order. */
    Collection<T> values();

    /** Returns an immutable snapshot mapping every id to its element. */
    Map<ResourceLocation, T> entries();

    /** Returns an immutable snapshot of every registered id. */
    Set<ResourceLocation> ids();

    /** Returns whether an element is registered under the id. */
    boolean contains(ResourceLocation id);

    /**
     * Returns the ids of every tag currently bound for this registry.
     *
     * @return the tag ids, or an empty list when the registry has no tags or tags are not loaded yet
     */
    List<ResourceLocation> tagIds();

    /**
     * Returns the elements currently in a tag.
     *
     * @param tag tag to resolve
     * @return the elements, or an empty list when the tag is unknown or tags are not loaded yet
     */
    List<T> valuesInTag(TagKey<T> tag);

    /** Returns whether the element is in the tag; {@code false} while tags are not loaded. */
    boolean isInTag(T value, TagKey<T> tag);

    /**
     * Returns whether the element is in the tag with this id; {@code false} for a {@code null} id or while tags are
     * not loaded.
     */
    boolean isInTag(T value, ResourceLocation tagId);

    /**
     * Returns the registry holder of a registered element, for APIs that require holders.
     *
     * @param value registered element
     * @return the holder
     * @throws IllegalArgumentException if the element is not registered
     */
    Holder.Reference<T> holder(T value);
}
