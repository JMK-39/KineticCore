package dev.xyat.kineticcore.feature.attribute.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.MixinPlugin;
import dev.xyat.kineticcore.feature.attribute.mixin.RangedAttributeAccessor;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AttributeConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/attributes.toml");
    private static CommentedFileConfig configData;

    private static final Map<ResourceLocation, DefaultEntry> PREDEFINED_DEFAULTS = new HashMap<>();
    private static final Map<ResourceLocation, AttributeSettings> REGISTRY_DEFAULTS = new HashMap<>();

    public static boolean autoScan = true;

    static {
        addDefault("minecraft:generic.max_health", true, 0, 1000000000.0);
        addDefault("minecraft:generic.attack_damage", true, 0, 1000000000.0);
        addDefault("minecraft:generic.armor", true, 0, 100000000.0);
        addDefault("minecraft:generic.armor_toughness", true, 0, 100000000.0);
        addDefault("minecraft:generic.knockback_resistance", true, 0, 1.0);
        addDefault("minecraft:generic.movement_speed", true, 0, 1024.0);
        addDefault("minecraft:generic.flying_speed", true, 0, 1024.0);
        addDefault("minecraft:generic.attack_speed", true, 0, 1024.0);
        addDefault("minecraft:horse.jump_strength", true, 0, 2.0);
        addDefault("minecraft:zombie.spawn_reinforcements", true, 0, 1.0);
        addDefault("minecraft:generic.luck", true, -1024, 1024.0);
        addDefault("minecraft:generic.follow_range", true, 0, 2048.0);
        addDefault("forge:block_reach", true, 2, 1024.0);
        addDefault("forge:entity_reach", false, 0, 1024.0);
        addDefault("forge:swim_speed", false, 0, 1024.0);
        addDefault("forge:step_height_addition", false, -512, 512.0);
    }

    public static synchronized void loadAndApply() {
        if (configData != null) return;

        try {
            captureRegistryDefaults();
            configData = CommentedFileConfig.builder(CONFIG_PATH)
                    .sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build();
            configData.load();
            readValues();

            if (autoScan) {
                setupConfig();
                configData.save();
                readValues();
            } else {
                KineticCore.LOGGER.info("自动注册表扫描已关闭，配置文件保持不变，仅读取并应用已有数值。");
            }

            if (MixinPlugin.isFeatureEnabled("feature.attribute.RangedAttributeAccessor")) {
                applyToAttributes();
            }
            registerServerConfig();
        } catch (Exception e) {
            configData = null;
            KineticCore.LOGGER.error("AttributeConfig Load Failed", e);
        }
    }

    private static void registerServerConfig() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticcore:attributes")
                .booleanValue("auto_scan", AttributeConfig::isAutoScanEnabled, AttributeConfig::setAutoScanEnabled)
                .onSave(AttributeConfig::save)
                .build());

        KTServerConfigSpec.Builder editor = KTServerConfigSpec.builder("kineticcore:attributes/editor");
        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : getSortedAttributes()) {
            if (!(entry.getValue() instanceof RangedAttribute)) continue;
            ResourceLocation id = entry.getKey().location();
            String prefix = stableEntryPrefix(id);
            editor.booleanValue(
                    prefix + "_enabled",
                    () -> getAttributeSettings(id).enabled(),
                    value -> setAttributeEnabled(id, value)
            );
            editor.stringValue(
                    prefix + "_minimum",
                    () -> formatEditableBoundary(getAttributeSettings(id).minimum()),
                    value -> setAttributeMinimumText(id, value)
            );
            editor.stringValue(
                    prefix + "_maximum",
                    () -> formatEditableBoundary(getAttributeSettings(id).maximum()),
                    value -> setAttributeMaximumText(id, value)
            );
        }
        KTServerConfigApi.register(editor.onSave(AttributeConfig::save).build());
    }

    public static String stableEntryPrefix(ResourceLocation id) {
        return "attribute_" + HexFormat.of().formatHex(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void refreshTranslationsAndSave() {
        if (configData == null || !autoScan) return;

        boolean changed = false;
        List<Map.Entry<ResourceKey<Attribute>, Attribute>> sortedAttributes = getSortedAttributes();
        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : sortedAttributes) {
            if (entry.getValue() instanceof RangedAttribute ranged) {
                String idStr = entry.getKey().location().toString();
                if (configData.contains(Collections.singletonList(idStr))) {
                    String newComment = getAttributeComment(entry.getKey().location(), ranged);
                    String oldComment = configData.getComment(Collections.singletonList(idStr));

                    if (!Objects.equals(newComment, oldComment)) {
                        configData.setComment(Collections.singletonList(idStr), newComment);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            configData.save();
            KineticCore.LOGGER.info("AttributeConfig: 属性中文注释已刷新。");
        }
    }

    private static void setupConfig() {
        configData.setComment("_GLOBAL_SETTINGS",
                """
                         在此配置文件中，你可以修改游戏中所有限制范围属性的最小值(min)和最大值(max)。
                         [!] 警告：如需让某项修改生效，请务必将该项的 'enabled' 改为 true ！！！
                         [!] 支持写入负数或缩小最大值。为避免出错，建议写带小数点的格式（例如 1.0 而不是 1）。
                         Function Description:
                         Modify the min/max limits of all attributes here.
                         [!] Make sure to set 'enabled = true' for your target attribute!""");

        List<String> scanPath = Arrays.asList("_GLOBAL_SETTINGS", "enable_auto_registry_scan");
        if (!configData.contains(scanPath)) {
            configData.set(scanPath, true);
        }
        configData.setComment(scanPath,
                """
                         是否启用自动注册表扫描。
                         开启后：自动发现新属性、删除已失效的旧属性(如卸载模组)、刷新中文翻译注释。
                         关闭后：完全停止对该文件的自动增删改，仅读取里面的数值并生效，保护你的自定义配置布局。
                         Whether to enable automatic registry scanning.
                         If enabled, it discovers new attributes, deletes obsolete ones, and updates comments.
                         If disabled, the file becomes effectively 'read-only' for the mod, preserving your manual layout.""");

        autoScan = configData.getOrElse(scanPath, true);

        if (autoScan) {
            Set<String> validAttributeIds = ForgeRegistries.ATTRIBUTES.getEntries().stream()
                    .filter(e -> e.getValue() instanceof RangedAttribute)
                    .map(e -> e.getKey().location().toString())
                    .collect(Collectors.toSet());

            List<String> keysToRemove = new ArrayList<>();
            for (String key : configData.valueMap().keySet()) {
                if (!key.equals("_GLOBAL_SETTINGS") && !validAttributeIds.contains(key)) {
                    keysToRemove.add(key);
                }
            }
            for (String key : keysToRemove) {
                configData.remove(Collections.singletonList(key));
            }
        }

        List<Map.Entry<ResourceKey<Attribute>, Attribute>> sortedAttributes = getSortedAttributes();
        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : sortedAttributes) {
            if (entry.getValue() instanceof RangedAttribute ranged) {
                String idStr = entry.getKey().location().toString();
                if (!autoScan && !configData.contains(Collections.singletonList(idStr))) continue;

                setupAttributeEntry(idStr, entry.getKey().location(), ranged);
            }
        }
    }

    private static List<Map.Entry<ResourceKey<Attribute>, Attribute>> getSortedAttributes() {
        return ForgeRegistries.ATTRIBUTES.getEntries().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .collect(Collectors.toList());
    }

    private static void setupAttributeEntry(String idStr, ResourceLocation id, RangedAttribute ranged) {
        AttributeSettings defaults = getDefaultSettings(id);

        List<String> pathEnabled = Arrays.asList(idStr, "enabled");
        List<String> pathMin = Arrays.asList(idStr, "min");
        List<String> pathMax = Arrays.asList(idStr, "max");

        if (!configData.contains(pathEnabled)) configData.set(pathEnabled, defaults.enabled());
        if (!configData.contains(pathMin)) configData.set(pathMin, defaults.minimum());
        if (!configData.contains(pathMax)) configData.set(pathMax, defaults.maximum());

        configData.setComment(Collections.singletonList(idStr), getAttributeComment(id, ranged));
    }

    private static void readValues() {
        autoScan = configData.getOrElse(Arrays.asList("_GLOBAL_SETTINGS", "enable_auto_registry_scan"), true);
    }

    /**
     * Snapshot used by the shared configuration GUI. The registry attribute is
     * the source of truth for defaults; malformed persisted values are replaced
     * with those defaults before they reach an editor widget.
     */
    public record AttributeSettings(boolean enabled, double minimum, double maximum) {
    }

    public static synchronized boolean isAutoScanEnabled() {
        return autoScan;
    }

    public static synchronized void setAutoScanEnabled(boolean enabled) {
        requireLoaded();
        autoScan = enabled;
        configData.set(Arrays.asList("_GLOBAL_SETTINGS", "enable_auto_registry_scan"), enabled);
    }

    public static synchronized AttributeSettings getDefaultSettings(ResourceLocation id) {
        RangedAttribute ranged = requireRangedAttribute(id);
        return REGISTRY_DEFAULTS.computeIfAbsent(id, ignored -> createDefaultSettings(id, ranged));
    }

    public static synchronized AttributeSettings getAttributeSettings(ResourceLocation id) {
        AttributeSettings defaults = getDefaultSettings(id);
        if (configData == null) return defaults;

        String idString = id.toString();
        Object enabledValue = configData.get(Arrays.asList(idString, "enabled"));
        boolean enabled = enabledValue instanceof Boolean value ? value : defaults.enabled();
        double minimum = getDoubleSafe(Arrays.asList(idString, "min"), defaults.minimum());
        double maximum = getDoubleSafe(Arrays.asList(idString, "max"), defaults.maximum());

        if (minimum > maximum) {
            minimum = defaults.minimum();
            maximum = defaults.maximum();
        }
        return new AttributeSettings(enabled, minimum, maximum);
    }

    public static synchronized void setAttributeEnabled(ResourceLocation id, boolean enabled) {
        requireLoaded();
        requireRangedAttribute(id);
        configData.set(Arrays.asList(id.toString(), "enabled"), enabled);
    }

    public static synchronized void setAttributeMinimum(ResourceLocation id, double minimum) {
        requireLoaded();
        requireRangedAttribute(id);
        requireFinite(minimum, "minimum", id);
        configData.set(Arrays.asList(id.toString(), "min"), minimum);
    }

    public static synchronized void setAttributeMaximum(ResourceLocation id, double maximum) {
        requireLoaded();
        requireRangedAttribute(id);
        requireFinite(maximum, "maximum", id);
        configData.set(Arrays.asList(id.toString(), "max"), maximum);
    }

    static synchronized void setAttributeMinimumText(ResourceLocation id, String rawValue) {
        setAttributeBoundary(id, "min", parseEditableBoundary(rawValue));
    }

    static synchronized void setAttributeMaximumText(ResourceLocation id, String rawValue) {
        setAttributeBoundary(id, "max", parseEditableBoundary(rawValue));
    }

    static String formatEditableBoundary(double value) {
        return AttributeBoundaryCodec.format(value);
    }

    private static double parseEditableBoundary(String rawValue) {
        return AttributeBoundaryCodec.parse(rawValue);
    }

    private static void setAttributeBoundary(ResourceLocation id, String key, double value) {
        requireLoaded();
        requireRangedAttribute(id);
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("NaN attribute boundary: " + id);
        }
        configData.set(Arrays.asList(id.toString(), key), value);
    }

    /**
     * Persists changes made through the shared GUI. Attribute bounds are
     * installed during normal load-complete handling, so a restart remains the
     * reliable point at which every existing attribute instance observes them.
     */
    public static synchronized void save() {
        requireLoaded();
        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : getSortedAttributes()) {
            if (!(entry.getValue() instanceof RangedAttribute)) continue;

            String id = entry.getKey().location().toString();
            double minimum = getDoubleSafe(
                    Arrays.asList(id, "min"), getDefaultSettings(entry.getKey().location()).minimum());
            double maximum = getDoubleSafe(
                    Arrays.asList(id, "max"), getDefaultSettings(entry.getKey().location()).maximum());
            if (minimum > maximum) {
                throw new IllegalArgumentException("Attribute minimum exceeds maximum: " + id);
            }
        }
        configData.save();
    }

    private static double getDoubleSafe(List<String> path, double defValue) {
        Object val = configData.get(path);
        if (val instanceof Number num) {
            double value = num.doubleValue();
            if (!Double.isNaN(value)) return value;
        }
        return defValue;
    }

    private static void requireLoaded() {
        if (configData == null) {
            throw new IllegalStateException("Attribute configuration is not loaded");
        }
    }

    private static RangedAttribute requireRangedAttribute(ResourceLocation id) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
        if (!(attribute instanceof RangedAttribute ranged)) {
            throw new IllegalArgumentException("Not a registered ranged attribute: " + id);
        }
        return ranged;
    }

    private static void requireFinite(double value, String field, ResourceLocation id) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite attribute " + field + ": " + id);
        }
    }

    private static void applyToAttributes() {
        int count = 0;
        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : ForgeRegistries.ATTRIBUTES.getEntries()) {
            if (entry.getValue() instanceof RangedAttribute ranged) {
                String idStr = entry.getKey().location().toString();
                List<String> pathEnabled = Arrays.asList(idStr, "enabled");

                if (configData.contains(pathEnabled) && Boolean.TRUE.equals(configData.get(pathEnabled))) {
                    AttributeSettings settings = getAttributeSettings(entry.getKey().location());

                    RangedAttributeAccessor accessor = (RangedAttributeAccessor) ranged;
                    accessor.kineticcore$setMinValue(settings.minimum());
                    accessor.kineticcore$setMaxValue(settings.maximum());
                    count++;
                }
            }
        }
        if (count > 0) KineticCore.LOGGER.info("AttributeFix: 已应用 {} 个属性修改。", count);
    }

    private static void addDefault(String id, boolean enabled, double min, double max) {
        PREDEFINED_DEFAULTS.put(new ResourceLocation(id), new DefaultEntry(enabled, min, max));
    }

    private record DefaultEntry(boolean enabled, double min, double max) {}

    private static synchronized void captureRegistryDefaults() {
        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : getSortedAttributes()) {
            if (entry.getValue() instanceof RangedAttribute ranged) {
                ResourceLocation id = entry.getKey().location();
                REGISTRY_DEFAULTS.computeIfAbsent(id, ignored -> createDefaultSettings(id, ranged));
            }
        }
    }

    private static AttributeSettings createDefaultSettings(ResourceLocation id, RangedAttribute ranged) {
        DefaultEntry preset = PREDEFINED_DEFAULTS.get(id);
        return new AttributeSettings(
                preset != null && preset.enabled,
                preset != null ? preset.min : ranged.getMinValue(),
                preset != null ? preset.max : ranged.getMaxValue()
        );
    }

    private static String getAttributeComment(ResourceLocation id, RangedAttribute ranged) {
        String descKey = ranged.getDescriptionId();
        String localizedText = Component.translatable(descKey).getString();
        AttributeSettings defaults = getDefaultSettings(id);

        StringBuilder sb = new StringBuilder();

        if (!localizedText.equals(descKey) && !localizedText.isEmpty()) {
            if (!localizedText.matches("^[\\x00-\\x7F]*$")) {
                sb.append(" ").append(localizedText).append("\n");
            }
        }

        sb.append(" 原始范围 / Original Range: [")
                .append(formatValue(defaults.minimum()))
                .append(", ")
                .append(formatValue(defaults.maximum()))
                .append("]\n")
                .append(" 默认数值 / Default Value: ")
                .append(formatValue(ranged.getDefaultValue()));

        return sb.toString();
    }

    private static String formatValue(double val) {
        if (val == Double.POSITIVE_INFINITY) return "Infinity";
        if (val == Double.NEGATIVE_INFINITY) return "-Infinity";
        if (Double.isNaN(val)) return "NaN";
        if (val >= 1.7e308) return "MAX";
        if (val <= -1.7e308) return "-MAX";
        return val == (long) val ? String.valueOf((long) val) : String.valueOf(val);
    }
}
