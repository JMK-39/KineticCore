package dev.xyat.kineticcore.api.registry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface KineticRegistryView<T> {
    T get(ResourceLocation id);

    ResourceLocation id(T value);

    Collection<T> values();

    Set<ResourceLocation> ids();

    boolean contains(ResourceLocation id);

    List<ResourceLocation> tagIds();

    List<T> valuesInTag(TagKey<T> tag);

    boolean isInTag(T value, TagKey<T> tag);

    boolean isInTag(T value, ResourceLocation tagId);

    Holder.Reference<T> holder(T value);
}
