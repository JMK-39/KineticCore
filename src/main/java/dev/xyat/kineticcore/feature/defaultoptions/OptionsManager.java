package dev.xyat.kineticcore.feature.defaultoptions;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class OptionsManager {
    private static final File CUSTOM_DEFAULTS_FILE = FMLPaths.CONFIGDIR.get().resolve("kineticcore/defaultoptions.txt").toFile();

    public static class KeyData {
        public InputConstants.Key key = null;
        public KeyModifier modifier = KeyModifier.NONE;
    }

    public static void enforceDefaultOptions() {
        if (!CUSTOM_DEFAULTS_FILE.exists()) return;

        File gameOptionsFile = FMLPaths.GAMEDIR.get().resolve("options.txt").toFile();
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

    public static void applyCustomKeyDefaults(Options options) {
        if (!CUSTOM_DEFAULTS_FILE.exists()) return;

        Map<String, KeyData> newDefaults = parseKeysFromDefaultFile();
        boolean changed = false;

        for (KeyMapping mapping : options.keyMappings) {
            KeyData newData = newDefaults.get(mapping.getName());
            if (newData != null && newData.key != null) {
                IKineticKeyAccess access = (IKineticKeyAccess) mapping;
                access.setkineticcore$defaultKey(newData.key);
                access.setkineticcore$keyModifierDefault(newData.modifier);
                changed = true;
            }
        }

        if (changed) {
            KeyMapping.resetMapping();
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
                            KeyModifier modifier = KeyModifier.NONE;

                            int secondColon = payload.indexOf(':');
                            if (secondColon != -1) {
                                keyCodeStr = payload.substring(0, secondColon);
                                try {
                                    modifier = KeyModifier.valueOf(payload.substring(secondColon + 1));
                                } catch (Exception ignored) {}
                            } else {
                                keyCodeStr = payload;
                            }

                            InputConstants.Key key = InputConstants.getKey(keyCodeStr);
                            KeyData data = defaultKeys.computeIfAbsent(name, k -> new KeyData());
                            data.key = key;
                            if (modifier != KeyModifier.NONE || data.modifier == null) {
                                data.modifier = modifier;
                            }
                        }
                    } else if (line.startsWith("keyModifier_")) {
                        int firstColon = line.indexOf(':');
                        if (firstColon != -1) {
                            String name = line.substring(12, firstColon);
                            try {
                                KeyModifier modifier = KeyModifier.valueOf(line.substring(firstColon + 1));
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

        Minecraft mc = Minecraft.getInstance();
        mc.options.save();

        File gameOptionsFile = new File(mc.gameDirectory, "options.txt");
        if (!gameOptionsFile.exists()) throw new FileNotFoundException();

        if (!CUSTOM_DEFAULTS_FILE.getParentFile().exists() && !CUSTOM_DEFAULTS_FILE.getParentFile().mkdirs()) {
            throw new IOException();
        }

        Files.copy(gameOptionsFile.toPath(), CUSTOM_DEFAULTS_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        applyCustomKeyDefaults(mc.options);
    }
}