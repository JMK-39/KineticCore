package dev.xyat.kineticcore.api.client.gui.widget;

import dev.xyat.kineticcore.api.client.search.KineticSuggestion;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

/** 带候选弹窗的文本输入框 / Text input with a suggestion popup. */
public interface KineticAutoCompleteField extends KineticTextField {
    /** 用户选中候选项时的回调 / Callback when the user picks a suggestion. */
    void setSelectionResponder(Consumer<String> responder);

    /** 自定义候选匹配规则 / Custom suggestion matcher. */
    void setSuggestionMatcher(BiPredicate<KineticSuggestion, String> matcher);

    /** 弹窗最多可见行数 / Maximum visible suggestion rows. */
    void setMaxVisibleSuggestions(int maxVisibleSuggestions);

    /** 弹窗最大宽度 / Maximum popup width. */
    void setSuggestionPopupMaxWidth(int maxWidth);

    /** 弹窗是否打开 / Whether the popup is open. */
    boolean isSuggestionPopupOpen();
}
