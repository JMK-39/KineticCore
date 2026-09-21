package dev.xyat.kineticcore.internal.client.search;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ItemSearchIndex {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CACHE_TOAST_ID = "cache_building";
    private static final Object CACHE_LOCK = new Object();
    private static final AtomicInteger THREAD_INDEX = new AtomicInteger();
    private static final AtomicInteger CACHE_GENERATION = new AtomicInteger();
    private static final int WORKER_COUNT = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    private static final ExecutorService CACHE_EXECUTOR = Executors.newFixedThreadPool(WORKER_COUNT, new CacheThreadFactory());

    private record ItemSnapshot(ItemStack stack, String idStr, String uniqueKey, String displayName, String namespace, List<String> tags) {
    }

    private static class CacheThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "kineticcore-ItemSearchIndex-" + THREAD_INDEX.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    public static class CachedItem {
        public final ItemStack stack;
        public final String idStr;
        public final String displayName;
        public final String namespace;
        public final List<String> tagIds;
        public final String searchData;

        public CachedItem(ItemStack stack) {
            this(stack, null);
        }

        private CachedItem(ItemStack stack, String identifierOverride) {
            this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
            this.idStr = identifierOverride == null ? cleanId(this.stack) : identifierOverride.trim();
            this.displayName = getDisplayName(this.stack);
            ResourceLocation location = KineticRegistries.items().id(this.stack.getItem());
            this.namespace = location == null ? "" : location.getNamespace();
            this.tagIds = List.copyOf(getRegistryTagIds(this.stack));
            this.searchData = buildSearchData(this.displayName, this.idStr, this.namespace, this.tagIds);
        }


        private CachedItem(ItemSnapshot snapshot, String searchData) {
            this.stack = snapshot.stack == null ? ItemStack.EMPTY : snapshot.stack;
            this.idStr = snapshot.idStr == null ? "" : snapshot.idStr;
            this.displayName = snapshot.displayName == null ? "" : snapshot.displayName;
            this.namespace = snapshot.namespace == null ? "" : snapshot.namespace;
            this.tagIds = snapshot.tags == null ? List.of() : snapshot.tags;
            this.searchData = searchData == null ? "" : searchData;
        }

        private static String getDisplayName(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return "";
            try {
                return stack.getHoverName().getString();
            } catch (Exception ignored) {
                return "";
            }
        }

        private static String cleanId(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return "";
            ResourceLocation id = KineticRegistries.items().id(stack.getItem());
            return id == null ? "" : id.toString();
        }

        private static String uniqueKey(ItemStack stack, String id) {
            if (stack == null || stack.isEmpty()) return "";
            if (stack.hasTag() && stack.getTag() != null) return id + "|" + stack.getTag();
            return id;
        }

        private static String buildSearchData(String displayName, String id, String namespace, List<String> tags) {
            StringBuilder searchBuilder = new StringBuilder();
            if (displayName != null && !displayName.isBlank()) searchBuilder.append(displayName);
            if (id != null && !id.isBlank()) searchBuilder.append(' ').append(id);
            if (namespace != null && !namespace.isBlank()) searchBuilder.append(" @").append(namespace);
            if (tags != null) {
                for (String tag : tags) searchBuilder.append(" #").append(tag);
            }
            return buildFinalSearchData(searchBuilder.toString());
        }
    }

    private static volatile List<CachedItem> CACHED_ITEMS = Collections.emptyList();
    private static final List<Runnable> PENDING_CALLBACKS = new ArrayList<>();
    private static volatile boolean isCaching = false;
    private static volatile boolean cacheReady = false;
    private static volatile int lastProgress = -1;
    private static String cacheLanguage;
    private static Object cacheConnectionToken;
    private static Object cacheLevelToken;

    private static Set<String> getRegistryTagIds(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Set.of();

        Set<String> tags = new TreeSet<>();
        try {
            stack.getItem().builtInRegistryHolder().tags()
                    .forEach(tag -> tags.add(tag.location().toString()));
        } catch (Throwable ignored) {
        }
        return tags.isEmpty() ? Set.of() : Set.copyOf(tags);
    }

    public static CachedItem customItem(ItemStack stack, String identifier) {
        return new CachedItem(stack, identifier);
    }

    public static List<CachedItem> getItems() {
        synchronized (CACHE_LOCK) {
            synchronizeCacheContextLocked();
            return CACHED_ITEMS;
        }
    }

    public static boolean isReady() {
        synchronized (CACHE_LOCK) {
            synchronizeCacheContextLocked();
            return cacheReady && !isCaching;
        }
    }

    public static void clear() {
        synchronized (CACHE_LOCK) {
            CACHE_GENERATION.incrementAndGet();
            CACHED_ITEMS = Collections.emptyList();
            PENDING_CALLBACKS.clear();
            isCaching = false;
            cacheReady = false;
            lastProgress = -1;
        }
    }

    public static void prepareCache(Runnable onDone) {
        int generation;
        synchronized (CACHE_LOCK) {
            synchronizeCacheContextLocked();
            if (isReady()) {
                runCallback(onDone);
                return;
            }

            if (onDone != null) {
                PENDING_CALLBACKS.add(onDone);
            }

            if (isCaching) {
                showProgressToast(CACHE_GENERATION.get(), Math.max(lastProgress, 0));
                return;
            }

            generation = CACHE_GENERATION.incrementAndGet();
            isCaching = true;
            cacheReady = false;
            lastProgress = 0;
            CACHED_ITEMS = Collections.emptyList();
        }

        showProgressToast(generation, 0);
        startBuildTask(generation);
    }

    private static void synchronizeCacheContextLocked() {
        Minecraft minecraft = Minecraft.getInstance();
        String currentLanguage = minecraft.getLanguageManager().getSelected();
        Object currentConnection = minecraft.getConnection();
        Object currentLevel = minecraft.level;
        if (java.util.Objects.equals(cacheLanguage, currentLanguage)
                && cacheConnectionToken == currentConnection
                && cacheLevelToken == currentLevel) {
            return;
        }

        if (isCaching) {
            CACHE_GENERATION.incrementAndGet();
        }
        CACHED_ITEMS = Collections.emptyList();
        PENDING_CALLBACKS.clear();
        isCaching = false;
        cacheReady = false;
        lastProgress = -1;
        cacheLanguage = currentLanguage;
        cacheConnectionToken = currentConnection;
        cacheLevelToken = currentLevel;
    }

    private static void startBuildTask(int generation) {
        executeOnClient(() -> {
            if (isStaleGeneration(generation)) return;
            try {
                List<ItemSnapshot> snapshots = collectSnapshots(generation);
                if (isStaleGeneration(generation)) return;
                updateProgressIfChanged(generation, 10);
                buildSnapshotsAsync(snapshots, generation);
            } catch (Throwable throwable) {
                failBuild(generation, throwable);
            }
        });
    }

    private static List<ItemSnapshot> collectSnapshots(int generation) {
        Set<String> seen = new HashSet<>();
        List<ItemSnapshot> snapshots = new ArrayList<>();

        var items = KineticRegistries.items().values();
        int itemTotal = Math.max(1, items.size());
        int itemCount = 0;

        for (var item : items) {
            if (item != null && item != Items.AIR) {
                addSnapshot(new ItemStack(item), seen, snapshots);
            }
            itemCount++;
            updateProgressIfChanged(generation, itemCount * 5 / itemTotal);
        }

        List<ItemStack> tabStacks = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            tabStacks.addAll(tab.getDisplayItems());
        }

        int tabTotal = Math.max(1, tabStacks.size());
        int tabCount = 0;

        for (ItemStack stack : tabStacks) {
            addSnapshot(stack, seen, snapshots);
            tabCount++;
            updateProgressIfChanged(generation, 5 + tabCount * 5 / tabTotal);
        }

        return snapshots;
    }

    private static void addSnapshot(ItemStack stack, Set<String> seen, List<ItemSnapshot> snapshots) {
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation idLoc = KineticRegistries.items().id(stack.getItem());
        if (idLoc == null) return;

        ItemStack copy = stack.copy();
        String idStr = idLoc.toString();
        String uniqueKey = CachedItem.uniqueKey(copy, idStr);
        if (idStr.isBlank() || uniqueKey.isBlank() || !seen.add(uniqueKey)) return;

        String displayName = "";
        try {
            displayName = copy.getHoverName().getString();
        } catch (Exception ignored) {
        }

        List<String> tags = new ArrayList<>(getRegistryTagIds(copy));

        snapshots.add(new ItemSnapshot(copy, idStr, uniqueKey, displayName, idLoc.getNamespace(), List.copyOf(tags)));
    }

    private static void buildSnapshotsAsync(List<ItemSnapshot> snapshots, int generation) {
        if (isStaleGeneration(generation)) return;
        if (snapshots.isEmpty()) {
            executeOnClient(() -> finishBuild(generation, Collections.emptyList()));
            return;
        }

        int total = snapshots.size();
        int chunkSize = Math.max(1, (int) Math.ceil((double) total / WORKER_COUNT));
        AtomicInteger finished = new AtomicInteger();
        List<CompletableFuture<List<CachedItem>>> futures = new ArrayList<>();

        for (int start = 0; start < total; start += chunkSize) {
            int end = Math.min(total, start + chunkSize);
            List<ItemSnapshot> chunk = snapshots.subList(start, end);
            futures.add(CompletableFuture.supplyAsync(() -> buildChunk(chunk, finished, total, generation), CACHE_EXECUTOR));
        }

        CompletableFuture<?>[] futureArray = futures.toArray(new CompletableFuture<?>[0]);
        CompletableFuture.allOf(futureArray).whenComplete((ignored, throwable) -> {
            if (isStaleGeneration(generation)) return;
            if (throwable != null) {
                executeOnClient(() -> failBuild(generation, throwable));
                return;
            }

            List<CachedItem> result = new ArrayList<>(total);

            try {
                for (CompletableFuture<List<CachedItem>> future : futures) {
                    result.addAll(future.join());
                }
            } catch (CompletionException exception) {
                executeOnClient(() -> failBuild(generation, exception));
                return;
            }

            updateProgressIfChanged(generation, 99);
            executeOnClient(() -> finishBuild(generation, result));
        });
    }

    private static List<CachedItem> buildChunk(List<ItemSnapshot> snapshots, AtomicInteger finished, int total, int generation) {
        List<CachedItem> result = new ArrayList<>(snapshots.size());

        for (ItemSnapshot snapshot : snapshots) {
            if (isStaleGeneration(generation)) break;
            try {
                String searchData = buildSearchData(snapshot);
                result.add(new CachedItem(snapshot, searchData));
            } catch (Throwable ignored) {
            } finally {
                int done = finished.incrementAndGet();
                updateProgressIfChanged(generation, 10 + done * 89 / total);
            }
        }

        return result;
    }

    private static String buildSearchData(ItemSnapshot snapshot) {
        return CachedItem.buildSearchData(
                snapshot.displayName,
                snapshot.idStr,
                snapshot.namespace,
                snapshot.tags
        );
    }

    private static String buildFinalSearchData(String raw) {
        String searchRaw = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        try {
            return KineticSearchRuntime.normalize(searchRaw + " " + KineticSearchRuntime.pinyinSearchData(searchRaw));
        } catch (Throwable ignored) {
            return KineticSearchRuntime.normalize(searchRaw);
        }
    }

    private static boolean isStaleGeneration(int generation) {
        return generation != CACHE_GENERATION.get();
    }

    private static void updateProgressIfChanged(int generation, int progress) {
        int safeProgress = Math.max(0, Math.min(99, progress));
        synchronized (CACHE_LOCK) {
            if (isStaleGeneration(generation) || !isCaching || safeProgress <= lastProgress) return;
            lastProgress = safeProgress;
        }
        showProgressToast(generation, safeProgress);
    }

    private static void showProgressToast(int generation, int progress) {
        executeOnClient(() -> {
            if (isStaleGeneration(generation) || !isCaching) return;
            KineticOverlays.toast(
                    CACHE_TOAST_ID,
                    KineticText.translatable("gui.kineticcore.items.cache.building", Component.literal(progress + "%")),
                    KineticOverlays.Position.BOTTOM_CENTER,
                    3000,
                    0,
                    -30
            );
        });
    }

    private static void finishBuild(int generation, List<CachedItem> tempCache) {
        List<Runnable> callbacks;

        synchronized (CACHE_LOCK) {
            if (isStaleGeneration(generation) || !isCaching) return;
            CACHED_ITEMS = List.copyOf(tempCache);
            isCaching = false;
            cacheReady = true;
            lastProgress = -1;
            callbacks = new ArrayList<>(PENDING_CALLBACKS);
            PENDING_CALLBACKS.clear();
        }

        try {
            KineticOverlays.toast(
                    CACHE_TOAST_ID,
                    KineticText.translatable("gui.kineticcore.items.cache.done", Component.literal(String.valueOf(tempCache.size()))),
                    KineticOverlays.Position.BOTTOM_CENTER,
                    2500,
                    0,
                    -30
            );
        } catch (RuntimeException | Error failure) {
            // Publishing the completion toast is optional. All selectors waiting for
            // this successful cache build must still receive their callbacks.
            LOGGER.error("Failed to show completed item-search cache notification", failure);
        }

        for (Runnable callback : callbacks) {
            runCallback(callback);
        }
    }

    private static void failBuild(int generation, Throwable throwable) {
        synchronized (CACHE_LOCK) {
            if (isStaleGeneration(generation) || !isCaching) return;
            CACHED_ITEMS = Collections.emptyList();
            isCaching = false;
            cacheReady = false;
            lastProgress = -1;
            PENDING_CALLBACKS.clear();
        }

        LOGGER.error("Cache build failed", throwable);

        KineticOverlays.toast(
                CACHE_TOAST_ID,
                KineticText.translatable("gui.kineticcore.items.cache.failed"),
                KineticOverlays.Position.BOTTOM_CENTER,
                4000,
                0,
                -30
        );
    }

    private static void runCallback(Runnable callback) {
        if (callback == null) return;
        // One addon callback must not interrupt delivery to other waiting selectors.
        executeOnClient(() -> {
            try {
                callback.run();
            } catch (Throwable failure) {
                LOGGER.error("Item-search cache callback failed", failure);
            }
        });
    }

    private static void executeOnClient(Runnable runnable) {
        // Some executors run tasks synchronously and propagate task failures;
        // others can throw after accepting a task. The fallback must never run
        // an already accepted task a second time.
        AtomicBoolean started = new AtomicBoolean();
        Runnable guarded = () -> {
            if (started.compareAndSet(false, true)) runnable.run();
        };
        try {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(guarded);
        } catch (Throwable failure) {
            if (started.get()) throw failure;
            guarded.run();
        }
    }
}
