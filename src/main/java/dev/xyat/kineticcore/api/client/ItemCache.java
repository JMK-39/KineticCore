package dev.xyat.kineticcore.api.client;

import net.minecraft.ChatFormatting;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
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

public class ItemCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CACHE_TOAST_ID = "cache_building";
    private static final Object CACHE_LOCK = new Object();
    private static final AtomicInteger THREAD_INDEX = new AtomicInteger();
    private static final int WORKER_COUNT = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    private static final ExecutorService CACHE_EXECUTOR = Executors.newFixedThreadPool(WORKER_COUNT, new CacheThreadFactory());

    private record ItemSnapshot(ItemStack stack, String idStr, String uniqueKey, String displayName, String namespace, List<String> tags) {
    }

    private static class CacheThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "kineticcore-ItemCache-" + THREAD_INDEX.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    public static class CachedItem {
        public final ItemStack stack;
        public final String idStr;
        public final String uniqueKey;
        public final String displayName;
        public final String namespace;
        public final List<String> tagIds;
        public final String searchData;

        public CachedItem(ItemStack stack) {
            this(stack, null);
        }

        private CachedItem(ItemStack stack, String idOverride) {
            this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
            String clean = idOverride == null ? cleanId(this.stack) : idOverride.trim();
            this.idStr = clean;
            this.uniqueKey = idOverride == null ? uniqueKey(this.stack, this.idStr) : clean;
            this.displayName = getDisplayName(this.stack);
            ResourceLocation location = ForgeRegistries.ITEMS.getKey(this.stack.getItem());
            this.namespace = location == null ? "" : location.getNamespace();
            this.tagIds = List.copyOf(getRegistryTagIds(this.stack));
            this.searchData = buildSearchData(this.displayName, this.idStr, this.namespace, this.tagIds);
        }

        private CachedItem(ItemSnapshot snapshot, String searchData) {
            this.stack = snapshot.stack == null ? ItemStack.EMPTY : snapshot.stack;
            this.idStr = snapshot.idStr == null ? "" : snapshot.idStr;
            this.uniqueKey = snapshot.uniqueKey == null ? this.idStr : snapshot.uniqueKey;
            this.displayName = snapshot.displayName == null ? "" : snapshot.displayName;
            this.namespace = snapshot.namespace == null ? "" : snapshot.namespace;
            this.tagIds = snapshot.tags == null ? List.of() : snapshot.tags;
            this.searchData = searchData == null ? "" : searchData;
        }

        public static CachedItem custom(ItemStack stack, String idStr) {
            return new CachedItem(stack, idStr);
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
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
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

    public static String getUniqueKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : CachedItem.uniqueKey(stack, id.toString());
    }

    public static Set<String> getRegistryTagIds(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Set.of();

        Set<String> tags = new TreeSet<>();
        try {
            stack.getItem().builtInRegistryHolder().tags()
                    .forEach(tag -> tags.add(tag.location().toString()));
        } catch (Throwable ignored) {
        }
        return tags.isEmpty() ? Set.of() : Set.copyOf(tags);
    }

    public static List<CachedItem> getItems() {
        return CACHED_ITEMS;
    }

    public static boolean isReady() {
        return cacheReady && !isCaching;
    }

    public static void clear() {
        synchronized (CACHE_LOCK) {
            CACHED_ITEMS = Collections.emptyList();
            PENDING_CALLBACKS.clear();
            isCaching = false;
            cacheReady = false;
            lastProgress = -1;
        }
        executeOnClient(() -> GuiToastUtil.removeToast(CACHE_TOAST_ID));
    }

    public static void prepareCache(Runnable onDone) {
        synchronized (CACHE_LOCK) {
            if (isReady()) {
                runCallback(onDone);
                return;
            }

            if (onDone != null) {
                PENDING_CALLBACKS.add(onDone);
            }

            if (isCaching) {
                showProgressToast(Math.max(lastProgress, 0));
                return;
            }

            isCaching = true;
            cacheReady = false;
            lastProgress = 0;
            CACHED_ITEMS = Collections.emptyList();
        }

        showProgressToast(0);
        startBuildTask();
    }

    private static void startBuildTask() {
        executeOnClient(() -> {
            try {
                List<ItemSnapshot> snapshots = collectSnapshots();
                updateProgressIfChanged(10);
                buildSnapshotsAsync(snapshots);
            } catch (Throwable throwable) {
                failBuild(throwable);
            }
        });
    }

    private static List<ItemSnapshot> collectSnapshots() {
        Set<String> seen = new HashSet<>();
        List<ItemSnapshot> snapshots = new ArrayList<>();

        var items = ForgeRegistries.ITEMS.getValues();
        int itemTotal = Math.max(1, items.size());
        int itemCount = 0;

        for (var item : items) {
            if (item != null && item != Items.AIR) {
                addSnapshot(new ItemStack(item), seen, snapshots);
            }
            itemCount++;
            updateProgressIfChanged(itemCount * 5 / itemTotal);
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
            updateProgressIfChanged(5 + tabCount * 5 / tabTotal);
        }

        return snapshots;
    }

    private static void addSnapshot(ItemStack stack, Set<String> seen, List<ItemSnapshot> snapshots) {
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation idLoc = ForgeRegistries.ITEMS.getKey(stack.getItem());
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

    private static void buildSnapshotsAsync(List<ItemSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            executeOnClient(() -> finishBuild(Collections.emptyList()));
            return;
        }

        int total = snapshots.size();
        int chunkSize = Math.max(1, (int) Math.ceil((double) total / WORKER_COUNT));
        AtomicInteger finished = new AtomicInteger();
        List<CompletableFuture<List<CachedItem>>> futures = new ArrayList<>();

        for (int start = 0; start < total; start += chunkSize) {
            int end = Math.min(total, start + chunkSize);
            List<ItemSnapshot> chunk = snapshots.subList(start, end);
            futures.add(CompletableFuture.supplyAsync(() -> buildChunk(chunk, finished, total), CACHE_EXECUTOR));
        }

        CompletableFuture<?>[] futureArray = futures.toArray(new CompletableFuture<?>[0]);
        CompletableFuture.allOf(futureArray).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                executeOnClient(() -> failBuild(throwable));
                return;
            }

            List<CachedItem> result = new ArrayList<>(total);

            try {
                for (CompletableFuture<List<CachedItem>> future : futures) {
                    result.addAll(future.join());
                }
            } catch (CompletionException exception) {
                executeOnClient(() -> failBuild(exception));
                return;
            }

            updateProgressIfChanged(99);
            executeOnClient(() -> finishBuild(result));
        });
    }

    private static List<CachedItem> buildChunk(List<ItemSnapshot> snapshots, AtomicInteger finished, int total) {
        List<CachedItem> result = new ArrayList<>(snapshots.size());

        for (ItemSnapshot snapshot : snapshots) {
            try {
                String searchData = buildSearchData(snapshot);
                result.add(new CachedItem(snapshot, searchData));
            } catch (Throwable ignored) {
            } finally {
                int done = finished.incrementAndGet();
                updateProgressIfChanged(10 + done * 89 / total);
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
            return AdvancedSearchUtil.normalizeForSearch(searchRaw + " " + PinyinUtil.getSearchData(searchRaw));
        } catch (Throwable ignored) {
            return AdvancedSearchUtil.normalizeForSearch(searchRaw);
        }
    }

    private static void updateProgressIfChanged(int progress) {
        int safeProgress = Math.max(0, Math.min(99, progress));
        if (safeProgress == lastProgress) return;
        lastProgress = safeProgress;
        showProgressToast(safeProgress);
    }

    private static void showProgressToast(int progress) {
        executeOnClient(() -> GuiToastUtil.showToast(
                CACHE_TOAST_ID,
                Component.translatable("gui.kineticcore.items.cache.building", Component.literal(progress + "%").withStyle(ChatFormatting.YELLOW)),
                GuiToastUtil.Position.BOTTOM_CENTER,
                3000,
                0,
                -30
        ));
    }

    private static void finishBuild(List<CachedItem> tempCache) {
        List<Runnable> callbacks;

        synchronized (CACHE_LOCK) {
            CACHED_ITEMS = List.copyOf(tempCache);
            isCaching = false;
            cacheReady = true;
            lastProgress = -1;
            callbacks = new ArrayList<>(PENDING_CALLBACKS);
            PENDING_CALLBACKS.clear();
        }

        GuiToastUtil.showToast(
                CACHE_TOAST_ID,
                Component.translatable("gui.kineticcore.items.cache.done", Component.literal(String.valueOf(tempCache.size())).withStyle(ChatFormatting.GREEN)),
                GuiToastUtil.Position.BOTTOM_CENTER,
                2500,
                0,
                -30
        );

        for (Runnable callback : callbacks) {
            runCallback(callback);
        }
    }

    private static void failBuild(Throwable throwable) {
        LOGGER.error("Cache build failed", throwable);

        synchronized (CACHE_LOCK) {
            CACHED_ITEMS = Collections.emptyList();
            isCaching = false;
            cacheReady = false;
            lastProgress = -1;
            PENDING_CALLBACKS.clear();
        }

        GuiToastUtil.showToast(
                CACHE_TOAST_ID,
                Component.translatable("gui.kineticcore.items.cache.failed"),
                GuiToastUtil.Position.BOTTOM_CENTER,
                4000,
                0,
                -30
        );
    }

    private static void runCallback(Runnable callback) {
        if (callback == null) return;
        executeOnClient(callback);
    }

    private static void executeOnClient(Runnable runnable) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(runnable);
        } catch (Throwable ignored) {
            runnable.run();
        }
    }
}