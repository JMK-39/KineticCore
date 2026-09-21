package dev.xyat.kineticcore.api.flight;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Public Kinetic API facade for flight. */
public final class KineticFlight {
    private static final String NBT_FLIGHT_SOURCES = "flight_sources_list";
    private static final String NBT_LAST_FLYING = "last_known_flying";
    private static final String NBT_NOCLIP = "kt_noclip";

    private static final Set<UUID> DEBOUNCING_PLAYERS = new HashSet<>();
    private static volatile BiConsumer<ServerPlayer, Boolean> noclipSyncSender = (player, enabled) -> { };

    /**
     * Exposes the is processing explicit cancel API value.
     */
    public static boolean isProcessingExplicitCancel;
    /**
     * Exposes the is internal update API value.
     */
    public static boolean isInternalUpdate;
    /**
     * Exposes the is gamemode switching API value.
     */
    public static boolean isGamemodeSwitching;

    private KineticFlight() {
    }

    /**
     * Performs the install noclip sync sender API operation.
     */
    public static void installNoclipSyncSender(BiConsumer<ServerPlayer, Boolean> sender) {
        noclipSyncSender = sender == null ? (player, enabled) -> { } : sender;
    }

    /**
     * Returns whether debouncing.
     */
    public static boolean isDebouncing(Player player) {
        return DEBOUNCING_PLAYERS.contains(player.getUUID());
    }

    /**
     * Updates debouncing.
     */
    public static void setDebouncing(Player player, boolean value) {
        if (value) DEBOUNCING_PLAYERS.add(player.getUUID());
        else DEBOUNCING_PLAYERS.remove(player.getUUID());
    }

    /**
     * Returns the sources.
     */
    public static Set<String> sources(Player player) {
        Set<String> sources = new HashSet<>();
        ListTag list = player.getPersistentData().getList(NBT_FLIGHT_SOURCES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            sources.add(list.getString(i));
        }
        return sources;
    }

    /**
     * Adds source.
     */
    public static void addSource(LivingEntity entity, String sourceId) {
        if (!(entity instanceof Player player)) return;
        Set<String> sources = sources(player);
        if (sources.add(sourceId)) {
            saveSources(player, sources);
            refresh(player);
        }
    }

    /**
     * Removes source.
     */
    public static void removeSource(LivingEntity entity, String sourceId) {
        if (!(entity instanceof Player player)) return;
        Set<String> sources = sources(player);
        if (sources.remove(sourceId)) {
            saveSources(player, sources);
            refresh(player);
        }
    }

    private static void saveSources(Player player, Set<String> sources) {
        ListTag list = new ListTag();
        for (String source : sources) list.add(StringTag.valueOf(source));
        player.getPersistentData().put(NBT_FLIGHT_SOURCES, list);
    }

    /**
     * Updates last known flying.
     */
    public static void setLastKnownFlying(Player player, boolean flying) {
        player.getPersistentData().putBoolean(NBT_LAST_FLYING, flying);
    }

    /**
     * Performs the last known flying API operation.
     */
    public static boolean lastKnownFlying(Player player) {
        return player.getPersistentData().getBoolean(NBT_LAST_FLYING);
    }

    /**
     * Returns whether flight allowed.
     */
    public static boolean isFlightAllowed(Player player) {
        return player.isCreative() || player.isSpectator() || !sources(player).isEmpty();
    }

    /**
     * Refreshes the current API state.
     */
    public static void refresh(Player player) {
        if (player.level().isClientSide) return;

        if (isFlightAllowed(player)) {
            player.getAbilities().mayfly = true;
            isInternalUpdate = true;
            player.onUpdateAbilities();
            isInternalUpdate = false;
        } else if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            isInternalUpdate = true;
            player.onUpdateAbilities();
            isInternalUpdate = false;
        }
    }


    /**
     * Performs the noclip enabled API operation.
     */
    public static boolean noclipEnabled(Player player) {
        return player.getPersistentData().getBoolean(NBT_NOCLIP);
    }

    /**
     * Copies persistent state.
     */
    public static void copyPersistentState(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        if (oldPlayer.getPersistentData().contains(NBT_FLIGHT_SOURCES)) {
            Tag sources = oldPlayer.getPersistentData().get(NBT_FLIGHT_SOURCES);
            if (sources != null) {
                newPlayer.getPersistentData().put(NBT_FLIGHT_SOURCES, sources.copy());
            }
        }
        if (oldPlayer.getAbilities().flying) {
            newPlayer.getAbilities().flying = true;
        }
        boolean noclip = noclipEnabled(oldPlayer);
        newPlayer.getPersistentData().putBoolean(NBT_NOCLIP, noclip);
        newPlayer.noPhysics = noclip;
        newPlayer.refreshDimensions();
    }

    /**
     * Performs the server noclip enabled API operation.
     */
    public static boolean serverNoclipEnabled(Player player) {
        return noclipEnabled(player);
    }

    /**
     * Applies server noclip.
     */
    public static void applyServerNoclip(ServerPlayer player, boolean requestedState) {
        boolean enabled = requestedState && player.isCreative();
        player.getPersistentData().putBoolean(NBT_NOCLIP, enabled);
        player.noPhysics = enabled;
        player.refreshDimensions();
        noclipSyncSender.accept(player, enabled);
    }

    /**
     * Performs the sync server noclip API operation.
     */
    public static void syncServerNoclip(ServerPlayer player) {
        applyServerNoclip(player, serverNoclipEnabled(player));
    }
}
