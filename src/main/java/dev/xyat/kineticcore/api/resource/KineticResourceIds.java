package dev.xyat.kineticcore.api.resource;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Creates resource identifiers without exposing version-specific construction details to addons.
 */
public final class KineticResourceIds {
    private KineticResourceIds() {
    }

    /**
     * Creates one validated resource identifier from namespace and path components.
     *
     * @param namespace resource namespace
     * @param path resource path
     * @return validated resource identifier
     * @throws IllegalArgumentException when the namespace/path pair is not a valid resource identifier
     */
    public static ResourceLocation of(String namespace, String path) {
        String joined = Objects.requireNonNull(namespace, "namespace") + ":" + Objects.requireNonNull(path, "path");
        ResourceLocation id = ResourceLocation.tryParse(joined);
        if (id == null) {
            throw new IllegalArgumentException("Invalid resource location: " + joined);
        }
        return id;
    }

    /**
     * Parses one resource identifier using the same strict constructor semantics as Minecraft 1.20.1.
     *
     * @param value complete resource identifier text
     * @return parsed resource identifier
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static ResourceLocation parse(String value) {
        return new ResourceLocation(value);
    }

    /**
     * Attempts to parse one complete resource identifier without throwing for invalid syntax.
     *
     * @param value complete resource identifier text
     * @return parsed identifier, or {@code null} when invalid
     */
    public static ResourceLocation tryParse(String value) {
        return ResourceLocation.tryParse(value);
    }
}
