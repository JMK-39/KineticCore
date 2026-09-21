package dev.xyat.kineticcore.internal.runtime;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import dev.xyat.kineticcore.api.runtime.KineticCreativeTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class KineticCreativeTabsRuntime {
    private static final Map<String, DeferredRegister<CreativeModeTab>> REGISTRIES = new LinkedHashMap<>();
    private static final CopyOnWriteArrayList<KineticCreativeTabs.Handler> BUILD_CONTENT_HANDLERS =
            new CopyOnWriteArrayList<>();
    private static boolean buildContentsListenerRegistered;
    private KineticCreativeTabsRuntime() {
    }

    public static synchronized KineticRegistryHandle<CreativeModeTab> register(
            ResourceLocation id,
            Supplier<? extends CreativeModeTab> factory
    ) {
        DeferredRegister<CreativeModeTab> registry = REGISTRIES.computeIfAbsent(id.getNamespace(), namespace -> {
            DeferredRegister<CreativeModeTab> created = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, namespace);
            created.register(FMLJavaModLoadingContext.get().getModEventBus());
            return created;
        });

        RegistryObject<CreativeModeTab> object = registry.register(id.getPath(), factory::get);
        return new Handle(id, object);
    }

    public static synchronized void onBuildContents(KineticCreativeTabs.Handler handler) {
        ensureBuildContentsListener();
        BUILD_CONTENT_HANDLERS.add(handler);
    }

    private static void ensureBuildContentsListener() {
        if (buildContentsListenerRegistered) return;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticCreativeTabsRuntime::onBuildContentsEvent);
        buildContentsListenerRegistered = true;
    }

    private static void onBuildContentsEvent(BuildCreativeModeTabContentsEvent event) {
        ContextImpl context = new ContextImpl(event);
        KineticCallbackBatch.runAll(BUILD_CONTENT_HANDLERS, handler -> handler.handle(context));
    }

    public static List<KineticCreativeTabs.TabEntry> entries() {
        List<KineticCreativeTabs.TabEntry> result = new ArrayList<>();
        for (var entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            result.add(new KineticCreativeTabs.TabEntry(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }

    public static List<CreativeModeTab> values() {
        List<CreativeModeTab> result = new ArrayList<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            result.add(tab);
        }
        return List.copyOf(result);
    }

    public static CreativeModeTab get(ResourceLocation id) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
    }

    public static ResourceLocation id(CreativeModeTab tab) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
    }

    public static boolean contains(ResourceLocation id) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id);
    }

    private record Handle(ResourceLocation id, RegistryObject<CreativeModeTab> object) implements KineticRegistryHandle<CreativeModeTab> {
        @Override
        public boolean isPresent() {
            return object.isPresent();
        }

        @Override
        public CreativeModeTab get() {
            return object.get();
        }
    }

    private record ContextImpl(BuildCreativeModeTabContentsEvent event) implements KineticCreativeTabs.Context {
        @Override
        public ResourceKey<CreativeModeTab> tabKey() {
            return event.getTabKey();
        }

        @Override
        public void accept(ItemLike item) {
            event.accept(item);
        }

        @Override
        public void accept(ItemStack stack) {
            event.accept(stack);
        }
    }
}
