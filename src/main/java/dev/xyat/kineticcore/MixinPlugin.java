package dev.xyat.kineticcore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger("kineticcore/MixinPlugin");
    private static final Path CONFIG_FILE_PATH = Paths.get("config", "kineticcore", "mixin_toggles.toml");
    private static final String MOD_PACKAGE_PREFIX = "dev.xyat.kineticcore.";

    public static final Map<String, Boolean> MIXIN_TOGGLES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> CONFIGURED_MIXIN_TOGGLES = new ConcurrentHashMap<>();
    private final List<ToggleInfo> masterList = new ArrayList<>();
    private static volatile boolean isInitialized = false;
    private static volatile MixinPlugin activeInstance;

    private static class ToggleInfo {
        String key;
        boolean defaultValue;
        String commentZh;
        String commentEn;
        boolean isHeader = false;

        ToggleInfo(String key, boolean defaultValue, String commentZh, String commentEn) {
            this.key = key; this.defaultValue = defaultValue; this.commentZh = commentZh; this.commentEn = commentEn;
        }

        ToggleInfo(String titleZh, String titleEn) {
            this.commentZh = titleZh; this.commentEn = titleEn; this.isHeader = true;
        }
    }

    /** Read-only UI view of the same sections and comments written to TOML. */
    public record ToggleDescriptor(
            String key,
            boolean defaultValue,
            String titleZh,
            String titleEn,
            String commentZh,
            String commentEn,
            boolean header
    ) {
    }

    public static boolean isFeatureEnabled(String key) {
        return switch (key) {
            case "feature.effects.LivingEntityAccessor" -> rawFeatureEnabled("feature.effects.MiniEffectsMixins");
            case "feature.defaultoptions.KeyAccess" -> rawFeatureEnabled("feature.defaultoptions.DefaultOptionsMixins");
            case "feature.recipebook.ButtonAccess" -> rawFeatureEnabled("feature.recipebook.RecipeBookClientMixins");
            default -> rawFeatureEnabled(key);
        };
    }

    private static boolean rawFeatureEnabled(String key) {
        return MIXIN_TOGGLES.getOrDefault(key, true);
    }

    public static List<String> toggleKeys() {
        MixinPlugin plugin = activeInstance;
        if (plugin == null) return MIXIN_TOGGLES.keySet().stream().sorted().toList();
        return plugin.masterList.stream()
                .filter(info -> !info.isHeader)
                .map(info -> info.key)
                .toList();
    }

    public static List<ToggleDescriptor> toggleDescriptors() {
        MixinPlugin plugin = activeInstance;
        if (plugin == null) {
            return MIXIN_TOGGLES.keySet().stream()
                    .sorted()
                    .map(key -> new ToggleDescriptor(
                            key, true, null, null, null, null, false
                    ))
                    .toList();
        }
        return plugin.masterList.stream()
                .map(info -> info.isHeader
                        ? new ToggleDescriptor(
                                null, true,
                                info.commentZh, info.commentEn,
                                null, null, true
                        )
                        : new ToggleDescriptor(
                                info.key, info.defaultValue,
                                null, null,
                                info.commentZh, info.commentEn, false
                        ))
                .toList();
    }

    public static boolean configuredFeatureEnabled(String key) {
        return CONFIGURED_MIXIN_TOGGLES.getOrDefault(key, isFeatureEnabled(key));
    }

    public static void setConfiguredFeatureEnabled(String key, boolean enabled) {
        if (!MIXIN_TOGGLES.containsKey(key)) {
            throw new IllegalArgumentException("Unknown mixin toggle: " + key);
        }
        CONFIGURED_MIXIN_TOGGLES.put(key, enabled);
    }

    public static synchronized void saveConfiguredToggles() {
        MixinPlugin plugin = activeInstance;
        if (plugin == null) {
            throw new IllegalStateException("Mixin configuration is not initialized");
        }
        plugin.writeToDisk(CONFIGURED_MIXIN_TOGGLES);
    }

    @Override
    public void onLoad(String mixinPackage) {
        if (isInitialized) return;
        synchronized (MixinPlugin.class) {
            if (isInitialized) return;
            activeInstance = this;
            try {
                defineToggles();

                // 1. 初始化标准列表的默认值
                for (ToggleInfo info : masterList) {
                    if (!info.isHeader) MIXIN_TOGGLES.put(info.key, info.defaultValue);
                }

                // 2. 加载磁盘旧配置，读取玩家的选择
                Map<String, Boolean> diskConfig = loadFromDisk();
                for (Map.Entry<String, Boolean> entry : diskConfig.entrySet()) {
                    // 仅当当前 masterList 中仍包含该 key 时，才覆盖默认值。旧的、被废弃的 key 会被直接抛弃
                    if (MIXIN_TOGGLES.containsKey(entry.getKey())) {
                        MIXIN_TOGGLES.put(entry.getKey(), entry.getValue());
                    }
                }

                CONFIGURED_MIXIN_TOGGLES.clear();
                CONFIGURED_MIXIN_TOGGLES.putAll(MIXIN_TOGGLES);

                // 3. 删除旧文件，重新生成一份绝对干净、按最新架构排列的配置文件
                writeToDisk();
                LOGGER.info("kineticcore Mixin Config Loaded. {} toggles defined, {} overrides applied.",
                        masterList.stream().filter(i -> !i.isHeader).count(), diskConfig.size());
            } catch (Exception e) {
                LOGGER.error("Failed to initialize MixinPlugin.", e);
            } finally {
                isInitialized = true;
            }
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String key = parseConfigKey(mixinClassName);
        if (key.contains("$")) {
            key = key.substring(0, key.indexOf('$'));
        }
        return isFeatureEnabled(key);
    }

    private void defineToggles() {

        // --- 飞行与移动控制 ---
        addToggle("飞行与移动控制", "Flight & Movement Control");
        addToggle("feature.flight.FlightServerMixins",
                """
                        服务端飞行核心：
                        # 1. 飞行守门员：防止第三方模组或网络波动意外关闭玩家飞行姿态，解决掉落问题。
                        # 2. 跨维度恢复：确保玩家在跨维度、死亡重生或切换游戏模式后仍能继承飞行状态。
                        # 3. 拦截校验：拦截服务端对创造模式穿墙(No-Clip)的非法移动拉回。
                        # 4. 速度限制移除：彻底禁用原版服务端对行走、鞘翅飞行、载具移动的 256/300 码速度限制校验。
                        # 5. 实体尺寸干涉：允许在启用穿墙时动态修改玩家碰撞箱和眼高。""",
                """
                        Server Flight Core:
                        # 1. Flight Guardian: Prevents accidental flight loss from mod conflicts or network jitter.
                        # 2. State Inheritance: Retains flight status across dimensions, respawn, or gamemode swaps.
                        # 3. Check Interception: Bypasses server-side movement 'rubber-banding' during No-Clip.
                        # 4. Speed Limit Removal: Disables vanilla server-side walking/elytra/vehicle speed checks.
                        # 5. Dimensions Hack: Modifies hitboxes and eye-height during No-Clip.""");

        addToggle("feature.flight.FlightClientMixins",
                """
                        客户端移动与物理：
                        # 1. 惯性抑制：当停止按键时，强制清除飞行惯性，实现瞬间起停，大幅提升建筑操作精准度。
                        # 2. 穿墙物理：接管 LocalPlayer 物理循环，实现客户端平滑穿墙。
                        # 3. 动态调速：支持创造模式下通过 Alt+Shift+滚轮 动态调节飞行速度（0.1x-100x）。
                        # 4. 视觉防抖：修正模式切换瞬间玩家会向下坠落一下的视觉抖动问题。""",
                """
                        Client Movement & Physics:
                        # 1. Inertia Suppression: Instantly stops movement when input is released for precise building.
                        # 2. Noclip Physics: Overrides local player physics for smooth client-side noclip.
                        # 3. Dynamic Speed: Adjust creative flight speed (0.1x-100x) with Alt+Shift+Scroll.
                        # 4. Anti-Jitter: Fixes the visual falling glitch during gamemode swaps.""");


        // --- 世界出生点 ---
        addToggle("世界出生点", "World Spawn Override");
        addToggle("feature.setspawn.SetSpawnMixins",
                "自定义世界出生点：接管首次加入、无床复活、共享出生点坐标以及出生区块准备流程。",
                "Custom world spawn override: handles first join, no-bed respawn fallback, exact shared-spawn coordinates, and spawn-chunk preparation.");

        // --- 存档管理 ---
        addToggle("存档管理", "World Management");
        addToggle("feature.worldmanagement.WorldManagementMixins", "存档管理：包含存档回收站和界面返回逻辑。", "World Management: Includes recycle bin and navigation logic.");

        // --- 蜜蜂修复与优化 ---
        addToggle("蜜蜂修复与优化", "Bee Fixes & Tweaks");

        addToggle("feature.bee.BeeMixins",
                "服务端蜜蜂修复与微型化：移除重力影响、修复寻路浮点数漂移导致的偏移、修复生成位置偏移(MC-206401)、防止蜜蜂破坏海龟蛋，并将物理碰撞箱与视线高度缩小至 25%。",
                "Server-side Bee Tweaks: Removes gravity effect, fixes pathfinding float drift, fixes spawn position offset (MC-206401), prevents bees from destroying turtle eggs, and scales physical hitbox and eye height to 25%.");

        addToggle("feature.bee.BeeRendererMixin",
                "客户端蜜蜂渲染修复与微型化：为蜜蜂强制设定翻转角度（180度），并将体积缩小至原来的 25%。",
                "Client-side Bee Renderer Tweaks: Forces flip degrees to 180.0F for bees, and reduced the volume to 25% of the original.");

        // --- 玩家与实体逻辑 ---
        addToggle("玩家与实体逻辑", "Player & Entity Logic");
        addToggle("feature.crawl.PlayerCrawlPoseMixin", "允许玩家主动进入爬行姿态。", "Allows players to actively trigger crawling state.");

        // --- 原版系统优化 ---
        addToggle("原版系统优化", "Vanilla System Tweaks");
        addToggle("feature.recipebook.RecipeBookServerMixins",
                "配方书服务端移除：停止保存、读取、同步和授予配方，并过滤 recipes/ 进度数据。",
                "Recipe Book Server Removal: Stops recipe-book save/load/sync/award operations and filters recipes/ advancements.");
        addToggle("feature.recipebook.RecipeBookClientMixins",
                "配方书客户端移除：阻止客户端构建配方集合，并移除原版配方书按钮。",
                "Recipe Book Client Removal: Prevents client recipe collection setup and removes the vanilla recipe-book button.");
        addToggle("feature.attribute.RangedAttributeAccessor",
                "属性范围解限：允许配置并修改 RangedAttribute 的最小值与最大值。",
                "Attribute Range Uncapping: Allows configured min/max limits to be applied to RangedAttribute instances.");

        // --- 性能监控 ---
        addToggle("性能监控", "Performance Monitoring");
        addToggle("feature.tps.ServerMixin",
                "TPS/MSPT 采样：记录服务端 Tick 耗时，为 /kt tps 与 TPS HUD 提供统计数据。",
                "TPS/MSPT Sampling: Records server tick times for /kt tps reports and the TPS HUD.");

        // --- 性能与实体优化 ---
        addToggle("性能与实体优化", "Optimization & Entity Tweaks");
        addToggle("feature.damageindicator.ServerLevelMixin", "禁用伤害指示器粒子生成，优化战斗性能。", "Disables damage indicator particles to improve combat performance.");
        addToggle("feature.despawn.MobDespawnMixins",
                """
                        生物消失逻辑优化（Let Me Despawn）：
                        # 1. 防止实体积压：解决原版中僵尸、骷髅等生物捡起地上的垃圾物品后会永久占据实体位而不消失的问题，有效缓解服务器长期运行后的卡顿。
                        # 2. 末影人优化：允许手持方块的末影人像普通生物一样自然消失，防止末地或主世界地表堆积大量末影人。
                        # 3. 物品掉落：当受此逻辑影响的生物消失（Despawn）时，它捡起的物品会重新掉落在地上，不会导致玩家掉落的装备永久丢失。
                        # 4. 智能识别：仅针对捡起物品产生的“强制持久化”进行拦截，玩家通过命名牌命名或手动生成的生物依然会保留。""",
                """
                        Mob Despawning Optimizations (Let Me Despawn):
                        # 1. Entity Backlog Prevention: Fixes the vanilla issue where mobs (Zombies, Skeletons, etc.) become persistent after picking up items, reducing long-term server lag.
                        # 2. Endermen Tweak: Allows Endermen holding blocks to despawn naturally like other mobs, preventing entity buildup.
                        # 3. Item Recovery: When a mob despawns via this logic, any picked-up equipment is dropped back onto the ground, preventing gear loss.
                        # 4. Smart Persistence: Only intercepts automatic persistence from item pickups; mobs named with Name Tags or spawned manually remain persistent.""");

        // --- 性能与渲染修复 ---
        addToggle("性能与渲染修复", "Performance & Rendering Fixes");
        addToggle("feature.gpufix.RenderTargetMixin",
                """
                        GPU 内存泄漏修复 (VRAM Leak Fix)：
                        # 1. 拦截 RenderTarget (帧缓冲区对象) 的垃圾回收过程。
                        # 2. 当底层 OpenGL 纹理或缓冲区未被正常释放即被 Java 销毁时，接管其句柄。
                        # 3. 将遗留的 OpenGL ID 放入主线程队列进行安全销毁，有效防止长时间游戏后的显存溢出。""",
                """
                        GPU Memory Leak Fix:
                        # 1. Intercepts the garbage collection of RenderTargets.
                        # 2. Catches OpenGL texture and framebuffer IDs that were abandoned without being properly deleted.
                        # 3. Queues them for safe deletion on the main thread, preventing VRAM leaks over long play sessions.""");

        // --- 客户端界面优化 ---
        addToggle("客户端界面优化", "Client UI & Interaction");
        addToggle("feature.copyitem.AbstractContainerScreenAccessor",
                "物品复制工具容器访问器：允许 Alt+C / Alt+F 在原版及模组容器界面准确读取鼠标悬浮槽位。",
                "Copy-item container accessor: lets Alt+C / Alt+F accurately read the hovered slot in vanilla and modded container screens.");

        addToggle("feature.defaultoptions.DefaultOptionsMixins",
                "默认选项加载：在 options.txt 首次创建或异常损坏时应用 config/kineticcore/defaultoptions.txt，并同步自定义默认按键。",
                "Default options loader: applies config/kineticcore/defaultoptions.txt when options.txt is first created or damaged, including custom default key bindings.");

        addToggle("feature.clientui.ClientInterfaceMixins",
                """
                        客户端界面与流程自动化增强：
                        # 1. 彻底禁用复述器：阻止 Narrator 底层库加载，禁用其所有功能及快捷键，避免误触和潜在的库加载卡顿。
                        # 2. 多人列表清理：移除多人服务器列表中 Forge 模组版本的详细信息图标，使界面更整洁。
                        # 3. 智能弹窗拦截：将进入非正版/离线服务器时的全屏系统弹窗拦截，改为非侵入式的聊天栏金色文字提醒。
                        # 4. 自动化存档加载：创建世界或加载实验性存档时，自动跳过“实验性设置”和“数据包确认”二次弹窗，并强制标记生命周期为稳定，防止警告刷屏。
                        # 5. 阻止 UI 强制跳转：拦截因版本不兼容、数据包损坏或存档缺失而导致的自动跳转至“创建新世界”界面的行为，允许玩家保留在当前菜单进行手动调整。""",
                """
                        Client interface and workflow automation enhancements:
                        # 1. Narrator Removal: Prevents native narrator library loading and disables all TTS features/hotkeys to avoid accidental triggers and potential lag.
                        # 2. UI Cleanup: Removes Forge mod compatibility icons from the multiplayer screen for a cleaner visual experience.
                        # 3. Smart Toast Interception: Replaces intrusive 'Unsecure Server' system toasts with a non-intrusive gold-colored chat message alert.
                        # 4. Seamless World Loading: Automatically skips 'Experimental Settings' and 'Datapack' confirmation screens; forces world lifecycle to 'stable' to suppress warnings.
                        # 5. UI Redirect Prevention: Blocks the automated transition to the 'Create New World' screen triggered by version mismatch, missing saves, or datapack errors, allowing players to remain on the current menu for manual adjustment.""");

        addToggle("feature.effects.MiniEffectsMixins",
                """
                        状态效果 HUD 重构 (Mini Effects)：
                        # 1. 智能布局：突破原版挤压限制，效果卡片会利用屏幕 90% 的垂直空间自动等距排开，彻底告别重叠盲区。
                        # 2. 悬停置顶：鼠标悬停任意卡片时，该卡片立刻突破图层遮挡，置于最顶层清晰显示。
                        # 3. 兼容防冲突：完全避开 JEI 区域；完美接管并修复部分模组左侧渲染越界问题。
                        # 4. 夜视防闪烁：接管 GameRenderer 的夜视亮度计算，移除原版夜视快结束时的正弦闪烁，避免屏幕忽明忽暗。""",
                """
                        Status Effect HUD Overhaul (Mini Effects):
                        # 1. Smart Layout: Breaks vanilla squeeze limits. Cards now use up to 90% of vertical space, distributed evenly.
                        # 2. Hover-to-Top: Hovering over any card instantly brings it to the top layer, breaking through vanilla rendering occlusion.
                        # 3. Compatibility: Fully dodges JEI areas and fixes out-of-bounds left-side rendering.
                        # 4. Night Vision Anti-Flicker: Overrides GameRenderer night-vision brightness calculation and removes the vanilla sine flicker near effect expiration.""");

        // --- 网络与协议解限 ---
        addToggle("网络与协议解限", "Network Protocol");
        addToggle("feature.networklimit.NetworkLimitMixins",
                """
                        网络数据包与底层协议解限：
                        # 1. 突破硬编码限制：彻底接管并解除原版 Netty 底层对 NBT、字符串、区块数据、Payload 通信包的大小限制。
                        # 2. 大型模组支持：完美解决 "Payload may not be larger than..." 或 "VarInt too big" 等导致玩家断开连接的网络溢出错误。
                        # 3. 动态扩展：配合 config 中的全局网络限制参数，提供安全且极具弹性的网络数据流吐吞能力。""",
                """
                        Network Packet & Protocol Uncapping:
                        # 1. Break Hardcoded Limits: Completely overrides vanilla Netty restrictions on NBT, Strings, Chunk data, and Payload packets.
                        # 2. Heavy Modpack Support: Resolves "Payload may not be larger than..." or "VarInt too big" disconnect errors caused by network overflow.
                        # 3. Dynamic Scaling: Works with the global network limit config to provide secure and highly elastic network throughput.""");


        // --- 模组兼容 ---
        addToggle("模组兼容性", "Mod Compatibility");

    }

    private void addToggle(String titleZh, String titleEn) {
        masterList.add(new ToggleInfo(titleZh, titleEn));
    }

    private void addToggle(String key, String commentZh, String commentEn) {
        masterList.add(new ToggleInfo(key, true, commentZh, commentEn));
    }

    private String parseConfigKey(String mixinClassName) {
        if (!mixinClassName.startsWith(MOD_PACKAGE_PREFIX)) return mixinClassName;
        String key = mixinClassName.substring(MOD_PACKAGE_PREFIX.length());
        key = key.replace(".mixin.client.", ".").replace(".mixin.", ".");
        return key;
    }

    private Map<String, Boolean> loadFromDisk() {
        Map<String, Boolean> map = new HashMap<>();
        if (!Files.exists(CONFIG_FILE_PATH)) return map;
        try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE_PATH, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) map.put(parts[0].trim(), parts[1].trim().equalsIgnoreCase("true"));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        }
        return map;
    }

    private void writeToDisk() {
        writeToDisk(MIXIN_TOGGLES);
    }

    private void writeToDisk(Map<String, Boolean> values) {
        Path temporaryFile = null;
        try {
            Path parent = CONFIG_FILE_PATH.getParent();
            if (parent != null) Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, "mixin_toggles-", ".tmp");
            Map<String, Boolean> snapshot = Map.copyOf(values);

            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                // 开头注释严格遵循：不加分割线，中文一行，英文一行
                writeLine(writer, "# kineticcore 核心注入机制控制面板");
                writeLine(writer, "# kineticcore Mixin Toggles");
                writeLine(writer, "# 格式: Module.ClassName = true/false");
                writeLine(writer, "# Format: Module.ClassName = true/false");
                writeLine(writer, "# 注意: 修改此文件后必须重启游戏才能生效");
                writeLine(writer, "# Notice: Restart required after changes");

                for (ToggleInfo info : masterList) {
                    if (info.isHeader) {
                        writer.newLine();
                        writeLine(writer, "# [" + info.commentZh + "]");
                        writeLine(writer, "# [" + info.commentEn + "]");
                    } else {
                        writer.newLine();
                        writeCommentLine(writer, info.commentZh);
                        writeCommentLine(writer, info.commentEn);
                        writeLine(writer, info.key + " = " + snapshot.getOrDefault(info.key, info.defaultValue));
                    }
                }
            }

            try {
                Files.move(
                        temporaryFile,
                        CONFIG_FILE_PATH,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, CONFIG_FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            temporaryFile = null;
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
            throw new UncheckedIOException(e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupFailure) {
                    LOGGER.warn("Failed to remove temporary mixin config {}", temporaryFile, cleanupFailure);
                }
            }
        }
    }

    /**
     * 智能注释排版方法
     * 自动处理多行文本，确保按严格格式输出 (无多余空行，对齐 #)
     */
    private static void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    private void writeCommentLine(BufferedWriter writer, String comment) throws IOException {
        if (comment == null || comment.isEmpty()) return;
        String[] lines = comment.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#")) {
                line = line.substring(1).trim();
            }
            if (!line.isEmpty()) {
                writeLine(writer, "# " + line);
            }
        }
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String t, ClassNode tc, String m, IMixinInfo i) {}
    @Override public void postApply(String t, ClassNode tc, String m, IMixinInfo i) {}
    @Override public String getRefMapperConfig() { return null; }
}
