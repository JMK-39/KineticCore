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

/**
 * The only entry point the API uses to build translatable text.
 *
 * <p>Vanilla translation drops the color of a {@code %s} placeholder when the argument is a component with its own
 * style. These helpers read the mod's bundled {@code en_us.json}, find the {@code §} formatting that is active at
 * each placeholder and apply it to the matching argument, so language files stay the single place that decides
 * colors. The namespace is taken from the second segment of the key: {@code gui.mymod.title} reads
 * {@code assets/mymod/lang/en_us.json}. Safe to call from any thread.
 */
public final class KineticI18n {
    private static final Map<String, Map<String, String>> DEFAULT_LANGUAGES = new ConcurrentHashMap<>();

    private KineticI18n() {
    }

    /**
     * Creates a translatable component whose arguments inherit the formatting active at their placeholder.
     * Arguments that already carry their own color keep it; other styles such as bold are still added.
     *
     * @param key translation key such as {@code msg.mymod.saved}
     * @param args placeholder arguments: components, or any object rendered with {@link String#valueOf(Object)}
     * @return a new component; plain {@link Component#translatable(String, Object...)} behavior when the key has no
     *   namespace segment or there are no arguments
     */
    public static MutableComponent translatable(String key, Object... args) {
        String namespace = namespaceFromKey(key);
        if (namespace == null || args == null || args.length == 0) {
            return Component.translatable(key, args == null ? new Object[0] : args);
        }
        return translatableWithStyle(namespace, key, args);
    }

    /**
     * 当前客户端语言是否提供该翻译键（仅客户端可用）。
     * Whether the current client language provides the translation key (client side only).
     */
    public static boolean hasTranslation(String key) {
        return dev.xyat.kineticcore.internal.client.text.KineticTextRuntime.hasTranslation(key);
    }

    /**
     * Applies the formatting of a style entry in the language file, for example a key whose text is {@code §a%s}, to
     * a copy of one value. The formatting active at the entry's first placeholder is used and overrides the value's
     * own color; other text in the entry is ignored, so the result's plain text equals the value's. Formatting is
     * read from the bundled {@code en_us.json}.
     *
     * @param styleKey language key whose text contains one {@code %s}
     * @param value value to style: a component or any object rendered with {@link String#valueOf(Object)}
     * @return the styled copy, or an unstyled copy when the key has no namespace segment or no formatting
     */
    public static MutableComponent styled(String styleKey, Object value) {
        String namespace = namespaceFromKey(styleKey);
        String template = namespace == null ? null : translations(namespace).get(styleKey);
        return applyFormats(value, formatsAtFirstPlaceholder(template), false);
    }

    /**
     * Resolves {@link #translatable(String, Object...)} to plain text in the current language.
     *
     * @return the translated text including formatting codes; on a dedicated server this is the English text
     */
    public static String string(String key, Object... args) {
        return translatable(key, args).getString();
    }

    /**
     * Same as {@link #translatable(String, Object...)} but reads formatting from an explicit namespace, for keys
     * that do not follow the {@code category.namespace.name} layout.
     *
     * @param namespace mod id whose {@code en_us.json} holds the template
     * @param key translation key
     * @param args placeholder arguments
     * @return a new component
     */
    public static MutableComponent translatableIn(String namespace, String key, Object... args) {
        return translatableWithStyle(namespace, key, args);
    }

    private static MutableComponent translatableWithStyle(String namespace, String key, Object... args) {
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
            if (current == '§' && i + 1 < template.length()) {
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
                        // argIndex stays -1: no argument receives the style
                    }
                    tokenEnd = cursor + 1;
                }
            }

            if (argIndex >= 0 && argIndex < styledArgs.length && !activeFormats.isEmpty()) {
                styledArgs[argIndex] = applyFormats(styledArgs[argIndex], activeFormats, true);
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

    private static List<ChatFormatting> formatsAtFirstPlaceholder(String template) {
        List<ChatFormatting> activeFormats = new ArrayList<>();
        if (template == null) {
            return activeFormats;
        }
        for (int i = 0; i < template.length() - 1; i++) {
            char current = template.charAt(i);
            if (current == '§') {
                ChatFormatting formatting = ChatFormatting.getByCode(template.charAt(i + 1));
                if (formatting != null) {
                    applyFormat(activeFormats, formatting);
                }
                i++;
            } else if (current == '%') {
                char next = template.charAt(i + 1);
                if (next == '%') {
                    i++;
                } else if (next == 's' || Character.isDigit(next)) {
                    break;
                }
            }
        }
        return activeFormats;
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
