package dev.xyat.kineticcore.api.client.widget.input;

import dev.xyat.kineticcore.api.client.widget.KineticWidgets.FactoryAccess;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.internal.client.input.NumericInputRules;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.GridScrollController;

/**
 * Autocomplete controls returned by Kinetic screen and detached-widget factories.
 * Suggestions keep persisted {@code value} separate from display-only {@code translation}.
 */
public final class KineticAutoComplete {
    private KineticAutoComplete() {}

    /**
     * One autocomplete candidate. {@code value} is the only text written into the input;
     * {@code translation} is display-only and is hidden while the game language is English.
     */
    public record Suggestion(String value, Component translation) {
        /** Normalizes nullable suggestion value and translation metadata. */
        public Suggestion {
            value = value == null ? "" : value;
            translation = translation == null ? Component.empty() : translation;
        }
    }

    /** Adapts a legacy/raw string dictionary to value-only suggestions with no display translation. */
    public static Supplier<List<Suggestion>> stringDictionary(Supplier<? extends List<String>> dictionarySupplier) {
        return () -> {
            List<String> values = dictionarySupplier == null ? null : dictionarySupplier.get();
            if (values == null || values.isEmpty()) return List.of();
            return values.stream().map(value -> new Suggestion(value, Component.empty())).toList();
        };
    }

    /** Standard Kinetic text field with API-managed suggestion expansion. */
    public static class AutoCompleteBox extends KineticEditBox {
        private final Supplier<List<Suggestion>> dictionarySupplier;
        private List<Suggestion> suggestions = new ArrayList<>();
        private Consumer<String> externalResponder;
        private Consumer<String> selectionResponder;
        private BiPredicate<Suggestion, String> suggestionMatcher;

        private final GridScrollController suggestionScroll =
                new GridScrollController();

        private static final int DEFAULT_MAX_VISIBLE = 8;

        private int selectedIndex = -1;
        private int maxSuggestionWidth = 0;
        private int maxVisibleSuggestions = DEFAULT_MAX_VISIBLE;
        private int maxSuggestionPopupWidth = Integer.MAX_VALUE;

        /** Factory-only constructor; obtain instances from {@code KineticWidgets}. */
        public AutoCompleteBox(FactoryAccess access, Font font, int x, int y, int width, int height, Component message, Supplier<List<Suggestion>> dictionarySupplier) {
            super(access, font, x, y, width, height, message);
            this.dictionarySupplier = dictionarySupplier;
            this.setMaxLength(1024);
            this.setBordered(true);

            super.setResponder(val -> {
                this.updateSuggestions(val);
                if (this.externalResponder != null) this.externalResponder.accept(val);
            });
        }

        @Override
        public void setResponder(@NotNull Consumer<String> responder) {
            this.externalResponder = responder;
        }

        /** Sets the callback invoked only when a suggestion is explicitly selected; it receives the raw suggestion value. */
        public void setSelectionResponder(Consumer<String> responder) {
            this.selectionResponder = responder;
        }

        /**
         * Sets an optional suggestion matcher for callers that need domain-specific search semantics.
         * The matcher receives the suggestion and raw input text; null restores the standard value/translation contains match.
         */
        public void setSuggestionMatcher(BiPredicate<Suggestion, String> matcher) {
            BiPredicate<Suggestion, String> previous = this.suggestionMatcher;
            this.suggestionMatcher = matcher;
            try {
                updateSuggestions(this.getValue());
            } catch (RuntimeException | Error failure) {
                // A caller's matcher may throw mid-refresh. Preserve the previously
                // working matcher rather than poisoning every subsequent keypress.
                this.suggestionMatcher = previous;
                throw failure;
            }
        }

        /** Sets the maximum number of suggestion rows visible at once; values below one are clamped to one. */
        public void setMaxVisibleSuggestions(int maxVisibleSuggestions) {
            this.maxVisibleSuggestions = Math.max(1, maxVisibleSuggestions);
            suggestionScroll.update(suggestions.size(), this.maxVisibleSuggestions);
        }

        /** Limits popup width while keeping the API scrollbar beside the list rather than off screen. */
        public void setSuggestionPopupMaxWidth(int maxWidth) {
            this.maxSuggestionPopupWidth = Math.max(this.width, maxWidth);
            this.maxSuggestionWidth = Math.min(this.maxSuggestionWidth, this.maxSuggestionPopupWidth);
        }

