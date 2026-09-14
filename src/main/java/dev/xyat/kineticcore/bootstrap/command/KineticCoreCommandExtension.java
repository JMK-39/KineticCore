package dev.xyat.kineticcore.bootstrap.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticcore.api.command.CommandExtension;
import dev.xyat.kineticcore.api.command.CommandText;
import dev.xyat.kineticcore.api.command.KineticCommands;
import dev.xyat.kineticcore.feature.firstjoin.command.FirstJoinCommand;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfig;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import dev.xyat.kineticcore.feature.nbt.command.NbtCommand;
import dev.xyat.kineticcore.feature.networklimit.config.NetworkConfig;
import dev.xyat.kineticcore.feature.pvp.command.PvpCommand;
import dev.xyat.kineticcore.feature.setspawn.command.SetSpawnCommand;
import dev.xyat.kineticcore.feature.setspawn.config.SetSpawnConfig;
import dev.xyat.kineticcore.feature.tps.command.TpsCommand;
import dev.xyat.kineticcore.feature.worldinit.config.WorldInitConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public final class KineticCoreCommandExtension implements CommandExtension {
    private static final String EXTENSION_ID = "kineticcore:core";
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        KineticCommands.registerExtension(EXTENSION_ID, new KineticCoreCommandExtension());
    }

    @Override
    public void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        PvpCommand.register(root);
        FirstJoinCommand.register(root);
        SetSpawnCommand.register(root);
        TpsCommand.register(root);
        NbtCommand.register(root);
    }

    @Override
    public void appendHelpItems(CommandSourceStack source, List<MutableComponent> items) {
        items.add(CommandText.executable("/kt pvp", "cmd.kineticcore.pvp.desc"));
        if (source.hasPermission(2)) {
            items.add(CommandText.executable("/kt setfirstjoin", "cmd.kineticcore.setfirstjoin.desc"));
        }
        items.add(CommandText.executable("/kt world", "cmd.kineticcore.world.desc"));
        items.add(CommandText.executable("/kt tps", "cmd.kineticcore.tps.desc"));
        items.add(CommandText.executable("/kt nbt", "cmd.kineticcore.nbt.desc"));
    }

    @Override
    public void reload(CommandSourceStack source) {
        GeneralMechanicsConfig.load();
        PlayerConfig.load();
        NetworkConfig.load();
        LogCleanerConfig.load();
        WorldInitConfig.load();
        SetSpawnConfig.load();
    }
}
