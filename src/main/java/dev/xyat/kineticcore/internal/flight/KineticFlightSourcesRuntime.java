package dev.xyat.kineticcore.internal.flight;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/** Internal implementation for persistent flight sources and their ability refresh. */
public final class KineticFlightSourcesRuntime {
    private static final String NBT_FLIGHT_SOURCES = "flight_sources_list";
    private static final ThreadLocal<Boolean> ABILITY_REFRESH = ThreadLocal.withInitial(() -> false);

    private KineticFlightSourcesRuntime() {
    }

    public static Set<String> sources(Player player) {
        Set<String> sources = new HashSet<>();
        ListTag list = player.getPersistentData().getList(NBT_FLIGHT_SOURCES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            sources.add(list.getString(i));
        }
        return sources;
    }

    public static void addSource(LivingEntity entity, String sourceId) {
        if (!(entity instanceof Player player)) return;
        Set<String> sources = sources(player);
        if (!sources.add(sourceId)) return;
        saveSources(player, sources);
        refresh(player);
    }

    public static void removeSource(LivingEntity entity, String sourceId) {
        if (!(entity instanceof Player player)) return;
        Set<String> sources = sources(player);
        if (!sources.remove(sourceId)) return;
        saveSources(player, sources);
        refresh(player);
    }

    public static boolean allowsFlight(Player player) {
        return player.isCreative() || player.isSpectator() || !sources(player).isEmpty();
    }

    public static void refresh(Player player) {
        if (player.level().isClientSide) return;

        if (allowsFlight(player)) {
            player.getAbilities().mayfly = true;
            syncAbilities(player);
            return;
        }
        if (player.isCreative() || player.isSpectator()) return;

        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        syncAbilities(player);
    }

    public static void copySources(Player oldPlayer, Player newPlayer) {
        if (!oldPlayer.getPersistentData().contains(NBT_FLIGHT_SOURCES)) return;
        Tag sources = oldPlayer.getPersistentData().get(NBT_FLIGHT_SOURCES);
        if (sources != null) {
            newPlayer.getPersistentData().put(NBT_FLIGHT_SOURCES, sources.copy());
        }
    }

    public static boolean abilityRefreshInProgress() {
        return ABILITY_REFRESH.get();
    }

    private static void saveSources(Player player, Set<String> sources) {
        ListTag list = new ListTag();
        for (String source : sources) list.add(StringTag.valueOf(source));
        player.getPersistentData().put(NBT_FLIGHT_SOURCES, list);
    }

    private static void syncAbilities(Player player) {
        boolean previous = ABILITY_REFRESH.get();
        ABILITY_REFRESH.set(true);
        try {
            player.onUpdateAbilities();
        } finally {
            ABILITY_REFRESH.set(previous);
        }
    }
}
