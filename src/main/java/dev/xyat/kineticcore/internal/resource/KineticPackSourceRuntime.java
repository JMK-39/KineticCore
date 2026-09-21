package dev.xyat.kineticcore.internal.resource;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class KineticPackSourceRuntime {
    private static final Map<PackType, List<Supplier<? extends RepositorySource>>> SOURCES =
            new EnumMap<>(PackType.class);
    private static final EnumSet<PackType> CLOSED_TYPES = EnumSet.noneOf(PackType.class);
    private static boolean listenerRegistered;

    private KineticPackSourceRuntime() {
    }

    public static synchronized void register(
            PackType packType,
            Supplier<? extends RepositorySource> sourceFactory
    ) {
        Objects.requireNonNull(packType, "packType");
        Objects.requireNonNull(sourceFactory, "sourceFactory");
        if (CLOSED_TYPES.contains(packType)) {
            throw new IllegalStateException("Pack source registration window has already closed for " + packType);
        }
        // Installation must succeed before committing the factory. A failed registration
        // leaves no ghost entry and the next valid call may retry installing the listener.
        ensureListener();
        SOURCES.computeIfAbsent(packType, ignored -> new ArrayList<>()).add(sourceFactory);
    }

    private static void ensureListener() {
        if (listenerRegistered) return;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticPackSourceRuntime::onAddPackFinders);
        listenerRegistered = true;
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        List<Supplier<? extends RepositorySource>> factories;
        synchronized (KineticPackSourceRuntime.class) {
            CLOSED_TYPES.add(event.getPackType());
            factories = List.copyOf(SOURCES.getOrDefault(event.getPackType(), List.of()));
        }
        KineticCallbackBatch.runAll(factories, factory -> event.addRepositorySource(factory.get()));
    }
}
