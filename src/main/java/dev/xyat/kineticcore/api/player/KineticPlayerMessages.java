package dev.xyat.kineticcore.api.player;

import dev.xyat.kineticcore.internal.player.KineticPlayerMessageRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Sends standard server-driven player messages and titles without exposing packet details to addons.
 */
public final class KineticPlayerMessages {
    private KineticPlayerMessages() {
    }

    /** Sends one standard system message to the supplied server player. */
    public static void system(ServerPlayer player, Component message) {
        KineticPlayerMessageRuntime.system(
                Objects.requireNonNull(player, "player"),
                Objects.requireNonNull(message, "message")
        );
    }

    /** Sends one client message, using the overlay/action-bar channel when {@code overlay} is true. */
    public static void display(ServerPlayer player, Component message, boolean overlay) {
        KineticPlayerMessageRuntime.display(
                Objects.requireNonNull(player, "player"),
                Objects.requireNonNull(message, "message"),
                overlay
        );
    }

    /**
     * Shows one title with caller-defined vanilla timing values.
     * Timing values are forwarded unchanged so addons retain their existing business rules.
     */
    public static void title(
            ServerPlayer player,
            Component title,
            int fadeInTicks,
            int stayTicks,
            int fadeOutTicks
    ) {
        KineticPlayerMessageRuntime.title(
                Objects.requireNonNull(player, "player"),
                Objects.requireNonNull(title, "title"),
                fadeInTicks,
                stayTicks,
                fadeOutTicks
        );
    }
}
