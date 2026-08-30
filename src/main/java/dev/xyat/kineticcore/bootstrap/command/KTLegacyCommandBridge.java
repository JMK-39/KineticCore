package dev.xyat.kineticcore.bootstrap.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.command.CommandUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

final class KTLegacyCommandBridge {
    private static final String[] COMMAND_CLASSES = {
            "dev.xyat.kineticcore.bootstrap.command.ReloadCommand",
            "dev.xyat.kineticcore.feature.pvp.command.PvpCommand",
            "dev.xyat.kineticcore.feature.firstjoin.command.FirstJoinCommand",
            "dev.xyat.kineticcore.feature.setspawn.command.SetSpawnCommand"
    };

    private static final String[] RELOAD_CLASSES = {
            "dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig",
            "dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig",
            "dev.xyat.kineticcore.feature.networklimit.config.NetworkConfig",
            "dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfig",
            "dev.xyat.kineticcore.feature.worldinit.config.WorldInitConfig",
            "dev.xyat.kineticcore.feature.setspawn.config.SetSpawnConfig"
    };

    private static final List<HelpEntry> HELP_ENTRIES = List.of(
            HelpEntry.execute("dev.xyat.kineticcore.bootstrap.command.ReloadCommand", true, "/kt reload", "cmd.kineticcore.reload.desc"),
            HelpEntry.execute("dev.xyat.kineticcore.feature.pvp.command.PvpCommand", false, "/kt pvp", "cmd.kineticcore.pvp.desc"),
            HelpEntry.execute("dev.xyat.kineticcore.feature.firstjoin.command.FirstJoinCommand", true, "/kt setfirstjoin", "cmd.kineticcore.setfirstjoin.desc"),
            HelpEntry.execute("dev.xyat.kineticcore.feature.setspawn.command.SetSpawnCommand", false, "/kt world", "cmd.kineticcore.world.desc")
    );

    private KTLegacyCommandBridge() {
    }

    static void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        for (String className : COMMAND_CLASSES) {
            invokeRegisterIfPresent(className, root);
        }
    }

    static void appendHelpItems(CommandSourceStack source, List<MutableComponent> items) {
        for (HelpEntry entry : HELP_ENTRIES) {
            if (entry.requiresPermission && !source.hasPermission(2)) {
                continue;
            }
            if (entry.ownerClass != null && !isClassPresent(entry.ownerClass)) {
                continue;
            }

            if (entry.suggestPrefix == null) {
                items.add(CommandUtils.createExecutableCommand(entry.display, entry.descriptionKey));
            } else {
                items.add(CommandUtils.createSuggestCommand(entry.display, entry.suggestPrefix, entry.descriptionKey));
            }
        }
    }

    static void reloadCore() {
        for (String className : RELOAD_CLASSES) {
            invokeNoArgIfPresent(className);
        }
    }

    private static void invokeRegisterIfPresent(String className, LiteralArgumentBuilder<CommandSourceStack> root) {
        Class<?> type = findClass(className);
        if (type == null) {
            return;
        }

        try {
            Method method = type.getDeclaredMethod("register", LiteralArgumentBuilder.class);
            if (!Modifier.isStatic(method.getModifiers())) {
                KineticCore.LOGGER.error("KT command module register method is not static: {}", className);
                return;
            }
            method.invoke(null, root);
        } catch (NoSuchMethodException exception) {
            KineticCore.LOGGER.error("KT command module has no register(LiteralArgumentBuilder) method: {}", className);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            KineticCore.LOGGER.error("KT command module registration failed: {}", className, unwrap(exception));
        }
    }

    private static void invokeNoArgIfPresent(String className) {
        Class<?> type = findClass(className);
        if (type == null) {
            return;
        }

        try {
            Method method = type.getDeclaredMethod("load");
            if (!Modifier.isStatic(method.getModifiers())) {
                KineticCore.LOGGER.error("KT reload method is not static: {}#{}", className, "load");
                return;
            }
            method.invoke(null);
        } catch (NoSuchMethodException exception) {
            KineticCore.LOGGER.error("KT reload method does not exist: {}#{}", className, "load");
        } catch (IllegalAccessException | InvocationTargetException exception) {
            KineticCore.LOGGER.error("KT reload failed: {}#{}", className, "load", unwrap(exception));
        }
    }

    private static boolean isClassPresent(String className) {
        return findClass(className) != null;
    }

    private static Class<?> findClass(String className) {
        try {
            return Class.forName(className, false, KineticCore.class.getClassLoader());
        } catch (ClassNotFoundException | NoClassDefFoundError exception) {
            return null;
        } catch (LinkageError error) {
            KineticCore.LOGGER.error("KT module class cannot be linked: {}", className, error);
            return null;
        }
    }

    private static Throwable unwrap(Exception exception) {
        if (exception instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return exception;
    }

    private record HelpEntry(
            String ownerClass,
            boolean requiresPermission,
            String display,
            String suggestPrefix,
            String descriptionKey
    ) {
        private static HelpEntry execute(String ownerClass, boolean requiresPermission, String display, String descriptionKey) {
            return new HelpEntry(ownerClass, requiresPermission, display, null, descriptionKey);
        }

        private static HelpEntry suggest(String ownerClass, boolean requiresPermission, String display, String suggestPrefix, String descriptionKey) {
            return new HelpEntry(ownerClass, requiresPermission, display, suggestPrefix, descriptionKey);
        }
    }
}
