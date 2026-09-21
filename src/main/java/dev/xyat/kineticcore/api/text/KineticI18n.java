package dev.xyat.kineticcore.api.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Public Kinetic API facade for i18n. */
public final class KineticI18n {
    private static final Map<String, Map<String, String>> DEFAULT_LANGUAGES = new ConcurrentHashMap<>();

    private KineticI18n() {
    }

    /**
     * Performs the translatable API operation.
     */
    public static MutableComponent translatable(String key, Object... args) {
        String namespace = namespaceFromKey(key);
        if (namespace == null || args == null || args.length == 0) {
            return Component.translatable(key, args == null ? new Object[0] : args);
        }
        return translatableWithStyle(namespace, key, true, args);
    }

    /**
     * Performs the styled API operation.
     */
    public static MutableComponent styled(String styleKey, Object value) {
        String namespace = namespaceFromKey(styleKey);
        if (namespace == null) {
            return Component.literal(String.valueOf(value));
        }
        return translatableWithStyle(namespace, styleKey, false, value);
    }

    /**
     * Performs the string API operation.
     */
    public static String string(String key, Object... args) {
        return translatable(key, args).getString();
    }

    /**
     * Performs the translatable in API operation.
     */
    public static MutableComponent translatableIn(String namespace, String key, Object... args) {
        return translatableWithStyle(namespace, key, true, args);
    }

    private static MutableComponent translatableWithStyle(String namespace, String key, boolean preserveArgumentColors, Object... args) {
        if (args == null || args.length == 0) {
            return Component.translatable(key);
        }

        String template = translations(namespace).get(key);
        if (template == null || template.isEmpty()) {
            return Component.translatable(key, args);
        }

        Object[] styledArgs = args.clone();
        List<ChatFormatting> activeFormats = new ArrayList<>();
        int sequentialArg = 0;

        for (int i = 0; i < template.length(); i++) {
            char current = template.charAt(i);
            if (current == '\u00A7' && i + 1 < template.length()) {
                ChatFormatting formatting = ChatFormatting.getByCode(template.charAt(i + 1));
                if (formatting != null) {
                    applyFormat(activeFormats, formatting);
                }
                i++;
                continue;
            }

            if (current != '%' || i + 1 >= template.length()) {
                continue;
            }

            char next = template.charAt(i + 1);
            if (next == '%') {
                i++;
                continue;
            }

            int argIndex = -1;
            int tokenEnd = i + 1;
            if (next == 's') {
                argIndex = sequentialArg++;
            } else if (Character.isDigit(next)) {
                int digitsStart = i + 1;
                int cursor = digitsStart;
                while (cursor < template.length() && Character.isDigit(template.charAt(cursor))) {
                    cursor++;
                }
                if (cursor < template.length() - 1
                        && template.charAt(cursor) == '$'
                        && template.charAt(cursor + 1) == 's') {
                    try {
                        argIndex = Integer.parseInt(template.substring(digitsStart, cursor)) - 1;
                    } catch (NumberFormatException ignored) {
                        argIndex = -1;
                    }
                    tokenEnd = cursor + 1;
                }
            }

            if (argIndex >= 0 && argIndex < styledArgs.length && !activeFormats.isEmpty()) {
                styledArgs[argIndex] = applyFormats(styledArgs[argIndex], activeFormats, preserveArgumentColors);
                i = tokenEnd;
            }
        }

        return Component.translatable(key, styledArgs);
    }

    private static Map<String, String> translations(String namespace) {
        return DEFAULT_LANGUAGES.computeIfAbsent(namespace, KineticI18n::loadDefaultLanguage);
    }

    private static Map<String, String> loadDefaultLanguage(String namespace) {
        String path = "/assets/" + namespace + "/lang/en_us.json";
        try (InputStream input = KineticI18n.class.getResourceAsStream(path)) {
            if (input == null) {
                return Collections.emptyMap();
            }
            JsonObject object = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> result = new ConcurrentHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    result.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return Map.copyOf(result);
        } catch (IOException | RuntimeException ignored) {
            return Collections.emptyMap();
        }
    }

    private static String namespaceFromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        int firstDot = key.indexOf('.');
        if (firstDot < 0 || firstDot + 1 >= key.length()) {
            return null;
        }
        int secondDot = key.indexOf('.', firstDot + 1);
        if (secondDot < 0) {
            return null;
        }
        String namespace = key.substring(firstDot + 1, secondDot);
        return namespace.isBlank() ? null : namespace;
    }

    private static void applyFormat(List<ChatFormatting> activeFormats, ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) {
            activeFormats.clear();
            return;
        }
        if (formatting.getColor() != null) {
            activeFormats.removeIf(existing -> existing.getColor() != null);
        }
        if (!activeFormats.contains(formatting)) {
            activeFormats.add(formatting);
        }
    }

    private static MutableComponent applyFormats(Object value, List<ChatFormatting> formats, boolean preserveArgumentColors) {
        MutableComponent component = value instanceof Component existing
                ? existing.copy()
                : Component.literal(String.valueOf(value));
        boolean preserveColor = preserveArgumentColors && component.getStyle().getColor() != null;
        for (ChatFormatting format : formats) {
            if (preserveColor && format.getColor() != null) {
                continue;
            }
            component.withStyle(format);
        }
        return component;
    }
}
