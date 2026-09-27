package dev.xyat.kineticcore.api.client.search;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 自动补全与搜索字典中的一个候选项：{@code value} 是写入输入框的唯一文本，{@code translation} 仅用于显示。
 * One autocomplete / dictionary candidate. {@code value} is the only text written into an input;
 * {@code translation} is display-only and hidden while the game language is English.
 */
public record KineticSuggestion(String value, Component translation) {
    /** 规范化空值 / Normalizes nullable value and translation. */
    public KineticSuggestion {
        value = value == null ? "" : value;
        translation = translation == null ? Component.empty() : translation;
    }

    /** 创建仅含值、无翻译的候选项 / Creates a value-only suggestion. */
    public static KineticSuggestion of(String value) {
        return new KineticSuggestion(value, Component.empty());
    }

    /** 将字符串字典适配为候选项字典 / Adapts a plain string dictionary to value-only suggestions. */
    public static Supplier<List<KineticSuggestion>> fromStrings(Supplier<? extends List<String>> dictionarySupplier) {
        return () -> {
            List<String> values = dictionarySupplier == null ? null : dictionarySupplier.get();
            if (values == null || values.isEmpty()) return List.of();
            List<KineticSuggestion> suggestions = new ArrayList<>(values.size());
            for (String value : values) {
                if (value != null) suggestions.add(of(value));
            }
            return List.copyOf(suggestions);
        };
    }
}
