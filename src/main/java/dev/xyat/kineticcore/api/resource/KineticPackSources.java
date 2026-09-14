package dev.xyat.kineticcore.api.resource;

import dev.xyat.kineticcore.internal.resource.KineticPackSourceRuntime;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticPackSources {
    private KineticPackSources() {
    }

    public static void register(PackType packType, RepositorySource source) {
        Objects.requireNonNull(source, "source");
        register(packType, () -> source);
    }

    public static void register(PackType packType, Supplier<? extends RepositorySource> sourceFactory) {
        KineticPackSourceRuntime.register(
                Objects.requireNonNull(packType, "packType"),
                Objects.requireNonNull(sourceFactory, "sourceFactory")
        );
    }
}
