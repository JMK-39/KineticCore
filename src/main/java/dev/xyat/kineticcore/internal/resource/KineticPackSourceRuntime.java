package dev.xyat.kineticcore.internal.resource;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class KineticPackSourceRuntime {
    private static final Map<PackType, List<Supplier<? extends RepositorySource>>> SOURCES =
            new EnumMap<>(PackType.class);
    private static boolean listenerRegistered;

    private KineticPackSourceRuntime() {
    }

    public static synchronized void register(
            PackType packType,
            Supplier<? extends RepositorySource> sourceFactory
    ) {
        SOURCES.computeIfAbsent(packType, ignored -> new ArrayList<>()).add(sourceFactory);
        ensureListener();
    }

    private static void ensureListener() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticPackSourceRuntime::onAddPackFinders);
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        List<Supplier<? extends RepositorySource>> factories;
        synchronized (KineticPackSourceRuntime.class) {
            factories = List.copyOf(SOURCES.getOrDefault(event.getPackType(), List.of()));
        }
        for (Supplier<? extends RepositorySource> factory : factories) {
            event.addRepositorySource(factory.get());
        }
    }
}
