package dev.xyat.kineticcore.feature.defaultoptions;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.hook.ClientHooks;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.minecraft.MinecraftKeys;

import net.minecraft.client.KeyMapping;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class OptionsManager {
    private static final File CUSTOM_DEFAULTS_FILE = KineticPlatform.configDirectory().resolve("kineticcore/defaultoptions.txt").toFile();
    private static final KineticRegistrationBatch HOOK_REGISTRATION = new KineticRegistrationBatch();

    public static synchronized void registerHook() {
        if (!KineticFeatureSwitches.isEnabled("client.default_options")) return;
        HOOK_REGISTRATION.run(() -> ClientHooks.onOptionsLoading(options -> {
            enforceDefaultOptions();
            applyCustomKeyDefaults(MinecraftKeys.snapshotMappings(options.keyMappings));
        }));
    }

    public static class KeyData {
        public String serializedKey = null;
        public KineticKeyBindings.Modifier modifier = KineticKeyBindings.Modifier.NONE;
    }

    public static void enforceDefaultOptions() {
        if (!CUSTOM_DEFAULTS_FILE.exists()) return;

        File gameOptionsFile = KineticPlatform.gameDirectory().resolve("options.txt").toFile();
        boolean shouldReplace = false;

        if (!gameOptionsFile.exists()) {
            shouldReplace = true;
        } else if (gameOptionsFile.length() < 2048) {
            try {
                Files.delete(gameOptionsFile.toPath());
                shouldReplace = true;
            } catch (IOException ignored) {}
        }

        if (shouldReplace) {
            try {
                Files.copy(CUSTOM_DEFAULTS_FILE.toPath(), gameOptionsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }
    }

    public static void applyCustomKeyDefaults(Iterable<KeyMapping> mappings) {
        if (!CUSTOM_DEFAULTS_FILE.exists()) return;

        Map<String, KeyData> newDefaults = parseKeysFromDefaultFile();
        boolean changed = false;

        for (KeyMapping mapping : mappings) {
            KeyData newData = newDefaults.get(mapping.getName());
            if (newData != null && newData.serializedKey != null
                    && MinecraftKeys.setDefaultSerialized(mapping, newData.serializedKey, newData.modifier)) {
                changed = true;
            }
        }

        if (changed) {
            MinecraftKeys.resetMappings();
        }
    }

    private static Map<String, KeyData> parseKeysFromDefaultFile() {
        Map<String, KeyData> defaultKeys = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(CUSTOM_DEFAULTS_FILE.toPath(), StandardCharsets.UTF_8)) {
            reader.lines().forEach(line -> {
                try {
                    if (line.startsWith("key_")) {
                        int firstColon = line.indexOf(':');
                        if (firstColon != -1) {
                            String name = line.substring(4, firstColon);
                            String payload = line.substring(firstColon + 1);
                            String keyCodeStr;
                            KineticKeyBindings.Modifier modifier = KineticKeyBindings.Modifier.NONE;

                            int secondColon = payload.indexOf(':');
                            if (secondColon != -1) {
                                keyCodeStr = payload.substring(0, secondColon);
                                try {
                                    modifier = KineticKeyBindings.Modifier.valueOf(payload.substring(secondColon + 1));
                                } catch (Exception ignored) {}
                            } else {
                                keyCodeStr = payload;
                            }

                            KeyData data = defaultKeys.computeIfAbsent(name, k -> new KeyData());
                            data.serializedKey = keyCodeStr;
                            if (modifier != KineticKeyBindings.Modifier.NONE || data.modifier == null) {
                                data.modifier = modifier;
                            }
                        }
                    } else if (line.startsWith("keyModifier_")) {
                        int firstColon = line.indexOf(':');
                        if (firstColon != -1) {
                            String name = line.substring(12, firstColon);
                            try {
                                KineticKeyBindings.Modifier modifier = KineticKeyBindings.Modifier.valueOf(line.substring(firstColon + 1));
                                KeyData data = defaultKeys.computeIfAbsent(name, k -> new KeyData());
                                data.modifier = modifier;
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            });
        } catch (IOException ignored) {}
        return defaultKeys;
    }

    public static void saveAllSettingsAsDefault() throws IOException {

        KineticClientRuntime.saveOptions();

        File gameOptionsFile = KineticPlatform.gameDirectory().resolve("options.txt").toFile();
        if (!gameOptionsFile.exists()) throw new FileNotFoundException();

        if (!CUSTOM_DEFAULTS_FILE.getParentFile().exists() && !CUSTOM_DEFAULTS_FILE.getParentFile().mkdirs()) {
            throw new IOException();
        }

        Files.copy(gameOptionsFile.toPath(), CUSTOM_DEFAULTS_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        applyCustomKeyDefaults(MinecraftKeys.currentMappings());
    }
}