package dev.xyat.kineticcore.api.command;

import dev.xyat.kineticcore.api.text.KineticI18n;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.internal.runtime.event.KineticCommandRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Public Kinetic API facade for commands. */
public final class KineticCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, LinkedHashMap<Class<? extends CommandExtension>, CommandExtension>> EXTENSIONS = new LinkedHashMap<>();
    private static final Map<String, Consumer<CommandDispatcher<CommandSourceStack>>> TOP_LEVEL_COMMANDS = new LinkedHashMap<>();

    private KineticCommands() {
    }

    /**
     * Registers a command extension under a logical owner/group id.
     * <p>
     * A single owner may contribute multiple independent extension classes. Re-registering the same
     * implementation class under the same id is treated as an idempotent no-op so module bootstrap
     * code can safely be retried without duplicating Brigadier nodes.
     */
    public static synchronized void registerExtension(String id, CommandExtension extension) {
        String safeId = Objects.requireNonNull(id, "id");
        CommandExtension safeExtension = Objects.requireNonNull(extension, "extension");
        Class<? extends CommandExtension> extensionType = safeExtension.getClass();

        LinkedHashMap<Class<? extends CommandExtension>, CommandExtension> group =
                EXTENSIONS.computeIfAbsent(safeId, ignored -> new LinkedHashMap<>());
        if (group.containsKey(extensionType)) {
            return;
        }

        ensureRuntime();
        group.put(extensionType, safeExtension);
    }

    /**
     * Unregisters every command extension contributed under the supplied owner/group id.
     */
    public static synchronized void unregisterExtension(String id) {
        EXTENSIONS.remove(id);
    }

    /**
     * Registers top level.
     */
    public static synchronized void registerTopLevel(
            String id,
            Consumer<CommandDispatcher<CommandSourceStack>> registrar
    ) {
        String safeId = Objects.requireNonNull(id, "id");
        Consumer<CommandDispatcher<CommandSourceStack>> safeRegistrar = Objects.requireNonNull(registrar, "registrar");
        if (TOP_LEVEL_COMMANDS.containsKey(safeId)) {
            throw new IllegalStateException("Top-level command id is already registered: " + safeId);
        }
        ensureRuntime();
        TOP_LEVEL_COMMANDS.put(safeId, safeRegistrar);
    }

    /**
     * Unregisters top level.
     */
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
        for (ExtensionRegistration registration : snapshot()) {
            try {
                registration.extension().registerCommands(root);
            } catch (Throwable throwable) {
                logFailure(registration.id(), "command registration", throwable);
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
        for (ExtensionRegistration registration : snapshot()) {
            try {
                registration.extension().appendHelpItems(source, items);
            } catch (Throwable throwable) {
                logFailure(registration.id(), "help contribution", throwable);
            }
        }
    }

    private static int reload(CommandSourceStack source) {
        for (ExtensionRegistration registration : snapshot()) {
            try {
                registration.extension().reload(source);
            } catch (Throwable throwable) {
                logFailure(registration.id(), "reload hook", throwable);
            }
        }
        source.sendSuccess(
                () -> KineticI18n.translatable("cmd.kineticcore.reload.success"),
                true
        );
        return 1;
    }

    private static synchronized List<ExtensionRegistration> snapshot() {
        List<ExtensionRegistration> registrations = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<Class<? extends CommandExtension>, CommandExtension>> group : EXTENSIONS.entrySet()) {
            for (CommandExtension extension : group.getValue().values()) {
                registrations.add(new ExtensionRegistration(group.getKey(), extension));
            }
        }
        return List.copyOf(registrations);
    }

    private static synchronized List<Map.Entry<String, Consumer<CommandDispatcher<CommandSourceStack>>>> topLevelSnapshot() {
        return List.copyOf(TOP_LEVEL_COMMANDS.entrySet());
    }

    private record ExtensionRegistration(String id, CommandExtension extension) {
    }

    private static void logFailure(String id, String phase, Throwable throwable) {
        LOGGER.error("Kinetic command extension [{}] failed during {}", id, phase, throwable);
    }
}
