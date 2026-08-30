package dev.xyat.kineticcore.feature.despawn;

import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class LetMeDespawnLogic {

    private static final Pattern IGNORE_NAME_PATTERN = Pattern.compile(".* x\\d+");
    private static final ConcurrentHashMap<net.minecraft.world.entity.EntityType<?>, Boolean> WHITELIST_CACHE = new ConcurrentHashMap<>();
    private static int lastConfigHash = -1;
    private static final Set<String> whitelistIds = new HashSet<>();
    private static final Set<String> whitelistMods = new HashSet<>();

    private static void updateCacheIfNeeded() {
        List<String> currentList = GeneralMechanicsConfig.despawnWhiteList;
        int currentHash = currentList != null ? currentList.hashCode() : 0;
        if (currentHash != lastConfigHash) {
            lastConfigHash = currentHash;
            WHITELIST_CACHE.clear();
            whitelistIds.clear();
            whitelistMods.clear();
            if (currentList != null) {
                for (String s : currentList) {
                    if (s.startsWith("@")) whitelistMods.add(s.substring(1));
                    else whitelistIds.add(s);
                }
            }
        }
    }

    private static boolean isWhitelisted(Mob entity) {
        updateCacheIfNeeded();
        return WHITELIST_CACHE.computeIfAbsent(entity.getType(), type -> {
            ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (whitelistIds.contains(rl.toString())) return true;
            return whitelistMods.contains(rl.getNamespace());
        });
    }

    public static boolean shouldForceDespawn(Mob entity) {
        if (!GeneralMechanicsConfig.enableLetMeDespawn) return false;

        boolean isEnderman = entity instanceof EnderMan;
        boolean hasPickedUp = entity.getTags().contains("kt_picked_up_entity");

        if (!isEnderman && !hasPickedUp) return false;

        if (isWhitelisted(entity)) return false;

        if (hasImportantCustomName(entity)) return false;

        if (isEnderman) {
            if (((EnderMan) entity).getCarriedBlock() != null) return true;
        }

        if (hasPickedUp) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.hasTag()) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && tag.getBoolean("kt_picked_up")) return true;
                }
            }
        }

        return false;
    }

    public static void processPersistence(Mob entity, EquipmentSlot slot) {
        if (!GeneralMechanicsConfig.enableLetMeDespawn) return;
        ItemStack itemStack = entity.getItemBySlot(slot);
        if (itemStack.isEmpty()) return;

        itemStack.getOrCreateTag().putBoolean("kt_picked_up", true);
        entity.addTag("kt_picked_up_entity");
    }

    private static boolean hasImportantCustomName(Mob entity) {
        if (!entity.hasCustomName() || entity.getCustomName() == null) return false;
        return !IGNORE_NAME_PATTERN.matcher(entity.getCustomName().getString()).matches();
    }

    public static void dropPickedEquipment(Mob entity) {
        if (entity instanceof EnderMan enderman && enderman.getCarriedBlock() != null) {
            entity.spawnAtLocation(new ItemStack(enderman.getCarriedBlock().getBlock()));
            enderman.setCarriedBlock(null);
        }

        if (entity.getTags().contains("kt_picked_up_entity")) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.hasTag()) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && tag.getBoolean("kt_picked_up")) {
                        entity.spawnAtLocation(stack.copy());
                        entity.setItemSlot(slot, ItemStack.EMPTY);
                    }
                }
            }
        }
    }
}