        /** Returns whether the focused field currently has an open suggestion popup. */
        public boolean isSuggestionPopupOpen() {
            return visible && active && isFocused() && !suggestions.isEmpty();
        }

        /** Returns whether the supplied UI-space point is inside the open suggestion popup. */
        public boolean isSuggestionPopupHovered(double mouseX, double mouseY) {
            if (!isSuggestionPopupOpen()) return false;
            int x = this.getX() - 4;
            int y = this.getY() + this.getHeight() + 4;
            int listH = Math.min(suggestions.size(), maxVisibleSuggestions) * 12;
            int w = this.maxSuggestionWidth + (suggestions.size() > maxVisibleSuggestions ? 10 : 0);
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + listH;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY) || isSuggestionPopupHovered(mouseX, mouseY);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused) updateSuggestions(this.getValue());
            else clearSuggestions();
        }

        /** Clears suggestions. */
        public void clearSuggestions() {
            this.suggestions.clear();
            this.suggestionScroll.reset();
            this.selectedIndex = -1;
            this.maxSuggestionWidth = 0;
        }

        /** Loads suggestions. */
        public void loadSuggestions() {
            updateSuggestions(this.getValue());
        }

        private void updateSuggestions(String input) {
            List<Suggestion> supplied = dictionarySupplier == null ? List.of() : dictionarySupplier.get();
            List<Suggestion> allItems = supplied == null
                    ? List.of()
                    : supplied.stream()
                    .filter(item -> item != null && !item.value().isBlank())
                    .toList();

            boolean showTranslations = shouldShowTranslations();
            List<Suggestion> refreshed;
            if (input == null || input.isEmpty()) {
                refreshed = new ArrayList<>(allItems);
            } else if (suggestionMatcher != null) {
                refreshed = allItems.stream()
                        .filter(item -> suggestionMatcher.test(item, input))
                        .collect(Collectors.toList());
            } else {
                String lower = input.toLowerCase(Locale.ROOT);
                refreshed = allItems.stream()
                        .filter(item -> item.value().toLowerCase(Locale.ROOT).contains(lower)
                                || (showTranslations
                                && !item.translation().getString().isBlank()
                                && item.translation().getString().toLowerCase(Locale.ROOT).contains(lower)))
                        .collect(Collectors.toList());
            }

            Font font = KineticClientRuntime.font();
            int currentMax = this.width;
            for (Suggestion suggestion : refreshed) {
                int w = font.width(suggestion.value()) + 10;
                if (showTranslations && !suggestion.translation().getString().isBlank()) {
                    w += font.width("  ") + font.width(suggestion.translation());
                }
                if (w > currentMax) currentMax = w;
            }
            // Do not publish partially refreshed suggestions if an addon matcher,
            // translator or font-width lookup fails midway through this refresh.
            suggestionScroll.reset();
            suggestionScroll.update(
                    refreshed.size(),
                    maxVisibleSuggestions
            );
            this.suggestions = refreshed;
            this.maxSuggestionWidth = Math.min(currentMax, maxSuggestionPopupWidth);
            selectedIndex = -1;
        }

        /** Scrolls the open suggestion list when this field is focused and suggestions are visible. */
        public boolean handleMouseScrolled(double delta) {
            if (!isSuggestionPopupOpen()) {
                return false;
            }

            suggestionScroll.update(
                    suggestions.size(),
                    maxVisibleSuggestions
            );

            return suggestionScroll.scroll(delta, 1.0D);
        }

        /** Handles suggestion navigation/selection keys before delegating ordinary editing keys to the text field. */
        public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
            if (!visible || !active || !isFocused()) return false;
            if (!suggestions.isEmpty()) {
                if (keyCode == 265) {
                    selectedIndex = (selectedIndex <= 0) ? suggestions.size() - 1 : selectedIndex - 1;
                    ensureVisible(); return true;
                }
                if (keyCode == 264) {
                    selectedIndex = (selectedIndex >= suggestions.size() - 1) ? 0 : selectedIndex + 1;
                    ensureVisible(); return true;
                }
                if (keyCode == 256) {
                    clearSuggestions();
                    return true;
                }
                if (keyCode == 258) {
                    selectItem(selectedIndex >= 0 && selectedIndex < suggestions.size() ? selectedIndex : 0);
                    return true;
                }
                if (keyCode == 257 || keyCode == 335) {
                    if (selectedIndex >= 0 && selectedIndex < suggestions.size()) {
                        selectItem(selectedIndex); return true;
                    }
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private void ensureVisible() {
            suggestionScroll.update(
                    suggestions.size(),
                    maxVisibleSuggestions
            );

            if (selectedIndex < suggestionScroll.offset()) {
                suggestionScroll.setOffset(selectedIndex);
            }

            if (selectedIndex
                    >= suggestionScroll.offset() + maxVisibleSuggestions) {
                suggestionScroll.setOffset(
                        selectedIndex - maxVisibleSuggestions + 1
                );
            }
        }

        private void selectItem(int index) {
            String val = suggestions.get(index).value();
            this.setValue(val);
            this.setCursorPosition(this.getValue().length());
            this.clearSuggestions();
            if (this.selectionResponder != null) {
                this.selectionResponder.accept(val);
            }
        }

        /** Renders the open suggestion list with API-managed zebra rows, selection state, translation text, and scrollbar. */
        public void renderSuggestions(GuiGraphics gui, int mouseX, int mouseY) {
            if (!isSuggestionPopupOpen()) return;

            int x = this.getX() - 4;
            int y = this.getY() + this.getHeight() + 4;
            int itemH = 12;
            int w = this.maxSuggestionWidth;
            int visibleCount = Math.min(suggestions.size(), maxVisibleSuggestions);
            int totalH = visibleCount * itemH;

            suggestionScroll.update(
                    suggestions.size(),
                    maxVisibleSuggestions
            );

            int firstIndex = suggestionScroll.smoothIndexOffset();
            int visualShift = suggestionScroll.visualShift(itemH);
            int rowsToRender = Math.min(
                    visibleCount + (visualShift > 0 ? 1 : 0),
                    suggestions.size() - firstIndex
            );

            gui.pose().pushPose();
            try {
                gui.pose().translate(0, 0, 600);
                GuiTheme.Palette theme = GuiTheme.current();
                boolean showTranslations = shouldShowTranslations();
                gui.fill(x, y, x + w, y + totalH, theme.background());
                gui.renderOutline(x, y, w, totalH, theme.border());
                KineticRenderRuntime.enableScissor(gui, x, y, x + w, y + totalH);
                try {
                    for (int i = 0; i < rowsToRender; i++) {
                        int index = firstIndex + i;
                        int top = y + (i * itemH) - visualShift;

                        int rowBackground = (index & 1) == 0
                                ? theme.panelAlt()
                                : theme.panel();
                        gui.fill(x + 1, top, x + w - 1, top + itemH, rowBackground);

                        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY < y + totalH
                                && mouseY >= top && mouseY < top + itemH;
                        boolean selected = index == selectedIndex;
                        if (hovered || selected) {
                            gui.fill(x + 1, top, x + w - 1, top + itemH, theme.panel());
                            GuiTheme.stateOutline(gui, x + 1, top, w - 2, itemH, selected, hovered, false);
                        } else {
                            GuiTheme.indicatorOutline(gui, x + 1, top, w - 2, itemH, GuiTheme.Indicator.MUTED);
                        }

                        Suggestion suggestion = suggestions.get(index);
                        Font font = KineticClientRuntime.font();
                        gui.drawString(font, styledSuggestion(suggestion.value()), x + 4, top + 2, theme.text(), false);
                        if (showTranslations && !suggestion.translation().getString().isBlank()) {
                            int detailX = x + 4 + font.width(suggestion.value()) + font.width("  ");
                            gui.drawString(font, suggestion.translation(), detailX, top + 2, theme.translatedText(), false);
                        }
                    }

                } finally {
                    KineticRenderRuntime.disableScissor(gui);
                }
                suggestionScroll.render(
                        gui,
                        mouseX,
                        mouseY,
                        x + w + 2,
                        y,
                        4,
                        totalH,
                        10
                );
            } finally {
                gui.pose().popPose();
            }
        }

        /** Resolves the suggestion color from the current language instead of hardcoding it in Java. */
        private static Component styledSuggestion(String value) {
            String configured = KineticText.get("gui.kineticcore.autocomplete.suggestion_color");
            ChatFormatting formatting = configured.isEmpty() ? null
                    : ChatFormatting.getByCode(configured.charAt(configured.length() - 1));
            Component base = Component.literal(value);
            return formatting != null && formatting.isColor() ? base.copy().withStyle(formatting) : base;
        }

        /** Handles scrollbar interaction or suggestion selection inside the open suggestion popup. */
        public boolean handleMouseClick(double mouseX, double mouseY) {
            if (!isSuggestionPopupOpen()) return false;
            int x = this.getX() - 4;
            int y = this.getY() + this.getHeight() + 4;
            int itemH = 12;
            int w = this.maxSuggestionWidth;
            int totalH = Math.min(suggestions.size(), maxVisibleSuggestions) * itemH;

            suggestionScroll.update(
                    suggestions.size(),
                    maxVisibleSuggestions
            );

            if (suggestionScroll.beginDrag(
                    mouseX,
                    mouseY,
                    x + w + 2,
                    y,
                    4,
                    totalH,
                    10,
                    2
            )) {
                return true;
            }
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY < y + totalH) {
                int localY = (int) Math.floor(mouseY - y + suggestionScroll.visualShift(itemH));
                int clickedIdx = suggestionScroll.smoothIndexOffset() + localY / itemH;
                if (clickedIdx >= 0 && clickedIdx < suggestions.size()) {
                    selectItem(clickedIdx);
                    return true;
                }
            }
            return false;
        }

        /** Handles suggestion-scroll dragging using explicit mouse coordinates. */
        public boolean handleMouseDragged(
                double mouseX,
                double mouseY
        ) {
            if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY) || !isSuggestionPopupOpen()) {
                return false;
            }

            return suggestionScroll.drag(
                    mouseY,
                    this.getY() + this.getHeight() + 4,
                    Math.min(suggestions.size(), maxVisibleSuggestions) * 12,
                    10
            );
        }

        /** Ends an active suggestion-scroll drag for the supplied mouse button. */
        public boolean handleMouseReleased(int button) {
            return suggestionScroll.release(button);
        }

        private static boolean shouldShowTranslations() {
            return !KineticClientRuntime.isEnglishLanguage();
        }
    }

    /** Routes rendering and input for a group of detached auto-complete fields. */
    public static class AutoCompleteBoxGroup {
        private final List<AutoCompleteBox> boxes =
                new ArrayList<>();

        /** Replaces every autocomplete box routed by this group. */
        public void setBoxes(AutoCompleteBox... inputs) {
            boxes.clear();

            if (inputs == null) {
                return;
            }

            for (AutoCompleteBox input : inputs) {
                if (input != null) {
                    // A detached control can be supplied more than once by a panel
                    // rebuild. Keep just its latest position in the z-order so it
                    // is drawn and blurred exactly once.
                    boxes.removeIf(registered -> registered == input);
                    boxes.add(input);
                }
            }
        }

        /** Returns whether any managed autocomplete field currently has an open suggestion popup. */
        public boolean hasOpenPopup() {
            for (AutoCompleteBox box : boxes) {
                if (box.isSuggestionPopupOpen()) {
                    return true;
                }
            }
            return false;
        }

        /** Routes wheel scrolling to the first detached autocomplete popup that consumes it. */
        public boolean handleMouseScrolled(double delta) {
            for (int index = boxes.size() - 1; index >= 0; index--) {
                AutoCompleteBox box = boxes.get(index);
                if (box.isSuggestionPopupOpen()) {
                    box.handleMouseScrolled(delta);
                    return true;
                }
            }

            return false;
        }

        /** Routes wheel scrolling only when the pointer is inside an open managed suggestion popup. */
        public boolean handleHoveredMouseScrolled(double mouseX, double mouseY, double delta) {
            for (int index = boxes.size() - 1; index >= 0; index--) {
                AutoCompleteBox box = boxes.get(index);
                if (box.isSuggestionPopupHovered(mouseX, mouseY)) {
                    // The upper popup owns its region even when its scrollbar is already at the limit.
                    box.handleMouseScrolled(delta);
                    return true;
                }
            }
            return false;
        }

        /** Returns whether any open suggestion popup owns this point, including its scrollbar. */
        public boolean isAnySuggestionPopupHovered(double mouseX, double mouseY) {
            for (int index = boxes.size() - 1; index >= 0; index--) {
                if (boxes.get(index).isSuggestionPopupHovered(mouseX, mouseY)) return true;
            }
            return false;
        }

        /** Routes one mouse click to the first detached autocomplete popup that consumes it. */
        public boolean handleSuggestionClick(
                double mouseX,
                double mouseY
        ) {
            for (int index = boxes.size() - 1; index >= 0; index--) {
                AutoCompleteBox box = boxes.get(index);
                if (box.isSuggestionPopupHovered(mouseX, mouseY)) {
                    // Empty padding and scrollbar gaps must not dispatch clicks to a hidden popup.
                    box.handleMouseClick(mouseX, mouseY);
                    return true;
                }
            }

            return false;
        }

        /** Routes suggestion-scroll dragging to the first detached autocomplete popup that consumes it. */
        public boolean handleMouseDragged(
                double mouseX,
                double mouseY
        ) {
            for (int index = boxes.size() - 1; index >= 0; index--) {
                AutoCompleteBox box = boxes.get(index);
                if (box.handleMouseDragged(
                        mouseX,
                        mouseY
                )) {
                    return true;
                }
            }

            return false;
        }

        /** Routes mouse release to managed suggestion scrollbars and reports whether one consumed it. */
        public boolean handleMouseReleased(int button) {
            for (int index = boxes.size() - 1; index >= 0; index--) {
                AutoCompleteBox box = boxes.get(index);
                if (box.handleMouseReleased(button)) {
                    return true;
                }
            }

            return false;
        }

        /** Routes one key press to the first detached autocomplete box that consumes it. */
        public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
            for (int index = boxes.size() - 1; index >= 0; index--) {
                AutoCompleteBox box = boxes.get(index);
                if (box.handleKeyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }

            return false;
        }

        /** Clears focus from each managed autocomplete box that does not contain the supplied pointer position. */
        public void clearFocusOutside(
                double mouseX,
                double mouseY
        ) {
            for (AutoCompleteBox box : boxes) {
                if (!box.isMouseOver(
                        mouseX,
                        mouseY
                )) {
                    box.setFocused(false);
                }
            }
        }

        /** Renders suggestion popups for every managed detached autocomplete box. */
        public void renderSuggestions(
                GuiGraphics graphics,
                int mouseX,
                int mouseY
        ) {
            for (AutoCompleteBox box : boxes) {
                box.renderSuggestions(
                        graphics,
                        mouseX,
                        mouseY
                );
            }
        }
    }

    /** Standard Kinetic numeric field with API-managed suggestions. */
    public static class NumericAutoCompleteBox extends AutoCompleteBox {
        private final KineticNumericFields.Type type;
        private final boolean allowNegative;
        private final Number minValue;
        private final Number maxValue;
        private final Predicate<Number> validator;

        /** Factory-only constructor; obtain instances from {@code KineticWidgets}. */
        public NumericAutoCompleteBox(
                FactoryAccess access,
                Font font,
                int x,
                int y,
                int width,
                int height,
                Component message,
                Supplier<List<Suggestion>> dictionarySupplier,
                KineticNumericFields.Type type,
                boolean allowNegative,
                Number minValue,
                Number maxValue,
                Predicate<Number> validator
        ) {
            super(access, font, x, y, width, height, message, dictionarySupplier);
            this.type = type;
            this.allowNegative = allowNegative;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.validator = validator == null ? ignored -> true : validator;
            setFilter(this::isAllowedText);
        }

        /** Returns the validated integer value, or {@code null} while the text is incomplete or invalid. */
        public Integer getIntValue() {
            return NumericInputRules.integerValue(getValue(), minValue, maxValue, validator);
        }

        /** Returns the validated long value, or {@code null} while the text is incomplete or invalid. */
        public Long getLongValue() {
            return NumericInputRules.longValue(getValue(), minValue, maxValue, validator);
        }

        /** Returns the validated decimal value, or {@code null} while the text is incomplete or invalid. */
        public Double getDoubleValue() {
            return NumericInputRules.doubleValue(getValue(), minValue, maxValue, validator);
        }

        /** Returns whether the current raw input satisfies this field's numeric validation rules. */
        public boolean isValueValid() {
            return switch (type) {
                case INTEGER -> getIntValue() != null;
                case LONG -> getLongValue() != null;
                case DECIMAL -> getDoubleValue() != null;
            };
        }

        /** Sets int value. */
        public void setIntValue(int value) {
            setValue(Integer.toString(value));
        }

        /** Sets long value. */
        public void setLongValue(long value) {
            setValue(Long.toString(value));
        }

        /** Sets a decimal value using Kinetic's stable non-scientific formatting when possible. */
        public void setDoubleValue(double value) {
            setValue(NumericInputRules.formatDouble(value));
        }

        private boolean isAllowedText(String value) {
            return NumericInputRules.isAllowedText(value, type, allowNegative);
        }
    }

}
