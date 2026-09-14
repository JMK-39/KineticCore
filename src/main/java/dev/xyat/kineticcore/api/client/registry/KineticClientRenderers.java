package dev.xyat.kineticcore.api.client.registry;

import dev.xyat.kineticcore.internal.client.registry.KineticClientRendererRuntime;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticClientRenderers {
    private KineticClientRenderers() {
    }

    public static <T extends Entity> void registerEntityRenderer(
            Supplier<? extends EntityType<T>> entityType,
            EntityRendererProvider<T> provider
    ) {
        KineticClientRendererRuntime.registerEntityRenderer(
                Objects.requireNonNull(entityType, "entityType"),
                Objects.requireNonNull(provider, "provider")
        );
    }
}
