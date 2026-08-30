package dev.xyat.kineticcore.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RegistryDictUtil {
    private static List<String> attributeDictCache;
    private static List<String> potionDictCache;
    private static List<String> damageDictCache;
    private static List<String> specificDamageDictCache;

    public static String getCleanChinese(String... keys) {
        for (String key : keys) {
            String translated = Component.translatable(key).getString();
            if (!translated.equals(key) && !translated.contains("%") && translated.matches(".*[\\u4e00-\\u9fa5].*")) {
                return translated;
            }
        }
        return null;
    }

    public static String extractNameFromDictionary(String id, List<String> dict) {
        for (String entry : dict) {
            if (entry.startsWith(id + " - ")) return entry.substring(entry.indexOf(" - ") + 3);
        }
        return id;
    }

    public static List<String> getAttributeDict() {
        if (attributeDictCache == null) {
            attributeDictCache = ForgeRegistries.ATTRIBUTES.getEntries().stream().map(e -> {
                String id = e.getKey().location().toString();
                String trans = getCleanChinese(e.getValue().getDescriptionId());
                return trans != null ? id + " - " + trans : id;
            }).collect(Collectors.toList());
        }
        return attributeDictCache;
    }

    public static List<String> getPotionDict() {
        if (potionDictCache == null) {
            potionDictCache = ForgeRegistries.MOB_EFFECTS.getEntries().stream().map(e -> {
                String id = e.getKey().location().toString();
                String trans = getCleanChinese(e.getValue().getDescriptionId());
                return trans != null ? id + " - " + trans : id;
            }).collect(Collectors.toList());
        }
        return potionDictCache;
    }

    public static List<String> getDamageDict() {
        if (damageDictCache == null) {
            damageDictCache = new ArrayList<>();
            String allTrans = getCleanChinese("gui.kineticcore.damage.all");
            damageDictCache.add(allTrans != null ? "all - " + allTrans : "all");

            if (Minecraft.getInstance().level != null) {
                var damageRegistry = Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
                Set<String> addedMsgIds = new HashSet<>();
                damageRegistry.entrySet().forEach(entry -> {
                    ResourceLocation loc = entry.getKey().location();
                    String msgId = entry.getValue().msgId();
                    if (addedMsgIds.add(msgId)) {
                        String name = getCleanChinese("damage_type." + msgId.replace(":", "."), "dmg." + msgId, "damage_type." + loc.getNamespace() + "." + loc.getPath());
                        damageDictCache.add(name != null ? msgId + " - " + name : msgId);
                    }
                });
                damageRegistry.getTagNames().forEach(tagKey -> {
                    ResourceLocation loc = tagKey.location();
                    String tagName = getCleanChinese("tag.damage_type." + loc.getNamespace() + "." + loc.getPath(), "tag." + loc.getNamespace() + "." + loc.getPath(), "tag." + loc.getPath());
                    damageDictCache.add(tagName != null ? "#" + loc + " - " + tagName : "#" + loc);
                });
            }
        }
        return damageDictCache;
    }

    public static List<String> getSpecificDamageDict() {
        if (specificDamageDictCache == null) {
            specificDamageDictCache = new ArrayList<>();
            if (Minecraft.getInstance().level != null) {
                var damageRegistry = Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
                Set<String> addedMsgIds = new HashSet<>();
                damageRegistry.entrySet().forEach(entry -> {
                    ResourceLocation loc = entry.getKey().location();
                    String msgId = entry.getValue().msgId();
                    if (addedMsgIds.add(msgId)) {
                        String name = getCleanChinese("damage_type." + msgId.replace(":", "."), "dmg." + msgId, "damage_type." + loc.getNamespace() + "." + loc.getPath());
                        specificDamageDictCache.add(name != null ? msgId + " - " + name : msgId);
                    }
                });
            }
        }
        return specificDamageDictCache;
    }
}
