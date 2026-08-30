package dev.xyat.kineticcore.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticcore.KineticCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Public registry for dependent mods that extend kineticcore commands. */
public final class KTCommandApi {
    private static final Map<String, KTCommandExtension> EXTENSIONS = new LinkedHashMap<>();

    private KTCommandApi() {
    }

    public static synchronized void register(String id, KTCommandExtension extension) {
        EXTENSIONS.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(extension, "extension"));
    }

    public static synchronized void unregister(String id) {
        EXTENSIONS.remove(id);
    }

    public static void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        for (Map.Entry<String, KTCommandExtension> entry : snapshot()) {
            try {
                entry.getValue().registerCommands(root);
            } catch (Throwable throwable) {
                logFailure(entry.getKey(), "command registration", throwable);
            }
        }
    }

    public static void appendHelpItems(CommandSourceStack source, List<MutableComponent> items) {
        for (Map.Entry<String, KTCommandExtension> entry : snapshot()) {
            try {
                entry.getValue().appendHelpItems(source, items);
            } catch (Throwable throwable) {
                logFailure(entry.getKey(), "help contribution", throwable);
            }
        }
    }

    public static void reload(CommandSourceStack source) {
        for (Map.Entry<String, KTCommandExtension> entry : snapshot()) {
            try {
                entry.getValue().reload(source);
            } catch (Throwable throwable) {
                logFailure(entry.getKey(), "reload hook", throwable);
            }
        }
    }

    private static synchronized List<Map.Entry<String, KTCommandExtension>> snapshot() {
        return List.copyOf(EXTENSIONS.entrySet());
    }

    private static void logFailure(String id, String phase, Throwable throwable) {
        KineticCore.LOGGER.error("kineticcore extension [{}] failed during {}", id, phase, throwable);
    }
}
