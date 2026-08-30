package dev.xyat.kineticcore.feature.pvp.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticcore.feature.pvp.event.PvpEventHandler;
import dev.xyat.kineticcore.feature.pvp.network.PvpNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PvpCommand {

    public static final String PVP_TAG = "kt_pvp_protection_enabled";

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("pvp")
                .executes(ctx -> togglePvp(ctx.getSource()))
        );
    }

    public static boolean isPvpProtected(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(ServerPlayer.PERSISTED_NBT_TAG)) {
            CompoundTag forgeData = persistentData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
            return forgeData.getBoolean(PVP_TAG);
        }
        return false;
    }

    public static void setPvpProtected(ServerPlayer player, boolean state) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag forgeData;
        if (persistentData.contains(ServerPlayer.PERSISTED_NBT_TAG)) {
            forgeData = persistentData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        } else {
            forgeData = new CompoundTag();
            persistentData.put(ServerPlayer.PERSISTED_NBT_TAG, forgeData);
        }
        forgeData.putBoolean(PVP_TAG, state);
    }

    private static int togglePvp(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("cmd.kineticcore.error.players_only"));
            return 0;
        }

        boolean newState = !isPvpProtected(player);
        setPvpProtected(player, newState);
        if (newState) {
            PvpEventHandler.clearConflictingTargets(player);
        }
        PvpNetwork.sendState(player, newState);

        return 1;
    }
}
