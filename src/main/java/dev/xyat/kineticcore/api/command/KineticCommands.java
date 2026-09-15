package dev.xyat.kineticcore.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.internal.runtime.event.KineticCommandRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class KineticCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, CommandExtension> EXTENSIONS = new LinkedHashMap<>();
    private static final Map<String, Consumer<CommandDispatcher<CommandSourceStack>>> TOP_LEVEL_COMMANDS = new LinkedHashMap<>();

    private KineticCommands() {
    }

    public static synchronized void registerExtension(String id, CommandExtension extension) {
        ensureRuntime();
        EXTENSIONS.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(extension, "extension"));
    }

    public static synchronized void unregisterExtension(String id) {
        EXTENSIONS.remove(id);
    }

    public static synchronized void registerTopLevel(
            String id,
            Consumer<CommandDispatcher<CommandSourceStack>> registrar
    ) {
        ensureRuntime();
        TOP_LEVEL_COMMANDS.put(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(registrar, "registrar")
        );
    }

    public static synchronized void unregisterTopLevel(String id) {
        TOP_LEVEL_COMMANDS.remove(id);
    }

    private static void ensureRuntime() {
        KineticCommandRuntime.initialize(KineticCommands::registerAll);
    }

    private static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerRoot(dispatcher);
        for (Map.Entry<String, Consumer<CommandDispatcher<CommandSourceStack>>> entry : topLevelSnapshot()) {
            try {
                entry.getValue().accept(dispatcher);
            } catch (Throwable throwable) {
                logFailure(entry.getKey(), "top-level command registration", throwable);
            }
        }
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("kt");
        root.then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(context -> reload(context.getSource()))
        );
        registerCommands(root);
        root.executes(context -> sendGlobalHelp(context.getSource()));
        dispatcher.register(root);
    }

    private static void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        for (Map.Entry<String, CommandExtension> entry : snapshot()) {
            try {
                entry.getValue().registerCommands(root);
            } catch (Throwable throwable) {
                logFailure(entry.getKey(), "command registration", throwable);
            }
        }
    }

    private static int sendGlobalHelp(CommandSourceStack source) {
        MutableComponent message = CommandText.header("mod.kineticcore.full_desc").append("\n");
        List<MutableComponent> items = new ArrayList<>();
        if (source.hasPermission(2)) {
            items.add(CommandText.executable("/kt reload", "cmd.kineticcore.reload.desc"));
        }
        appendHelpItems(source, items);

        for (int i = 0; i < items.size(); i++) {
            if (i > 0 && i % 3 == 0) {
                message.append("\n");
            } else if (i % 3 != 0) {
                message.append("  ");
            }
            message.append(items.get(i));
        }

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static void appendHelpItems(CommandSourceStack source, List<MutableComponent> items) {
        for (Map.Entry<String, CommandExtension> entry : snapshot()) {
            try {
                entry.getValue().appendHelpItems(source, items);
            } catch (Throwable throwable) {
                logFailure(entry.getKey(), "help contribution", throwable);
            }
        }
    }

    private static int reload(CommandSourceStack source) {
        for (Map.Entry<String, CommandExtension> entry : snapshot()) {
            try {
                entry.getValue().reload(source);
            } catch (Throwable throwable) {
                logFailure(entry.getKey(), "reload hook", throwable);
            }
        }
        source.sendSuccess(
                () -> Component.translatable("cmd.kineticcore.reload.success"),
                true
        );
        return 1;
    }

    private static synchronized List<Map.Entry<String, CommandExtension>> snapshot() {
        return List.copyOf(EXTENSIONS.entrySet());
    }

    private static synchronized List<Map.Entry<String, Consumer<CommandDispatcher<CommandSourceStack>>>> topLevelSnapshot() {
        return List.copyOf(TOP_LEVEL_COMMANDS.entrySet());
    }

    private static void logFailure(String id, String phase, Throwable throwable) {
        LOGGER.error("Kinetic command extension [{}] failed during {}", id, phase, throwable);
    }
}
